package net.blosson.lflagger.checks;

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
     * Manually registers all available checks. This approach is more robust than reflection-based
     * classpath scanning, which can fail in non-standard environments like Android launchers.
     */
    private void loadChecks() {
        try {
            // Manually add each check instance. This is simple, reliable, and avoids classpath scanning issues.
            checks.add(new net.blosson.lflagger.checks.list.AntiKnockbackCheck());
            checks.add(new net.blosson.lflagger.checks.list.FlyCheck());
            checks.add(new net.blosson.lflagger.checks.list.NoFallCheck());
            checks.add(new net.blosson.lflagger.checks.list.SpeedCheck());
            checks.add(new net.blosson.lflagger.checks.list.StrafeCheck());
            checks.add(new net.blosson.lflagger.checks.list.MovementCheck());

            // Log the successful loading of each check.
            for (Check check : checks) {
                System.out.println("[LFlagger] Loaded check: " + check.getName());
            }
        } catch (Exception e) {
            // This will catch any unexpected errors during check instantiation.
            System.err.println("[LFlagger] A critical error occurred while manually loading checks.");
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