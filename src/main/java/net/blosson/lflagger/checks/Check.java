package net.blosson.lflagger.checks;

import net.blosson.lflagger.config.ConfigManager;
import net.blosson.lflagger.data.PlayerState;
import net.blosson.lflagger.simulation.SimulatedPlayer;
import net.blosson.lflagger.util.object.ObjectPool;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * The abstract base class for all cheat detections.
 * <p>
 * This class provides the foundational structure for any new check. It defines the core
 * {@link #tick} method where detection logic resides and provides several utility fields
 * and methods to reduce boilerplate code in concrete implementations. This includes
 * direct access to the {@link ConfigManager}, a helper for player validation, and
 * standardized methods for flagging suspicious behavior.
 */
public abstract class Check {

    private final String name;
    private final String description;
    protected final ConfigManager configManager = ConfigManager.getInstance();
    protected final MinecraftClient client = MinecraftClient.getInstance();

    /**
     * Constructs a new Check.
     * @param name The name of the check (e.g., "Fly", "Speed"). This is used in flag messages.
     * @param description A brief explanation of what the check does.
     */
    public Check(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * @return The name of the check.
     */
    public String getName() {
        return name;
    }

    /**
     * @return The description of the check.
     */
    public String getDescription() {
        return description;
    }

    /**
     * The core logic method for the check. This is called by the {@link CheckManager}
     * for every player on every game tick.
     *
     * @param player The {@link PlayerEntity} being checked in the current tick.
     * @param state The {@link PlayerState} object containing this player's tracked data.
     * @param simulatorPool The global {@link ObjectPool} for acquiring and releasing {@link SimulatedPlayer} instances.
     */
    public abstract void tick(PlayerEntity player, PlayerState state, ObjectPool<SimulatedPlayer> simulatorPool);

    /**
     * A utility method to perform common validation checks before running detection logic.
     * <p>
     * This helps reduce boilerplate code in subclasses by handling standard exclusion cases.
     * It should typically be the first call inside the {@link #tick} method.
     *
     * @param player The player to validate.
     * @return {@code true} if the player should be ignored by the check (e.g., they are the local player
     *         or in creative mode), {@code false} otherwise.
     */
    protected boolean isInvalid(PlayerEntity player) {
        // Ignore players in creative/spectator mode, or players who are allowed to fly.
        if (client.player == null) return true;
        if (player.getAbilities().allowFlying) {
            return true;
        }
        return !canCheckLocalPlayer() && player.getUuid().equals(client.player.getUuid());
    }

    public boolean canCheckLocalPlayer() {
        return false;
    }

    /**
     * Checks if this specific check is enabled in the mod's configuration file.
     * This allows users to toggle individual checks on or off.
     *
     * @return {@code true} if the check is enabled, {@code false} otherwise.
     */
    public abstract boolean isEnabled();

    /**
     * Sends a formatted flag message to the client's chat, including a certainty level.
     *
     * @param player The player suspected of cheating.
     * @param certainty The calculated certainty of the cheat detection, from 0.0 to 100.0.
     */
    protected void flag(PlayerEntity player, double certainty) {
        // Do not flag if certainty is zero, as this can be triggered by vanilla behavior.
        if (certainty <= 0) {
            return;
        }
        // Format the detection message as specified
        Text message = Text.literal("[LFlagger] ").formatted(Formatting.RED)
                .append(Text.literal(player.getName().getString() + " ").formatted(Formatting.WHITE))
                .append(Text.literal("is suspected of using ").formatted(Formatting.GRAY))
                .append(Text.literal(this.name + " ").formatted(Formatting.AQUA))
                .append(Text.literal("(Certainty: " + String.format("%.2f", certainty) + "%)").formatted(Formatting.YELLOW));

        // Send the message to the client's chat
        client.inGameHud.getChatHud().addMessage(message);
    }

    /**
     * Sends a formatted flag message to the client's chat without a certainty level.
     *
     * @param player The player suspected of cheating.
     */
    protected void flag(PlayerEntity player) {
        // Format the detection message as specified
        Text message = Text.literal("[LFlagger] ").formatted(Formatting.RED)
                .append(Text.literal(player.getName().getString() + " ").formatted(Formatting.WHITE))
                .append(Text.literal("is suspected of using ").formatted(Formatting.GRAY))
                .append(Text.literal(this.name).formatted(Formatting.AQUA));

        // Send the message to the client's chat
        client.inGameHud.getChatHud().addMessage(message);
    }
}