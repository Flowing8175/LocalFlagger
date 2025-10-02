package net.blosson.lflagger.simulation;

import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Represents the state of a player being simulated. It holds all relevant
 * data for physics calculations, such as position, velocity, and various
 * movement-related flags.
 */
public class SimulatedPlayer {

    // Core movement properties
    public Vec3d pos;
    public Vec3d lastPos;
    public Vec3d velocity;

    // Movement state flags
    public boolean onGround;
    public boolean lastOnGround;
    public boolean isSprinting;
    public boolean isSneaking;
    public boolean isJumping;
    public boolean isClimbing;
    public boolean isSwimming;
    public boolean isFlying;
    public boolean isInWater;
    public boolean isInLava;

    // Physics attributes
    public double fallDistance; // Changed to double
    public Box boundingBox;
    public float speed; // Movement speed attribute

    /**
     * Creates a new SimulatedPlayer instance, capturing the state from a real player entity.
     * @param player The player entity to base the simulation on.
     */
    public SimulatedPlayer(PlayerEntity player) {
        // Capture initial state from the target player
        this.pos = player.getPos();
        this.lastPos = new Vec3d(player.lastX, player.lastY, player.lastZ); // Updated from getPrevPos()
        this.velocity = player.getVelocity();
        this.onGround = player.isOnGround();
        this.lastOnGround = player.isOnGround();
        this.isSprinting = player.isSprinting();
        this.isSneaking = player.isSneaking();
        // isJumping is client-side only, so we can't get it from other players directly.
        // This will need to be inferred or handled differently in the simulation.
        this.isJumping = false;
        this.isClimbing = player.isClimbing();
        this.isSwimming = player.isSwimming();
        this.isFlying = player.getAbilities().flying;
        this.isInWater = player.isSubmergedInWater(); // Updated from isInsideWaterOrBubbleColumn
        this.isInLava = player.isInLava();
        this.fallDistance = player.fallDistance;
        this.boundingBox = player.getBoundingBox();
        this.speed = (float) player.getAttributeValue(EntityAttributes.MOVEMENT_SPEED); // Updated from getSpeed()
    }
}