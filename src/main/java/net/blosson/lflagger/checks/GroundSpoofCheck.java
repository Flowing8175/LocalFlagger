package net.blosson.lflagger.checks;

import net.blosson.lflagger.data.PlayerData;
import net.blosson.lflagger.physics.MovementSimulator;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

public class GroundSpoofCheck {

    private final MovementSimulator simulator = new MovementSimulator();

    public boolean check(ClientPlayerEntity player, PlayerData data) {
        Vec3d predictedVelocity = simulator.simulate(player, data);

        // A very simplified check for now:
        boolean predictedOnGround = Math.abs(predictedVelocity.getY()) < 0.001;

        if (data.onGround && !predictedOnGround && data.velocity.getY() < -0.1) {
            return true;
        }

        return false;
    }
}