package net.blosson.lflagger.data;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralizes all stateful data for a single player that needs to be tracked across game ticks.
 * <p>
 * Before this class, each {@code Check} was responsible for its own state management,
 * typically using a {@code Map<UUID, ...>}. This led to scattered data and repeated logic.
 * This class consolidates all that information into a single, cohesive object, managed by the
 * {@code CheckManager}. It holds violation levels, physics-related states like fall distance,
 * and other tick-by-tick data.
 */
public class PlayerState {

    private final PlayerEntity player;

    /** A map to store violation levels for various checks, keyed by the check's name. */
    private final Map<String, Integer> violationLevels = new HashMap<>();

    // REFACTOR: Javadoc added to explain the purpose of these state fields.
    /** The player's on-ground status from the previous tick. Used by NoFallCheck. */
    public boolean wasOnGround;
    /** The player's fall distance from the previous tick. Used by NoFallCheck. */
    public double lastFallDistance;

    /** The number of consecutive ticks the player has been considered speeding. Used by SpeedCheck. */
    public int speedingTicks;

    /** The player's hurt time from the previous tick. Used by AntiKnockbackCheck. */
    public int lastHurtTime;


    /**
     * @return The {@link PlayerEntity} this state belongs to.
     */
    public PlayerEntity getPlayer() {
        return player;
    }

    /**
     * Gets the current violation level for a specific check.
     *
     * @param checkName The name of the check (e.g., from {@code Check.getName()}).
     * @return The current violation level, defaulting to 0 if not present.
     */
    public int getViolationLevel(String checkName) {
        return violationLevels.getOrDefault(checkName, 0);
    }

    /**
     * Increases the violation level for a specific check by a given amount.
     *
     * @param checkName The name of the check.
     * @param amount The positive integer amount to increase by.
     * @return The new violation level.
     */
    public int increaseViolationLevel(String checkName, int amount) {
        int newLevel = getViolationLevel(checkName) + amount;
        violationLevels.put(checkName, newLevel);
        return newLevel;
    }

    /**
     * Increases the violation level for a specific check by 1.
     *
     * @param checkName The name of the check.
     * @return The new violation level.
     */
    public int increaseViolationLevel(String checkName) {
        return increaseViolationLevel(checkName, 1);
    }

    /**
     * Decreases the violation level for a specific check by a given amount.
     * The level will not go below zero.
     *
     * @param checkName The name of the check.
     * @param amount The positive integer amount to decrease by.
     * @return The new violation level.
     */
    public int decreaseViolationLevel(String checkName, int amount) {
        int newLevel = Math.max(0, getViolationLevel(checkName) - amount);
        violationLevels.put(checkName, newLevel);
        return newLevel;
    }

    /**
     * Decreases the violation level for a specific check by 1.
     * The level will not go below zero.
     *
     * @param checkName The name of the check.
     * @return The new violation level.
     */
    public int decreaseViolationLevel(String checkName) {
        return decreaseViolationLevel(checkName, 1);
    }

    /**
     * Resets the violation level for a specific check to zero.
     *
     * @param checkName The name of the check.
     */
    public void resetViolationLevel(String checkName) {
        violationLevels.put(checkName, 0);
    }

    /**
     * Updates the state data that needs to be tracked from one tick to the next.
     * This method is called by the {@code CheckManager} at the end of each tick for the player,
     * ensuring that data for the next tick's checks (e.g., {@code wasOnGround}) is fresh.
     */
    // REFACTOR: Added fields to track position history for velocity calculation.
    /** The player's position in the last tick. */
    public Vec3d lastPosition;
    /** The timestamp of the last tick update, for calculating time deltas. */
    public long lastTickTime;
    public PlayerData lastPlayerData;
    public Vec3d lastVelocity;

    /**
     * Constructs a new PlayerState object, capturing the initial state from the player entity.
     * @param player The player this state object belongs to.
     */
    public PlayerState(PlayerEntity player) {
        this.player = player;
        this.wasOnGround = player.isOnGround();
        this.lastFallDistance = player.fallDistance;
        this.lastHurtTime = player.hurtTime;
        this.speedingTicks = 0;
        // Initialize historical data
        this.lastPosition = player.getEntityPos();
        this.lastTickTime = System.currentTimeMillis();
        this.lastPlayerData = null;
        this.lastVelocity = Vec3d.ZERO;
    }

    /**
     * Calculates the player's velocity based on the change in position since the last tick.
     * This is crucial for checks on remote players where {@code getVelocity()} is unreliable.
     * @return The calculated velocity as a {@link Vec3d}. Returns a zero vector if the time delta is zero.
     */
    public Vec3d getCalculatedVelocity() {
        long timeDelta = System.currentTimeMillis() - lastTickTime;
        if (timeDelta > 0) {
            Vec3d currentPos = player.getEntityPos();
            return new Vec3d(
                (currentPos.x - lastPosition.x) * 1000.0 / timeDelta,
                (currentPos.y - lastPosition.y) * 1000.0 / timeDelta,
                (currentPos.z - lastPosition.z) * 1000.0 / timeDelta
            );
        }
        return Vec3d.ZERO; // Avoid division by zero
    }

    /**
     * Updates the state data that needs to be tracked from one tick to the next.
     * This method is called by the {@code CheckManager} at the end of each tick for the player,
     * ensuring that data for the next tick's checks (e.g., {@code wasOnGround}) is fresh.
     */
    public void update() {
        this.wasOnGround = player.isOnGround();
        this.lastFallDistance = player.fallDistance;
        this.lastHurtTime = player.hurtTime;
        // Update historical data for the next tick's velocity calculation
        this.lastPosition = player.getEntityPos();
        this.lastTickTime = System.currentTimeMillis();
    }
}