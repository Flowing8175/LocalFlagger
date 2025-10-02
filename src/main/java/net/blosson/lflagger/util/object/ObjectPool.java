package net.blosson.lflagger.util.object;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

/**
 * A generic and reusable object pool designed to reduce object allocation and garbage collection overhead.
 * <p>
 * This is a key performance optimization, particularly for objects like {@link net.blosson.lflagger.simulation.SimulatedPlayer}
 * that are frequently created and discarded. Instead of instantiating a new object for every use,
 * the pool recycles and reuses existing instances, leading to smoother performance by minimizing GC pauses.
 *
 * @param <T> The type of object to be pooled. It is recommended that pooled objects have a
 *            reset method to re-initialize their state upon acquisition.
 */
public class ObjectPool<T> {

    private final Deque<T> pool = new ArrayDeque<>();
    private final Supplier<T> objectFactory;
    private final int maxSize;

    /**
     * Constructs a new ObjectPool.
     *
     * @param objectFactory A function (typically a constructor reference like {@code T::new}) that creates new objects when the pool is empty.
     * @param maxSize The maximum number of idle objects to store in the pool. This prevents the pool from holding onto an excessive number of objects during quiet periods.
     */
    public ObjectPool(Supplier<T> objectFactory, int maxSize) {
        this.objectFactory = objectFactory;
        this.maxSize = maxSize;
    }

    /**
     * Acquires an object from the pool.
     * <p>
     * If the pool contains an available object, it is returned. Otherwise, a new object is created
     * using the provided factory. The caller is responsible for calling {@link #release(T)} when done.
     *
     * @return A ready-to-use object, either recycled or newly created.
     */
    public T acquire() {
        T object = pool.poll();
        if (object == null) {
            object = objectFactory.get();
        }
        return object;
    }

    /**
     * Releases an object back to the pool for future reuse.
     * <p>
     * If the pool has not reached its maximum size, the object is added back to the pool.
     * Otherwise, the object is abandoned and will be handled by the garbage collector. This
     * prevents the pool from growing indefinitely.
     *
     * @param object The object to release back into the pool.
     */
    public void release(T object) {
        if (pool.size() < maxSize) {
            pool.add(object);
        }
        // If the pool is full, the object is intentionally not returned to the pool
        // and will be eligible for garbage collection.
    }
}