package routing;

import core.DTNHost;
import core.Message;
import core.Settings;

public class EpidemicActiveReceipt extends EpidemicRouter {
    public static final String ACK_PREFIX = "ACK_";

    public static final int ACK_SIZE = 64;

    public EpidemicActiveReceipt(Settings s) {
        super(s);
    }

    public EpidemicActiveReceipt(EpidemicActiveReceipt r) {
        super(r);
    }

    @Override
    public int receiveMessage(Message m, DTNHost from) {
        // reject original message if already have ack
        if (this.hasMessage(ACK_PREFIX + m.getId())) {
            return DENIED_OLD; // Treat it as already received/delivered
        }

        return super.receiveMessage(m, from);
    }

    @Override
    public Message messageTransferred(String id, DTNHost from) {
        Message m = super.messageTransferred(id, from);

        if (m == null) {
            return m;
        }

        // If this host receives an ACK message, delete the original message from buffer
        if (m.getId().startsWith(ACK_PREFIX)) {
            String originalMessageId = m.getId().substring(ACK_PREFIX.length());
            if (this.hasMessage(originalMessageId)) {
                this.deleteMessage(originalMessageId, false);
            }
        }
        // If message arrived at final destination, create an ACK/cure mess
        else if (m.getTo() == getHost() && !m.getId().startsWith(ACK_PREFIX)) {
            // Create ACK message that will be actively spread
            Message ack = new Message(this.getHost(), from, ACK_PREFIX + m.getId(), ACK_SIZE);
            this.createNewMessage(ack);
        }

        return m;
    }

    @Override
    public EpidemicActiveReceipt replicate() {
        return new EpidemicActiveReceipt(this);
    }

}
