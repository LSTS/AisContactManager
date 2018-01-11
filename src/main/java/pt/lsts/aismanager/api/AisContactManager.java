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
    protected static AisContactManager manager;

    public static void getInstance() {

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

    public void setShipPosition(int mnsi, double sog, double cog, double heading, double latRads, double lonRads,
                                long timestamp, String label) {
        ShipAisSnapshot shipSnapshot = new ShipAisSnapshot(mnsi, sog, cog, heading, latRads, lonRads, timestamp, label);

        synchronized (manager.snapshots) {
            Stack<ShipAisSnapshot> stack = manager.snapshots.get(mnsi);
            if (stack == null) {
                stack = new Stack<>();
                manager.snapshots.put(mnsi, stack);
            }

            stack.push(shipSnapshot);
        }
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
}
