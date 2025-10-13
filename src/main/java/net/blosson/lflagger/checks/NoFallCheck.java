package net.blosson.lflagger.checks;

import net.blosson.lflagger.data.PlayerData;
import net.minecraft.client.network.ClientPlayerEntity;

public class NoFallCheck {

    public boolean check(ClientPlayerEntity player, PlayerData data, PlayerData lastData) {
        if (lastData == null) {
            return false;
        }

        // If the player just landed (was in air, is now on ground)
        if (!lastData.onGround && data.onGround) {
            // If they landed with a high vertical velocity, it's suspicious.
            if (lastData.velocity.getY() < -0.5) {
                return true;
            }
        }

        return false;
    }
}