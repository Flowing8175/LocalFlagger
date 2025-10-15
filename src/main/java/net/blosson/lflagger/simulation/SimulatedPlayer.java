package net.blosson.lflagger.simulation;

import net.blosson.lflagger.util.object.ObjectPool;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * A mutable data object that represents a snapshot of a player's state for physics simulation.
 * <p>
 * It holds all data relevant for physics calculations, such as position, velocity, and various
 * movement-related flags. Instances of this class are designed to be managed by an {@link ObjectPool}
 * to reduce memory allocation. The {@link #reset(PlayerEntity)} method is used to re-initialize
 * an existing object with fresh data from a real player, making it ready for a new simulation tick.
 */
public class SimulatedPlayer {

    // Core movement properties
    public Vec3d pos;
    public Vec3d lastPos;
    public Vec3d velocity;
    public float yaw;

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
    public double fallDistance;
    public Box boundingBox;
    public float speed; // The player's base movement speed attribute
    public java.util.Map<net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect>, net.minecraft.entity.effect.StatusEffectInstance> activeStatusEffects;

    /**
     * Default constructor for use with an {@link ObjectPool} factory.
     */
    public SimulatedPlayer() {}

    /**
     * Creates a new SimulatedPlayer instance by capturing the state from a real player entity.
     * This is a convenience constructor that immediately calls {@link #reset(PlayerEntity)}.
     * @param player The player entity to base the simulation on.
     */
    public SimulatedPlayer(PlayerEntity player) {
        reset(player);
    }

    /**
     * Resets the state of this object to match the given player's current state.
     * <p>
     * This method is the cornerstone of the object pooling mechanism. It allows a single
     * {@code SimulatedPlayer} instance to be recycled and reused for different players
     * or different ticks, avoiding the performance cost of new object allocations.
     *
     * @param player The player entity from which to capture the current state.
     */
    public void reset(PlayerEntity player) {
        // Capture the player's current physical state
        this.pos = player.getEntityPos();
        this.lastPos = new Vec3d(player.lastX, player.lastY, player.lastZ);
        this.velocity = player.getVelocity();
        this.boundingBox = player.getBoundingBox();

        // Capture movement-related flags
        this.onGround = player.isOnGround();
        this.lastOnGround = player.isOnGround(); // A simplified assumption, sufficient for most checks
        this.isSprinting = player.isSprinting();
        this.isSneaking = player.isSneaking();
        this.isClimbing = player.isClimbing();
        this.isSwimming = player.isSwimming();
        this.isFlying = player.getAbilities().flying;
        this.isInWater = player.isSubmergedInWater();
        this.isInLava = player.isInLava();

        // isJumping is a client-side prediction flag and cannot be reliably obtained from other players.
        // It is assumed to be false for simulation purposes.
        this.isJumping = false;

        // Capture physics attributes
        this.fallDistance = player.fallDistance;
        this.speed = (float) player.getAttributeValue(EntityAttributes.MOVEMENT_SPEED);
        this.yaw = player.getYaw();
        this.activeStatusEffects = player.getActiveStatusEffects();
    }
}