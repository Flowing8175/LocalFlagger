package net.blosson.lflagger.grim.prediction;

import net.blosson.lflagger.physics.Collisions;
import net.blosson.lflagger.util.grim.GrimMath;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import java.util.HashSet;
import java.util.Set;

public class PredictionEngineWater extends GrimPredictionEngine {

    @Override
    public PredictionResult checkMovement(PlayerEntity player, Vec3d lastVelocity, Vec3d actualMovement) {
        Set<VectorData> startVelocities = generatePossibleStartVelocities(player, lastVelocity);
        Set<VectorData> inputVelocities = applyInputsToVelocityPossibilities(player, startVelocities);

        double bestOffset = Double.MAX_VALUE;
        VectorData bestVectorData = null;
        Vec3d bestPreCollisionVelocity = null;

        for (VectorData vectorData : inputVelocities) {
            Vec3d preCollisionVelocity = applyEndOfTickPhysics(player, vectorData.vector);
            Vec3d predictedMovement = Collisions.collide(player, preCollisionVelocity);
            double offset = actualMovement.squaredDistanceTo(predictedMovement);

            if (offset < bestOffset) {
                bestOffset = offset;
                bestVectorData = new VectorData(predictedMovement, vectorData.types, vectorData);
                bestPreCollisionVelocity = preCollisionVelocity;
            }
        }

        return new PredictionResult(
            Math.sqrt(bestOffset),
            bestVectorData != null ? bestVectorData.vector : Vec3d.ZERO,
            bestPreCollisionVelocity != null ? bestPreCollisionVelocity : Vec3d.ZERO
        );
    }

    @Override
    protected Vec3d applyEndOfTickPhysics(PlayerEntity player, Vec3d velocity) {
        velocity = velocity.multiply(0.800000011920929D, 0.800000011920929D, 0.800000011920929D);
        velocity = velocity.add(0, -0.02, 0); // Simplified fluid falling
        return velocity;
    }

    @Override
    protected void addJumpsToPossibilities(PlayerEntity player, Set<VectorData> existingVelocities) {
        Set<VectorData> jumpPossibilities = new HashSet<>();
        for (VectorData vector : existingVelocities) {
            jumpPossibilities.add(vector.returnNewModified(vector.vector.add(0, 0.04f, 0), VectorData.VectorType.Jump));
        }
        existingVelocities.addAll(jumpPossibilities);
    }
}