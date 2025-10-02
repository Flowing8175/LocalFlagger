package net.blosson.lflagger.checks;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NoFallCheck extends Check {

    // Inner class to store the state of a player between ticks
    private static class PlayerFallState {
        boolean wasOnGround;
        double lastFallDistance; // Changed to double

        PlayerFallState(PlayerEntity player) {
            this.wasOnGround = player.isOnGround();
            this.lastFallDistance = player.fallDistance;
        }
    }

    private final Map<UUID, PlayerFallState> playerStates = new HashMap<>();

    public NoFallCheck() {
        super("NoFall", "Detects when a player ignores fall damage.");
    }

    /**
     * This method will be called every tick for each player to check for NoFall violations.
     * @param player The player to check.
     */
    public void tick(PlayerEntity player) {
        // Don't check the local player
        if (player.getUuid().equals(MinecraftClient.getInstance().player.getUuid())) {
            return;
        }

        PlayerFallState state = playerStates.computeIfAbsent(player.getUuid(), u -> new PlayerFallState(player));
        boolean isCurrentlyOnGround = player.isOnGround();

        // Check if the player just landed
        if (!state.wasOnGround && isCurrentlyOnGround) {
            // A fall distance of > 3.0 is required for damage. We use 4.0 for a conservative check.
            if (state.lastFallDistance > 4.0) { // Changed to double comparison
                // The player fell far enough to take damage.
                // A player's hurtTime becomes > 0 when they take damage.
                // If it's 0, they likely negated the damage.
                if (player.hurtTime == 0) {
                    // The certainty is higher the further they fell.
                    double certainty = (state.lastFallDistance - 4.0) * 10.0;
                    flag(player, Math.min(100.0, certainty));
                }
            }
        }

        // Update the state for the next tick's comparison
        state.wasOnGround = isCurrentlyOnGround;
        state.lastFallDistance = player.fallDistance;
    }
}