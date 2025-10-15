package net.blosson.lflagger.mixin;

import net.blosson.lflagger.data.PlayerData;
import net.blosson.lflagger.data.PlayerDataProvider;
import net.blosson.lflagger.manager.TickManager;
import net.blosson.lflagger.manager.UncertaintyManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Queue;

import net.blosson.lflagger.LFlagger;
import net.blosson.lflagger.checks.CheckManager;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;

import java.util.UUID;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    /**
     * REFACTOR: This injection is new. It hooks into the packet that signals a player
     * leaving the game to trigger a cleanup in the CheckManager. This is critical
     * for preventing memory leaks by ensuring that the PlayerState for a disconnected
     * player is properly removed.
     *
     * @param packet The packet containing the UUIDs of the players who left.
     * @param ci     Callback info provided by Mixin.
     */
    @Inject(method = "onPlayerRemove", at = @At("TAIL"))
    private void onPlayerRemove(PlayerRemoveS2CPacket packet, CallbackInfo ci) {
        CheckManager checkManager = LFlagger.getInstance().getCheckManager();
        if (checkManager != null) {
            for (UUID playerUuid : packet.profileIds()) {
                checkManager.onPlayerLeave(playerUuid);
            }
        }
    }

    @Inject(method = "onPlayerPositionLook", at = @At("TAIL"))
    private void onPlayerPositionLook(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
        UncertaintyManager.getInstance().onPositionLookPacket(packet);
        // This packet is only sent for the client player, but the principle
        // of reconciliation can be applied to other players when their position is updated.
        // For now, this is a placeholder for future, more advanced logic.
    }

    @Inject(method = "onEntityVelocityUpdate", at = @At("TAIL"))
    private void onEntityVelocityUpdate(EntityVelocityUpdateS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && packet.getEntityId() == client.player.getId()) {
            UncertaintyManager.getInstance().onVelocityPacket(client.player, packet);
        }
    }

    @Inject(method = "onWorldTimeUpdate", at = @At("TAIL"))
    private void onWorldTimeUpdate(WorldTimeUpdateS2CPacket packet, CallbackInfo ci) {
        TickManager.getInstance().onWorldTimeUpdate(packet);
    }
}