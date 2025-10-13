package net.blosson.lflagger.physics;

import net.blosson.lflagger.data.PlayerData;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PredictionEngineLava extends PredictionEngine {

    private static final double LAVA_DRAG = 0.5;

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
            double dx = velocity.getX() * LAVA_DRAG;
            double dy = velocity.getY() * LAVA_DRAG;
            double dz = velocity.getZ() * LAVA_DRAG;

            dy += 0.04; // Simplified buoyancy

            finalVelocities.add(new Vec3d(dx, dy, dz));
        }
        return finalVelocities;
    }
}