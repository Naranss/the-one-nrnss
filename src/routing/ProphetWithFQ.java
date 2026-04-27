package routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.Tuple;

public class ProphetWithFQ extends ProphetRouter {

    public static final String QUEUE_TYPE = "queueType";

    public static final String FORWARDING_STRATEGY = "forwardingStrategy";

    protected enum QueueType {
        FIFO, MOFO, MOPR, SHLI, LEPR
    }

    protected enum ForwardingStrategy {
        GRTR, GRTRSORT, GRTRMAX, COIN
    }

    protected QueueType queueType;
    protected ForwardingStrategy forwardingStrategy;

    public ProphetWithFQ(Settings s) {
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

        if (s.contains(FORWARDING_STRATEGY)) {
            try {
                forwardingStrategy = ForwardingStrategy.valueOf(s.getSetting(FORWARDING_STRATEGY).toUpperCase());
            } catch (IllegalArgumentException e) {
                forwardingStrategy = ForwardingStrategy.GRTRMAX;
            }
        } else {
            forwardingStrategy = ForwardingStrategy.GRTRMAX;
        }
    }

    protected ProphetWithFQ(ProphetWithFQ r) {
        super(r);
        this.queueType = r.queueType;
        this.forwardingStrategy = r.forwardingStrategy;
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
            case LEPR:
                if (this.getPredFor(m.getTo()) < this.getPredFor(crm.getTo()))
                    return m;
            case SHLI:
                if (m.getTtl() < crm.getTtl())
                    return m;
            default:
                if (m.getTtl() < crm.getTtl())
                    return m;
        }
        return crm;
    }

    /**
     * Tries to send all other messages to all connected hosts ordered by
     * their delivery probability
     * 
     * @return The return value of {@link #tryMessagesForConnected(List)}
     */
    @Override
    protected Tuple<Message, Connection> tryOtherMessages() {
        List<Tuple<Message, Connection>> messages = new ArrayList<Tuple<Message, Connection>>();

        Collection<Message> msgCollection = getMessageCollection();

        /*
         * for all connected hosts collect all messages that have a higher
         * probability of delivery by the other host
         */
        for (Connection con : getConnections()) {
            DTNHost other = con.getOtherNode(getHost());
            ProphetRouter othRouter = (ProphetRouter) other.getRouter();

            if (othRouter.isTransferring()) {
                continue; // skip hosts that are transferring
            }

            for (Message m : msgCollection) {
                if (othRouter.hasMessage(m.getId())) {
                    continue; // skip messages that the other one has
                }
                tryAllMessagesToAllConnections();
                if (othRouter.getPredFor(m.getTo()) > getPredFor(m.getTo())) {
                    // the other node has higher probability of delivery
                    messages.add(new Tuple<Message, Connection>(m, con));
                }
            }
        }

        if (messages.size() == 0) {
            return null;
        }
        // System.out.println(messages);
        // sort the message-connection tuples
        Collections.sort(messages, new TupleComparator());
        return tryMessagesForConnected(messages); // try to send messages
    }

    /**
     * Comparator for Message-Connection-Tuples that orders the tuples by
     * their delivery probability by the host on the other side of the
     * connection (GRTRMax)
     */
    private class TupleComparator implements Comparator<Tuple<Message, Connection>> {

        public int compare(Tuple<Message, Connection> tuple1,
                Tuple<Message, Connection> tuple2) {
            // P(B,D) - probability of tuple1's message reaching destination via other host
            double pB1 = ((ProphetRouter) tuple1.getValue().getOtherNode(getHost()).getRouter()).getPredFor(
                    tuple1.getKey().getTo());
            // P(A,D) - probability of tuple1's message reaching destination via current
            // host
            double pA1 = getPredFor(tuple1.getKey().getTo());

            // P(B,D) - probability of tuple2's message reaching destination via other host
            double pB2 = ((ProphetRouter) tuple2.getValue().getOtherNode(getHost()).getRouter()).getPredFor(
                    tuple2.getKey().getTo());
            // P(A,D) - probability of tuple2's message reaching destination via current
            // host
            double pA2 = getPredFor(tuple2.getKey().getTo());

            switch (forwardingStrategy) {
                case GRTR:
                    // Sort by P(B,D) descending (other host's probability)
                    if (pB2 - pB1 == 0) {
                        return compareByQueueMode(tuple1.getKey(), tuple2.getKey());
                    } else if (pB2 - pB1 < 0) {
                        return -1;
                    } else {
                        return 1;
                    }
                case GRTRSORT:
                    // Sort by P(B,D) - P(A,D) descending (difference in delivery probability)
                    double diff1 = pB1 - pA1;
                    double diff2 = pB2 - pA2;
                    if (diff2 - diff1 == 0) {
                        return compareByQueueMode(tuple1.getKey(), tuple2.getKey());
                    } else if (diff2 - diff1 < 0) {
                        return -1;
                    } else {
                        return 1;
                    }
                case COIN:
                    // Random ordering (delegate to queue mode)
                    return compareByQueueMode(tuple1.getKey(), tuple2.getKey());
                default: // default is GRTRMAX
                    // Sort by P(B,D) descending (other host's probability)
                    if (pB2 - pB1 == 0) {
                        return compareByQueueMode(tuple1.getKey(), tuple2.getKey());
                    } else if (pB2 - pB1 < 0) {
                        return -1;
                    } else {
                        return 1;
                    }
            }
        }
    }

    @Override
    public ProphetWithFQ replicate() {
        return new ProphetWithFQ(this);
    }
}
