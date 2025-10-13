package net.blosson.lflagger.data;

import net.minecraft.util.math.Vec3d;

/**
 * A simple record to store a player's position at a specific timestamp.
 *
 * @param timestamp The time the position was recorded (in milliseconds).
 * @param position  The position of the player.
 */
public record PositionSnapshot(long timestamp, Vec3d position) {
}