package net.blosson.lflagger.physics;

import net.minecraft.util.math.Vec3d;

/**
 * A record to store the result of a movement prediction.
 *
 * @param predictedPosition The predicted position of the player.
 * @param tolerance         The acceptable error margin for this prediction, in squared units.
 */
public record PredictionResult(Vec3d predictedPosition, double tolerance) {
}