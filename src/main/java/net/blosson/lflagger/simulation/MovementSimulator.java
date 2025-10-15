package net.blosson.lflagger.simulation;

import net.blosson.lflagger.util.grim.GrimMath;
import net.minecraft.block.Block;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class MovementSimulator {

    private static final float SPRINTING_MULTIPLIER = 1.3f;
    private static final float GROUND_FRICTION_MULTIPLIER = 0.91f;
    private static final double BASE_GRAVITY = 0.08;
    private static final double BASE_AIR_FRICTION = 0.9800000190734863D;

    public void simulate(PlayerEntity realPlayer, SimulatedPlayer player, float forward, float strafe) {
        float friction = player.onGround ? getBlockFriction(realPlayer) * GROUND_FRICTION_MULTIPLIER : GROUND_FRICTION_MULTIPLIER;

        // This is a direct port of Grim's moveFlying method
        final float[] speed = {(float) player.speed};
        player.activeStatusEffects.forEach((statusEffect, statusEffectInstance) -> {
            if (statusEffect.value().equals(StatusEffects.SPEED)) {
                int amplifier = statusEffectInstance.getAmplifier();
                speed[0] *= 1.0 + (0.2 * (amplifier + 1));
            }
            if (statusEffect.value().equals(StatusEffects.SLOWNESS)) {
                int amplifier = statusEffectInstance.getAmplifier();
                speed[0] *= 1.0 - (0.15 * (amplifier + 1));
            }
        });
        if (player.isSprinting) {
            speed[0] *= SPRINTING_MULTIPLIER;
        }

        Vec3d travelVector = new Vec3d(strafe, 0, forward);
        if (travelVector.lengthSquared() > 1.0) {
            travelVector = travelVector.normalize();
        }

        float yawRad = player.yaw * 0.017453292F;
        float sinYaw = GrimMath.sin(yawRad);
        float cosYaw = GrimMath.cos(yawRad);

        double x = (travelVector.x * cosYaw - travelVector.z * sinYaw) * speed[0];
        double z = (travelVector.z * cosYaw + travelVector.x * sinYaw) * speed[0];

        player.velocity = player.velocity.add(x, 0, z);

        // End of tick physics
        if (!player.onGround) {
            player.velocity = player.velocity.subtract(0, BASE_GRAVITY, 0);
        }

        player.velocity = new Vec3d(
            player.velocity.x * friction,
            player.velocity.y,
            player.velocity.z * friction
        );
        player.velocity = player.velocity.multiply(1.0, BASE_AIR_FRICTION, 1.0);

        player.pos = player.pos.add(player.velocity);
        player.boundingBox = player.boundingBox.offset(player.velocity);
    }

    private float getBlockFriction(PlayerEntity player) {
        Block groundBlock = player.getEntityWorld().getBlockState(player.getBlockPos().down()).getBlock();
        return groundBlock.getSlipperiness();
    }
}