package net.blosson.lflagger.physics;

import net.blosson.lflagger.data.PlayerData;
import net.minecraft.util.math.Vec3d;

public class PredictionEngineLava extends PredictionEngine {

    private static final double LAVA_DRAG = 0.5;
    private static final double LAVA_BUOYANCY = 0.04;

    @Override
    public PredictionResult predictNextPosition(PlayerData data) {
        if (data.positionHistory.size() < 2) {
            return new PredictionResult(data.position, 1.0);
        }

        // Apply lava physics
        long timeDelta = data.clientTimestamp - data.positionHistory.getLast().timestamp();
        if (timeDelta == 0) return new PredictionResult(data.position, 1.0);
        Vec3d velocity = data.position.subtract(data.positionHistory.getLast().position()).multiply(1000.0 / timeDelta);

        double dx = velocity.getX() * LAVA_DRAG;
        double dy = velocity.getY() * LAVA_DRAG;
        double dz = velocity.getZ() * LAVA_DRAG;
        dy += LAVA_BUOYANCY;

        Vec3d newVelocity = new Vec3d(dx, dy, dz);
        Vec3d predictedPosition = data.position.add(newVelocity.multiply(0.05)); // Assume 50ms tick time

        return new PredictionResult(predictedPosition, 0.0);
    }
}