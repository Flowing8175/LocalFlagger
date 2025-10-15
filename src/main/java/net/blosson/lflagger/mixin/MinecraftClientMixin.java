package net.blosson.lflagger.mixin;

import net.blosson.lflagger.LFlagger;
import net.blosson.lflagger.checks.CheckManager;
import net.blosson.lflagger.util.DamageTiltTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Shadow
    public ClientWorld world;

    /**
     * Injects into the client's main tick loop to run all cheat checks.
     * This is the main driver for the cheat detection system.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void onClientTick(CallbackInfo ci) {
        if (this.world != null) {
            this.world.getPlayers().forEach(player -> LFlagger.getInstance().getCheckManager().tick(player));
        }
    }
}