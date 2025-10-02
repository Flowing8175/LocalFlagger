package net.blosson.lflagger.checks;

import net.blosson.lflagger.simulation.MovementSimulator;
import net.blosson.lflagger.simulation.SimulatedPlayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpeedCheck extends Check {

    private static class PlayerSpeedState {
        int speedingTicks = 0;
    }

    private final Map<UUID, PlayerSpeedState> playerStates = new HashMap<>();
    private final MovementSimulator simulator = new MovementSimulator();

    public SpeedCheck() {
        super("Speed", "Detects movement exceeding normal speed limits.");
    }

    public void tick(PlayerEntity player) {
        if (player.getUuid().equals(MinecraftClient.getInstance().player.getUuid()) || player.getAbilities().allowFlying) {
            return;
        }

        PlayerSpeedState state = playerStates.computeIfAbsent(player.getUuid(), u -> new PlayerSpeedState());

        double actualHorizontalSpeed = new Vec3d(player.getVelocity().x, 0, player.getVelocity().z).length();

        // Simulate one tick with maximum forward input to get the max possible speed
        SimulatedPlayer simulatedPlayer = new SimulatedPlayer(player);
        simulator.tick(simulatedPlayer, 1.0f, 0.0f); // Max forward input
        double maxPredictedSpeed = new Vec3d(simulatedPlayer.velocity.x, 0, simulatedPlayer.velocity.z).length();

        // Check if the player is moving faster than 115% of the predicted max speed
        // Add a small buffer for inaccuracies
        if (actualHorizontalSpeed > maxPredictedSpeed * 1.15 + 0.01) {
            state.speedingTicks++;
        } else {
            state.speedingTicks = Math.max(0, state.speedingTicks - 1); // Decay violations
        }

        if (state.speedingTicks > 60) { // Flag after 3 seconds of sustained speeding
            double vanillaMax = maxPredictedSpeed * 1.15;
            double certainty = ((actualHorizontalSpeed - vanillaMax) / vanillaMax) * 100.0;

            flag(player, certainty);
        }
    }
}