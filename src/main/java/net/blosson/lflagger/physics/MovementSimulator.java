package net.blosson.lflagger.physics;

import net.blosson.lflagger.data.PlayerData;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

public class MovementSimulator {

    private final PredictionEngineNormal normalEngine = new PredictionEngineNormal();
    private final PredictionEngineWater waterEngine = new PredictionEngineWater();
    private final PredictionEngineLava lavaEngine = new PredictionEngineLava();

    public Vec3d simulate(ClientPlayerEntity player, PlayerData data) {
        PredictionEngine engine = selectEngine(player);
        return engine.guessBestMovement(player, data);
    }

    private PredictionEngine selectEngine(ClientPlayerEntity player) {
        if (player.isInLava()) {
            return lavaEngine;
        }
        if (player.isSubmergedInWater()) {
            return waterEngine;
        }
        return normalEngine;
    }
}