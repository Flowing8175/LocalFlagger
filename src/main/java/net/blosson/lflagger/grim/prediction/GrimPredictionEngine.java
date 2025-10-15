package net.blosson.lflagger.grim.prediction;

import net.blosson.lflagger.physics.Collisions;
import net.blosson.lflagger.util.grim.GrimMath;
import net.blosson.lflagger.util.grim.JumpPower;
import net.minecraft.block.BlockState;
import net.minecraft.block.SlimeBlock;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.HashSet;
import java.util.Set;

public class GrimPredictionEngine {

    public static class PredictionResult {
        public final double bestOffset;
        public final Vec3d predictedMovement;
        public final Vec3d nextClientVelocity;

        public PredictionResult(double bestOffset, Vec3d predictedMovement, Vec3d nextClientVelocity) {
            this.bestOffset = bestOffset;
            this.predictedMovement = predictedMovement;
            this.nextClientVelocity = nextClientVelocity;
        }
    }

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

    protected Set<VectorData> generatePossibleStartVelocities(PlayerEntity player, Vec3d lastVelocity) {
        Set<VectorData> velocities = new HashSet<>();
        velocities.add(new VectorData(lastVelocity));
        addJumpsToPossibilities(player, velocities);
        return velocities;
    }

    protected void addJumpsToPossibilities(PlayerEntity player, Set<VectorData> existingVelocities) {
        if (!player.isOnGround()) return;
        Set<VectorData> jumpPossibilities = new HashSet<>();
        for (VectorData vector : existingVelocities) {
            Vec3d jumpVec = JumpPower.jumpFromGround(player, vector.vector);
            jumpPossibilities.add(vector.returnNewModified(jumpVec, VectorData.VectorType.Jump));
        }
        existingVelocities.addAll(jumpPossibilities);
    }

    protected Set<VectorData> applyInputsToVelocityPossibilities(PlayerEntity player, Set<VectorData> possibleVectors) {
        Set<VectorData> newPossibilities = new HashSet<>();
        for (VectorData startingVector : possibleVectors) {
            for (int f = -1; f <= 1; f++) {
                for (int s = -1; s <= 1; s++) {
                    if (f == 0 && s == 0) {
                        newPossibilities.add(startingVector);
                        continue;
                    }
                    if (f <= 0 && player.isSprinting()) continue;

                    Vec3d input = transformInputsToVector(player, new Vec3d(s, 0, f));
                    newPossibilities.add(startingVector.returnNewModified(startingVector.vector.add(input), VectorData.VectorType.InputResult));
                }
            }
        }
        return newPossibilities;
    }

    protected Vec3d applyEndOfTickPhysics(PlayerEntity player, Vec3d velocity) {
        BlockPos blockPos = player.getBlockPos().down();
        BlockState blockBelow = player.getEntityWorld().getBlockState(blockPos);

        if (player.isOnGround() && blockBelow.getBlock() instanceof SlimeBlock) {
            // This is still a placeholder, but the structure is now more correct.
        } else {
            velocity = velocity.multiply(getFriction(player, blockBelow), 1.0, getFriction(player, blockBelow));
        }

        if (player.hasNoGravity()) {
            velocity = velocity.multiply(1.0, 0.98, 1.0);
        } else {
            velocity = velocity.add(0, -0.08, 0);
            velocity = velocity.multiply(1.0, 0.9800000190734863D, 1.0);
        }

        if (player.hasStatusEffect(StatusEffects.LEVITATION)) {
            int levitationAmplifier = player.getStatusEffect(StatusEffects.LEVITATION).getAmplifier();
            double levitationMotion = (0.05 * (double)(levitationAmplifier + 1) - velocity.y) * 0.2;
            velocity = velocity.add(0, levitationMotion, 0);
        }

        if (player.isSneaking()) {
            velocity = velocity.multiply(0.3, 1.0, 0.3);
        }

        return velocity;
    }

    protected Vec3d transformInputsToVector(PlayerEntity player, Vec3d theoreticalInput) {
        float speed = (float) player.getMovementSpeed();
        if (player.isSprinting()) {
            speed *= 1.3f;
        }

        float forward = (float) theoreticalInput.z;
        float strafe = (float) theoreticalInput.x;

        float f = strafe * strafe + forward * forward;
        if (f >= 1.0E-4F) {
            f = GrimMath.sqrt(f);
            if (f < 1.0F) f = 1.0F;
            f = speed / f;
            strafe *= f;
            forward *= f;

            float yawRad = player.getYaw() * 0.017453292F;
            float sinYaw = GrimMath.sin(yawRad);
            float cosYaw = GrimMath.cos(yawRad);

            double x = (double)(strafe * cosYaw - forward * sinYaw);
            double z = (double)(forward * cosYaw + strafe * sinYaw);
            return new Vec3d(x, 0.0, z);
        }
        return Vec3d.ZERO;
    }

    protected float getFriction(PlayerEntity player, BlockState blockBelow) {
        return blockBelow.getBlock().getSlipperiness() * 0.91f;
    }
}