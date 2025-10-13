package net.blosson.lflagger.manager;

import net.blosson.lflagger.data.PlayerData;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

public class UncertaintyManager {

    private static final UncertaintyManager INSTANCE = new UncertaintyManager();

    private int velocityAppliedTicks = 0;
    private int teleportedTicks = 0;

    private UncertaintyManager() {
        // Private constructor for singleton
    }

    public static UncertaintyManager getInstance() {
        return INSTANCE;
    }

    public void onVelocityPacket(ClientPlayerEntity player, EntityVelocityUpdateS2CPacket packet) {
        if (packet.getEntityId() == player.getId()) {
            velocityAppliedTicks = 10; // Be uncertain for the next 10 ticks
        }
    }

    public void onPositionLookPacket(PlayerPositionLookS2CPacket packet) {
        teleportedTicks = 10; // Be uncertain for the next 10 ticks
    }

    public void tick() {
        if (velocityAppliedTicks > 0) {
            velocityAppliedTicks--;
        }
        if (teleportedTicks > 0) {
            teleportedTicks--;
        }
    }

    public boolean isUncertain() {
        return velocityAppliedTicks > 0 || teleportedTicks > 0;
    }

    public double getTolerance(PlayerData data) {
        double tolerance = 0.003; // Base tolerance for client-side prediction noise

        // Add tolerance for network latency. Higher ping means the server's view of us is more outdated.
        // This factor should be tuned based on testing.
        double pingInSeconds = data.serverPing / 1000.0;
        tolerance += pingInSeconds * 0.5; // e.g., 200ms ping adds 0.1 tolerance

        // Add tolerance for server TPS drops. Lower TPS means more movement happens between ticks.
        float tpsDrop = 20.0f - data.serverTps;
        if (tpsDrop > 0) {
            tolerance += tpsDrop * 0.05; // e.g., 5 TPS drop adds 0.25 tolerance
        }

        // Add a larger, flat tolerance for major physics events
        if (isUncertain()) {
            tolerance += 0.2;
        }

        return tolerance;
    }
}