package routing.decisionengine;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;
import routing.community.Duration;

public class ConHistoryDecisionEngine implements RoutingDecisionEngine {
    /**
     * identifier for the interconnectivity-mode setting ({@value})
     * 1 for connection up duration
     * 2 for interconnection duration
     * 3 for connection frequency
     * Outside of that will default to connection up duration
     */
    public static final String CONNECTION_HISTORY_MODE = "connectionHistoryMode";

    protected Map<DTNHost, Double> startTimestamps;
    protected Map<DTNHost, List<Duration>> connHistory;
    protected int mode;

    public ConHistoryDecisionEngine(Settings s) {
        if (!s.contains(CONNECTION_HISTORY_MODE))
            return;
        switch (s.getInt(CONNECTION_HISTORY_MODE)) {
            case 1:
                this.mode = 1;
                break;
            case 2:
                this.mode = 2;
                break;
            case 3:
                this.mode = 3;
                break;
            default:
                this.mode = 1;
                break;
        }

        // this.startTimestamps = new HashMap<DTNHost, Double>();
        // this.connHistory = new HashMap<DTNHost, List<Duration>>();
    }

    public ConHistoryDecisionEngine(ConHistoryDecisionEngine ch) {
        this.startTimestamps = new HashMap<DTNHost, Double>();
        this.connHistory = new HashMap<DTNHost, List<Duration>>();
    }

    /**
     * Starts timing the duration of this new connection and informs the community
     * detection object that a new connection was formed.
     * 
     * @see routing.RoutingDecisionEngine#doExchangeForNewConnection(core.Connection,
     *      core.DTNHost)
     */
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost myHost = con.getOtherNode(peer);
        ConHistoryDecisionEngine de = this.getOtherDecisionEngine(peer);

        this.startTimestamps.put(peer, SimClock.getTime());
        de.startTimestamps.put(myHost, SimClock.getTime());
    }

    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        // double time = startTimestamps.get(peer);
        double time = cek(thisHost, peer);
        double etime = SimClock.getTime();

        // Find or create the connection history list
        List<Duration> history;
        if (!connHistory.containsKey(peer)) {
            history = new LinkedList<Duration>();
            connHistory.put(peer, history);
        } else
            history = connHistory.get(peer);

        // add this connection to the list
        if (etime - time > 0)
            history.add(new Duration(time, etime));

        startTimestamps.remove(peer);
    }

    public double cek(DTNHost thisHost, DTNHost peer) {
        if (startTimestamps.containsKey(thisHost)) {
            startTimestamps.get(peer);
        }
        return 0;
    }

    public boolean newMessage(Message m) {
        return true; // Always keep and attempt to forward a created message
    }

    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo() == aHost; // Unicast Routing
    }

    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        return m.getTo() != thisHost;
    }

    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        if (m.getTo() == otherHost)
            return true; // trivial to deliver to final dest

        ConHistoryDecisionEngine de = getOtherDecisionEngine(otherHost);

        if (mode == 1) {
            if (de.getTotalDuration(m) > this.getTotalDuration(m))
                return true;
        } else if (mode == 2) {
            if (de.getTotalInter(m) < this.getTotalInter(m))
                return true;
        } else {
            if (de.getContactFrequency(m) > this.getContactFrequency(m))
                return true;
        }

        return false;
    }

    public double getTotalDuration(Message m) {
        double total = 0.0;

        if (connHistory.get(m.getTo()) == null)
            return total;

        Iterator<Duration> it = connHistory.get(m.getTo()).iterator();

        while (it.hasNext()) {
            Duration d = it.next();
            total += (d.end - d.start);
        }

        return total;
    }

    public double getTotalInter(Message m) {
        double total = 0.0;
        if (connHistory.get(m.getTo()) == null)
            return total;

        Iterator<Duration> it = connHistory.get(m.getTo()).listIterator();

        Duration before = it.next();
        Duration after;
        while (it.hasNext()) {
            after = it.next();
            total += (after.start - before.end);
            before = after;
        }
        return total;
    }

    public int getContactFrequency(Message m) {
        List<Duration> d = connHistory.get(m.getTo());
        return d == null ? 0 : d.size();
    }

    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        return false;
    }

    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        ConHistoryDecisionEngine de = this.getOtherDecisionEngine(hostReportingOld);

        return de.isFinalDest(m, null);
    }

    public RoutingDecisionEngine replicate() {
        return new ConHistoryDecisionEngine(this);
    }

    private ConHistoryDecisionEngine getOtherDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works " +
                " with other routers of same type";

        return (ConHistoryDecisionEngine) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    @Override
    public void update(DTNHost thisHost) {
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
    }
}
