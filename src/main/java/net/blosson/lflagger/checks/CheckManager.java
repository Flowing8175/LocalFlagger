package net.blosson.lflagger.checks;

import com.google.common.reflect.ClassPath;
import net.blosson.lflagger.data.PlayerState;
import net.blosson.lflagger.simulation.SimulatedPlayer;
import net.blosson.lflagger.util.object.ObjectPool;
import net.minecraft.entity.player.PlayerEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages the entire lifecycle of cheat checks and player-specific data.
 * <p>
 * This class acts as the central orchestrator for the mod. Its key responsibilities include:
 * 1.  <b>Dynamic Check Loading:</b> On startup, it scans a specific package for all classes that
 *     extend {@link Check} and automatically instantiates them. This makes the system modular
 *     and extensible, as new checks can be added without modifying this manager.
 * 2.  <b>Player State Management:</b> It maintains a map of {@link PlayerState} objects, creating a new
 *     one for each player that appears and crucially, removing it when the player disconnects
 *     to prevent memory leaks.
 * 3.  <b>Tick-Based Execution:</b> It is called every game tick for every player, and it, in turn,
 *     calls the {@code tick} method on every loaded check, driving the detection process.
 */
public class CheckManager {

    private final List<Check> checks = new ArrayList<>();
    private final Map<UUID, PlayerState> playerStates = new ConcurrentHashMap<>();
    private final ObjectPool<SimulatedPlayer> simulatorPool;

    public CheckManager() {
        // Use an object pool for SimulatedPlayer to improve performance by recycling objects.
        this.simulatorPool = new ObjectPool<>(SimulatedPlayer::new, 20); // Pool up to 20 simulator objects
        loadChecks();
    }

    /**
     * Uses Guava's ClassPath reflection to find and instantiate all classes that extend {@link Check}
     * in the specified package. This makes the system extensible, as new checks
     * can be added without needing to register them manually.
     */
    private void loadChecks() {
        final String packageName = "net.blosson.lflagger.checks.list";
        try {
            // Use the context class loader, which is aware of mod classes in Fabric.
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            ClassPath classPath = ClassPath.from(classLoader);

            List<Class<?>> checkClasses = classPath.getTopLevelClasses(packageName)
                    .stream()
                    .map(classInfo -> {
                        try {
                            return classInfo.load();
                        } catch (NoClassDefFoundError e) {
                            // Log the error but don't crash; this can happen in some dev environments.
                            System.err.println("Could not load class: " + classInfo.getName() + ". This may be expected in a development environment.");
                            return null;
                        }
                    })
                    .filter(java.util.Objects::nonNull) // Filter out classes that failed to load
                    .collect(Collectors.toList());

            for (Class<?> clazz : checkClasses) {
                // Ensure the class is a concrete implementation of Check.
                if (Check.class.isAssignableFrom(clazz) && !clazz.isInterface() && !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
                    try {
                        Check check = (Check) clazz.getDeclaredConstructor().newInstance();
                        checks.add(check);
                        System.out.println("[LFlagger] Loaded check: " + check.getName());
                    } catch (Exception e) {
                        System.err.println("[LFlagger] Failed to instantiate check: " + clazz.getName());
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[LFlagger] Failed to scan for checks in package: " + packageName);
            e.printStackTrace();
        }
    }

    /**
     * The main entry point for processing a player's movement and actions for a single game tick.
     * It retrieves or creates the player's state, runs all active checks, and then updates the
     * state for the next tick.
     *
     * @param player The player to check.
     */
    public void tick(PlayerEntity player) {
        // Get or create the state for the player.
        PlayerState state = playerStates.computeIfAbsent(player.getUuid(), u -> new PlayerState(player));

        // Tick all loaded checks for the player.
        for (Check check : checks) {
            // Pass the player, their state, and the simulator pool to each check.
            check.tick(player, state, simulatorPool);
        }

        // After all checks have run, update the player's state for the next tick's comparisons.
        state.update();
    }

    /**
     * Called when a player leaves the game to clean up their state data.
     * This is crucial to prevent a memory leak from holding onto data for disconnected players.
     *
     * @param playerUuid The UUID of the player who left.
     */
    public void onPlayerLeave(UUID playerUuid) {
        if (playerStates.remove(playerUuid) != null) {
            System.out.println("[LFlagger] Cleaned up state for player " + playerUuid);
        }
    }
}