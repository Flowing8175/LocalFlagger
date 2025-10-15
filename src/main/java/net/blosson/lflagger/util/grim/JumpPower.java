package net.blosson.lflagger.util.grim;

import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class JumpPower {

    public static Vec3d jumpFromGround(PlayerEntity player, Vec3d velocity) {
        double jumpPower = 0.42F;
        if (player.hasStatusEffect(StatusEffects.JUMP_BOOST)) {
            jumpPower += 0.1F * (player.getStatusEffect(StatusEffects.JUMP_BOOST).getAmplifier() + 1);
        }
        return velocity.add(0, jumpPower, 0);
    }
}