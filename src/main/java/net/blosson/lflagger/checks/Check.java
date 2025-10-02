package net.blosson.lflagger.checks;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public abstract class Check {

    private final String name;
    private final String description;

    public Check(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Sends a formatted flag message to the client's chat.
     * @param player The player suspected of cheating.
     * @param certainty The calculated certainty of the cheat detection.
     */
    protected void flag(PlayerEntity player, double certainty) {
        // Format the detection message as specified
        Text message = Text.literal("[LFlagger] ").formatted(Formatting.RED)
                .append(Text.literal(player.getName().getString() + " ").formatted(Formatting.WHITE))
                .append(Text.literal("is suspected of using ").formatted(Formatting.GRAY))
                .append(Text.literal(this.name + " ").formatted(Formatting.AQUA))
                .append(Text.literal("(Certainty: " + String.format("%.2f", certainty) + "%)").formatted(Formatting.YELLOW));

        // Send the message to the client's chat
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(message);
    }

    /**
     * Sends a formatted flag message to the client's chat without certainty.
     * @param player The player suspected of cheating.
     */
    protected void flag(PlayerEntity player) {
        // Format the detection message as specified
        Text message = Text.literal("[LFlagger] ").formatted(Formatting.RED)
                .append(Text.literal(player.getName().getString() + " ").formatted(Formatting.WHITE))
                .append(Text.literal("is suspected of using ").formatted(Formatting.GRAY))
                .append(Text.literal(this.name).formatted(Formatting.AQUA));

        // Send the message to the client's chat
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(message);
    }
}