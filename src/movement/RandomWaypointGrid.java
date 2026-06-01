package movement;

import core.Coord;
import core.Settings;

public class RandomWaypointGrid extends MovementModel {
    /** how many waypoints should there be per path */
    private static final int PATH_LENGTH = 1;
    private Coord lastWaypoint;

    public static final String GRID_SIZE_X = "gridSizeX";
    public static final String GRID_SIZE_Y = "gridSizeY";

    public static final String GRID_X = "gridX";
    public static final String GRID_Y = "gridY";

    private int homeX;
    private int homeY;
    protected int gridSizeX;
    protected int gridSizeY;

    public RandomWaypointGrid(Settings settings) {
        super(settings);
        // Setting for grid size
        if (settings.contains(GRID_SIZE_X)) {
            gridSizeX = settings.getInt(GRID_SIZE_X);
        } else {
            gridSizeX = 3;
        }
        if (settings.contains(GRID_SIZE_Y)) {
            gridSizeY = settings.getInt(GRID_SIZE_Y);
        } else {
            gridSizeY = 3;
        }

        // Setting for which grid the host belongs to
        if (settings.contains(GRID_X)) {
            homeX = settings.getInt(GRID_X);
        } else {
            homeX = rng.nextInt(gridSizeX);
        }
        if (settings.contains(GRID_Y)) {
            homeY = settings.getInt(GRID_Y);
        } else {
            homeY = rng.nextInt(gridSizeY);
        }

    }

    protected RandomWaypointGrid(RandomWaypointGrid rwp) {
        super(rwp);
        this.gridSizeX = rwp.gridSizeX;
        this.gridSizeY = rwp.gridSizeY;
        this.homeX = rwp.homeX;
        this.homeY = rwp.homeY;
    }

    /**
     * Returns a possible (random) placement for a host
     * 
     * @return Random position on the map
     */
    @Override
    public Coord getInitialLocation() {
        assert rng != null : "MovementModel not initialized!";
        Coord c = randomCoord();

        this.lastWaypoint = c;
        return c;
    }

    @Override
    public Path getPath() {
        Path p;
        p = new Path(generateSpeed());
        p.addWaypoint(lastWaypoint.clone());
        Coord c = lastWaypoint;

        for (int i = 0; i < PATH_LENGTH; i++) {
            c = randomCoord();
            p.addWaypoint(c);
        }

        this.lastWaypoint = c;
        return p;
    }

    @Override
    public RandomWaypointGrid replicate() {
        return new RandomWaypointGrid(this);
    }

    protected Coord randomCoord() {
        return new Coord(rng.nextDouble() * getGridMaxX() + homeX * getGridMaxX(),
                rng.nextDouble() * getGridMaxY() + homeY * getGridMaxY());
    }

    protected int getGridMaxX() {
        return getMaxX() / gridSizeX;
    }

    protected int getGridMaxY() {
        return getMaxY() / gridSizeY;
    }
}
