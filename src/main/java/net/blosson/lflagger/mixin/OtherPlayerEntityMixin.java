package net.blosson.lflagger.mixin;

import net.blosson.lflagger.manager.PlayerManager;
import net.minecraft.client.network.OtherClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OtherClientPlayerEntity.class)
public class OtherPlayerEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        OtherClientPlayerEntity player = (OtherClientPlayerEntity) (Object) this;
        PlayerManager.getInstance().onPlayerTick(player);
    }
}