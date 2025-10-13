package net.blosson.lflagger.physics;

import net.blosson.lflagger.data.PlayerData;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PredictionEngineNormal extends PredictionEngine {

    private static final double GRAVITY = 0.08;
    private static final double JUMP_VELOCITY = 0.42F;

    @Override
    protected Set<Vec3d> fetchPossibleStartTickVectors(ClientPlayerEntity player, PlayerData data) {
        Set<Vec3d> velocities = new HashSet<>();
        velocities.add(data.velocity);
        return velocities;
    }

    @Override
    protected List<Vec3d> applyInputsToVelocityPossibilities(ClientPlayerEntity player, PlayerData data, Set<Vec3d> startingVelocities) {
        List<Vec3d> finalVelocities = new ArrayList<>();
        float friction = player.getWorld().getBlockState(player.getSteppingPos()).getBlock().getSlipperiness() * 0.91f;

        for (Vec3d velocity : startingVelocities) {
            double dx = velocity.getX() * friction;
            double dy = (velocity.getY() - GRAVITY) * 0.98;
            double dz = velocity.getZ() * friction;

            Vec3d baseMovement = new Vec3d(dx, dy, dz);

            // TODO: Add player movement inputs (WASD)
            finalVelocities.add(baseMovement);

            if (data.onGround) {
                finalVelocities.add(new Vec3d(baseMovement.x, JUMP_VELOCITY, baseMovement.z));
            }
        }
        return finalVelocities;
    }
}