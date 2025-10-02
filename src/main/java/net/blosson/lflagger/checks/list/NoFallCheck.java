package net.blosson.lflagger.checks.list;

import net.blosson.lflagger.checks.Check;
import net.blosson.lflagger.config.ModConfig;
import net.blosson.lflagger.data.PlayerState;
import net.blosson.lflagger.simulation.SimulatedPlayer;
import net.blosson.lflagger.util.object.ObjectPool;
import net.minecraft.entity.player.PlayerEntity;

/**
 * REFACTOR: This check has been simplified to align with the new architecture.
 * - The PlayerFallState inner class and map have been removed.
 * - State is now managed by the central PlayerState object.
 * - Thresholds are now loaded from the config file.
 * - Redundant validation logic is removed.
 */
public class NoFallCheck extends Check {

    private static final double MAX_CERTAINTY = 100.0;

    public NoFallCheck() {
        super("NoFall", "Detects when a player ignores fall damage.");
    }

    @Override
    public void tick(PlayerEntity player, PlayerState state, ObjectPool<SimulatedPlayer> simulatorPool) {
        // The simulatorPool is not used in this check, which is perfectly fine.
        if (!isEnabled() || isInvalid(player)) {
            return;
        }

        boolean isCurrentlyOnGround = player.isOnGround();
        ModConfig.NoFallCheckConfig config = configManager.getConfig().getNoFallCheck();

        // Check if the player just landed after a fall.
        if (!state.wasOnGround && isCurrentlyOnGround) {
            // Check if they fell far enough to take damage, based on the configurable threshold.
            if (state.lastFallDistance > config.minFallDistance) {
                // A player's hurtTime becomes > 0 when they take damage.
                // If it's 0 after a damaging fall, they likely negated the damage.
                if (player.hurtTime == 0) {
                    // The certainty is higher the further they fell.
                    double certainty = (state.lastFallDistance - config.minFallDistance) * config.certaintyMultiplier;
                    flag(player, Math.min(MAX_CERTAINTY, certainty));
                }
            }
        }
        // The player's state (wasOnGround, lastFallDistance) is automatically updated in CheckManager after this.
    }

    @Override
    public boolean isEnabled() {
        return configManager.getConfig().getNoFallCheck().enabled;
    }
}