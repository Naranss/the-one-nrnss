package routing;

import java.util.Collection;

import core.Message;
import core.Settings;

public class EpidemicQueuePolicies extends ActiveRouter {

	/**
	 * identifier for the queue type setting ({@value})
	 * Outside of that will default to connection up duration
	 */
	public static final String QUEUE_TYPE = "queueType";

	protected enum QueueType {
		FIFO, MOFO, MOPR, SHLI
	}

	protected QueueType queueType;

	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * 
	 * @param s The settings object
	 */
	public EpidemicQueuePolicies(Settings s) {
		super(s);

		if (s.contains(QUEUE_TYPE)) {
			try {
				queueType = QueueType.valueOf(s.getSetting(QUEUE_TYPE).toUpperCase());
			} catch (IllegalArgumentException e) {
				queueType = QueueType.FIFO;
			}
		} else {
			queueType = QueueType.FIFO;
		}

	}

	/**
	 * Copy constructor.
	 * 
	 * @param r The router prototype where setting values are copied from
	 */
	protected EpidemicQueuePolicies(EpidemicQueuePolicies r) {
		super(r);
	}

	@Override
	public void update() {
		super.update();
		if (isTransferring() || !canStartTransfer()) {
			return; // transferring, don't try other connections yet
		}

		// Try first the messages that can be delivered to final recipient
		if (exchangeDeliverableMessages() != null) {
			return; // started a transfer, don't try others (yet)
		}

		// then try any/all message to any/all connection
		this.tryAllMessagesToAllConnections();
	}

	@Override
	public EpidemicQueuePolicies replicate() {
		return new EpidemicQueuePolicies(this);
	}

	@Override
	protected boolean makeRoomForMessage(int size) {
		if (size > this.getBufferSize()) {
			return false; // message too big for the buffer
		}

		int freeBuffer = this.getFreeBufferSize();
		/* delete messages from the buffer until there's enough space */
		while (freeBuffer < size) {
			Message m = getMessageByQueue(true); // don't remove msgs being sent

			if (m == null) {
				return false; // couldn't remove any more messages
			}

			/* delete message from the buffer as "drop" */
			deleteMessage(m.getId(), true);
			freeBuffer += m.getSize();
		}

		return true;
	}

	protected Message getMessageByQueue(boolean excludeMsgBeingSent) {
		Collection<Message> messages = this.getMessageCollection();
		Message crm = null;
		for (Message m : messages) {

			if (excludeMsgBeingSent && isSending(m.getId())) {
				continue; // skip the message(s) that router is sending
			}

			if (crm == null) {
				crm = m;
			} else {
				crm = getByQueue(m, crm);
			}
		}
		return crm;
	}

	protected Message getByQueue(Message m, Message crm) {
		switch (queueType) {
			case MOFO:
				if (m.getHopCount() < crm.getHopCount())
					return m;
			case MOPR:
				break;
			case SHLI:
				if (m.getTtl() < crm.getTtl())
					return m;
			default:
				if (m.getTtl() < crm.getTtl())
					return m;
		}
		return crm;
	}
}
