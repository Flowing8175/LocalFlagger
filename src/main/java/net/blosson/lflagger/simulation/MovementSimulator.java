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
     * This implementation correctly follows the Grim Anticheat physics model,
     * ensuring momentum is preserved and forces are applied in the correct order.
     *
     * @param realPlayer The actual PlayerEntity, used to query status effects and world state.
     * @param player The SimulatedPlayer object representing the player's state.
     * @param forwardInput The player's forward/backward input.
     * @param strafeInput The player's left/right input.
     * @param tpsFactor The TPS compensation factor.
     */
    private void simulateNormalMovement(PlayerEntity realPlayer, SimulatedPlayer player, float forwardInput, float strafeInput, double tpsFactor) {
        // --- Corrected Grim Anticheat Physics Implementation ---

        // 1. Calculate acceleration from player input, adjusted for status effects.
        Vec3d travelVector = getTravelVector(realPlayer, player, forwardInput, strafeInput);
        player.velocity = player.velocity.add(travelVector);

        // 2. Apply gravity if airborne.
        if (!player.onGround) {
            player.velocity = player.velocity.subtract(0, BASE_GRAVITY * tpsFactor, 0);
        }

        // 3. Apply friction. Grim's model applies ground friction to X/Z and air drag to Y.
        float groundFriction = getGroundFriction(realPlayer);
        player.velocity = new Vec3d(
            player.velocity.x * groundFriction,
            player.velocity.y * BASE_AIR_FRICTION, // Air friction is always applied to Y.
            player.velocity.z * groundFriction
        );

        // 4. Update position based on the final, calculated velocity.
        player.pos = player.pos.add(player.velocity.multiply(tpsFactor));
        player.boundingBox = player.boundingBox.offset(player.velocity.multiply(tpsFactor));
    }

    /**
     * Calculates the friction of the block the player is standing on.
     */
    private float getGroundFriction(PlayerEntity realPlayer) {
        Block groundBlock = realPlayer.getEntityWorld().getBlockState(realPlayer.getBlockPos().down()).getBlock();
        return groundBlock.getSlipperiness() * GROUND_FRICTION_MULTIPLIER;
    }

    /**
     * Calculates the acceleration vector from player input and status effects.
     */
    private Vec3d getTravelVector(PlayerEntity realPlayer, SimulatedPlayer player, float forwardInput, float strafeInput) {
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

        Vec3d travelVector = new Vec3d(strafeInput, 0, forwardInput);
        if (travelVector.lengthSquared() > 1.0) {
            travelVector = travelVector.normalize();
        }
        return travelVector.multiply(baseSpeed);
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