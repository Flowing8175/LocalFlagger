package net.blosson.lflagger.util;

import net.minecraft.network.packet.s2c.play.DamageTiltS2CPacket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A utility that tracks players who have recently received a {@link DamageTiltS2CPacket}.
 * <p>
 * This tracking is necessary because in modern Minecraft versions, the visual "damage tilt"
 * animation is communicated via a separate packet, rather than being directly tied to
 * {@code LivingEntity.hurtTime}. This class acts as a singleton listener for that packet,
 * providing a secondary piece of evidence for checks like {@code AntiKnockbackCheck} to
 * confirm that a hit was legitimate and not just a server-side health update.
 */
public class DamageTiltTracker {

    private static final DamageTiltTracker INSTANCE = new DamageTiltTracker();
    /** Stores the last tick a tilt was recorded for each entity ID. */
    private final Map<Integer, Long> recentTilts = new ConcurrentHashMap<>();
    /** The window (in ticks) for which a damage tilt is considered "recent". */
    private static final int TILT_EXPIRATION_TICKS = 10; // 0.5 seconds at 20 TPS

    private DamageTiltTracker() {}

    /**
     * @return The singleton instance of the DamageTiltTracker.
     */
    public static DamageTiltTracker getInstance() {
        return INSTANCE;
    }

    /**
     * Records that a player has just received a damage tilt packet. This should be called
     * from a mixin that intercepts the {@link DamageTiltS2CPacket}.
     *
     * @param entityId The entity ID of the player who received the tilt.
     * @param currentTick The current game tick at which the packet was received.
     */
    public void recordTilt(int entityId, long currentTick) {
        recentTilts.put(entityId, currentTick);
    }

    /**
     * Checks if a player has had a damage tilt recorded within the expiration window.
     *
     * @param entityId The entity ID of the player to check.
     * @param currentTick The current game tick to check against.
     * @return {@code true} if a recent tilt was recorded, {@code false} otherwise.
     */
    public boolean hasRecentTilt(int entityId, long currentTick) {
        if (!recentTilts.containsKey(entityId)) {
            return false;
        }
        long lastTiltTick = recentTilts.get(entityId);
        // Check if the last recorded tilt is within the valid time window.
        return (currentTick - lastTiltTick) <= TILT_EXPIRATION_TICKS;
    }

    /**
     * Removes old entries from the map to prevent it from growing indefinitely.
     * This maintenance method should be called periodically (e.g., once every few seconds)
     * to ensure the memory usage of the tracker remains low.
     *
     * @param currentTick The current game tick.
     */
    public void pruneOldEntries(long currentTick) {
        recentTilts.entrySet().removeIf(entry -> (currentTick - entry.getValue()) > TILT_EXPIRATION_TICKS);
    }
}