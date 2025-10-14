package net.blosson.lflagger.checks.list;

import net.blosson.lflagger.checks.Check;
import net.blosson.lflagger.config.ModConfig;
import net.blosson.lflagger.data.PlayerState;
import net.blosson.lflagger.simulation.SimulatedPlayer;
import net.blosson.lflagger.util.object.ObjectPool;
import net.minecraft.entity.player.PlayerEntity;

/**
 * REFACTOR: This check is now part of the new, centralized check system.
 * - It uses the injected PlayerState to get historical on-ground status.
 * - It's configured via the main config file.
 * - It no longer needs to manage its own state.
 */
public class NoFallCheck extends Check {

    public NoFallCheck() {
        super("NoFall", "Detects players surviving falls from impossible heights.");
    }

    @Override
    public void tick(PlayerEntity player, PlayerState state, ObjectPool<SimulatedPlayer> pool) {
        if (!isEnabled() || isInvalid(player)) {
            return;
        }

        // Check if the player has just landed.
        if (!state.wasOnGround && player.isOnGround()) {
            ModConfig.NoFallCheckConfig config = configManager.getConfig().getNoFallCheck();

            // Use the player's actual fallDistance, which is reset by the game after landing.
            // state.lastFallDistance now correctly stores the value from the previous tick.
            if (state.lastFallDistance > config.maxFallDistance) {
                // A player cannot survive a fall greater than 3 blocks without taking damage.
                // If they received no damage (hurtTime is 0), it's a very high-certainty flag.
                if (player.hurtTime == 0) {
                     if (state.increaseViolationLevel(getName()) > config.violationThreshold) {
                        flag(player, 100.0);
                    }
                }
            }
        }

        // The player's state (including wasOnGround and lastFallDistance)
        // is updated by the CheckManager after this tick method completes.
    }

    @Override
    public boolean isEnabled() {
        return configManager.getConfig().getNoFallCheck().enabled;
    }
}