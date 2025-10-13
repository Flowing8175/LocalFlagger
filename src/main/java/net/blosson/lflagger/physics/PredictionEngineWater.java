package net.blosson.lflagger.physics;

import net.blosson.lflagger.data.PlayerData;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PredictionEngineWater extends PredictionEngine {

    private static final double WATER_DRAG = 0.8;
    private static final double WATER_JUMP_VELOCITY = 0.04;

    @Override
    protected Set<Vec3d> fetchPossibleStartTickVectors(ClientPlayerEntity player, PlayerData data) {
        Set<Vec3d> velocities = new HashSet<>();
        velocities.add(data.velocity);
        return velocities;
    }

    @Override
    protected List<Vec3d> applyInputsToVelocityPossibilities(ClientPlayerEntity player, PlayerData data, Set<Vec3d> startingVelocities) {
        List<Vec3d> finalVelocities = new ArrayList<>();

        for (Vec3d velocity : startingVelocities) {
            double dx = velocity.getX() * WATER_DRAG;
            double dy = velocity.getY() * WATER_DRAG;
            double dz = velocity.getZ() * WATER_DRAG;

            dy += 0.04; // Simplified buoyancy

            finalVelocities.add(new Vec3d(dx, dy, dz));
            finalVelocities.add(new Vec3d(dx, dy + WATER_JUMP_VELOCITY, dz));
        }
        return finalVelocities;
    }
}