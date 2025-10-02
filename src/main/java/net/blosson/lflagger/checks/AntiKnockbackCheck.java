package net.blosson.lflagger.checks;

import net.blosson.lflagger.util.DamageTiltTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AntiKnockbackCheck extends Check {

    private static class PlayerKnockbackState {
        int lastHurtTime = 0;
    }

    private final Map<UUID, PlayerKnockbackState> playerStates = new HashMap<>();

    public AntiKnockbackCheck() {
        super("AntiKnockback", "Detects when a player ignores knockback from a hit.");
    }

    public void tick(PlayerEntity player) {
        if (player.getUuid().equals(MinecraftClient.getInstance().player.getUuid())) {
            return;
        }

        PlayerKnockbackState state = playerStates.computeIfAbsent(player.getUuid(), u -> new PlayerKnockbackState());

        // A hit is confirmed when hurtTime has just become positive.
        boolean justHit = player.hurtTime > 0 && state.lastHurtTime == 0;

        if (justHit) {
            // A player blocking with a shield is a legitimate reason to not take full knockback.
            if (player.isBlocking()) {
                state.lastHurtTime = player.hurtTime;
                return;
            }

            // A second piece of evidence: a genuine hit also causes a damage tilt effect.
            // In modern versions, this is a packet, so we check our tracker.
            if (MinecraftClient.getInstance().world == null) {
                return; // Should not happen here, but good practice
            }
            long currentTick = MinecraftClient.getInstance().world.getTime();
            boolean hasDamageTilt = DamageTiltTracker.getInstance().hasRecentTilt(player.getId(), currentTick);

            // Get the player's velocity magnitude at the moment of the hit.
            double velocityMagnitude = player.getVelocity().length();

            // A typical knockback impulse is around 0.4. We set a low threshold for what's considered "no knockback".
            double knockbackThreshold = 0.1;

            // If the player was hit (hurtTime) and shows a damage tilt, but their velocity is negligible,
            // it's a strong sign of anti-knockback cheats.
            if (hasDamageTilt && velocityMagnitude < knockbackThreshold) {
                // Certainty is based on how little the player moved compared to a standard knockback.
                double vanillaKnockbackSpeed = 0.4;
                double certainty = (1.0 - (velocityMagnitude / vanillaKnockbackSpeed)) * 100.0;

                flag(player, Math.max(0, certainty));
            }
        }

        // Update the state for the next tick's comparison.
        state.lastHurtTime = player.hurtTime;
    }
}