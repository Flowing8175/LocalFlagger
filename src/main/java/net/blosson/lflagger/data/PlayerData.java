package net.blosson.lflagger.data;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class PlayerData {

    public final double x, y, z;
    public final double lastX, lastY, lastZ;
    public final Vec3d velocity;
    public final Box boundingBox;
    public final boolean onGround;
    public final boolean isSprinting;
    public final boolean isSneaking;
    public final boolean isFlying;
    public final boolean isSwimming;
    public final boolean isClimbing;
    public final float fallDistance;
    public final float flySpeed;

    public PlayerData(ClientPlayerEntity player, PlayerData lastData) {
        this.x = player.getX();
        this.y = player.getY();
        this.z = player.getZ();
        if (lastData != null) {
            this.lastX = lastData.x;
            this.lastY = lastData.y;
            this.lastZ = lastData.z;
        } else {
            this.lastX = player.getX();
            this.lastY = player.getY();
            this.lastZ = player.getZ();
        }
        this.velocity = player.getVelocity();
        this.boundingBox = player.getBoundingBox();
        this.onGround = player.isOnGround();
        this.isSprinting = player.isSprinting();
        this.isSneaking = player.isSneaking();
        this.isFlying = player.getAbilities().flying;
        this.isSwimming = player.isSwimming();
        this.isClimbing = player.isClimbing();
        this.fallDistance = (float) player.fallDistance;
        this.flySpeed = player.getAbilities().getFlySpeed();
    }
}