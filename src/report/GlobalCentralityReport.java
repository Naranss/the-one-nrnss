package report;

import java.util.List;

import core.DTNHost;
import core.SimScenario;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;
import routing.community.GCentralityDetectionEngine;

public class GlobalCentralityReport extends Report {

    @Override
    protected void init() {
        // TODO Auto-generated method stub
        super.init();
    }

    @Override
    public void done() {
        List<DTNHost> nodes = SimScenario.getInstance().getHosts();

        for (DTNHost h : nodes) {
            MessageRouter r = h.getRouter();
            if (!(r instanceof DecisionEngineRouter)) {
                continue;
            }
            RoutingDecisionEngine de = ((DecisionEngineRouter) r).getDecisionEngine();
            if (!(de instanceof GCentralityDetectionEngine)) {
                continue;
            }
            GCentralityDetectionEngine gcd = (GCentralityDetectionEngine) de;
            int[] centralities = gcd.getGlobalCentralities();

            String print = "";
            for (double c : centralities) {
                print = print + c + ",";
            }

            write(print);
        }
        super.done();
    }
}
