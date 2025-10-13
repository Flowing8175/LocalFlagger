package net.blosson.lflagger.mixin;

import net.blosson.lflagger.checks.GroundSpoofCheck;
import net.blosson.lflagger.checks.NoFallCheck;
import net.blosson.lflagger.data.PlayerData;
import net.blosson.lflagger.util.AlertManager;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

    private final GroundSpoofCheck groundSpoofCheck = new GroundSpoofCheck();
    private final NoFallCheck noFallCheck = new NoFallCheck();
    private PlayerData lastPlayerData = null;

    @Inject(method = "sendMovementPackets", at = @At("HEAD"))
    private void onSendMovementPackets(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        PlayerData currentPlayerData = new PlayerData(player, lastPlayerData);

        if (groundSpoofCheck.check(player, currentPlayerData)) {
            AlertManager.sendAlert("GroundSpoof", 1);
        }

        if (noFallCheck.check(player, currentPlayerData, lastPlayerData)) {
            AlertManager.sendAlert("NoFall", 1);
        }

        lastPlayerData = currentPlayerData;
    }
}