package net.blosson.lflagger.physics;

import net.blosson.lflagger.data.PlayerData;
import net.blosson.lflagger.data.PlayerState;
import net.blosson.lflagger.manager.UncertaintyManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class MovementSimulator {

    private final PredictionEngineNormal normalEngine = new PredictionEngineNormal();
    private final PredictionEngineWater waterEngine = new PredictionEngineWater();
    private final PredictionEngineLava lavaEngine = new PredictionEngineLava();

    public PredictionResult simulate(PlayerEntity player, PlayerState state) {
        UncertaintyManager uncertaintyManager = UncertaintyManager.getInstance();
        PredictionEngine engine = selectEngine(player);
        return engine.guessBestMovement(player, state);
    }

    private PredictionEngine selectEngine(PlayerEntity player) {
        if (player.isInLava()) {
            return lavaEngine;
        }
        if (player.isSubmergedInWater()) {
            return waterEngine;
        }
        return normalEngine;
    }
}