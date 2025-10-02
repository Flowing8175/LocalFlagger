package net.blosson.lflagger.mixin;

import net.blosson.lflagger.util.DamageTiltTracker;
import net.blosson.lflagger.util.TpsTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.DamageTiltS2CPacket;
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

    /**
     * Injects into the handler for DamageTiltS2CPacket to track when this animation event occurs.
     * This is the new way to detect the damage tilt effect in modern Minecraft versions.
     */
    @Inject(method = "onDamageTilt", at = @At("HEAD"))
    private void onDamageTilt(DamageTiltS2CPacket packet, CallbackInfo ci) {
        // We need the world to get the current tick
        if (MinecraftClient.getInstance().world == null) {
            return;
        }
        long currentTick = MinecraftClient.getInstance().world.getTime();
        DamageTiltTracker.getInstance().recordTilt(packet.id(), currentTick);
    }
}