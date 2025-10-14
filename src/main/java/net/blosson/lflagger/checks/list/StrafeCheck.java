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
 * - Redundant validation logic is removed.
 */
public class StrafeCheck extends Check {

    private static final MovementSimulator SIMULATOR = new MovementSimulator();
    private final TpsTracker tpsTracker = TpsTracker.getInstance();

    public StrafeCheck() {
        super("Strafe", "Detects unnatural mid-air movement control.");
    }

    @Override
    public void tick(PlayerEntity player, PlayerState state, ObjectPool<SimulatedPlayer> simulatorPool) {
        if (!isEnabled() || isInvalid(player)) {
            state.resetViolationLevel(getName());
            return;
        }

        // This check only applies to airborne players
        if (player.isOnGround()) {
            state.resetViolationLevel(getName());
            return;
        }

        SimulatedPlayer simulatedPlayer = simulatorPool.acquire();
        try {
            simulatedPlayer.reset(player);

            // Get TPS and Ping for compensation
            double serverTps = tpsTracker.getTps();
            int ping = tpsTracker.getPing();

            // Simulate the player's movement with no input to get a baseline for air friction decay.
            SIMULATOR.tick(player, simulatedPlayer, 0.0f, 0.0f, serverTps, ping);

            Vec3d actualVel = player.getVelocity();
            Vec3d predictedVel = simulatedPlayer.velocity;

            double actualHorizontalSpeed = new Vec3d(actualVel.x, 0, actualVel.z).length();
            double predictedHorizontalSpeed = new Vec3d(predictedVel.x, 0, predictedVel.z).length();

            ModConfig.StrafeCheckConfig config = configManager.getConfig().getStrafeCheck();
            // If the player is accelerating or maintaining speed horizontally in the air beyond what friction allows
            if (actualHorizontalSpeed > predictedHorizontalSpeed + config.airStrafeLeniency) {
                if (state.increaseViolationLevel(getName()) > config.violationThreshold) {
                    flag(player); // Flag without certainty as per original logic
                }
            } else {
                state.decreaseViolationLevel(getName());
            }
        } finally {
            simulatorPool.release(simulatedPlayer);
        }
    }

    @Override
    public boolean isEnabled() {
        return configManager.getConfig().getStrafeCheck().enabled;
    }
}