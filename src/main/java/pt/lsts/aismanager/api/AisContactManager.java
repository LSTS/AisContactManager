package pt.lsts.aismanager.api;

import pt.lsts.aismanager.ShipAisSnapshot;

import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final HashMap<Integer, Deque<ShipAisSnapshot>> snapshots = new HashMap<>();

    /***
     *
     * */
    public void setShipPosition(int mmsi, double sogKnots, double cog, double heading, double latRads, double lonRads,
                                long timestamp, String label) {
        ShipAisSnapshot shipSnapshot = new ShipAisSnapshot(mmsi, sogKnots, cog, heading, latRads, lonRads, timestamp, label);

        synchronized (manager.snapshots) {
            Deque<ShipAisSnapshot> stack = manager.snapshots.get(mmsi);
            if (stack == null) {
                stack = new ArrayDeque<>();
                manager.snapshots.put(mmsi, stack);
            }

            stack.push(shipSnapshot);
        }
    }

    public HashMap<Integer, Deque<ShipAisSnapshot>> getAllSnapshots() {
        HashMap<Integer, Deque<ShipAisSnapshot>> clone;
        synchronized (manager.snapshots) {
            clone = (HashMap<Integer, Deque<ShipAisSnapshot>>) manager.snapshots.clone();
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
                Deque<ShipAisSnapshot> stack = (Deque<ShipAisSnapshot>) entry.getValue();
                for (Iterator<ShipAisSnapshot> it1 = stack.iterator(); it1.hasNext(); ) {
                    ShipAisSnapshot it = it1.next();
                    if(it.getTimestampMs() > timestampMs)
                        continue;

                    ships.add(it);
                    break;
                }
            }
            return ships;
        }
    }

    /***
     * Get future snapshots from all the known systems
     */
    // https://stackoverflow.com/a/19561470/2183904
    public HashMap<String, ShipAisSnapshot> getFutureSnapshots(long offsetMs) {
        HashMap<String, ShipAisSnapshot> futureSnapshots = new HashMap<>();

        // get current snapshots
        List<ShipAisSnapshot> curr = getShips();

        for (ShipAisSnapshot ais : curr) {
            ShipAisSnapshot snap = getFutureSnapshot(ais.getMmsi(), offsetMs);
            futureSnapshots.put(ais.getLabel(), snap);
        }
        return futureSnapshots;
    }

    /***
     * Get multiple future snapshots from a given system
     */
    public List<ShipAisSnapshot> getFutureSnapshot(int mmsi, long... offsetsMs) {
        return Arrays.stream(offsetsMs).mapToObj(offset -> getFutureSnapshot(mmsi, offset))
                .collect(Collectors.toCollection(() -> new ArrayList<>(offsetsMs.length)));
    }

    /***
     * Get future snapshot from the given system
     */
    public ShipAisSnapshot getFutureSnapshot(int mmsi, long offsetMs) {
        final double R = 6371;
        ShipAisSnapshot ais = snapshots.get(mmsi).getFirst();
        double distance = ais.getSogMps() * (offsetMs / 1000) / 1000;
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

        return snap;
    }

    /**
     *  Save snapshots in a persistent manner, provided as argument. Thread-safe
     *  @param saveFunction The function that does the saving
     * */
    public boolean saveContacts(Function<HashMap<Integer, Deque<ShipAisSnapshot>>, Boolean> saveFunction) {
        synchronized (snapshots) {
            return saveFunction.apply(snapshots);
        }
    }

    public static void main(String[] args) {
        AisContactManager.getInstance().setShipPosition(1, 2, 2, 2, 0, 0, System.currentTimeMillis(), "A");
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

        if (res.get(0).getTimestampMs() != 2000 ||
                res.get(1).getTimestampMs() != 1762 ||
                res.get(2).getTimestampMs() != 1256)
            throw new Error("Failed tests");

//        AisContactManager.getInstance().setShipPosition(1, 3.88768898, Math.toRadians(30), Math.toRadians(30),
//                Math.toRadians(60), Math.toRadians(25), 10, "A");
//        ShipAisSnapshot future = AisContactManager.getInstance().getFutureSnapshots(1).get("A");
//
//        System.out.println(future.getLatRads() + " " + future.getLonRads());
    }

    /**
     * Load saved snapshots. Thread-safe
     * */
    public void loadContacts(HashMap<Integer, Deque<ShipAisSnapshot>> contacts) {
        synchronized (snapshots) {
            this.snapshots.putAll(contacts);
        }
    }
}
