package net.blosson.lflagger.simulation;

import net.minecraft.block.Block;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Handles the simulation of player movement based on vanilla Minecraft physics.
 * This class aims to predict a player's next position by applying forces like
 * gravity, friction, and player input to their current state. The simulation is
 * compensated for server TPS to remain accurate under lag.
 * <p>
 * The physics logic is modeled after principles found in anti-cheat systems like
 * Grim Anticheat to ensure a faithful prediction of vanilla movement.
 */
public class MovementSimulator {

    // REFACTOR: Javadoc added to explain the purpose of these constants.
    /** The base gravitational acceleration per tick. */
    private static final double BASE_GRAVITY = 0.08;
    /** The friction factor applied to velocity each tick when airborne. */
    private static final double BASE_AIR_FRICTION = 0.9800000190734863D;
    /** The multiplier applied to speed when sprinting. */
    private static final float SPRINTING_MULTIPLIER = 1.3f;
    /** The multiplier applied to ground friction. */
    private static final float GROUND_FRICTION_MULTIPLIER = 0.91f;
    /** The default slipperiness factor of a standard block. */
    private static final float DEFAULT_SLIPPERINESS = 0.6f;

    /**
     * Ticks the simulation forward by one step for the given player state.
     * It compensates for server performance (TPS) and selects the appropriate
     * physics model (e.g., normal, water, lava) based on the player's environment.
     *
     * @param simulatedPlayer The player state to update. This object will be modified.
     * @param forwardInput The player's forward/backward input, from -1.0 to 1.0.
     * @param strafeInput The player's strafe input, from -1.0 to 1.0.
     * @param serverTps The estimated server Ticks Per Second, used for lag compensation.
     * @param ping The player's ping in milliseconds. (Currently unused, reserved for future enhancements).
     */
    public void tick(PlayerEntity player, SimulatedPlayer simulatedPlayer, float forwardInput, float strafeInput, double serverTps, int ping) {
        // --- TPS/Ping Compensation ---
        // Calculate the time delta factor. If TPS is low, more time has passed per tick,
        // so we must scale physics calculations accordingly.
        double tpsFactor = 20.0 / Math.max(1.0, serverTps);

        // TODO: Ping compensation would involve adding a buffer to checks based on latency.
        // For the simulator itself, we focus on TPS.

        if (simulatedPlayer.isInWater) {
            simulateWaterMovement(simulatedPlayer, tpsFactor);
        } else if (simulatedPlayer.isInLava) {
            simulateLavaMovement(simulatedPlayer, tpsFactor);
        } else {
            simulateNormalMovement(player, simulatedPlayer, forwardInput, strafeInput, tpsFactor);
        }
    }

    /**
     * Simulates movement for a player under normal (on land or in air) conditions.
     *
     * @param player The player state to simulate.
     * @param forwardInput The forward/backward input.
     * @param strafeInput The left/right input.
     * @param tpsFactor The TPS compensation factor.
     */
    private void simulateNormalMovement(PlayerEntity realPlayer, SimulatedPlayer player, float forwardInput, float strafeInput, double tpsFactor) {
        // --- This logic is modeled after Grim's MovementTickerPlayer and PredictionEngineNormal ---

        // 1. Calculate ground friction, taking the actual block into account.
        Block groundBlock = realPlayer.getEntityWorld().getBlockState(realPlayer.getBlockPos().down()).getBlock();
        float slipperiness = groundBlock.getSlipperiness();
        float friction = player.onGround ? slipperiness * GROUND_FRICTION_MULTIPLIER : GROUND_FRICTION_MULTIPLIER;

        // 2. Calculate the base travel vector from inputs, adjusted for status effects.
        float baseSpeed = player.speed;

        // Apply Speed/Slowness effects
        if (realPlayer.hasStatusEffect(StatusEffects.SPEED)) {
            int amplifier = realPlayer.getStatusEffect(StatusEffects.SPEED).getAmplifier();
            baseSpeed *= 1.0 + (0.2 * (amplifier + 1));
        }
        if (realPlayer.hasStatusEffect(StatusEffects.SLOWNESS)) {
            int amplifier = realPlayer.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier();
            baseSpeed *= 1.0 - (0.15 * (amplifier + 1));
        }

        if (player.isSprinting) {
            baseSpeed *= SPRINTING_MULTIPLIER;
        }

        // Apply Jump Boost effect to vertical movement
        if (realPlayer.hasStatusEffect(StatusEffects.JUMP_BOOST) && !player.onGround) {
            int amplifier = realPlayer.getStatusEffect(StatusEffects.JUMP_BOOST).getAmplifier();
            player.velocity = player.velocity.add(0, (amplifier + 1) * 0.1, 0);
        }

        Vec3d travelVector = new Vec3d(strafeInput, 0, forwardInput);
        // Normalize to prevent diagonal movement from being faster.
        if (travelVector.lengthSquared() > 1.0) {
            travelVector = travelVector.normalize();
        }
        travelVector = travelVector.multiply(baseSpeed);

        // 3. Apply gravity if airborne. This is scaled by the TPS factor.
        if (!player.onGround) {
            player.velocity = player.velocity.subtract(0, BASE_GRAVITY * tpsFactor, 0);
        }

        // 4. Add the acceleration from player input to the current velocity.
        player.velocity = player.velocity.add(travelVector);

        // 5. Apply end-of-tick friction.
        // This simulates air and ground drag, slowing the player down over time.
        player.velocity = new Vec3d(
            player.velocity.x * friction,
            player.velocity.y * BASE_AIR_FRICTION, // Vertical velocity has its own friction (air drag).
            player.velocity.z * friction
        );

        // 6. Update the player's position based on the final calculated velocity.
        // The final velocity is also scaled by the TPS factor to account for tick duration.
        player.pos = player.pos.add(player.velocity.multiply(tpsFactor));
        player.boundingBox = player.boundingBox.offset(player.velocity.multiply(tpsFactor));
    }

    /**
     * Simulates movement for a player inside water.
     *
     * @param player The player state to simulate.
     * @param tpsFactor The TPS compensation factor.
     */
    private void simulateWaterMovement(SimulatedPlayer player, double tpsFactor) {
        // Based on PredictionEngineWater
        double waterFriction = 0.800000011920929D;
        double buoyancy = 0.02D; // Water buoyancy force

        // Add buoyancy force, pushing the player upwards.
        player.velocity = player.velocity.add(0.0D, buoyancy * tpsFactor, 0.0D);

        // Apply water friction (drag), which is much higher than air friction.
        player.velocity = player.velocity.multiply(waterFriction);

        // Apply gravity, which is counteracted by buoyancy.
        player.velocity = player.velocity.subtract(0, BASE_GRAVITY * tpsFactor, 0);

        player.pos = player.pos.add(player.velocity.multiply(tpsFactor));
        player.boundingBox = player.boundingBox.offset(player.velocity.multiply(tpsFactor));
    }

    /**
     * Simulates movement for a player inside lava.
     *
     * @param player The player state to simulate.
     * @param tpsFactor The TPS compensation factor.
     */
    private void simulateLavaMovement(SimulatedPlayer player, double tpsFactor) {
        // Based on PredictionEngineLava
        double lavaFriction = 0.5D; // Lava is extremely viscous.

        // Apply lava friction (drag).
        player.velocity = player.velocity.multiply(lavaFriction);

        // Apply gravity.
        player.velocity = player.velocity.subtract(0, BASE_GRAVITY * tpsFactor, 0);

        player.pos = player.pos.add(player.velocity.multiply(tpsFactor));
        player.boundingBox = player.boundingBox.offset(player.velocity.multiply(tpsFactor));
    }
}