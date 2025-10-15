package net.blosson.lflagger.checks.list;

import net.blosson.lflagger.checks.Check;
import net.blosson.lflagger.data.PlayerData;
import net.blosson.lflagger.data.PlayerState;
import net.blosson.lflagger.physics.MovementSimulator;
import net.blosson.lflagger.physics.PredictionResult;
import net.blosson.lflagger.simulation.SimulatedPlayer;
import net.blosson.lflagger.util.object.ObjectPool;
import net.minecraft.entity.player.PlayerEntity;

public class MovementCheck extends Check {

    private final MovementSimulator movementSimulator = new MovementSimulator();

    public MovementCheck() {
        super("Movement", "Detects impossible movement");
    }

    @Override
    public void tick(PlayerEntity player, PlayerState state, ObjectPool<SimulatedPlayer> simulatorPool) {
        PredictionResult result = movementSimulator.simulate(player, state);
        double distance = result.predictedPosition().distanceTo(player.getEntityPos());

        if (distance > result.tolerance()) {
            flag(player, distance);
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean canCheckLocalPlayer() {
        return true;
    }
}