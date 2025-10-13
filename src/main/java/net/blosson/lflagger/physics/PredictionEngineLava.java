package net.blosson.lflagger.physics;

import net.blosson.lflagger.data.PlayerData;

public class PredictionEngineLava extends PredictionEngine {

    @Override
    public PredictionResult predictNextPosition(PlayerData data) {
        // Placeholder implementation
        // A real implementation would apply lava physics (stronger drag)
        return new PredictionResult(data.position, 1.0);
    }
}