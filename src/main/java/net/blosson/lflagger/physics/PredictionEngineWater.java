package net.blosson.lflagger.physics;

import net.blosson.lflagger.data.PlayerData;
import net.minecraft.util.math.Vec3d;

public class PredictionEngineWater extends PredictionEngine {

    private static final double WATER_DRAG = 0.8;
    private static final double WATER_BUOYANCY = 0.04;

    @Override
    public PredictionResult predictNextPosition(PlayerData data) {
        if (data.positionHistory.isEmpty()) {
            return new PredictionResult(data.position, 1.0);
        }

        // Apply water physics
        Vec3d velocity = data.velocity;
        double dx = velocity.getX() * WATER_DRAG;
        double dy = velocity.getY() * WATER_DRAG;
        double dz = velocity.getZ() * WATER_DRAG;
        dy += WATER_BUOYANCY;

        Vec3d newVelocity = new Vec3d(dx, dy, dz);
        Vec3d predictedPosition = data.position.add(newVelocity.multiply(0.05)); // Assume 50ms tick time

        return new PredictionResult(predictedPosition, 0.0);
    }
}