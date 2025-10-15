package net.blosson.lflagger.grim.prediction;

import net.blosson.lflagger.physics.Collisions;
import net.blosson.lflagger.util.grim.GrimMath;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import java.util.HashSet;
import java.util.Set;

public class PredictionEngineWater extends GrimPredictionEngine {

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