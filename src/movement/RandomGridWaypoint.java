package movement;

import core.Coord;
import core.Settings;

// Movement class that randomly choose grid other than current grid as the next waypoint
public class RandomGridWaypoint extends MovementModel {
    /** how many waypoints should there be per path */
    private static final int PATH_LENGTH = 1;
    private Coord lastWaypoint;

    public static final String GRID_SIZE = "gridSize";
    public static final String HOME_GRID = "homeGrid";
    public static final String DEST_MONITOR = "destMonitor";

    private int homeX;
    private int homeY;
    protected int gridSizeX;
    protected int gridSizeY;

    protected int[] currentGrid;
    protected boolean destMonitor;

    public RandomGridWaypoint(Settings settings) {
        super(settings);
        settings.setNameSpace(MOVEMENT_MODEL_NS);

        // Setting for grid size
        int[] gridSize = settings.getCsvInts(GRID_SIZE);
        gridSizeX = gridSize[0];
        gridSizeY = gridSize[1];

        settings.restoreNameSpace();

        if (settings.contains(DEST_MONITOR)) {
            destMonitor = settings.getBoolean(DEST_MONITOR);
        } else {
            destMonitor = false;
        }

        // Setting for which grid the host belongs to
        if (settings.contains(HOME_GRID)) {
            int[] homeGrid = settings.getCsvInts(HOME_GRID);
            homeX = homeGrid[0];
            homeY = homeGrid[1];
        } else {
            homeX = rng.nextInt(gridSizeX);
            homeY = rng.nextInt(gridSizeY);
        }
    }

    protected RandomGridWaypoint(RandomGridWaypoint rgw) {
        super(rgw);
        this.gridSizeX = rgw.gridSizeX;
        this.gridSizeY = rgw.gridSizeY;
        this.homeX = rgw.homeX;
        this.homeY = rgw.homeY;
        this.destMonitor = rgw.destMonitor;
    }

    /**
     * Returns a possible (random) placement for a host
     * 
     * @return Random position on the map
     */
    @Override
    public Coord getInitialLocation() {
        assert rng != null : "MovementModel not initialized!";
        currentGrid = new int[] { homeX, homeY };
        Coord c = new Coord(rng.nextDouble() * getGridMaxX() + homeX * getGridMaxX(),
                rng.nextDouble() * getGridMaxY() + homeY * getGridMaxY());

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
    public RandomGridWaypoint replicate() {
        return new RandomGridWaypoint(this);
    }

    protected Coord randomCoord() {
        int[] randGrid = new int[2];
        do {
            randGrid[0] = rng.nextInt(gridSizeX);
            randGrid[1] = rng.nextInt(gridSizeY);
        } while (randGrid[0] == currentGrid[0] && randGrid[1] == currentGrid[1]);
        currentGrid = randGrid;
        if (destMonitor)
            System.out.println("going to grid " + currentGrid[0] + "," + currentGrid[1]);

        return new Coord(rng.nextDouble() * getGridMaxX() + currentGrid[0] * getGridMaxX(),
                rng.nextDouble() * getGridMaxY() + currentGrid[1] * getGridMaxY());
    }

    protected int getGridMaxX() {
        return getMaxX() / gridSizeX;
    }

    protected int getGridMaxY() {
        return getMaxY() / gridSizeY;
    }

}
