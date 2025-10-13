package net.blosson.lflagger.checks;

import net.blosson.lflagger.data.PlayerData;
import net.blosson.lflagger.physics.MovementSimulator;
import net.blosson.lflagger.physics.PredictionResult;
import net.minecraft.entity.player.PlayerEntity;

public class GroundSpoofCheck {

    private final MovementSimulator simulator = new MovementSimulator();

    public boolean check(PlayerEntity player, PlayerData data) {
        PredictionResult result = simulator.simulate(player, data);

        double error = player.getEntityPos().squaredDistanceTo(result.predictedPosition());
        double tolerance = result.tolerance();

        if (error > tolerance * tolerance) {
            // A simple heuristic: if the error is large, and the player claims to be on ground
            // while the prediction is significantly different, it might be a spoof.
            if (data.onGround && Math.abs(player.getY() - result.predictedPosition().getY()) > 0.5) {
                 return true;
            }
        }

        return false;
    }
}