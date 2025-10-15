package net.blosson.lflagger.grim.prediction;

import net.minecraft.util.math.Vec3d;
import java.util.EnumSet;

public class VectorData {

    public final Vec3d vector;
    public final EnumSet<VectorType> types;
    public final VectorData lastVector;

    public VectorData(Vec3d vector, EnumSet<VectorType> types, VectorData lastVector) {
        this.vector = vector;
        this.types = types;
        this.lastVector = lastVector;
    }

    public VectorData(Vec3d vector, VectorType type) {
        this(vector, EnumSet.of(type), null);
    }

    public VectorData(Vec3d vector) {
        this(vector, EnumSet.noneOf(VectorType.class), null);
    }

    public VectorData returnNewModified(Vec3d newVector, VectorType newType) {
        EnumSet<VectorType> newTypes = this.types.clone();
        newTypes.add(newType);
        return new VectorData(newVector, newTypes, this);
    }

    public boolean isType(VectorType type) {
        return this.types.contains(type);
    }

    public enum VectorType {
        InputResult,
        Jump,
        Knockback,
        Explosion,
        AttackSlow,
        Climbable,
        StuckMultiplier,
        Trident,
        ZeroPointZeroThree, // Represents movement from a "skipped" tick
        BestVelPicked,
        SwimHop,
        Flip_Use_Item,
        FirstBreadExplosion,
        FirstBreadKb
    }
}