package net.blosson.lflagger.checks.list;

import net.blosson.lflagger.checks.Check;
import net.blosson.lflagger.config.ModConfig;
import net.blosson.lflagger.data.PlayerState;
import net.blosson.lflagger.simulation.SimulatedPlayer;
import net.blosson.lflagger.util.DamageTiltTracker;
import net.blosson.lflagger.util.object.ObjectPool;
import net.minecraft.entity.player.PlayerEntity;

/**
 * REFACTOR: This check has been simplified to align with the new architecture.
 * - The PlayerKnockbackState inner class and map have been removed.
 * - State is now managed by the central PlayerState object.
 * - Thresholds are now loaded from the config file.
 * - Redundant validation logic is removed.
 */
public class AntiKnockbackCheck extends Check {

    private static final double MAX_CERTAINTY = 100.0;
    private final DamageTiltTracker damageTiltTracker = DamageTiltTracker.getInstance();

    public AntiKnockbackCheck() {
        super("AntiKnockback", "Detects when a player ignores knockback from a hit.");
    }

    @Override
    public void tick(PlayerEntity player, PlayerState state, ObjectPool<SimulatedPlayer> simulatorPool) {
        if (!isEnabled() || isInvalid(player)) {
            return;
        }

        // A hit is confirmed when hurtTime has just become positive.
        boolean justHit = player.hurtTime > 0 && state.lastHurtTime == 0;

        if (justHit) {
            // A player blocking with a shield is a legitimate reason to not take full knockback.
            if (player.isBlocking()) {
                return;
            }

            if (client.world == null) {
                return; // Should not happen here, but good practice
            }

            // A genuine hit also causes a damage tilt effect.
            long currentTick = client.world.getTime();
            boolean hasDamageTilt = damageTiltTracker.hasRecentTilt(player.getId(), currentTick);

            // Get the player's velocity magnitude at the moment of the hit.
            double velocityMagnitude = player.getVelocity().length();

            ModConfig.AntiKnockbackCheckConfig config = configManager.getConfig().getAntiKnockbackCheck();

            // If the player was hit and shows a damage tilt, but their velocity is negligible,
            // it's a strong sign of anti-knockback cheats.
            if (hasDamageTilt && velocityMagnitude < config.knockbackThreshold) {
                // Certainty is based on how little the player moved compared to a standard knockback.
                double certainty = (1.0 - (velocityMagnitude / config.assumedVanillaKnockback)) * MAX_CERTAINTY;
                flag(player, Math.max(0, certainty));
            }
        }
        // The player's lastHurtTime is updated in PlayerState by the CheckManager after this.
    }

    @Override
    public boolean isEnabled() {
        return configManager.getConfig().getAntiKnockbackCheck().enabled;
    }
}