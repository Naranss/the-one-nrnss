package routing.decisionengine;

import java.util.List;
import java.util.Set;

import core.DTNHost;
import core.Message;

public interface EventsDetectionEngine{

    public List<Double> getKeys();

    public DTNHost getCurrent();

    public List<DTNHost> getDestinations();

    public List<Set<DTNHost>> getNeighbours();

    public List<Message> getMessages();

    public List<DTNHost> getForwarders();

    public List<Double> getCurrentPreds();

    public List<Double> getForwarderPreds();
}
