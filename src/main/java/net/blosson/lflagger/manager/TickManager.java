package net.blosson.lflagger.manager;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;

import java.util.Deque;
import java.util.LinkedList;

public class TickManager {

    private static final TickManager INSTANCE = new TickManager();
    private final Deque<Long> timePacketStamps = new LinkedList<>();
    private float currentTps = 20.0f;

    private TickManager() {
        // Private constructor for singleton
    }

    public static TickManager getInstance() {
        return INSTANCE;
    }

    public void onWorldTimeUpdate(WorldTimeUpdateS2CPacket packet) {
        long now = System.currentTimeMillis();
        timePacketStamps.add(now);

        // Keep the deque size reasonable
        if (timePacketStamps.size() > 20) {
            timePacketStamps.poll();
        }
        updateTps();
    }

    private void updateTps() {
        if (timePacketStamps.size() < 2) {
            return; // Not enough data
        }

        long totalTime = 0;
        int packetCount = 0;
        Long lastTime = null;

        for (Long time : timePacketStamps) {
            if (lastTime != null) {
                totalTime += (time - lastTime);
                packetCount++;
            }
            lastTime = time;
        }

        if (packetCount > 0) {
            long averageTimePerPacket = totalTime / packetCount;
            // Assuming the server sends time updates every 20 ticks (1 second)
            currentTps = 20.0f * (1000.0f / averageTimePerPacket);
            // Clamp TPS to a reasonable range
            currentTps = Math.max(0.0f, Math.min(20.0f, currentTps));
        }
    }

    public float getCurrentTps() {
        return currentTps;
    }
}