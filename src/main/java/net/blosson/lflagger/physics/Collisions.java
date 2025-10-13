package net.blosson.lflagger.physics;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

import java.util.List;
import java.util.stream.StreamSupport;

public class Collisions {

    public static Vec3d collide(PlayerEntity player, Vec3d velocity) {
        World world = player.getWorld();
        Box playerBox = player.getBoundingBox();
        List<Box> collisionBoxes = StreamSupport.stream(world.getCollisions(player, playerBox.stretch(velocity)).spliterator(), false)
                .flatMap(voxelShape -> voxelShape.getBoundingBoxes().stream())
                .toList();

        if (collisionBoxes.isEmpty()) {
            return velocity;
        }

        // Iterative collision logic
        double dx = velocity.getX();
        double dy = velocity.getY();
        double dz = velocity.getZ();

        // Y-axis collision
        for (Box box : collisionBoxes) {
            dy = calculateMaxY(playerBox, box, dy);
        }
        playerBox = playerBox.offset(0, dy, 0);

        // X-axis collision
        for (Box box : collisionBoxes) {
            dx = calculateMaxX(playerBox, box, dx);
        }
        playerBox = playerBox.offset(dx, 0, 0);

        // Z-axis collision
        for (Box box : collisionBoxes) {
            dz = calculateMaxZ(playerBox, box, dz);
        }

        // Step-up logic
        boolean collidedHorizontally = velocity.getX() != dx || velocity.getZ() != dz;
        boolean canStep = player.isOnGround();

        if (collidedHorizontally && canStep) {
            Vec3d originalVelocity = new Vec3d(dx, dy, dz);
            Box stepBox = player.getBoundingBox();
            double stepDx = velocity.getX();
            double stepDy = player.getStepHeight();
            double stepDz = velocity.getZ();

            for (Box box : collisionBoxes) {
                stepDy = calculateMaxY(stepBox, box, stepDy);
            }
            stepBox = stepBox.offset(0, stepDy, 0);

            for (Box box : collisionBoxes) {
                stepDx = calculateMaxX(stepBox, box, stepDx);
            }
            stepBox = stepBox.offset(stepDx, 0, 0);

            for (Box box : collisionBoxes) {
                stepDz = calculateMaxZ(stepBox, box, stepDz);
            }

            Vec3d stepUpVelocity = new Vec3d(stepDx, stepDy, stepDz);
            if (stepUpVelocity.horizontalLengthSquared() > originalVelocity.horizontalLengthSquared()) {
                for (Box box : collisionBoxes) {
                    stepUpVelocity = new Vec3d(stepUpVelocity.x, calculateMaxY(stepBox, box, stepUpVelocity.y), stepUpVelocity.z);
                }
                return stepUpVelocity;
            }
        }

        return new Vec3d(dx, dy, dz);
    }

    private static double calculateMaxY(Box playerBox, Box blockBox, double dy) {
        if (playerBox.maxX > blockBox.minX && playerBox.minX < blockBox.maxX && playerBox.maxZ > blockBox.minZ && playerBox.minZ < blockBox.maxZ) {
            if (dy > 0.0 && playerBox.maxY <= blockBox.minY) {
                dy = Math.min(blockBox.minY - playerBox.maxY, dy);
            } else if (dy < 0.0 && playerBox.minY >= blockBox.maxY) {
                dy = Math.max(blockBox.maxY - playerBox.minY, dy);
            }
        }
        return dy;
    }

    private static double calculateMaxX(Box playerBox, Box blockBox, double dx) {
        if (playerBox.maxY > blockBox.minY && playerBox.minY < blockBox.maxY && playerBox.maxZ > blockBox.minZ && playerBox.minZ < blockBox.maxZ) {
            if (dx > 0.0 && playerBox.maxX <= blockBox.minX) {
                dx = Math.min(blockBox.minX - playerBox.maxX, dx);
            } else if (dx < 0.0 && playerBox.minX >= blockBox.maxX) {
                dx = Math.max(blockBox.maxX - playerBox.minX, dx);
            }
        }
        return dx;
    }

    private static double calculateMaxZ(Box playerBox, Box blockBox, double dz) {
        if (playerBox.maxY > blockBox.minY && playerBox.minY < blockBox.maxY && playerBox.maxX > blockBox.minX && playerBox.minX < blockBox.maxX) {
            if (dz > 0.0 && playerBox.maxZ <= blockBox.minZ) {
                dz = Math.min(blockBox.minZ - playerBox.maxZ, dz);
            } else if (dz < 0.0 && playerBox.minZ >= blockBox.maxZ) {
                dz = Math.max(blockBox.maxZ - playerBox.minZ, dz);
            }
        }
        return dz;
    }
}