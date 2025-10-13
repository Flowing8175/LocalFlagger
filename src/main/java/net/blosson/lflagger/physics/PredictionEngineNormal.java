package net.blosson.lflagger.physics;

import net.blosson.lflagger.data.PlayerData;
import net.blosson.lflagger.data.PositionSnapshot;
import net.minecraft.util.math.Vec3d;

import java.util.Deque;

public class PredictionEngineNormal extends PredictionEngine {

    @Override
    public PredictionResult predictNextPosition(PlayerData data) {
        if (data.positionHistory.size() < 3) {
            // Not enough data for acceleration, fall back to linear extrapolation or default
            if (data.positionHistory.size() < 2) {
                return new PredictionResult(data.position, 1.0);
            }
            // Linear extrapolation
            PositionSnapshot current = data.positionHistory.getLast();
            PositionSnapshot previous = data.positionHistory.stream().skip(data.positionHistory.size() - 2).findFirst().get();
            long timeDelta = current.timestamp() - previous.timestamp();
            if (timeDelta == 0) return new PredictionResult(data.position, 1.0);
            Vec3d velocity = current.position().subtract(previous.position()).multiply(1.0 / timeDelta);
            long timeToPredict = System.currentTimeMillis() - current.timestamp();
            Vec3d predictedPosition = current.position().add(velocity.multiply(timeToPredict));
            return new PredictionResult(predictedPosition, 0.0);
        }

        // --- Acceleration Extrapolation Logic ---
        PositionSnapshot current = data.positionHistory.getLast();
        PositionSnapshot prev1 = data.positionHistory.stream().skip(data.positionHistory.size() - 2).findFirst().get();
        PositionSnapshot prev2 = data.positionHistory.stream().skip(data.positionHistory.size() - 3).findFirst().get();

        long dt1 = current.timestamp() - prev1.timestamp();
        long dt2 = prev1.timestamp() - prev2.timestamp();
        if (dt1 == 0 || dt2 == 0) return new PredictionResult(data.position, 1.0);

        Vec3d v1 = current.position().subtract(prev1.position()).multiply(1.0 / dt1);
        Vec3d v2 = prev1.position().subtract(prev2.position()).multiply(1.0 / dt2);

        Vec3d acceleration = v1.subtract(v2).multiply(2.0 / (dt1 + dt2));

        long timeToPredict = 50; // Predict one client tick (50ms) into the future
        Vec3d predictedPosition = current.position()
                .add(v1.multiply(timeToPredict))
                .add(acceleration.multiply(0.5 * timeToPredict * timeToPredict));

        return new PredictionResult(predictedPosition, 0.0);
    }
}