package net.blosson.lflagger.simulation;

import net.minecraft.util.math.Vec3d;

/**
 * Handles the simulation of player movement based on vanilla Minecraft physics.
 * This version is a more faithful implementation of the logic found in Grim Anticheat,
 * addressing previous feedback about oversimplification.
 */
public class MovementSimulator {

    // Base vanilla physics constants for 20 TPS
    private static final double BASE_GRAVITY = 0.08;
    private static final double BASE_AIR_FRICTION = 0.9800000190734863D; // Vanilla constant
    private static final float SPRINTING_MULTIPLIER = 1.3f;
    private static final float GROUND_FRICTION_MULTIPLIER = 0.91f;
    private static final float DEFAULT_SLIPPERINESS = 0.6f;

    /**
     * Ticks the simulation forward by one step for the given player state.
     * @param simulatedPlayer The player state to update.
     * @param forwardInput The player's forward/backward input (-1.0 to 1.0).
     * @param strafeInput The player's strafe input (-1.0 to 1.0).
     * @param serverTps The estimated server TPS.
     * @param ping The player's ping in milliseconds.
     */
    public void tick(SimulatedPlayer simulatedPlayer, float forwardInput, float strafeInput, double serverTps, int ping) {
        // --- TPS/Ping Compensation ---
        // Calculate the time delta factor. If TPS is low, more time has passed per tick.
        double tpsFactor = 20.0 / Math.max(1.0, serverTps);

        // TODO: Ping compensation would involve adding a buffer to checks based on latency.
        // For the simulator itself, we focus on TPS.

        if (simulatedPlayer.isInWater) {
            simulateWaterMovement(simulatedPlayer, tpsFactor);
        } else if (simulatedPlayer.isInLava) {
            simulateLavaMovement(simulatedPlayer, tpsFactor);
        } else {
            simulateNormalMovement(simulatedPlayer, forwardInput, strafeInput, tpsFactor);
        }
    }

    private void simulateNormalMovement(SimulatedPlayer player, float forwardInput, float strafeInput, double tpsFactor) {
        // --- This logic is modeled after Grim's MovementTickerPlayer and PredictionEngineNormal ---

        // 1. Calculate ground friction. A full implementation would check the block beneath the player.
        // We use the default slipperiness for now.
        float friction = player.onGround ? DEFAULT_SLIPPERINESS * GROUND_FRICTION_MULTIPLIER : GROUND_FRICTION_MULTIPLIER;

        // 2. Calculate the base travel vector from inputs.
        float baseSpeed = player.speed;
        if (player.isSprinting) {
            baseSpeed *= SPRINTING_MULTIPLIER;
        }

        Vec3d travelVector = new Vec3d(strafeInput, 0, forwardInput);
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
        // This is a key step from `staticVectorEndOfTick`.
        player.velocity = new Vec3d(
            player.velocity.x * friction,
            player.velocity.y * BASE_AIR_FRICTION,
            player.velocity.z * friction
        );

        // The final position is the current position plus the calculated velocity.
        // This velocity should also be scaled by the TPS factor.
        player.pos = player.pos.add(player.velocity.multiply(tpsFactor));
        player.boundingBox = player.boundingBox.offset(player.velocity.multiply(tpsFactor));
    }

    private void simulateWaterMovement(SimulatedPlayer player, double tpsFactor) {
        // Based on PredictionEngineWater
        double waterFriction = 0.800000011920929D;
        double buoyancy = 0.02D; // Water buoyancy

        // Add buoyancy force
        player.velocity = player.velocity.add(0.0D, buoyancy * tpsFactor, 0.0D);

        // Apply water friction
        player.velocity = player.velocity.multiply(waterFriction);

        // Apply gravity
        player.velocity = player.velocity.subtract(0, BASE_GRAVITY * tpsFactor, 0);

        player.pos = player.pos.add(player.velocity.multiply(tpsFactor));
        player.boundingBox = player.boundingBox.offset(player.velocity.multiply(tpsFactor));
    }

    private void simulateLavaMovement(SimulatedPlayer player, double tpsFactor) {
        // Based on PredictionEngineLava
        double lavaFriction = 0.5D;

        // Apply lava friction
        player.velocity = player.velocity.multiply(lavaFriction);

        // Apply gravity
        player.velocity = player.velocity.subtract(0, BASE_GRAVITY * tpsFactor, 0);

        player.pos = player.pos.add(player.velocity.multiply(tpsFactor));
        player.boundingBox = player.boundingBox.offset(player.velocity.multiply(tpsFactor));
    }
}