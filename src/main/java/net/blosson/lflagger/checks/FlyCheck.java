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

public class FlyCheck extends Check {

    private final Map<UUID, Integer> violationLevels = new HashMap<>();
    private final MovementSimulator simulator = new MovementSimulator();
    private final TpsTracker tpsTracker = TpsTracker.getInstance();

    public FlyCheck() {
        super("Fly", "Detects abnormal hovering and flying behaviors.");
    }

    public void tick(PlayerEntity player) {
        if (player.getUuid().equals(MinecraftClient.getInstance().player.getUuid()) || player.getAbilities().allowFlying) {
            violationLevels.remove(player.getUuid());
            return;
        }

        if (player.isOnGround() || player.isClimbing() || player.isSubmergedInWater()) {
            violationLevels.put(player.getUuid(), 0);
            return;
        }

        // Get TPS and Ping for compensation
        double serverTps = tpsTracker.getTps();
        int ping = tpsTracker.getPing();

        // Create a simulated player to predict movement
        SimulatedPlayer simulatedPlayer = new SimulatedPlayer(player);

        // Run a simulation tick with NO player input to isolate the effect of gravity,
        // and pass in the server TPS and ping for compensation.
        simulator.tick(simulatedPlayer, 0.0f, 0.0f, serverTps, ping);

        Vec3d actualVelocity = player.getVelocity();
        Vec3d predictedVelocity = simulatedPlayer.velocity;

        // Compare the actual vertical movement to the predicted vertical movement.
        double actualY = actualVelocity.y;
        double predictedY = predictedVelocity.y;

        // If the player is moving up or falling significantly slower than predicted by gravity.
        // A lenient buffer is added to prevent false positives.
        if (actualY > predictedY + 0.1) {
            int violations = violationLevels.getOrDefault(player.getUuid(), 0) + 1;
            violationLevels.put(player.getUuid(), violations);

            if (violations > 5) {
                if (predictedVelocity.lengthSquared() < 1E-6 || actualVelocity.lengthSquared() < 1E-6) {
                    flag(player, 100.0);
                    return;
                }

                // Calculate the angle between the predicted (gravity-affected) and actual velocity vectors.
                double dotProduct = predictedVelocity.dotProduct(actualVelocity);
                double angle = Math.toDegrees(Math.acos(dotProduct / (predictedVelocity.length() * actualVelocity.length())));

                double certainty = (angle / 90.0) * 100.0;
                flag(player, Math.min(100.0, certainty));
            }
        } else {
            int violations = violationLevels.getOrDefault(player.getUuid(), 0);
            if (violations > 0) {
                violationLevels.put(player.getUuid(), violations - 1);
            }
        }
    }
}