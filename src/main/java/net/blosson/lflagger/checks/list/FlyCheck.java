package net.blosson.lflagger.checks.list;

import net.blosson.lflagger.checks.Check;
import net.blosson.lflagger.config.ModConfig;
import net.blosson.lflagger.data.PlayerState;
import net.blosson.lflagger.simulation.MovementSimulator;
import net.blosson.lflagger.simulation.SimulatedPlayer;
import net.blosson.lflagger.util.TpsTracker;
import net.blosson.lflagger.util.object.ObjectPool;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * REFACTOR: This check has been completely overhauled to align with the new architecture.
 * - Magic numbers are replaced with constants loaded from the config.
 * - Per-player state is now managed by the injected PlayerState object.
 * - SimulatedPlayer instances are recycled using an ObjectPool.
 * - Redundant validation logic is removed in favor of the base class's isInvalid() method.
 */
public class FlyCheck extends Check {

    // REFACTOR: A single, stateless simulator instance can be shared.
    private static final MovementSimulator SIMULATOR = new MovementSimulator();
    private static final double MIN_VELOCITY_SQUARED = 1E-6;
    private static final double MAX_ANGLE_FOR_CERTAINTY = 90.0;
    private static final double MAX_CERTAINTY = 100.0;

    private final TpsTracker tpsTracker = TpsTracker.getInstance();

    public FlyCheck() {
        super("Fly", "Detects abnormal hovering and flying behaviors.");
    }

    @Override
    public void tick(PlayerEntity player, PlayerState state, ObjectPool<SimulatedPlayer> simulatorPool) {
        if (!isEnabled() || isInvalid(player)) {
            state.resetViolationLevel(getName());
            return;
        }

        if (player.isOnGround() || player.isClimbing() || player.isSubmergedInWater()) {
            state.decreaseViolationLevel(getName(), 1); // Decrease violations when grounded/safe
            return;
        }

        SimulatedPlayer simulatedPlayer = simulatorPool.acquire();
        try {
            simulatedPlayer.reset(player);

            // Get TPS and Ping for compensation
            double serverTps = tpsTracker.getTps();
            int ping = tpsTracker.getPing();

            // Run a simulation tick with NO player input to isolate the effect of gravity.
            SIMULATOR.tick(player, simulatedPlayer, 0.0f, 0.0f, serverTps, ping);

            // Use calculated velocity for remote players, direct velocity for local player.
            Vec3d actualVelocity = player.isMainPlayer() ? player.getVelocity() : state.getCalculatedVelocity();
            Vec3d predictedVelocity = simulatedPlayer.velocity;

            double actualY = actualVelocity.y;
            double predictedY = predictedVelocity.y;

            ModConfig.FlyCheckConfig config = configManager.getConfig().getFlyCheck();
            // Check for both flying up and falling too slowly (slow fall)
            if (actualY > predictedY + config.verticalLeniency || (actualY < predictedY && actualY > predictedY - config.verticalLeniency)) {
                if (state.increaseViolationLevel(getName()) > config.violationThreshold) {
                    handleFlag(player, actualVelocity, predictedVelocity);
                }
            } else {
                state.decreaseViolationLevel(getName());
            }
        } finally {
            simulatorPool.release(simulatedPlayer);
        }
    }

    private void handleFlag(PlayerEntity player, Vec3d actualVelocity, Vec3d predictedVelocity) {
        if (predictedVelocity.lengthSquared() < MIN_VELOCITY_SQUARED || actualVelocity.lengthSquared() < MIN_VELOCITY_SQUARED) {
            flag(player, MAX_CERTAINTY);
            return;
        }

        // Calculate the angle between the predicted (gravity-affected) and actual velocity vectors.
        double dotProduct = predictedVelocity.dotProduct(actualVelocity);
        double angle = Math.toDegrees(Math.acos(dotProduct / (predictedVelocity.length() * actualVelocity.length())));

        double certainty = (angle / MAX_ANGLE_FOR_CERTAINTY) * MAX_CERTAINTY;
        flag(player, Math.min(MAX_CERTAINTY, certainty));
    }

    @Override
    public boolean isEnabled() {
        // REFACTOR: Check is enabled/disabled via the config file.
        return configManager.getConfig().getFlyCheck().enabled;
    }
}