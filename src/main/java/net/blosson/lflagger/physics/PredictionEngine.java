package net.blosson.lflagger.physics;

import net.blosson.lflagger.data.PlayerData;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Set;

public abstract class PredictionEngine {

    public Vec3d guessBestMovement(ClientPlayerEntity player, PlayerData data) {
        Set<Vec3d> possibleVelocities = fetchPossibleStartTickVectors(player, data);
        List<Vec3d> finalVelocities = applyInputsToVelocityPossibilities(player, data, possibleVelocities);

        Vec3d bestMatch = new Vec3d(0, 0, 0);
        double bestMatchDistance = Double.MAX_VALUE;

        for (Vec3d velocity : finalVelocities) {
            Vec3d collided = Collisions.collide(player, velocity);
            double distance = collided.squaredDistanceTo(player.getVelocity());

            if (distance < bestMatchDistance) {
                bestMatchDistance = distance;
                bestMatch = collided;
            }
        }

        return bestMatch;
    }

    protected abstract Set<Vec3d> fetchPossibleStartTickVectors(ClientPlayerEntity player, PlayerData data);
    protected abstract List<Vec3d> applyInputsToVelocityPossibilities(ClientPlayerEntity player, PlayerData data, Set<Vec3d> startingVelocities);
}