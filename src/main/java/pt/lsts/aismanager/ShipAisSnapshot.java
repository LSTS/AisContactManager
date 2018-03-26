package pt.lsts.aismanager;

/**
 * This class represents a snapshot in time of a certain Ais ship
 * */
public class ShipAisSnapshot {
    private final double KNOTS_TO_MPS = 0.514444444;

    private int mmsi;
    private double sog;
    private double cog;
    private double heading;
    private double latRads;
    private double lonRads;
    private long timestampMs;
    private String label;

    /**
     * Create a snapshot of ship's AIS information
     *
     * @param mmsi Maritime Mobile Service Identity
     * @param sog Speed over ground
     * @param cog Course over ground
     * @param heading Heading
     * @param latRads Latitude in radians
     * @param lonRads Longitude in radians
     * @param timestampMs Timestamp in Milliseconds
     * @param label AIS label
     * */
    public ShipAisSnapshot(int mmsi, double sog, double cog, double heading, double latRads, double lonRads,
                           long timestampMs, String label) {
        this.mmsi = mmsi;
        this.sog = sog;
        this.cog = cog;
        this.heading = heading;
        this.latRads = latRads;
        this.lonRads = lonRads;
        this.timestampMs = timestampMs;
        this.label = label;
    }

    /**
     * Ship's Maritime Mobile Service Identity
     * */
    public int getMmsi() {
        return mmsi;
    }

    /**
     * Ship's speed over ground in knots
     * */
    public double getSog() {
        return sog;
    }

    /**
     * Ship's speed over ground in meters per second
     * */
    public double getSogMps() {
        return sog * KNOTS_TO_MPS;
    }

    /**
     * Ship's course over ground
     * */
    public double getCog() {
        return cog;
    }

    /**
     * Ship's Heading
     * */
    public double getHeading() {
        return heading;
    }

    /**
     * Ship's latitude in radians
     * */
    public double getLatRads() {
        return latRads;
    }

    /**
     * Ship's longitude in radians
     * */
    public double getLonRads() {
        return lonRads;
    }

    public double getLatDegs() {
        return Math.toDegrees(latRads);
    }

    public double getLonDegs() {
        return Math.toDegrees(lonRads);
    }

    /**
     * This snaptshot's timestamp in Milliseconds
     * */
    public long getTimestampMs() {
        return timestampMs;
    }

    /**
     * AIS contact's label
     * */
    public String getLabel() {
        return label;
    }
}
