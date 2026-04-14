package routing.decisionengine;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;
import routing.community.NeighbourInfo;

public class PeopleRank implements RoutingDecisionEngine {
    /**
     * identifier for minimum frequency before other node added to friend lsit
     * setting ({@value})
     */
    public static final String FRIEND_MIN_FREQUENCY = "prFriendMinFrequency";
    /** identifier for people rank damping factor setting ({@value}) */
    public static final String PR_DAMPING_FACTOR = "prDampingFactor";
    /**
     * identifier for minimum connection to be regarded as friend setting
     * ({@value})
     */
    public static final String PR_CONTACT_DURATION = "prContactDuration";

    protected Map<DTNHost, Double> startTimestamps;
    protected Map<DTNHost, NeighbourInfo> nbInfo;
    protected Set<DTNHost> neighbours;

    protected int minFrequency;
    protected double dampingFactor;
    protected double contactDuration;

    protected double myRank;

    public PeopleRank(Settings s) {
        if (s.contains(FRIEND_MIN_FREQUENCY)) {
            minFrequency = s.getInt(FRIEND_MIN_FREQUENCY);
        } else {
            minFrequency = 5;
        }
        if (s.contains(PR_DAMPING_FACTOR)) {
            dampingFactor = s.getDouble(PR_DAMPING_FACTOR);
        } else {
            dampingFactor = 0.8;
        }
        if (s.contains(PR_CONTACT_DURATION)) {
            contactDuration = s.getDouble(PR_CONTACT_DURATION);
        } else {
            contactDuration = 0;
        }
        myRank = 0;
    }

    public PeopleRank(PeopleRank pr) {
        this.startTimestamps = new HashMap<DTNHost, Double>();
        this.nbInfo = new HashMap<DTNHost, NeighbourInfo>();
        this.neighbours = new HashSet<DTNHost>();

        this.minFrequency = pr.minFrequency;
        this.dampingFactor = pr.dampingFactor;
        this.contactDuration = pr.contactDuration;

        this.myRank = pr.myRank;
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        PeopleRank de = getOtherDecisionEngine(peer);

        // check other host is in friend range (nrofcontact >= minFrequency)
        if (!nbInfo.containsKey(peer))
            return;
        if (nbInfo.get(peer).nrOfContact < minFrequency)
            return;

        // get other host's rank and nrof neighbour
        NeighbourInfo ni = nbInfo.get(peer);
        ni.rank = de.getRank();
        ni.nrOfNeighbour = de.getNrOfNeighbour();

        // update it's own rank
        updateRank();

    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost myHost = con.getOtherNode(peer);
        PeopleRank de = this.getOtherDecisionEngine(peer);

        this.startTimestamps.put(peer, SimClock.getTime());
        de.startTimestamps.put(myHost, SimClock.getTime());

    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        // double time = startTimestamps.get(peer);
        double time = cek(thisHost, peer);
        double etime = SimClock.getTime();

        // create if not exist
        if (!nbInfo.containsKey(peer)) {
            nbInfo.put(peer, new NeighbourInfo(0, 0, 0));
        }
        if ((etime - time) >= contactDuration) {
            nbInfo.get(peer).nrOfContact++;
        }
        if (nbInfo.get(peer).nrOfContact >= minFrequency) {
            neighbours.add(peer);
        }
        startTimestamps.remove(peer);

    }

    // check starttime
    public double cek(DTNHost thisHost, DTNHost peer) {
        if (startTimestamps.containsKey(peer)) {
            return startTimestamps.get(peer);
        }
        return 0;
    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        if (m.getTo() == otherHost)
            return true; // trivial to deliver to final dest

        // get other host's rank and nrof neighbour
        NeighbourInfo ni = nbInfo.get(otherHost);

        // if other host rank greater send message
        if (ni == null) {
            return false;
        }
        if (ni.rank > myRank)
            return true;

        return false;
    }

    public void updateRank() {
        // neighbours rank per it's neighbours count summed
        double nrpnc = 0;
        int validNeighbours = 0;

        for (DTNHost h : nbInfo.keySet()) {
            NeighbourInfo nb = nbInfo.get(h);
            if (nb.nrOfContact < minFrequency)
                continue;
            // Only include neighbour if they also have at least 1 neighbour
            if (nb.nrOfNeighbour > 0) {
                nrpnc += nb.rank / nb.nrOfNeighbour;
                validNeighbours++;
            }
        }

        // Only update rank if this host has at least 1 valid neighbour
        if (validNeighbours > 0) {
            myRank = (1 - dampingFactor) + dampingFactor * nrpnc;
        } else {
            myRank = 0; // Reset rank if no valid neighbours
        }

        // for (DTNHost h : neighbours) {
        // NeighbourInfo nb = nbInfo.get(h);
        // nrpnc += nb.rank / nb.nrOfNeighbour;
        // }

        // myRank = (1 - dampingFactor) + dampingFactor * nrpnc;
    }

    public double getRank() {
        return myRank;
    }

    public int getNrOfNeighbour() {
        int totalNb = 0;
        for (NeighbourInfo nb : nbInfo.values()) {
            if (nb.nrOfContact < minFrequency) {
                continue;
            }
            totalNb++;
        }
        return totalNb;
    }

    // public int getNrOfNeighbour() {
    // return neighbours.size();
    // }

    @Override
    public boolean newMessage(Message m) {
        return true; // Always keep and attempt to forward a created message
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo() == aHost; // Unicast Routing
    }

    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        return m.getTo() != thisHost;
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        return false;
    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        PeopleRank de = this.getOtherDecisionEngine(hostReportingOld);

        return de.isFinalDest(m, null);
    }

    @Override
    public void update(DTNHost thisHost) {
    }

    @Override
    public RoutingDecisionEngine replicate() {
        return new PeopleRank(this);
    }

    private PeopleRank getOtherDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works " +
                " with other routers of same type";

        return (PeopleRank) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }
}
