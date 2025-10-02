package net.blosson.lflagger.checks;

import net.blosson.lflagger.simulation.MovementSimulator;
import net.blosson.lflagger.simulation.SimulatedPlayer;
import net.blosson.lflagger.util.TpsTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StrafeCheck extends Check {

    private final Map<UUID, Integer> violationLevels = new HashMap<>();
    private final MovementSimulator simulator = new MovementSimulator();
    private final TpsTracker tpsTracker = TpsTracker.getInstance();

    public StrafeCheck() {
        super("Strafe", "Detects unnatural mid-air movement control.");
    }

    public void tick(PlayerEntity player) {
        if (player.getUuid().equals(MinecraftClient.getInstance().player.getUuid()) || player.getAbilities().allowFlying) {
            violationLevels.remove(player.getUuid());
            return;
        }

        // This check only applies to airborne players
        if (player.isOnGround()) {
            violationLevels.put(player.getUuid(), 0);
            return;
        }

        // Get TPS and Ping for compensation
        double serverTps = tpsTracker.getTps();
        int ping = tpsTracker.getPing();

        // Simulate the player's movement with no input to get a baseline for air friction decay.
        SimulatedPlayer simulatedPlayer = new SimulatedPlayer(player);
        simulator.tick(simulatedPlayer, 0.0f, 0.0f, serverTps, ping); // No input

        // Get the horizontal speed of the actual and predicted movement
        Vec3d actualVel = player.getVelocity();
        Vec3d predictedVel = simulatedPlayer.velocity;

        double actualHorizontalSpeed = new Vec3d(actualVel.x, 0, actualVel.z).length();
        double predictedHorizontalSpeed = new Vec3d(predictedVel.x, 0, predictedVel.z).length();

        // If the player is accelerating or maintaining speed horizontally in the air beyond what friction allows
        if (actualHorizontalSpeed > predictedHorizontalSpeed + 0.02) { // Small buffer for leniency
            int violations = violationLevels.getOrDefault(player.getUuid(), 0) + 1;
            violationLevels.put(player.getUuid(), violations);

            if (violations > 5) {
                flag(player); // Flag without certainty, as requested
            }
        } else {
            int violations = violationLevels.getOrDefault(player.getUuid(), 0);
            if (violations > 0) {
                violationLevels.put(player.getUuid(), violations - 1);
            }
        }
    }
}