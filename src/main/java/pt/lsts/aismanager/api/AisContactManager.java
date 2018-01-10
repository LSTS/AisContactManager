package pt.lsts.aismanager.api;

import pt.lsts.aismanager.ShipAisSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;

public abstract class AisContactManager {
    /**
     *  Save snapshots in a persistent manner
     *  @im
     * */
    public abstract void saveContacts();

    /**
     * Load save snapshots
     * */
    public abstract void loadContacts();

    /**
     * Contains the several contact's snapshots
     * throughout (and sorted by) time
     * Map from MNSI to Stack of snapshots
     */
    private final HashMap<Integer, Stack<ShipAisSnapshot>> snapshots = new HashMap<>();

    public void setShipPosition(int mnsi, double sog, double cog, double heading, double latRads, double lonRads,
                                long timestamp, String label) {
        ShipAisSnapshot shipSnapshot = new ShipAisSnapshot(mnsi, sog, cog, heading, latRads, lonRads, timestamp, label);

        Stack<ShipAisSnapshot> stack = snapshots.get(mnsi);
        if (stack == null)
            stack = new Stack<>();

        stack.push(shipSnapshot);
    }

    /**
     * Get a list of all current AIS snapshots of known vehicles,
     * or the closest
     * @return List of ShipAisSnapshot
     * */
    public List<ShipAisSnapshot> getShips() {
        return getShips(System.currentTimeMillis());
    }

    /**
     * Get a list of all AIS snapshots of known vehicles
     * closest to the given timestamp
     * @param timestampMs Timestamp in Milliseconds
     *
     * @return List of ShipAisSnapshot
     * */
    public List<ShipAisSnapshot> getShips(long timestampMs) {
        final List<ShipAisSnapshot> ships = new ArrayList<>(snapshots.size());

        for (Map.Entry entry : snapshots.entrySet()) {
            Optional<ShipAisSnapshot> ret = ((Stack<ShipAisSnapshot>) entry.getValue())
                    .stream()
                    .filter(s -> s.getTimestampMs() <= timestampMs)
                    .findFirst();

            ret.ifPresent(ships::add);
        }

        return ships;
    }
}
