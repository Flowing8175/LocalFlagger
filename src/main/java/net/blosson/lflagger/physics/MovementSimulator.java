package net.blosson.lflagger.physics;

import net.blosson.lflagger.data.PlayerData;
import net.blosson.lflagger.manager.UncertaintyManager;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

public class MovementSimulator {

    private final PredictionEngineNormal normalEngine = new PredictionEngineNormal();
    // private final PredictionEngineWater waterEngine = new PredictionEngineWater();
    // private final PredictionEngineLava lavaEngine = new PredictionEngineLava();

    public PredictionResult simulate(ClientPlayerEntity player, PlayerData data) {
        UncertaintyManager uncertaintyManager = UncertaintyManager.getInstance();
        PredictionEngine engine = selectEngine(player);

        // First, get the extrapolated, ideal position from the engine
        PredictionResult extrapolatedResult = engine.predictNextPosition(data);
        Vec3d extrapolatedPosition = extrapolatedResult.predictedPosition();

        // Calculate the ideal velocity vector for this tick
        Vec3d idealVelocity = extrapolatedPosition.subtract(player.getPos());

        // Now, collide that ideal velocity with the world to get the realistic final position
        Vec3d finalPosition = Collisions.collide(player, idealVelocity).add(player.getPos());

        double tolerance = uncertaintyManager.getTolerance(data);

        return new PredictionResult(finalPosition, tolerance);
    }

    private PredictionEngine selectEngine(ClientPlayerEntity player) {
        // TODO: Re-implement water and lava engines
        // if (player.isInLava()) {
        //     return lavaEngine;
        // }
        // if (player.isSubmergedInWater()) {
        //     return waterEngine;
        // }
        return normalEngine;
    }
}