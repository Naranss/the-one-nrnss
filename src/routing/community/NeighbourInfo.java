package routing.community;
/**
 * A helper class for People Rank neighbour's information
 */
public class NeighbourInfo {
    public double rank;
    public int nrOfNeighbour;
    public int nrOfContact;

    public NeighbourInfo(double r, int nrOfN, int nrOfC) {
        this.rank = r;
        this.nrOfContact = nrOfC;
        this.nrOfNeighbour = nrOfN;
    }
}
