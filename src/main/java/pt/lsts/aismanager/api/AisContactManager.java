package pt.lsts.aismanager.api;

import pt.lsts.aismanager.ShipAisSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.function.Function;

/**
 * Singleton class to maintain an histoy of AIS contacts
 * */
public class AisContactManager {
    /**
     * Singleton object
     * */
    protected static final AisContactManager manager = new AisContactManager();

    /**
     * Get or create and instance of AisContactManager
     * @return manager singleton instace of AisContactManager
     * */
    public static AisContactManager getInstance() {
        return manager;
    }

    private AisContactManager() {
        // nothing
    }

    /**
     * Contains the several contact's snapshots
     * throughout (and sorted by) time
     * Map from MNSI to Stack of snapshots
     */
    private final HashMap<Integer, Stack<ShipAisSnapshot>> snapshots = new HashMap<>();

    public void setShipPosition(int mmsi, double sog, double cog, double heading, double latRads, double lonRads,
                                long timestamp, String label) {
        ShipAisSnapshot shipSnapshot = new ShipAisSnapshot(mmsi, sog, cog, heading, latRads, lonRads, timestamp, label);

        synchronized (manager.snapshots) {
            Stack<ShipAisSnapshot> stack = manager.snapshots.get(mmsi);
            if (stack == null) {
                stack = new Stack<>();
                manager.snapshots.put(mmsi, stack);
            }

            stack.push(shipSnapshot);
        }
    }

    public HashMap<Integer, Stack<ShipAisSnapshot>> getAllSnapshots() {
        HashMap<Integer, Stack<ShipAisSnapshot>> clone;
        synchronized (manager.snapshots) {
            clone = (HashMap<Integer, Stack<ShipAisSnapshot>>) manager.snapshots.clone();
        }
        return clone;
    }

    /**
     * Get a list of all current AIS snapshots of known vehicles,
     * or the closest
     * @return List of ShipAisSnapshot
     * */
    public List<ShipAisSnapshot> getShips() {
        return manager.getShips(System.currentTimeMillis());
    }

    /**
     * Get a list of all AIS snapshots of known vehicles
     * closest to the given timestamp
     * @param timestampMs Timestamp in Milliseconds
     *
     * @return List of ShipAisSnapshot
     * */
    public List<ShipAisSnapshot> getShips(long timestampMs) {
        synchronized (manager.snapshots) {
            final List<ShipAisSnapshot> ships = new ArrayList<>(manager.snapshots.size());

            for (Map.Entry entry : manager.snapshots.entrySet()) {
                Optional<ShipAisSnapshot> ret = ((Stack<ShipAisSnapshot>) entry.getValue())
                        .stream()
                        .filter(s -> s.getTimestampMs() <= timestampMs)
                        .findFirst();

                ret.ifPresent(ships::add);
            }
            return ships;
        }
    }

    // https://stackoverflow.com/a/19561470/2183904
    private HashMap<String, ShipAisSnapshot> getFutureSnapshots(long offsetMs) {
        HashMap<String, ShipAisSnapshot> futureSnapshots = new HashMap<>();

        // get current snapshots
        List<ShipAisSnapshot> curr = getShips();

        for (ShipAisSnapshot ais : curr) {
            // earth radius
            final double R = 6371;
            double distance = ais.getSogMps() * (offsetMs / 1000);
            double bearing = Math.toDegrees(ais.getCog());

            double lat = Math.toDegrees(ais.getLatRads());
            double lon = Math.toDegrees(ais.getLonRads());

            double lat2 = Math.asin(Math.sin(Math.PI / 180 * lat) * Math.cos(distance / R) +
                    Math.cos(Math.PI / 180 * lat) * Math.sin(distance / R) * Math.cos(Math.PI / 180 * bearing));
            double lon2 = Math.PI / 180 * lon + Math.atan2(Math.sin( Math.PI / 180 * bearing) *
                    Math.sin(distance / R) * Math.cos( Math.PI / 180 * lat ), Math.cos(distance / R) - Math.sin( Math.PI / 180 * lat) * Math.sin(lat2));

            lat2 = Math.toRadians(180 / Math.PI * lat2);
            lon2 = Math.toRadians(180 / Math.PI * lon2);

            ShipAisSnapshot snap = new ShipAisSnapshot(ais.getMmsi(), ais.getSog(), ais.getCog(), ais.getHeading(),
                    lat2, lon2, ais.getTimestampMs() + offsetMs, ais.getLabel());

            futureSnapshots.put(ais.getLabel(), snap);
        }
        return futureSnapshots;
    }

    /**
     *  Save snapshots in a persistent manner, provided as argument. Thread-safe
     *  @param saveFunction The function that does the saving
     * */
    public boolean saveContacts(Function<HashMap<Integer, Stack<ShipAisSnapshot>>, Boolean> saveFunction) {
        synchronized (snapshots) {
            return saveFunction.apply(snapshots);
        }
    }

    /**
     * Load saved snapshots. Thread-safe
     * */
    public void loadContacts(HashMap<Integer, Stack<ShipAisSnapshot>> contacts) {
        synchronized (snapshots) {
            this.snapshots.putAll(contacts);
        }
    }

    public static void main(String[] args) {
        AisContactManager.getInstance().setShipPosition(1, 2, 2, 2, 0, 0, 1233, "A");
        AisContactManager.getInstance().setShipPosition(1, 2, 2, 2, 0, 0, 2000, "A");
        AisContactManager.getInstance().setShipPosition(1, 2, 2, 2, 0, 0, 2423, "A");
        AisContactManager.getInstance().setShipPosition(1, 2, 2, 2, 0, 0, 3023, "A");
        AisContactManager.getInstance().setShipPosition(1, 2, 2, 2, 0, 0, 3342, "A");
        AisContactManager.getInstance().setShipPosition(2, 2, 2, 2, 0, 0, 213, "B");
        AisContactManager.getInstance().setShipPosition(2, 2, 2, 2, 0, 0, 768, "B");
        AisContactManager.getInstance().setShipPosition(2, 2, 2, 2, 0, 0, 1234, "B");
        AisContactManager.getInstance().setShipPosition(2, 2, 2, 2, 0, 0, 1762, "B");
        AisContactManager.getInstance().setShipPosition(2, 2, 2, 2, 0, 0, 2423, "B");
        AisContactManager.getInstance().setShipPosition(2, 2, 2, 2, 0, 0, 2987, "B");
        AisContactManager.getInstance().setShipPosition(3, 2, 2, 2, 0, 0, 1256, "C");

        List<ShipAisSnapshot> res = AisContactManager.getInstance().getShips(2000);

        System.out.println(" ::: " + AisContactManager.getInstance().getAllSnapshots().size());
        System.out.println(" ::: " + res.size());
        res.stream().forEach(e -> System.out.println(" :::: " + e.getLabel() + " " + e.getTimestampMs()));

        if (res.get(0).getTimestampMs() != 1233 ||
                res.get(1).getTimestampMs() != 213 ||
                res.get(2).getTimestampMs() != 1256)
            throw new Error("Failed tests");
    }
}
