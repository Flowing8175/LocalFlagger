package net.blosson.lflagger.physics;

import net.blosson.lflagger.data.PlayerData;
import net.blosson.lflagger.data.PlayerState;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.SoulSandBlock;
import net.minecraft.block.SlimeBlock;
import net.minecraft.block.CobwebBlock;
import net.blosson.lflagger.physics.PlayerInput;

import java.util.ArrayList;
import java.util.List;

public class PredictionEngineNormal extends PredictionEngine {

    @Override
    public PredictionResult predictNextPosition(PlayerData data) {
        // This method is not used by the MovementCheck, but it must be implemented.
        return new PredictionResult(data.position, 0.0);
    }

    @Override
    public PredictionResult guessBestMovement(PlayerEntity player, PlayerState state) {
        List<PlayerInput> possibleInputs = generatePossibleInputs(player, state);
        Vec3d bestPredictedPosition = player.getEntityPos();
        double minDistance = Double.MAX_VALUE;

        for (PlayerInput input : possibleInputs) {
            Vec3d predictedPosition = simulateTick(player, state, input);
            double distance = predictedPosition.distanceTo(player.getEntityPos());
            if (distance < minDistance) {
                minDistance = distance;
                bestPredictedPosition = predictedPosition;
            }
        }

        return new PredictionResult(bestPredictedPosition, minDistance);
    }

    private Vec3d simulateTick(PlayerEntity player, PlayerState state, PlayerInput input) {
        Vec3d velocity = state.getCalculatedVelocity();

        // Apply friction
        BlockPos groundBlockPos = BlockPos.ofFloored(player.getX(), player.getY() - 0.1, player.getZ());
        boolean onIce = player.getEntityWorld().getBlockState(groundBlockPos).isOf(Blocks.ICE) ||
                        player.getEntityWorld().getBlockState(groundBlockPos).isOf(Blocks.PACKED_ICE) ||
                        player.getEntityWorld().getBlockState(groundBlockPos).isOf(Blocks.FROSTED_ICE);
        double friction = onIce ? PhysicsConstants.ICE_FRICTION : 0.91;

        if (player.getEntityWorld().getBlockState(groundBlockPos).getBlock() instanceof SoulSandBlock) {
            friction *= PhysicsConstants.SOUL_SAND_FRICTION_MULTIPLIER;
        }
        if (player.getEntityWorld().getBlockState(groundBlockPos).getBlock() instanceof SlimeBlock) {
            friction = PhysicsConstants.SLIME_BLOCK_FRICTION;
        }


        velocity = new Vec3d(velocity.x * friction, velocity.y, velocity.z * friction);


        // Apply input
        float speed = player.getMovementSpeed();
        if (input.sneaking) {
            speed *= 0.3f;
        }
        if (player.isSprinting()) {
            speed *= PhysicsConstants.SPRINTING_MULTIPLIER;
        }
        if (player.hasStatusEffect(StatusEffects.SPEED)) {
            speed *= 1.0 + (PhysicsConstants.SPEED_EFFECT_MULTIPLIER * (player.getStatusEffect(StatusEffects.SPEED).getAmplifier() + 1));
        }
        if (player.hasStatusEffect(StatusEffects.SLOWNESS)) {
            speed *= 1.0 - (PhysicsConstants.SLOWNESS_EFFECT_MULTIPLIER * (player.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() + 1));
        }

        Vec3d inputVector = new Vec3d(input.strafe, 0, input.forward).multiply(speed);
        velocity = velocity.add(inputVector.rotateY(-player.getYaw() * ((float)Math.PI / 180F)));


        // Apply gravity
        velocity = velocity.subtract(0, PhysicsConstants.GRAVITY, 0);

        if (player.getEntityWorld().getBlockState(new BlockPos((int)player.getX(), (int)player.getY(), (int)player.getZ())).getBlock() instanceof CobwebBlock) {
            velocity = velocity.multiply(PhysicsConstants.COBWEB_FRICTION);
        }


        // Apply air drag
        velocity = velocity.multiply(1.0, PhysicsConstants.AIR_DRAG, 1.0);

        // Apply status effects
        if (input.jumping && player.isOnGround()) {
             velocity = velocity.add(0, PhysicsConstants.JUMP_VERTICAL_SPEED, 0);
        }
        if (player.hasStatusEffect(StatusEffects.JUMP_BOOST)) {
            velocity = velocity.add(0, (player.getStatusEffect(StatusEffects.JUMP_BOOST).getAmplifier() + 1) * PhysicsConstants.JUMP_BOOST_MULTIPLIER, 0);
        }
        if (player.hasStatusEffect(StatusEffects.LEVITATION)) {
            velocity = new Vec3d(velocity.x, (PhysicsConstants.LEVITATION_MULTIPLIER * (player.getStatusEffect(StatusEffects.LEVITATION).getAmplifier() + 1) - velocity.y) * PhysicsConstants.LEVITATION_DRAG, velocity.z);
        }


        // Collide with world
        velocity = Collisions.collide(player, velocity);

        return player.getEntityPos().add(velocity);
    }

    private List<PlayerInput> generatePossibleInputs(PlayerEntity player, PlayerState state) {
        List<PlayerInput> inputs = new ArrayList<>();

        inputs.add(new PlayerInput(0, 0, false, false)); // No input
        inputs.add(new PlayerInput(1, 0, false, false)); // Forward
        inputs.add(new PlayerInput(-1, 0, false, false)); // Backward
        inputs.add(new PlayerInput(0, 1, false, false)); // Strafe Right
        inputs.add(new PlayerInput(0, -1, false, false)); // Strafe Left
        inputs.add(new PlayerInput(1, 1, false, false)); // Forward-Right
        inputs.add(new PlayerInput(1, -1, false, false)); // Forward-Left
        inputs.add(new PlayerInput(-1, 1, false, false)); // Backward-Right
        inputs.add(new PlayerInput(-1, -1, false, false)); // Backward-Left

        // Add jumping variations
        for (int i = 0, n = inputs.size(); i < n; i++) {
            PlayerInput input = inputs.get(i);
            inputs.add(new PlayerInput(input.forward, input.strafe, true, input.sneaking));
        }

        // Add sneaking variations
        for (int i = 0, n = inputs.size(); i < n; i++) {
            PlayerInput input = inputs.get(i);
            inputs.add(new PlayerInput(input.forward, input.strafe, input.jumping, true));
        }

        return inputs;
    }
}