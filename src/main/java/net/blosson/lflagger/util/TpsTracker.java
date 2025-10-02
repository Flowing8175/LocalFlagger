package net.blosson.lflagger.util;

import net.minecraft.client.MinecraftClient;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A utility to track the server's Ticks Per Second (TPS) from the client-side.
 * It works by measuring the time between WorldTimeUpdateS2CPackets.
 */
public class TpsTracker {

    private static final TpsTracker INSTANCE = new TpsTracker();
    private static final int MAX_SAMPLES = 40; // ~2 seconds of data at 20 TPS
    private final Deque<Long> deltas = new ArrayDeque<>();
    private long lastPacketTime = 0;

    private TpsTracker() {}

    public static TpsTracker getInstance() {
        return INSTANCE;
    }

    /**
     * Called when a time update packet is received from the server.
     * Records the time delta since the last packet.
     */
    public void onTimeUpdatePacket() {
        long currentTime = System.currentTimeMillis();
        if (lastPacketTime != 0) {
            long delta = currentTime - lastPacketTime;
            synchronized (deltas) {
                if (deltas.size() >= MAX_SAMPLES) {
                    deltas.poll();
                }
                deltas.add(delta);
            }
        }
        lastPacketTime = currentTime;
    }

    /**
     * Calculates the server's average TPS over the last few seconds.
     * @return The estimated server TPS, or 20.0 if not enough data is available.
     */
    public double getTps() {
        synchronized (deltas) {
            if (deltas.size() < MAX_SAMPLES / 2) {
                return 20.0; // Assume 20 TPS if we don't have enough data
            }

            // Calculate the average time delta between packets
            double averageDelta = deltas.stream()
                                        .mapToLong(Long::longValue)
                                        .average()
                                        .orElse(50.0); // Default to 50ms (20 TPS)

            // TPS is 1000ms / average delta per tick
            double tps = 1000.0 / averageDelta;

            // Clamp TPS to a max of 20, as the server can't tick faster.
            return Math.min(20.0, tps);
        }
    }

    /**
     * Gets the current player's ping (latency).
     * @return The player's latency in milliseconds, or 0 if unavailable.
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