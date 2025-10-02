package net.blosson.lflagger.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks players who have recently received a DamageTiltS2CPacket.
 * This is necessary because the damage tilt animation is no longer a simple field
 * on the LivingEntity, but an event communicated via a packet.
 */
public class DamageTiltTracker {

    private static final DamageTiltTracker INSTANCE = new DamageTiltTracker();
    private final Map<Integer, Long> recentTilts = new ConcurrentHashMap<>();
    private static final int TILT_EXPIRATION_TICKS = 10; // A 10-tick window (0.5s) should be plenty

    private DamageTiltTracker() {}

    public static DamageTiltTracker getInstance() {
        return INSTANCE;
    }

    /**
     * Records that a player has just received a damage tilt packet.
     * @param entityId The entity ID of the player.
     * @param currentTick The current game tick.
     */
    public void recordTilt(int entityId, long currentTick) {
        recentTilts.put(entityId, currentTick);
    }

    /**
     * Checks if a player has had a damage tilt recorded within the expiration window.
     * @param entityId The entity ID of the player to check.
     * @param currentTick The current game tick.
     * @return True if a recent tilt was recorded, false otherwise.
     */
    public boolean hasRecentTilt(int entityId, long currentTick) {
        if (!recentTilts.containsKey(entityId)) {
            return false;
        }
        long lastTiltTick = recentTilts.get(entityId);
        return (currentTick - lastTiltTick) <= TILT_EXPIRATION_TICKS;
    }

    /**
     * Removes old entries from the map to prevent it from growing indefinitely.
     * This should be called periodically.
     * @param currentTick The current game tick.
     */
    public void pruneOldEntries(long currentTick) {
        recentTilts.entrySet().removeIf(entry -> (currentTick - entry.getValue()) > TILT_EXPIRATION_TICKS);
    }
}