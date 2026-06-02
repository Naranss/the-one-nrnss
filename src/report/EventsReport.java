package report;

import java.util.List;
import java.util.Set;

import core.DTNHost;
import core.Message;
import core.SimScenario;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;
import routing.decisionengine.EventsDetectionEngine;

public class EventsReport extends Report {

    public EventsReport() {
        init();
    }

    @Override
    public void done() {
        List<DTNHost> nodes = SimScenario.getInstance().getHosts();

        write("id,keys,current_node,destination_node,message_id,selected_forwarder,predictability_of _the_current_node,predictability_of_the_selected_ forwarder,neighbours");

        for (DTNHost h : nodes) {
            MessageRouter r = h.getRouter();
            if (!(r instanceof DecisionEngineRouter)) {
                continue;
            }
            RoutingDecisionEngine de = ((DecisionEngineRouter) r).getDecisionEngine();
            if (!(de instanceof EventsDetectionEngine)) {
                continue;
            }
            EventsDetectionEngine ede = (EventsDetectionEngine) de;

            List<Double> keys = ede.getKeys();
            DTNHost crNode = ede.getCurrent();
            List<DTNHost> destNodes = ede.getDestinations();
            List<Set<DTNHost>> nbNodes = ede.getNeighbours();
            List<Message> msgs = ede.getMessages();
            List<DTNHost> fwNodes = ede.getForwarders();
            List<Double> crPreds = ede.getCurrentPreds();
            List<Double> fwPreds = ede.getForwarderPreds();

            for (int i = 0; i < keys.size(); i++) {
                String neigbours = "{";
                for (DTNHost host : nbNodes.get(i)) {
                    neigbours = neigbours + host.toString() + ";";
                }
                neigbours = neigbours + "}";

                write("Event" + i + "," + String.format("%.1f", keys.get(i)) + "," + crNode + "," + destNodes.get(i) + "," + msgs.get(i) + "," + fwNodes.get(i) + "," + crPreds.get(i) + "," + fwPreds.get(i) + "," + neigbours);
            }

        }

        super.done();
    }
}
