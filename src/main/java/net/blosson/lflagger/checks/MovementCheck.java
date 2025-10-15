package net.blosson.lflagger.checks;

import net.blosson.lflagger.data.PlayerState;
import net.blosson.lflagger.grim.prediction.GrimPredictionEngine;
import net.blosson.lflagger.simulation.SimulatedPlayer;
import net.blosson.lflagger.util.object.ObjectPool;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.blosson.lflagger.grim.prediction.PredictionEngineWater;


public class MovementCheck extends Check {

    private final GrimPredictionEngine predictionEngine = new GrimPredictionEngine();
    private final PredictionEngineWater waterPredictionEngine = new PredictionEngineWater();

    public MovementCheck() {
        super("Movement", "Checks for impossible movement based on GrimAC's physics model.");
    }

    @Override
    public void tick(PlayerEntity player, PlayerState state, ObjectPool<SimulatedPlayer> simulatorPool) {
        if (isInvalid(player) || !isEnabled()) {
            return;
        }

        GrimPredictionEngine engine = selectEngine(player);
        Vec3d actualMovement = player.getEntityPos().subtract(state.lastPosition);
        GrimPredictionEngine.PredictionResult result = engine.checkMovement(player, state.lastVelocity, actualMovement);

        if (result.bestOffset > configManager.getConfig().getMovementCheck().distanceThreshold) {
            flag(player, result.bestOffset * 10); // Scale offset to a certainty percentage
        }

        state.lastVelocity = result.nextClientVelocity;
    }

    private GrimPredictionEngine selectEngine(PlayerEntity player) {
        if (player.isSubmergedInWater()) {
            return waterPredictionEngine;
        }
        return predictionEngine;
    }

    @Override
    public boolean isEnabled() {
        return configManager.getConfig().getMovementCheck().enabled;
    }
}