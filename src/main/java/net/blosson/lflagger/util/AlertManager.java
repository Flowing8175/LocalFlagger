package net.blosson.lflagger.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class AlertManager {

    public static void sendAlert(String checkName, int violationLevel) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        Text prefix = Text.literal("[LFlagger]").formatted(Formatting.BOLD);
        Text nickname = Text.literal(" " + client.player.getName().getString() + " ").formatted(Formatting.YELLOW);
        Text failed = Text.literal("failed ").formatted(Formatting.WHITE);
        Text check = Text.literal(checkName + " ").formatted(Formatting.AQUA);
        Text vl = Text.literal("(VL: " + violationLevel + ")").formatted(Formatting.WHITE);

        client.player.sendMessage(prefix.copy().append(nickname).append(failed).append(check).append(vl), false);
    }
}