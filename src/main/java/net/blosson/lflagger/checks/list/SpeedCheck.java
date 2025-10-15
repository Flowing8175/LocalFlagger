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
 * - The inner PlayerSpeedState class has been removed in favor of the central PlayerState.
 * - SimulatedPlayer instances are recycled using an ObjectPool.
 * - Redundant validation logic is removed.
 */
public class SpeedCheck extends Check {

    private static final MovementSimulator SIMULATOR = new MovementSimulator();
    private static final float SIMULATION_FORWARD_INPUT = 1.0f;
    private static final float SIMULATION_STRAFE_INPUT = 0.0f;
    private static final double MAX_CERTAINTY = 100.0;

    private final TpsTracker tpsTracker = TpsTracker.getInstance();

    public SpeedCheck() {
        super("Speed", "Detects movement exceeding normal speed limits.");
    }

    @Override
    public void tick(PlayerEntity player, PlayerState state, ObjectPool<SimulatedPlayer> simulatorPool) {
        if (!isEnabled() || isInvalid(player)) {
            state.speedingTicks = 0; // Reset speeding ticks if check is invalid
            return;
        }

        // Use the consistent, calculated velocity for all players.
        Vec3d velocity = state.getCalculatedVelocity();
        double actualHorizontalSpeed = new Vec3d(velocity.x, 0, velocity.z).length();

        SimulatedPlayer simulatedPlayer = simulatorPool.acquire();
        try {
            simulatedPlayer.reset(player);

            // Get TPS and Ping for compensation
            double serverTps = tpsTracker.getTps();
            int ping = tpsTracker.getPing();

            // Simulate one tick with maximum forward input to get the max possible speed
            SIMULATOR.tick(player, simulatedPlayer, SIMULATION_FORWARD_INPUT, SIMULATION_STRAFE_INPUT, serverTps, ping);
            double maxPredictedSpeed = new Vec3d(simulatedPlayer.velocity.x, 0, simulatedPlayer.velocity.z).length();

            // REFACTOR: Use leniency values from config
            ModConfig.SpeedCheckConfig config = configManager.getConfig().getSpeedCheck();
            double lenientMaxSpeed = maxPredictedSpeed * config.speedMultiplierLeniency + config.speedFlatLeniency;

            if (actualHorizontalSpeed > lenientMaxSpeed) {
                state.speedingTicks++;
            } else {
                state.speedingTicks = Math.max(0, state.speedingTicks - 1); // Decay violations
            }

            if (state.speedingTicks > config.violationThreshold) {
                double vanillaMax = maxPredictedSpeed * config.speedMultiplierLeniency;
                double certainty = ((actualHorizontalSpeed - vanillaMax) / vanillaMax) * MAX_CERTAINTY;
                flag(player, Math.min(MAX_CERTAINTY, certainty));
            }
        } finally {
            simulatorPool.release(simulatedPlayer);
        }
    }

    @Override
    public boolean isEnabled() {
        return configManager.getConfig().getSpeedCheck().enabled;
    }
}