package net.blosson.lflagger.config;

import com.google.gson.annotations.SerializedName;

/**
 * Holds all configurable settings for the mod.
 * <p>
 * This class acts as a data structure that is serialized to and deserialized from
 * the {@code lflagger.json} file by the {@link ConfigManager}. It uses nested static
 * classes to logically group settings for each type of cheat check, which results

 * in a clean and organized JSON structure.
 */
public class ModConfig {

    @SerializedName("fly_check")
    private final FlyCheckConfig flyCheck = new FlyCheckConfig();

    @SerializedName("speed_check")
    private final SpeedCheckConfig speedCheck = new SpeedCheckConfig();

    @SerializedName("no_fall_check")
    private final NoFallCheckConfig noFallCheck = new NoFallCheckConfig();

    @SerializedName("strafe_check")
    private final StrafeCheckConfig strafeCheck = new StrafeCheckConfig();

    @SerializedName("anti_knockback_check")
    private final AntiKnockbackCheckConfig antiKnockbackCheck = new AntiKnockbackCheckConfig();

    @SerializedName("movement_check")
    private final MovementCheckConfig movementCheck = new MovementCheckConfig();

    public FlyCheckConfig getFlyCheck() { return flyCheck; }
    public SpeedCheckConfig getSpeedCheck() { return speedCheck; }
    public NoFallCheckConfig getNoFallCheck() { return noFallCheck; }
    public StrafeCheckConfig getStrafeCheck() { return strafeCheck; }
    public AntiKnockbackCheckConfig getAntiKnockbackCheck() { return antiKnockbackCheck; }
    public MovementCheckConfig getMovementCheck() { return movementCheck; }

    /** Contains settings related to the Fly check. */
    public static class FlyCheckConfig {
        /** If true, the Fly check will be active. */
        @SerializedName("enabled")
        public boolean enabled = true;
        /** A lenient buffer added to predicted vertical movement to prevent false positives. */
        @SerializedName("vertical_leniency")
        public double verticalLeniency = 0.05;
        /** The number of violations a player must accumulate before a flag is triggered. */
        @SerializedName("violation_threshold")
        public int violationThreshold = 10;
    }

    /** Contains settings related to the Speed check. */
    public static class SpeedCheckConfig {
        /** If true, the Speed check will be active. */
        @SerializedName("enabled")
        public boolean enabled = true;
        /** The lenient multiplier applied to the predicted max speed (e.g., 1.05 = 105%). */
        @SerializedName("speed_multiplier_leniency")
        public double speedMultiplierLeniency = 1.05;
        /** A small, flat speed buffer added to the max speed to account for minor inaccuracies. */
        @SerializedName("speed_flat_leniency")
        public double speedFlatLeniency = 0.005;
        /** The number of consecutive ticks a player must be speeding to trigger a flag. */
        @SerializedName("violation_threshold")
        public int violationThreshold = 20; // Default: 1 second
    }

    /** Contains settings related to the NoFall check. */
    public static class NoFallCheckConfig {
        /** If true, the NoFall check will be active. */
        @SerializedName("enabled")
        public boolean enabled = true;
        /** The maximum distance a player can fall without taking damage. */
        @SerializedName("max_fall_distance")
        public double maxFallDistance = 3.0;
        /** The number of violations a player must accumulate before a flag is triggered. */
        @SerializedName("violation_threshold")
        public int violationThreshold = 5;
    }

    /** Contains settings related to the Strafe check. */
    public static class StrafeCheckConfig {
        /** If true, the Strafe check will be active. */
        @SerializedName("enabled")
        public boolean enabled = true;
        /** A small buffer to allow for minor, legitimate mid-air speed changes. */
        @SerializedName("air_strafe_leniency")
        public double airStrafeLeniency = 0.02;
        /** The number of violations a player must accumulate before a flag is triggered. */
        @SerializedName("violation_threshold")
        public int violationThreshold = 5;
    }

    /** Contains settings related to the AntiKnockback check. */
    public static class AntiKnockbackCheckConfig {
        /** If true, the Anti-Knockback check will be active. */
        @SerializedName("enabled")
        public boolean enabled = true;
        /** The velocity magnitude below which a player is considered to have taken no knockback after a hit. */
        @SerializedName("knockback_threshold")
        public double knockbackThreshold = 0.1;
        /** The assumed velocity magnitude of a standard vanilla knockback, used for calculating certainty. */
        @SerializedName("assumed_vanilla_knockback")
        public double assumedVanillaKnockback = 0.4;
    }

    /** Contains settings related to the Movement check. */
    public static class MovementCheckConfig {
        /** If true, the Movement check will be active. */
        @SerializedName("enabled")
        public boolean enabled = true;
        /** The threshold for the distance between predicted and actual velocity. */
        @SerializedName("distance_threshold")
        public double distanceThreshold = 1.0;
    }
}