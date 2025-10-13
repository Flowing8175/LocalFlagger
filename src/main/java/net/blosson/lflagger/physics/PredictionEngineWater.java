package net.blosson.lflagger.physics;

import net.blosson.lflagger.data.PlayerData;

public class PredictionEngineWater extends PredictionEngine {

    @Override
    public PredictionResult predictNextPosition(PlayerData data) {
        // Placeholder implementation
        // A real implementation would apply water physics (drag, buoyancy)
        return new PredictionResult(data.position, 1.0);
    }
}