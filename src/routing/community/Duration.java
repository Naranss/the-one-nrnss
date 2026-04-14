package routing.community;

/**
 * A helper class for the community package that stores a start and end value
 * for some abstract duration. Generally, in this package, the duration being
 * stored is a time duration.
 *
 * @author PJ Dillon, University of Pittsburgh
 */
public class Duration {
	/**
	 * The start value
	 */
	public double start;

	/**
	 * The end value
	 */
	public double end;

	/**
	 * Standard constructor that assigns s to start and e to end.
	 *
	 * @param s Initial start value
	 * @param e Initial end value
	 */
	public Duration(double s, double e) {
		start = s;
		end = e;
	}

	/**
	 * A static factory method for creating Duration objects.
	 *
	 * @param s Initial start value
	 * @param e Initial end value
	 * @return A new Duration object with start s and end e
	 */
	public static Duration from(double s, double e) {
		return new Duration(s, e);
	}
}
