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

        // The core logic: if the player was not on the ground last tick but is now,
        // and their fall distance was greater than the survivable limit, it's a potential flag.
        if (!state.wasOnGround && player.isOnGround()) {
            // Player has just landed. Check the fall distance.
            ModConfig.NoFallCheckConfig config = configManager.getConfig().getNoFallCheck();
            double fallDistance = player.isMainPlayer() ? player.fallDistance : state.lastFallDistance;

            if (fallDistance > config.maxFallDistance) {
                // The player "survived" a fall that should have caused damage or been fatal.
                if (state.increaseViolationLevel(getName()) > config.violationThreshold) {
                    flag(player, 100.0); // 100% certainty for this type of check
                }
            }
            // Reset fall distance after landing
            state.lastFallDistance = 0;
        } else if (!player.isOnGround()) {
            // If the player is in the air, accumulate fall distance.
            // For the local player, we trust player.fallDistance. For others, we calculate it.
            if (player.isMainPlayer()) {
                state.lastFallDistance = player.fallDistance;
            } else {
                // Velocity is negative when falling, so we subtract it to get a positive fall distance.
                if (player.getVelocity().y < 0) {
                    state.lastFallDistance -= player.getVelocity().y;
                }
            }
        } else {
            // If on ground, reset fall distance.
            state.lastFallDistance = 0;
        }
    }

    @Override
    public boolean isEnabled() {
        return configManager.getConfig().getNoFallCheck().enabled;
    }
}