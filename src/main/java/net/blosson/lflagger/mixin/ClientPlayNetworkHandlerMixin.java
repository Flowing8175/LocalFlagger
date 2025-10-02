package net.blosson.lflagger.mixin;

import net.blosson.lflagger.util.TpsTracker;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    /**
     * Injects into the handler for WorldTimeUpdateS2CPacket to track server ticks.
     * This packet is sent by the server every tick, making it a reliable source for TPS calculation.
     */
    @Inject(method = "onWorldTimeUpdate", at = @At("HEAD"))
    private void onWorldTimeUpdate(WorldTimeUpdateS2CPacket packet, CallbackInfo ci) {
        TpsTracker.getInstance().onTimeUpdatePacket();
    }
}