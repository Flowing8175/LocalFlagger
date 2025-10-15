package net.blosson.lflagger.physics;

import net.blosson.lflagger.data.PlayerData;
import net.blosson.lflagger.data.PlayerState;
import net.minecraft.entity.player.PlayerEntity;

public abstract class PredictionEngine {

    /**
     * Predicts the player's next position based on their movement history and current state.
     *
     * @param data The player's current data, including position history.
     * @return A PredictionResult containing the predicted position and the tolerance for error.
     */
    public abstract PredictionResult predictNextPosition(PlayerData data);

    public PredictionResult guessBestMovement(PlayerEntity player, PlayerState state) {
        return predictNextPosition(state.playerData);
    }
}