package ac.grim.grimac.utils.nmsutil;

import ac.grim.grimac.events.packets.PacketWorldBorder;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.chunks.Column;
import ac.grim.grimac.utils.collisions.CollisionData;
import ac.grim.grimac.utils.collisions.datatypes.CollisionBox;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.Pair;
import ac.grim.grimac.utils.data.VectorData;
import ac.grim.grimac.utils.data.tags.SyncedTags;
import ac.grim.grimac.utils.latency.CompensatedWorld;
import ac.grim.grimac.utils.math.GrimMath;
import ac.grim.grimac.utils.math.Location;
import ac.grim.grimac.utils.math.Vector3dm;
import ac.grim.grimac.utils.math.VectorUtils;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.potion.PotionTypes;
import com.github.retrooper.packetevents.protocol.world.Direction;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.floats.FloatArraySet;
import it.unimi.dsi.fastutil.floats.FloatArrays;
import it.unimi.dsi.fastutil.floats.FloatSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

@UtilityClass
public final class Collisions {
    private static final double COLLISION_EPSILON = 1.0E-7;

    private static final boolean IS_FOURTEEN = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_14); // Optimization for chunks with empty block count
    private static final List<List<Axis>> allAxisCombinations = Arrays.asList(
            Arrays.asList(Axis.Y, Axis.X, Axis.Z),
            Arrays.asList(Axis.Y, Axis.Z, Axis.X),

            Arrays.asList(Axis.X, Axis.Y, Axis.Z),
            Arrays.asList(Axis.X, Axis.Z, Axis.Y),

            Arrays.asList(Axis.Z, Axis.X, Axis.Y),
            Arrays.asList(Axis.Z, Axis.Y, Axis.X));
    private static final List<List<Axis>> nonStupidityCombinations = Arrays.asList(
            Arrays.asList(Axis.Y, Axis.X, Axis.Z),
            Arrays.asList(Axis.Y, Axis.Z, Axis.X));

    public static boolean slowCouldPointThreeHitGround(GrimPlayer player, double x, double y, double z) {
        SimpleCollisionBox oldBB = player.boundingBox;
        player.boundingBox = GetBoundingBox.getBoundingBoxFromPosAndSize(player, x, y, z, 0.6f, 0.06f);

        double movementThreshold = player.getMovementThreshold();
        double posXZ = collide(player, movementThreshold, -movementThreshold, movementThreshold).getY();
        double negXNegZ = collide(player, -movementThreshold, -movementThreshold, -movementThreshold).getY();
        double posXNegZ = collide(player, movementThreshold, -movementThreshold, -movementThreshold).getY();
        double posZNegX = collide(player, -movementThreshold, -movementThreshold, movementThreshold).getY();

        player.boundingBox = oldBB;
        return negXNegZ != -movementThreshold || posXNegZ != -movementThreshold || posXZ != -movementThreshold || posZNegX != -movementThreshold;
    }

    // Call this when there isn't uncertainty on the Y axis
    public static Vector3dm collide(GrimPlayer player, double desiredX, double desiredY, double desiredZ) {
        return collide(player, desiredX, desiredY, desiredZ, desiredY, null);
    }

    public static Vector3dm collide(GrimPlayer player, double desiredX, double desiredY, double desiredZ, double clientVelY, VectorData data) {
        if (desiredX == 0 && desiredY == 0 && desiredZ == 0) return new Vector3dm();

        final SimpleCollisionBox grabBoxesBB = player.boundingBox.copy();
        final double stepUpHeight = player.getMaxUpStep();

        if (desiredX == 0.0 && desiredZ == 0.0) {
            if (desiredY > 0.0) {
                grabBoxesBB.maxY += desiredY;
            } else {
                grabBoxesBB.minY += desiredY;
            }
        } else {
            if (stepUpHeight > 0.0 && (player.lastOnGround || desiredY < 0 || clientVelY < 0)) {
                // don't bother getting the collisions if we don't need them.
                if (desiredY <= 0.0) {
                    grabBoxesBB.expandToCoordinate(desiredX, desiredY, desiredZ);
                    grabBoxesBB.maxY += stepUpHeight;
                } else {
                    grabBoxesBB.expandToCoordinate(desiredX, Math.max(stepUpHeight, desiredY), desiredZ);
                }
            } else {
                grabBoxesBB.expandToCoordinate(desiredX, desiredY, desiredZ);
            }
        }

        List<SimpleCollisionBox> desiredMovementCollisionBoxes = new ArrayList<>();
        getCollisionBoxes(player, grabBoxesBB, desiredMovementCollisionBoxes, false);

        double bestInput = Double.MAX_VALUE;
        Vector3dm bestOrderResult = null;

        Vector3dm bestTheoreticalCollisionResult = VectorUtils.cutBoxToVector(player.actualMovement, new SimpleCollisionBox(0, Math.min(0, desiredY), 0, desiredX, Math.max(stepUpHeight, desiredY), desiredZ).sort());
        int zeroCount = (desiredX == 0 ? 1 : 0) + (desiredY == 0 ? 1 : 0) + (desiredZ == 0 ? 1 : 0);

        for (List<Axis> order : (data != null && data.isZeroPointZeroThree() ? allAxisCombinations : nonStupidityCombinations)) {
            Vector3dm collisionResult = collideBoundingBoxLegacy(new Vector3dm(desiredX, desiredY, desiredZ), player.boundingBox, desiredMovementCollisionBoxes, order);

            // While running up stairs and holding space, the player activates the "lastOnGround" part without otherwise being able to step
            // 0.03 movement must compensate for stepping elsewhere.  Too much of a hack to include in this method.
            boolean movingIntoGroundReal = player.pointThreeEstimator.closeEnoughToGroundToStepWithPointThree(data, clientVelY) || collisionResult.getY() != desiredY && (desiredY < 0 || clientVelY < 0);
            boolean movingIntoGround = player.lastOnGround || movingIntoGroundReal;

            // If the player has x or z collision, is going in the downwards direction in the last or this tick, and can step up
            // If not, just return the collisions without stepping up that we calculated earlier

            // At high ping, if you get setback, then you can reach the ground in time. When you are teleported back up by the setback, the game allows you to step up legitimately. By disallowing stepping we prevent a step exploit.
            final boolean disallowStepping = player.getSetbackTeleportUtil().getRequiredSetBack() != null && player.getSetbackTeleportUtil().getRequiredSetBack().getTicksComplete() == 1;
            if (!disallowStepping && stepUpHeight > 0.0F && movingIntoGround && (collisionResult.getX() != desiredX || collisionResult.getZ() != desiredZ)) {
                player.uncertaintyHandler.isStepMovement = true;
                // 1.21 significantly refactored this
                if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21)) {
                    SimpleCollisionBox startingOffsetBox = movingIntoGroundReal ? player.boundingBox.copy().offset(0.0, collisionResult.getY(), 0.0) : player.boundingBox.copy();
                    SimpleCollisionBox offsetByHorizAndStepBox = startingOffsetBox.copy().expandToCoordinate(desiredX, stepUpHeight, desiredZ);
                    if (!movingIntoGroundReal) {
                        offsetByHorizAndStepBox = offsetByHorizAndStepBox.copy().expandToCoordinate(0.0, -1.0E-5F, 0.0);
                    }

                    final List<SimpleCollisionBox> stepCollisions = new ArrayList<>();
                    getCollisionBoxes(player, offsetByHorizAndStepBox, stepCollisions, false);
                    final float[] stepHeights = collectStepHeights(startingOffsetBox, stepCollisions, (float) stepUpHeight, (float) collisionResult.getY());

                    for (float stepHeight : stepHeights) {
                        Vector3dm vec3d2 = collideBoundingBoxLegacy(new Vector3dm(desiredX, stepHeight, desiredZ), startingOffsetBox, stepCollisions, order);
                        if (getHorizontalDistanceSqr(vec3d2) > getHorizontalDistanceSqr(collisionResult)) {
                            final double d = player.boundingBox.minY - startingOffsetBox.minY;
                            collisionResult = vec3d2.add(new Vector3dm(0.0, -d, 0.0));
                            break;
                        }
                    }
                } else {
                    Vector3dm regularStepUp = collideBoundingBoxLegacy(new Vector3dm(desiredX, stepUpHeight, desiredZ), player.boundingBox, desiredMovementCollisionBoxes, order);

                    // 1.7 clients do not have this stepping bug fix
                    if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8)) {
                        Vector3dm stepUpBugFix = collideBoundingBoxLegacy(new Vector3dm(0, stepUpHeight, 0), player.boundingBox.copy().expandToCoordinate(desiredX, 0, desiredZ), desiredMovementCollisionBoxes, order);
                        if (stepUpBugFix.getY() < stepUpHeight) {
                            Vector3dm stepUpBugFixResult = collideBoundingBoxLegacy(new Vector3dm(desiredX, 0, desiredZ), player.boundingBox.copy().offset(0, stepUpBugFix.getY(), 0), desiredMovementCollisionBoxes, order).add(stepUpBugFix);
                            if (getHorizontalDistanceSqr(stepUpBugFixResult) > getHorizontalDistanceSqr(regularStepUp)) {
                                regularStepUp = stepUpBugFixResult;
                            }
                        }
                    }

                    if (getHorizontalDistanceSqr(regularStepUp) > getHorizontalDistanceSqr(collisionResult)) {
                        collisionResult = regularStepUp.add(collideBoundingBoxLegacy(new Vector3dm(0, -regularStepUp.getY() + (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14) ? desiredY : 0), 0), player.boundingBox.copy().offset(regularStepUp.getX(), regularStepUp.getY(), regularStepUp.getZ()), desiredMovementCollisionBoxes, order));
                    }
                }
            }

            double resultAccuracy = collisionResult.distanceSquared(bestTheoreticalCollisionResult);

            // Step movement doesn't care about ground (due to 0.03)
            if (player.wouldCollisionResultFlagGroundSpoof(desiredY, collisionResult.getY())) {
                resultAccuracy += 1;
            }

            if (resultAccuracy < bestInput) {
                bestOrderResult = collisionResult;
                bestInput = resultAccuracy;
                if (resultAccuracy < 0.00001 * 0.00001) break;
                if (zeroCount >= 2) break;
            }

        }
        return bestOrderResult;
    }

    private static float[] collectStepHeights(SimpleCollisionBox collisionBox, List<SimpleCollisionBox> collisions, float stepHeight, float collideY) {
        final FloatSet floatSet = new FloatArraySet(4);

        for (SimpleCollisionBox blockBox : collisions) {
            for (double possibleStepY : blockBox.getYPointPositions()) {
                float yDiff = (float) (possibleStepY - collisionBox.minY);
                if (!(yDiff < 0.0F) && yDiff != collideY) {
                    if (yDiff > stepHeight) {
                        break;
                    }

                    floatSet.add(yDiff);
                }
            }
        }

        float[] fs = floatSet.toFloatArray();
        FloatArrays.unstableSort(fs);
        return fs;
    }

    public static boolean addWorldBorder(GrimPlayer player, SimpleCollisionBox wantedBB, List<SimpleCollisionBox> listOfBlocks, boolean onlyCheckCollide) {
        // Worldborders were added in 1.8
        // Don't add to border unless the player is colliding with it and is near it
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8)) {
            PacketWorldBorder border = player.checkManager.getPacketCheck(PacketWorldBorder.class);
            double centerX = border.getCenterX();
            double centerZ = border.getCenterZ();

            // For some reason, the game limits the border to 29999984 blocks wide
            double size = border.getCurrentDiameter() / 2;
            double absoluteMaxSize = border.getAbsoluteMaxSize();

            double minX = Math.floor(GrimMath.clamp(centerX - size, -absoluteMaxSize, absoluteMaxSize));
            double minZ = Math.floor(GrimMath.clamp(centerZ - size, -absoluteMaxSize, absoluteMaxSize));
            double maxX = Math.ceil(GrimMath.clamp(centerX + size, -absoluteMaxSize, absoluteMaxSize));
            double maxZ = Math.ceil(GrimMath.clamp(centerZ + size, -absoluteMaxSize, absoluteMaxSize));

            // If the player is fully within the worldborder
            double toMinX = player.lastX - minX;
            double toMaxX = maxX - player.lastX;
            double minimumInXDirection = Math.min(toMinX, toMaxX);

            double toMinZ = player.lastZ - minZ;
            double toMaxZ = maxZ - player.lastZ;
            double minimumInZDirection = Math.min(toMinZ, toMaxZ);

            double distanceToBorder = Math.min(minimumInXDirection, minimumInZDirection);

            // If the player's is within 16 blocks of the worldborder, add the worldborder to the collisions (optimization)
            if (distanceToBorder < 16 && player.lastX > minX && player.lastX < maxX && player.lastZ > minZ && player.lastZ < maxZ) {
                if (listOfBlocks == null) listOfBlocks = new ArrayList<>();

                // South border
                listOfBlocks.add(new SimpleCollisionBox(minX - 10, Double.NEGATIVE_INFINITY, maxZ, maxX + 10, Double.POSITIVE_INFINITY, maxZ, false));
                // North border
                listOfBlocks.add(new SimpleCollisionBox(minX - 10, Double.NEGATIVE_INFINITY, minZ, maxX + 10, Double.POSITIVE_INFINITY, minZ, false));
                // East border
                listOfBlocks.add(new SimpleCollisionBox(maxX, Double.NEGATIVE_INFINITY, minZ - 10, maxX, Double.POSITIVE_INFINITY, maxZ + 10, false));
                // West border
                listOfBlocks.add(new SimpleCollisionBox(minX, Double.NEGATIVE_INFINITY, minZ - 10, minX, Double.POSITIVE_INFINITY, maxZ + 10, false));

                if (onlyCheckCollide) {
                    for (SimpleCollisionBox box : listOfBlocks) {
                        if (box.isIntersected(wantedBB)) return true;
                    }
                }
            }
        }
        return false;
    }

    // This is mostly taken from Tuinity collisions
    public static boolean getCollisionBoxes(GrimPlayer player, SimpleCollisionBox wantedBB, List<SimpleCollisionBox> listOfBlocks, boolean onlyCheckCollide) {
        SimpleCollisionBox expandedBB = wantedBB.copy();

        boolean collided = addWorldBorder(player, wantedBB, listOfBlocks, onlyCheckCollide);
        if (onlyCheckCollide && collided) return true;

        int minBlockX = (int) Math.floor(expandedBB.minX - COLLISION_EPSILON) - 1;
        int maxBlockX = (int) Math.floor(expandedBB.maxX + COLLISION_EPSILON) + 1;
        int minBlockY = (int) Math.floor(expandedBB.minY - COLLISION_EPSILON) - 1;
        int maxBlockY = (int) Math.floor(expandedBB.maxY + COLLISION_EPSILON) + 1;
        int minBlockZ = (int) Math.floor(expandedBB.minZ - COLLISION_EPSILON) - 1;
        int maxBlockZ = (int) Math.floor(expandedBB.maxZ + COLLISION_EPSILON) + 1;

        final int minSection = player.compensatedWorld.getMinHeight() >> 4;
        final int minBlock = minSection << 4;
        final int maxBlock = player.compensatedWorld.getMaxHeight() - 1;

        int minChunkX = minBlockX >> 4;
        int maxChunkX = maxBlockX >> 4;

        int minChunkZ = minBlockZ >> 4;
        int maxChunkZ = maxBlockZ >> 4;

        int minYIterate = Math.max(minBlock, minBlockY);
        int maxYIterate = Math.min(maxBlock, maxBlockY);

        for (int currChunkZ = minChunkZ; currChunkZ <= maxChunkZ; ++currChunkZ) {
            int minZ = currChunkZ == minChunkZ ? minBlockZ & 15 : 0; // coordinate in chunk
            int maxZ = currChunkZ == maxChunkZ ? maxBlockZ & 15 : 15; // coordinate in chunk

            for (int currChunkX = minChunkX; currChunkX <= maxChunkX; ++currChunkX) {
                int minX = currChunkX == minChunkX ? minBlockX & 15 : 0; // coordinate in chunk
                int maxX = currChunkX == maxChunkX ? maxBlockX & 15 : 15; // coordinate in chunk

                int chunkXGlobalPos = currChunkX << 4;
                int chunkZGlobalPos = currChunkZ << 4;

                Column chunk = player.compensatedWorld.getChunk(currChunkX, currChunkZ);
                if (chunk == null) continue;

                BaseChunk[] sections = chunk.chunks();

                for (int y = minYIterate; y <= maxYIterate; ++y) {
                    int sectionIndex = (y >> 4) - minSection;

                    BaseChunk section = sections[sectionIndex];

                    if (section == null || (IS_FOURTEEN && section.isEmpty())) { // Check for empty on 1.13+ servers
                        // empty
                        // skip to next section
                        y = (y & ~15) + 15; // increment by 15: iterator loop increments by the extra one
                        continue;
                    }

                    for (int currZ = minZ; currZ <= maxZ; ++currZ) {
                        for (int currX = minX; currX <= maxX; ++currX) {
                            int x = currX | chunkXGlobalPos;
                            int z = currZ | chunkZGlobalPos;

                            WrappedBlockState data = section.get(CompensatedWorld.blockVersion, x & 0xF, y & 0xF, z & 0xF, false);

                            // Works on both legacy and modern!  Faster than checking for material types, most common case
                            if (data.getGlobalId() == 0) continue;

                            // Thanks SpottedLeaf for this optimization, I took edgeCount from Tuinity
                            int edgeCount = ((x == minBlockX || x == maxBlockX) ? 1 : 0) +
                                    ((y == minBlockY || y == maxBlockY) ? 1 : 0) +
                                    ((z == minBlockZ || z == maxBlockZ) ? 1 : 0);

                            final StateType type = data.getType();
                            if (edgeCount != 3 && (edgeCount != 1 || Materials.isShapeExceedsCube(type))
                                    && (edgeCount != 2 || type == StateTypes.PISTON_HEAD)) {
                                final CollisionBox collisionBox = CollisionData.getData(type).getMovementCollisionBox(player, player.getClientVersion(), data, x, y, z);
                                // Don't add to a list if we only care if the player intersects with the block
                                if (!onlyCheckCollide) {
                                    collisionBox.downCast(listOfBlocks);
                                } else if (collisionBox.isCollided(wantedBB)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    public static Vector3dm collideBoundingBoxLegacy(Vector3dm toCollide, SimpleCollisionBox
            box, List<SimpleCollisionBox> desiredMovementCollisionBoxes, List<Axis> order) {
        double x = toCollide.getX();
        double y = toCollide.getY();
        double z = toCollide.getZ();

        SimpleCollisionBox setBB = box.copy();

        for (Axis axis : order) {
            if (axis == Axis.X) {
                for (SimpleCollisionBox bb : desiredMovementCollisionBoxes) {
                    x = bb.collideX(setBB, x);
                }
                setBB.offset(x, 0.0D, 0.0D);
            } else if (axis == Axis.Y) {
                for (SimpleCollisionBox bb : desiredMovementCollisionBoxes) {
                    y = bb.collideY(setBB, y);
                }
                setBB.offset(0.0D, y, 0.0D);
            } else if (axis == Axis.Z) {
                for (SimpleCollisionBox bb : desiredMovementCollisionBoxes) {
                    z = bb.collideZ(setBB, z);
                }
                setBB.offset(0.0D, 0.0D, z);
            }
        }

        return new Vector3dm(x, y, z);
    }

    public static boolean isEmpty(GrimPlayer player, SimpleCollisionBox playerBB) {
        return !getCollisionBoxes(player, playerBB, null, true);
    }

    public static double getHorizontalDistanceSqr(Vector3dm vector) {
        return vector.getX() * vector.getX() + vector.getZ() * vector.getZ();
    }

    public static Vector3dm maybeBackOffFromEdge(Vector3dm vec3, GrimPlayer player, boolean overrideVersion) {
        if (!player.isFlying && player.isSneaking && isAboveGround(player)) {
            double x = vec3.getX();
            double z = vec3.getZ();

            double maxStepDown = overrideVersion || player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_11) ? -player.getMaxUpStep() : -1 + COLLISION_EPSILON;

            while (x != 0.0 && isEmpty(player, player.boundingBox.copy().offset(x, maxStepDown, 0.0))) {
                if (x < 0.05D && x >= -0.05D) {
                    x = 0.0D;
                } else if (x > 0.0D) {
                    x -= 0.05D;
                } else {
                    x += 0.05D;
                }
            }
            while (z != 0.0 && isEmpty(player, player.boundingBox.copy().offset(0.0, maxStepDown, z))) {
                if (z < 0.05D && z >= -0.05D) {
                    z = 0.0D;
                } else if (z > 0.0D) {
                    z -= 0.05D;
                } else {
                    z += 0.05D;
                }
            }
            while (x != 0.0 && z != 0.0 && isEmpty(player, player.boundingBox.copy().offset(x, maxStepDown, z))) {
                if (x < 0.05D && x >= -0.05D) {
                    x = 0.0D;
                } else if (x > 0.0D) {
                    x -= 0.05D;
                } else {
                    x += 0.05D;
                }

                if (z < 0.05D && z >= -0.05D) {
                    z = 0.0D;
                } else if (z > 0.0D) {
                    z -= 0.05D;
                } else {
                    z += 0.05D;
                }
            }
            vec3 = new Vector3dm(x, vec3.getY(), z);
        }

        return vec3;
    }

    public static boolean isAboveGround(GrimPlayer player) {
        // https://bugs.mojang.com/browse/MC-2404
        return player.lastOnGround || (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_16_2) && (player.fallDistance < player.getMaxUpStep() &&
                !isEmpty(player, player.boundingBox.copy().offset(0.0, player.fallDistance - player.getMaxUpStep(), 0.0))));
    }

    public static void handleInsideBlocks(GrimPlayer player) {
        // Mojang rewrote this whole logic in 1.21.2 (see Collisions#applyEffectsFromBlocks)
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_2)) return;
        // Use the bounding box for after the player's movement is applied
        double expandAmount = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_19_4) ? 1e-5 : 0.001;
        SimpleCollisionBox aABB = player.inVehicle()
                ? GetBoundingBox.getCollisionBoxForPlayer(player, player.x, player.y, player.z).expand(-expandAmount)
                : player.boundingBox.copy().expand(-expandAmount);

        Location blockPos = new Location(null, aABB.minX, aABB.minY, aABB.minZ);
        Location blockPos2 = new Location(null, aABB.maxX, aABB.maxY, aABB.maxZ);

        if (CheckIfChunksLoaded.areChunksUnloadedAt(player, blockPos.getBlockX(), blockPos.getBlockY(), blockPos.getBlockZ(), blockPos2.getBlockX(), blockPos2.getBlockY(), blockPos2.getBlockZ()))
            return;

        for (int blockX = blockPos.getBlockX(); blockX <= blockPos2.getBlockX(); ++blockX) {
            for (int blockY = blockPos.getBlockY(); blockY <= blockPos2.getBlockY(); ++blockY) {
                for (int blockZ = blockPos.getBlockZ(); blockZ <= blockPos2.getBlockZ(); ++blockZ) {
                    WrappedBlockState block = player.compensatedWorld.getBlock(blockX, blockY, blockZ);
                    StateType blockType = block.getType();

                    if (blockType.isAir()) {
                        continue;
                    }

                    onInsideBlock(player, blockType, block, blockX, blockY, blockZ);
                }
            }
        }
    }

    public static void onInsideBlock(GrimPlayer player, StateType blockType, WrappedBlockState block, int blockX, int blockY, int blockZ) {
        if (blockType == StateTypes.COBWEB) {
            if (player.compensatedEntities.hasPotionEffect(PotionTypes.WEAVING)) {
                player.stuckSpeedMultiplier = new Vector3dm(0.5, 0.25, 0.5);
            } else {
                player.stuckSpeedMultiplier = new Vector3dm(0.25, 0.05f, 0.25);
            }
        }

        if (blockType == StateTypes.SWEET_BERRY_BUSH
                && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14)) {
            player.stuckSpeedMultiplier = new Vector3dm(0.8f, 0.75, 0.8f);
        }

        if (blockType == StateTypes.POWDER_SNOW && blockX == Math.floor(player.x) && blockY == Math.floor(player.y) && blockZ == Math.floor(player.z)
                && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_17)) {
            player.stuckSpeedMultiplier = new Vector3dm(0.9f, 1.5, 0.9f);
        }

        if (blockType == StateTypes.SOUL_SAND && player.getClientVersion().isOlderThan(ClientVersion.V_1_15)) {
            player.clientVelocity.setX(player.clientVelocity.getX() * 0.4D);
            player.clientVelocity.setZ(player.clientVelocity.getZ() * 0.4D);
        }

        if (blockType == StateTypes.LAVA && player.getClientVersion().isOlderThan(ClientVersion.V_1_16) && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14)) {
            player.wasTouchingLava = true;
        }

        if (blockType == StateTypes.BUBBLE_COLUMN && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13)) {
            WrappedBlockState blockAbove = player.compensatedWorld.getBlock(blockX, blockY + 1, blockZ);

            if (player.inVehicle() && player.compensatedEntities.self.getRiding().isBoat) {
                if (!blockAbove.getType().isAir()) {
                    if (block.isDrag()) {
                        player.clientVelocity.setY(Math.max(-0.3D, player.clientVelocity.getY() - 0.03D));
                    } else {
                        player.clientVelocity.setY(Math.min(0.7D, player.clientVelocity.getY() + 0.06D));
                    }
                }
            } else {
                if (blockAbove.getType().isAir()) {
                    for (VectorData vector : player.getPossibleVelocitiesMinusKnockback()) {
                        if (block.isDrag()) {
                            vector.vector.setY(Math.max(-0.9D, vector.vector.getY() - 0.03D));
                        } else {
                            vector.vector.setY(Math.min(1.8D, vector.vector.getY() + 0.1D));
                        }
                    }
                } else {
                    for (VectorData vector : player.getPossibleVelocitiesMinusKnockback()) {
                        if (block.isDrag()) {
                            vector.vector.setY(Math.max(-0.3D, vector.vector.getY() - 0.03D));
                        } else {
                            vector.vector.setY(Math.min(0.7D, vector.vector.getY() + 0.06D));
                        }
                    }
                }
            }

            // Reset fall distance inside bubble column
            player.fallDistance = 0;
        }

        if (blockType == StateTypes.HONEY_BLOCK && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_15)) {
            if (isSlidingDown(player.clientVelocity, player, blockX, blockY, blockZ)) {
                if (getOldDeltaY(player, player.clientVelocity.getY()) < -0.13D) {
                    double d0 = -0.05 / getOldDeltaY(player, player.clientVelocity.getY());
                    player.clientVelocity.setX(player.clientVelocity.getX() * d0);
                    player.clientVelocity.setY(getNewDeltaY(player, -0.05D));
                    player.clientVelocity.setZ(player.clientVelocity.getZ() * d0);
                } else {
                    player.clientVelocity.setY(getNewDeltaY(player, -0.05D));
                }
            }

            // If honey sliding, fall distance is 0
            player.fallDistance = 0;
        }
    }

    // Implementation of Collisions#handleInsideBlocks for >= 1.21.2
    public static void applyEffectsFromBlocks(GrimPlayer player) {
        for (GrimPlayer.Movement movement : player.finalMovementsThisTick) {
            Vector3d from = movement.from();
            Vector3d to = movement.to().subtract(movement.from());
            if (movement.axisIndependant() && to.lengthSquared() > 0.0) {
                for (Axis axis : Collisions.axisStepOrder(to)) {
                    double value = axis.choose(to.getX(), to.getY(), to.getZ());
                    if (value != 0.0) {
                        Vector3d vector = Collisions.relative(from, axis.getPositive(), value);
                        Collisions.checkInsideBlocks(player, from, vector);
                        from = vector;
                    }
                }
            } else {
                Collisions.checkInsideBlocks(player, movement.from(), movement.to());
            }
        }

        player.visitedBlocks.clear();
    }

    public static void checkInsideBlocks(GrimPlayer player, Vector3d from, Vector3d to) {
        SimpleCollisionBox boundingBox = (player.getClientVersion() == ClientVersion.V_1_21_2 ?
                player.boundingBox.copy() : GetBoundingBox.getCollisionBoxForPlayer(player, to.x, to.y, to.z)).expand(-1.0E-5F);

        for (Vector3i blockPos : Collisions.boxTraverseBlocks(player, from, to, boundingBox)) {
            WrappedBlockState blockState = player.compensatedWorld.getBlock(blockPos);
            StateType blockType = blockState.getType();

            if (blockType.isAir()) {
                continue;
            }

            if (player.visitedBlocks.add(GrimMath.asLong(blockPos.getX(), blockPos.getY(), blockPos.getZ()))) {
                Collisions.onInsideBlock(player, blockType, blockState, blockPos.x, blockPos.y, blockPos.z);
            }
        }
    }

    public static Iterable<Vector3i> boxTraverseBlocks(GrimPlayer player, Vector3d start, Vector3d end, SimpleCollisionBox boundingBox) {
        Vector3d direction = end.subtract(start);
        Iterable<Vector3i> initialBlocks = SimpleCollisionBox.betweenClosed(boundingBox);
        if (direction.lengthSquared() < (double) GrimMath.square(0.99999F)) {
            return initialBlocks;
        } else {
            LongSet alreadyVisited = player.getClientVersion().isOlderThan(ClientVersion.V_1_21_5) ? null : new LongOpenHashSet();
            Set<Vector3i> traversedBlocks = new ObjectLinkedOpenHashSet<>();
            Vector3d boxMinPosition = boundingBox.min().toVector3d();
            Vector3d subtractedMinPosition = boxMinPosition.subtract(direction);
            addCollisionsAlongTravel(alreadyVisited, traversedBlocks, subtractedMinPosition, boxMinPosition, boundingBox);

            for (Vector3i blockPos : initialBlocks) {
                if (alreadyVisited == null || !alreadyVisited.contains(GrimMath.asLong(blockPos.getX(), blockPos.getY(), blockPos.getZ()))) {
                    traversedBlocks.add(blockPos);
                }
            }

            return traversedBlocks;
        }
    }

    public static void addCollisionsAlongTravel(LongSet alreadyVisited, Set<Vector3i> output, Vector3d start, Vector3d end, SimpleCollisionBox boundingBox) {
        Vector3d direction = end.subtract(start);
        int currentX = GrimMath.floor(start.x);
        int currentY = GrimMath.floor(start.y);
        int currentZ = GrimMath.floor(start.z);
        int stepX = GrimMath.sign(direction.x);
        int stepY = GrimMath.sign(direction.y);
        int stepZ = GrimMath.sign(direction.z);
        double tMaxX = stepX == 0 ? Double.MAX_VALUE : stepX / direction.x;
        double tMaxY = stepY == 0 ? Double.MAX_VALUE : stepY / direction.y;
        double tMaxZ = stepZ == 0 ? Double.MAX_VALUE : stepZ / direction.z;
        double tDeltaX = tMaxX * (stepX > 0 ? 1.0 - GrimMath.frac(start.x) : GrimMath.frac(start.x));
        double tDeltaY = tMaxY * (stepY > 0 ? 1.0 - GrimMath.frac(start.y) : GrimMath.frac(start.y));
        double tDeltaZ = tMaxZ * (stepZ > 0 ? 1.0 - GrimMath.frac(start.z) : GrimMath.frac(start.z));
        int iterationCount = 0;

        while (tDeltaX <= 1.0 || tDeltaY <= 1.0 || tDeltaZ <= 1.0) {
            if (tDeltaX < tDeltaY) {
                if (tDeltaX < tDeltaZ) {
                    currentX += stepX;
                    tDeltaX += tMaxX;
                } else {
                    currentZ += stepZ;
                    tDeltaZ += tMaxZ;
                }
            } else if (tDeltaY < tDeltaZ) {
                currentY += stepY;
                tDeltaY += tMaxY;
            } else {
                currentZ += stepZ;
                tDeltaZ += tMaxZ;
            }

            if (iterationCount++ > 16) {
                break;
            }

            Optional<Vector3d> collisionPoint = clip(currentX, currentY, currentZ, currentX + 1, currentY + 1, currentZ + 1, start, end);
            if (collisionPoint.isPresent()) {
                Vector3d collisionVec = collisionPoint.get();
                double clampedX = GrimMath.clamp(collisionVec.x, currentX + 1.0E-5F, currentX + 1.0 - 1.0E-5F);
                double clampedY = GrimMath.clamp(collisionVec.y, currentY + 1.0E-5F, currentY + 1.0 - 1.0E-5F);
                double clampedZ = GrimMath.clamp(collisionVec.z, currentZ + 1.0E-5F, currentZ + 1.0 - 1.0E-5F);
                int endX = GrimMath.floor(clampedX + boundingBox.getXSize());
                int endY = GrimMath.floor(clampedY + boundingBox.getYSize());
                int endZ = GrimMath.floor(clampedZ + boundingBox.getZSize());

                for (int x = currentX; x <= endX; x++) {
                    for (int y = currentY; y <= endY; y++) {
                        for (int z = currentZ; z <= endZ; z++) {
                            if (alreadyVisited == null || alreadyVisited.add(GrimMath.asLong(x, y, z))) {
                                output.add(new Vector3i(x, y, z));
                            }
                        }
                    }
                }
            }
        }
    }

    public static Optional<Vector3d> clip(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Vector3d start, Vector3d end) {
        double[] minDistance = new double[]{1.0};
        double deltaX = end.x - start.x;
        double deltaY = end.y - start.y;
        double deltaZ = end.z - start.z;
        Direction direction = getDirection(minX, minY, minZ, maxX, maxY, maxZ, start, minDistance, null, deltaX, deltaY, deltaZ);
        if (direction == null) {
            return Optional.empty();
        } else {
            double distance = minDistance[0];
            return Optional.of(start.add(distance * deltaX, distance * deltaY, distance * deltaZ));
        }
    }

    private static Direction getDirection(
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ,
            Vector3d start,
            double[] minDistance,
            Direction facing,
            double deltaX,
            double deltaY,
            double deltaZ
    ) {
        if (deltaX > COLLISION_EPSILON) {
            facing = clipPoint(minDistance, facing, deltaX, deltaY, deltaZ, minX, minY, maxY, minZ, maxZ, Direction.WEST, start.x, start.y, start.z);
        } else if (deltaX < -COLLISION_EPSILON) {
            facing = clipPoint(minDistance, facing, deltaX, deltaY, deltaZ, maxX, minY, maxY, minZ, maxZ, Direction.EAST, start.x, start.y, start.z);
        }

        if (deltaY > COLLISION_EPSILON) {
            facing = clipPoint(minDistance, facing, deltaY, deltaZ, deltaX, minY, minZ, maxZ, minX, maxX, Direction.DOWN, start.y, start.z, start.x);
        } else if (deltaY < -COLLISION_EPSILON) {
            facing = clipPoint(minDistance, facing, deltaY, deltaZ, deltaX, maxY, minZ, maxZ, minX, maxX, Direction.UP, start.y, start.z, start.x);
        }

        if (deltaZ > COLLISION_EPSILON) {
            facing = clipPoint(minDistance, facing, deltaZ, deltaX, deltaY, minZ, minX, maxX, minY, maxY, Direction.NORTH, start.z, start.x, start.y);
        } else if (deltaZ < -COLLISION_EPSILON) {
            facing = clipPoint(minDistance, facing, deltaZ, deltaX, deltaY, maxZ, minX, maxX, minY, maxY, Direction.SOUTH, start.z, start.x, start.y);
        }

        return facing;
    }

    public static Direction clipPoint(
            double[] minDistance,
            Direction prevDirection,
            double distanceSide,
            double distanceOtherA,
            double distanceOtherB,
            double minSide,
            double minOtherA,
            double maxOtherA,
            double minOtherB,
            double maxOtherB,
            Direction hitSide,
            double startSide,
            double startOtherA,
            double startOtherB
    ) {
        double sideDistance = (minSide - startSide) / distanceSide;
        double otherDistanceA = startOtherA + sideDistance * distanceOtherA;
        double otherDistanceB = startOtherB + sideDistance * distanceOtherB;
        if (sideDistance > 0.0 && sideDistance < minDistance[0] &&
                minOtherA - COLLISION_EPSILON < otherDistanceA &&
                otherDistanceA < maxOtherA + COLLISION_EPSILON &&
                minOtherB - COLLISION_EPSILON < otherDistanceB &&
                otherDistanceB < maxOtherB + COLLISION_EPSILON) {
            minDistance[0] = sideDistance;
            return hitSide;
        } else {
            return prevDirection;
        }
    }

    public static final ImmutableList<Axis> YXZ_AXIS_ORDER = ImmutableList.of(Collisions.Axis.Y, Collisions.Axis.X, Collisions.Axis.Z);
    public static final ImmutableList<Collisions.Axis> YZX_AXIS_ORDER = ImmutableList.of(Collisions.Axis.Y, Collisions.Axis.Z, Collisions.Axis.X);

    public static Iterable<Collisions.Axis> axisStepOrder(Vector3d vector) {
        return Math.abs(vector.getX()) < Math.abs(vector.getZ()) ? YZX_AXIS_ORDER : YXZ_AXIS_ORDER;
    }

    public static Vector3d relative(Vector3d curr, Direction direction, double value) {
        Vector3i vec = direction.getVector();
        return new Vector3d(
                curr.x + value * vec.getX(), curr.y + value * vec.getY(), curr.z + value * vec.getZ()
        );
    }

    private static double getOldDeltaY(GrimPlayer player, double value) {
        return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_2) ? value / 0.98F + 0.08 : value;
    }

    private static double getNewDeltaY(GrimPlayer player, double value) {
        return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_2) ? (value - 0.08) * 0.98F : value;
    }

    private static boolean isSlidingDown(Vector3dm vector, GrimPlayer player, int locationX, int locationY,
                                         int locationZ) {
        if (player.onGround) {
            return false;
        } else if (player.y > (double) locationY + 0.9375D - COLLISION_EPSILON) {
            return false;
        } else if (getOldDeltaY(player, vector.getY()) >= -0.08D) {
            return false;
        } else {
            double d0 = Math.abs(locationX + 0.5D - player.lastX);
            double d1 = Math.abs(locationZ + 0.5D - player.lastZ);
            // Calculate player width using bounding box, which will change while swimming or gliding
            double d2 = 0.4375D + ((player.pose.width) / 2.0F);
            return d0 + COLLISION_EPSILON > d2 || d1 + COLLISION_EPSILON > d2;
        }
    }

    // 0.03 hack
    public static boolean checkStuckSpeed(GrimPlayer player, double expand) {
        // Use the bounding box for after the player's movement is applied
        SimpleCollisionBox aABB = GetBoundingBox.getCollisionBoxForPlayer(player, player.x, player.y, player.z).expand(expand);

        Location blockPos = new Location(null, aABB.minX, aABB.minY, aABB.minZ);
        Location blockPos2 = new Location(null, aABB.maxX, aABB.maxY, aABB.maxZ);

        if (CheckIfChunksLoaded.areChunksUnloadedAt(player, blockPos.getBlockX(), blockPos.getBlockY(), blockPos.getBlockZ(), blockPos2.getBlockX(), blockPos2.getBlockY(), blockPos2.getBlockZ()))
            return false;

        for (int i = blockPos.getBlockX(); i <= blockPos2.getBlockX(); ++i) {
            for (int j = blockPos.getBlockY(); j <= blockPos2.getBlockY(); ++j) {
                for (int k = blockPos.getBlockZ(); k <= blockPos2.getBlockZ(); ++k) {
                    WrappedBlockState block = player.compensatedWorld.getBlock(i, j, k);
                    StateType blockType = block.getType();

                    if (blockType == StateTypes.COBWEB) {
                        return true;
                    }

                    if (blockType == StateTypes.SWEET_BERRY_BUSH && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14)) {
                        return true;
                    }

                    if (blockType == StateTypes.POWDER_SNOW && i == Math.floor(player.x) && j == Math.floor(player.y) && k == Math.floor(player.z) && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_17)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static boolean suffocatesAt(GrimPlayer player, SimpleCollisionBox playerBB) {
        // Blocks are stored in YZX order
        for (int y = (int) Math.floor(playerBB.minY); y < Math.ceil(playerBB.maxY); y++) {
            for (int z = (int) Math.floor(playerBB.minZ); z < Math.ceil(playerBB.maxZ); z++) {
                for (int x = (int) Math.floor(playerBB.minX); x < Math.ceil(playerBB.maxX); x++) {
                    if (doesBlockSuffocate(player, x, y, z)) {
                        // Mojang re-added soul sand pushing by checking if the player is actually in the block
                        // (This is why from 1.14-1.15 soul sand didn't push)
                        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_16)) {
                            WrappedBlockState data = player.compensatedWorld.getBlock(x, y, z);
                            CollisionBox box = CollisionData.getData(data.getType()).getMovementCollisionBox(player, player.getClientVersion(), data, x, y, z);

                            if (!box.isIntersected(playerBB)) continue;
                        }

                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static boolean doesBlockSuffocate(GrimPlayer player, int x, int y, int z) {
        WrappedBlockState data = player.compensatedWorld.getBlock(x, y, z);
        StateType mat = data.getType();

        // Optimization - all blocks that can suffocate must have a hitbox
        if (!mat.isSolid()) return false;

        // 1.13- players can not be pushed by blocks that can emit power, for some reason, while 1.14+ players can
        if (mat == StateTypes.OBSERVER || mat == StateTypes.REDSTONE_BLOCK)
            return player.getClientVersion().isNewerThan(ClientVersion.V_1_13_2);
        // Tnt only pushes on 1.14+ clients
        if (mat == StateTypes.TNT)
            return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14);
        // Farmland only pushes on 1.16+ clients
        if (mat == StateTypes.FARMLAND)
            return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_16);
        // 1.14-1.15 doesn't push with soul sand, the rest of the versions do
        if (mat == StateTypes.SOUL_SAND)
            return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_16) || player.getClientVersion().isOlderThan(ClientVersion.V_1_14);
        // 1.13 and below exempt piston bases, while 1.14+ look to see if they are a full block or not
        if ((mat == StateTypes.PISTON || mat == StateTypes.STICKY_PISTON) && player.getClientVersion().isOlderThan(ClientVersion.V_1_14))
            return false;
        // 1.13 and below exempt ICE and FROSTED_ICE, 1.14 have them push
        if (mat == StateTypes.ICE || mat == StateTypes.FROSTED_ICE)
            return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14);
        // I believe leaves and glass are consistently exempted across all versions
        if (BlockTags.LEAVES.contains(mat) || BlockTags.GLASS_BLOCKS.contains(mat)) return false;
        // 1.16 players are pushed by dirt paths, 1.8 players don't have this block, so it gets converted to a full block
        if (mat == StateTypes.DIRT_PATH)
            return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_16) || player.getClientVersion().isOlderThan(ClientVersion.V_1_9);
        // Only 1.14+ players are pushed by beacons
        if (mat == StateTypes.BEACON)
            return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_14);

        // Thank god I already have the solid blocking blacklist written, but all these are exempt
        if (Materials.isSolidBlockingBlacklist(mat, player.getClientVersion())) return false;

        CollisionBox box = CollisionData.getData(mat).getMovementCollisionBox(player, player.getClientVersion(), data, x, y, z);
        return box.isFullBlock();
    }

    // Thanks Tuinity
    public static boolean hasMaterial(GrimPlayer player, SimpleCollisionBox checkBox, Predicate<Pair<WrappedBlockState, Vector3d>> searchingFor) {
        int minBlockX = (int) Math.floor(checkBox.minX);
        int maxBlockX = (int) Math.floor(checkBox.maxX);
        int minBlockY = (int) Math.floor(checkBox.minY);
        int maxBlockY = (int) Math.floor(checkBox.maxY);
        int minBlockZ = (int) Math.floor(checkBox.minZ);
        int maxBlockZ = (int) Math.floor(checkBox.maxZ);

        final int minSection = player.compensatedWorld.getMinHeight() >> 4;
        final int minBlock = minSection << 4;
        final int maxBlock = player.compensatedWorld.getMaxHeight() - 1;

        int minChunkX = minBlockX >> 4;
        int maxChunkX = maxBlockX >> 4;

        int minChunkZ = minBlockZ >> 4;
        int maxChunkZ = maxBlockZ >> 4;

        int minYIterate = Math.max(minBlock, minBlockY);
        int maxYIterate = Math.min(maxBlock, maxBlockY);

        for (int currChunkZ = minChunkZ; currChunkZ <= maxChunkZ; ++currChunkZ) {
            int minZ = currChunkZ == minChunkZ ? minBlockZ & 15 : 0; // coordinate in chunk
            int maxZ = currChunkZ == maxChunkZ ? maxBlockZ & 15 : 15; // coordinate in chunk

            for (int currChunkX = minChunkX; currChunkX <= maxChunkX; ++currChunkX) {
                int minX = currChunkX == minChunkX ? minBlockX & 15 : 0; // coordinate in chunk
                int maxX = currChunkX == maxChunkX ? maxBlockX & 15 : 15; // coordinate in chunk

                int chunkXGlobalPos = currChunkX << 4;
                int chunkZGlobalPos = currChunkZ << 4;

                Column chunk = player.compensatedWorld.getChunk(currChunkX, currChunkZ);

                if (chunk == null) continue;
                BaseChunk[] sections = chunk.chunks();

                for (int y = minYIterate; y <= maxYIterate; ++y) {
                    BaseChunk section = sections[(y >> 4) - minSection];

                    if (section == null || (IS_FOURTEEN && section.isEmpty())) { // Check for empty on 1.13+ servers
                        // empty
                        // skip to next section
                        y = (y & ~(15)) + 15; // increment by 15: iterator loop increments by the extra one
                        continue;
                    }

                    for (int currZ = minZ; currZ <= maxZ; ++currZ) {
                        for (int currX = minX; currX <= maxX; ++currX) {
                            int x = currX | chunkXGlobalPos;
                            int z = currZ | chunkZGlobalPos;

                            WrappedBlockState data = section.get(CompensatedWorld.blockVersion, x & 0xF, y & 0xF, z & 0xF, false);

                            if (searchingFor.test(new Pair<>(data, new Vector3d(x, y, z))))
                                return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    // Thanks Tuinity
    public static void forEachCollisionBox(@NotNull GrimPlayer player, @NotNull SimpleCollisionBox checkBox, @NotNull Consumer<@NotNull Vector3d> searchingFor) {
        int minBlockX = (int) Math.floor(checkBox.minX - COLLISION_EPSILON) - 1;
        int maxBlockX = (int) Math.floor(checkBox.maxX + COLLISION_EPSILON) + 1;
        int minBlockY = (int) Math.floor(checkBox.minY - COLLISION_EPSILON) - 1;
        int maxBlockY = (int) Math.floor(checkBox.maxY + COLLISION_EPSILON) + 1;
        int minBlockZ = (int) Math.floor(checkBox.minZ - COLLISION_EPSILON) - 1;
        int maxBlockZ = (int) Math.floor(checkBox.maxZ + COLLISION_EPSILON) + 1;

        final int minSection = player.compensatedWorld.getMinHeight() >> 4;
        final int minBlock = minSection << 4;
        final int maxBlock = player.compensatedWorld.getMaxHeight() - 1;

        int minChunkX = minBlockX >> 4;
        int maxChunkX = maxBlockX >> 4;

        int minChunkZ = minBlockZ >> 4;
        int maxChunkZ = maxBlockZ >> 4;

        int minYIterate = Math.max(minBlock, minBlockY);
        int maxYIterate = Math.min(maxBlock, maxBlockY);

        for (int currChunkZ = minChunkZ; currChunkZ <= maxChunkZ; ++currChunkZ) {
            int minZ = currChunkZ == minChunkZ ? minBlockZ & 15 : 0; // coordinate in chunk
            int maxZ = currChunkZ == maxChunkZ ? maxBlockZ & 15 : 15; // coordinate in chunk

            for (int currChunkX = minChunkX; currChunkX <= maxChunkX; ++currChunkX) {
                int minX = currChunkX == minChunkX ? minBlockX & 15 : 0; // coordinate in chunk
                int maxX = currChunkX == maxChunkX ? maxBlockX & 15 : 15; // coordinate in chunk

                int chunkXGlobalPos = currChunkX << 4;
                int chunkZGlobalPos = currChunkZ << 4;

                Column chunk = player.compensatedWorld.getChunk(currChunkX, currChunkZ);

                if (chunk == null) continue;
                BaseChunk[] sections = chunk.chunks();

                for (int y = minYIterate; y <= maxYIterate; ++y) {
                    BaseChunk section = sections[(y >> 4) - minSection];

                    if (section == null || (IS_FOURTEEN && section.isEmpty())) { // Check for empty on 1.13+ servers
                        // empty
                        // skip to next section
                        y = (y & ~(15)) + 15; // increment by 15: iterator loop increments by the extra one
                        continue;
                    }

                    for (int currZ = minZ; currZ <= maxZ; ++currZ) {
                        for (int currX = minX; currX <= maxX; ++currX) {
                            int x = currX | chunkXGlobalPos;
                            int z = currZ | chunkZGlobalPos;

                            WrappedBlockState data = section.get(CompensatedWorld.blockVersion, x & 0xF, y & 0xF, z & 0xF, false);

                            // Works on both legacy and modern!  Faster than checking for material types, most common case
                            if (data.getGlobalId() == 0) continue;

                            // Thanks SpottedLeaf for this optimization, I took edgeCount from Tuinity
                            int edgeCount = ((x == minBlockX || x == maxBlockX) ? 1 : 0) +
                                    ((y == minBlockY || y == maxBlockY) ? 1 : 0) +
                                    ((z == minBlockZ || z == maxBlockZ) ? 1 : 0);

                            final StateType type = data.getType();
                            if (edgeCount != 3 && (edgeCount != 1 || Materials.isShapeExceedsCube(type))
                                    && (edgeCount != 2 || type == StateTypes.PISTON_HEAD)) {
                                final CollisionBox collisionBox = CollisionData.getData(type).getMovementCollisionBox(player, player.getClientVersion(), data, x, y, z);

                                if (collisionBox.isIntersected(checkBox)) {
                                    searchingFor.accept(new Vector3d(x, y, z));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static boolean onClimbable(GrimPlayer player, double x, double y, double z) {
        WrappedBlockState blockState = player.compensatedWorld.getBlock(x, y, z);
        StateType blockMaterial = blockState.getType();

        // ViaVersion replacement block -> glow berry vines (cave vines) -> fern
        if (blockMaterial == StateTypes.CAVE_VINES || blockMaterial == StateTypes.CAVE_VINES_PLANT) {
            return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_17);
        }

        if (player.tagManager.block(SyncedTags.CLIMBABLE).contains(blockMaterial)) {
            return true;
        }

        // ViaVersion replacement block -> sweet berry bush to vines
        if (blockMaterial == StateTypes.SWEET_BERRY_BUSH && player.getClientVersion().isOlderThan(ClientVersion.V_1_14)) {
            return true;
        }

        return trapdoorUsableAsLadder(player, x, y, z, blockState);
    }

    public static boolean trapdoorUsableAsLadder(GrimPlayer player, double x, double y, double z, WrappedBlockState blockData) {
        if (!BlockTags.TRAPDOORS.contains(blockData.getType())) return false;
        // Feature implemented in 1.9
        if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8)) return false;

        if (blockData.isOpen()) {
            WrappedBlockState blockBelow = player.compensatedWorld.getBlock(x, y - 1, z);

            if (blockBelow.getType() == StateTypes.LADDER) {
                return blockData.getFacing() == blockBelow.getFacing();
            }
        }

        return false;
    }

    public enum Axis {
        X {
            @Override
            public double choose(double x, double y, double z) {
                return x;
            }

            @Override
            public Direction getPositive() {
                return Direction.EAST;
            }
        },
        Y {
            @Override
            public double choose(double x, double y, double z) {
                return y;
            }

            @Override
            public Direction getPositive() {
                return Direction.UP;
            }
        },
        Z {
            @Override
            public double choose(double x, double y, double z) {
                return z;
            }

            @Override
            public Direction getPositive() {
                return Direction.SOUTH;
            }
        };


        public abstract double choose(double x, double y, double z);

        public abstract Direction getPositive();

    }
}
