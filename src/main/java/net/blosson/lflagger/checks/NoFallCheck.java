package net.blosson.lflagger.checks;

import net.blosson.lflagger.data.PlayerData;
import net.minecraft.entity.player.PlayerEntity;

public class NoFallCheck {

    public boolean check(PlayerEntity player, PlayerData data, PlayerData lastData) {
        if (lastData == null) {
            return false;
        }

        // If the player just landed (was in air, is now on ground)
        if (!lastData.onGround && data.onGround) {
            // Calculate velocity from the last two points
            long timeDelta = data.clientTimestamp - lastData.clientTimestamp;
            if (timeDelta > 0) {
                double verticalVelocity = (data.position.getY() - lastData.position.getY()) / timeDelta * 1000.0;
                if (verticalVelocity < -0.5) { // Check against a threshold
                    return true;
                }
            }
        }

        return false;
    }
}