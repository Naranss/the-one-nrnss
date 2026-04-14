package routing;

import java.util.Collection;

import core.Message;
import core.Settings;

public class EvicShortestRouter extends ActiveRouter {
    /**
     * Constructor. Creates a new message router based on the settings in
     * the given Settings object.
     * 
     * @param s The settings object
     */
    public EvicShortestRouter(Settings s) {
        super(s);
    }

    /**
     * Copy constructor.
     * 
     * @param r The router prototype where setting values are copied from
     */
    protected EvicShortestRouter(EvicShortestRouter r) {
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
    public EvicShortestRouter replicate() {
        return new EvicShortestRouter(this);
    }

    @Override
	protected boolean makeRoomForMessage(int size){
		if (size > this.getBufferSize()) {
			return false; // message too big for the buffer
		}
			
		int freeBuffer = this.getFreeBufferSize();
		/* delete messages from the buffer until there's enough space */
		while (freeBuffer < size) {
			Message m = getShortestTtlMessage(true); // don't remove msgs being sent

			if (m == null) {
				return false; // couldn't remove any more messages
			}			
			
			/* delete message from the buffer as "drop" */
			deleteMessage(m.getId(), true);
			freeBuffer += m.getSize();
		}
		
		return true;
	}

	/**
	 * Returns the oldest (by receive time) message in the message buffer 
	 * (that is not being sent if excludeMsgBeingSent is true).
	 * @param excludeMsgBeingSent If true, excludes message(s) that are
	 * being sent from the oldest message check (i.e. if oldest message is
	 * being sent, the second oldest message is returned)
	 * @return The oldest message or null if no message could be returned
	 * (no messages in buffer or all messages in buffer are being sent and
	 * exludeMsgBeingSent is true)
	 */
	protected Message getShortestTtlMessage(boolean excludeMsgBeingSent) {
		Collection<Message> messages = this.getMessageCollection();
		Message shortest = null;
		for (Message m : messages) {
			
			if (excludeMsgBeingSent && isSending(m.getId())) {
				continue; // skip the message(s) that router is sending
			}

			if (shortest == null ) {
				shortest = m;
			}
			else if (shortest.getReceiveTime() + shortest.getTtl() < m.getReceiveTime() + m.getTtl()) {
				shortest = m;
			}
		}
		
		return shortest;
	}
	
}