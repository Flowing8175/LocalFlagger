package net.blosson.lflagger.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A client-side utility to track the server's Ticks Per Second (TPS).
 * <p>
 * This class operates as a singleton and works by measuring the time delta between
 * {@link WorldTimeUpdateS2CPacket}s, which are reliably sent by the server once per tick.
 * By averaging these deltas over a short period, it can produce a reasonably accurate
 * estimate of the server's health, which is crucial for lag compensation in physics simulations.
 */
public class TpsTracker {

    private static final TpsTracker INSTANCE = new TpsTracker();
    /** The number of recent tick deltas to store for averaging. */
    private static final int MAX_SAMPLES = 40; // ~2 seconds of data at 20 TPS

    private final Deque<Long> deltas = new ArrayDeque<>();
    private long lastPacketTime = 0;

    private TpsTracker() {}

    /**
     * @return The singleton instance of the TpsTracker.
     */
    public static TpsTracker getInstance() {
        return INSTANCE;
    }

    /**
     * Called when a time update packet is received from the server. This method should be
     * injected into the client's network handler. It records the time delta since the last packet.
     */
    public void onTimeUpdatePacket() {
        long currentTime = System.currentTimeMillis();
        if (lastPacketTime != 0) {
            long delta = currentTime - lastPacketTime;
            // Synchronize access to the deque to ensure thread safety.
            synchronized (deltas) {
                if (deltas.size() >= MAX_SAMPLES) {
                    deltas.poll(); // Remove the oldest sample to make room for the new one.
                }
                deltas.add(delta);
            }
        }
        lastPacketTime = currentTime;
    }

    /**
     * Calculates the server's average TPS over the last few seconds.
     *
     * @return The estimated server TPS, clamped between 0 and 20. Returns 20.0 if not enough data is available.
     */
    public double getTps() {
        synchronized (deltas) {
            // If we have too few samples, the result would be unreliable, so we assume a healthy server.
            if (deltas.size() < MAX_SAMPLES / 2) {
                return 20.0;
            }

            // Calculate the average time delta between packets in milliseconds.
            double averageDelta = deltas.stream()
                                        .mapToLong(Long::longValue)
                                        .average()
                                        .orElse(50.0); // Default to 50ms (20 TPS) if stream is empty.

            // TPS is 1000ms / average delta per tick.
            double tps = 1000.0 / averageDelta;

            // Clamp TPS to a maximum of 20, as the server cannot tick faster than this.
            return Math.min(20.0, tps);
        }
    }

    /**
     * Gets the current local player's ping (latency) from the player list.
     *
     * @return The player's latency in milliseconds, or 0 if it is unavailable.
     */
    public int getPing() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() != null && client.player != null) {
            var playerListEntry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
            if (playerListEntry != null) {
                return playerListEntry.getLatency();
            }
        }
        return 0;
    }
}