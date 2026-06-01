package movement;

import core.Coord;
import core.Settings;

public class CommunityModel extends MovementModel {

	public static final String COMMUNITY_MODEL_NS = "CommunityModel";

    // Comma seperated value for grid size (x, y)
    public static final String GRID_SIZE = "gridSize";
    public static final String HOME_GRID = "homeGrid";
    public static final String GATHERING_GRID = "gatheringGrid";

    /** how many waypoints should there be per path */
    private static final int PATH_LENGTH = 1;
    private Coord lastWaypoint;

    // Host home grid coordinate
    protected int homeX;
    protected int homeY;

    // Community gathering point coordinate
    protected int gatheringX;
    protected int gatheringY;

    // Size of the grid
    protected int gridSizeX;
    protected int gridSizeY;

    // true = home, false = elsewhere or gathering
    protected boolean isHome = true;

    public CommunityModel(Settings settings) {
        super(settings);
        // Setting for grid size
        if (settings.contains(GRID_SIZE)) {
            int[] gridSize = settings.getCsvInts(GRID_SIZE);
            gridSizeX = gridSize[0];
            gridSizeY = gridSize[1];
        } else {
            gridSizeX = 4;
            gridSizeY = 3;
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

        // Settings for which grid is the gathering grid
        if (settings.contains(GATHERING_GRID)) {
            int[] gatheringGrid = settings.getCsvInts(GATHERING_GRID);
            gatheringX = gatheringGrid[0];
            gatheringY = gatheringGrid[1];
        } else {
            gatheringX = gridSizeX - 1;
            gatheringY = gridSizeY - 1;
        }

        
		settings.setNameSpace(COMMUNITY_MODEL_NS);

        if (settings.contains(HOME_GRID)) {
            int[] homeGrid = settings.getCsvInts(HOME_GRID);
            homeX = homeGrid[0];
            homeY = homeGrid[1];
        } else {
            homeX = rng.nextInt(gridSizeX);
            homeY = rng.nextInt(gridSizeY);
        }

		settings.restoreNameSpace();
    }

    public CommunityModel(CommunityModel cm) {
        super(cm);
        this.homeX = cm.homeX;
        this.homeY = cm.homeY;
        this.gridSizeX = cm.gridSizeX;
        this.gridSizeY = cm.gridSizeY;
        this.gatheringX = cm.gatheringX;
        this.gatheringY = cm.gatheringY;
    }

    @Override
    public Coord getInitialLocation() {
        assert rng != null : "MovementModel not initialized!";
        Coord c = new Coord(rng.nextDouble() * getGridMaxX() + homeX * getGridMaxX(),
                rng.nextDouble() * getGridMaxY() + homeY * getGridMaxY());

        this.lastWaypoint = c;
        return c;
    }

    @Override
    public Path getPath() {
        Path p;
        // p = new Path(1);
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
    public MovementModel replicate() {
        return new CommunityModel(this);
    }

    protected Coord randomCoord() {
        double randDest = rng.nextDouble();
        Coord dest;

        if (isHome) { // from home to gathering or elsewhere
            if (randDest < 0.8) { // to gathering 80% prob
                dest = new Coord(rng.nextDouble() * getGridMaxX() + gatheringX * getGridMaxX(),
                        rng.nextDouble() * getGridMaxY() + gatheringY * getGridMaxY());
            } else { // to elsewhere 20% prob
                int[] destCoord = randomElsewhere();
                dest = new Coord(rng.nextDouble() * getGridMaxX() + destCoord[0] * getGridMaxX(),
                        rng.nextDouble() * getGridMaxY() + destCoord[1] * getGridMaxY());
            }
            isHome = false;
        } else { // from gathering or elsewhere to home or elsewhere
            if (randDest < 0.9) { // to home 90% prob
                dest = new Coord(rng.nextDouble() * getGridMaxX() + homeX * getGridMaxX(),
                        rng.nextDouble() * getGridMaxY() + homeY * getGridMaxY());
                isHome = true;
            } else { // to elsewhere 10% prob
                int[] destCoord = randomElsewhere();
                dest = new Coord(rng.nextDouble() * getGridMaxX() + destCoord[0] * getGridMaxX(),
                        rng.nextDouble() * getGridMaxY() + destCoord[1] * getGridMaxY());
            }
        }

        return dest;
    }

    protected int[] randomElsewhere() {
        int[] randCoord = new int[2];
        do {
            randCoord[0] = rng.nextInt(gridSizeX);
            randCoord[1] = rng.nextInt(gridSizeY);
        } while (randCoord[0] == homeX && randCoord[1] == homeY
                || randCoord[0] == gatheringX && randCoord[1] == gatheringY);

        return randCoord;
    }

    protected int getGridMaxX() {
        return getMaxX() / gridSizeX;
    }

    protected int getGridMaxY() {
        return getMaxY() / gridSizeY;
    }
}