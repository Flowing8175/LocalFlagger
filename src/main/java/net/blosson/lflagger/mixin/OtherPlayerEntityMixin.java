package net.blosson.lflagger.mixin;

import net.minecraft.client.network.OtherClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(OtherClientPlayerEntity.class)
public class OtherPlayerEntityMixin {
    // This mixin is now empty as check processing has been centralized
    // in MinecraftClientMixin to avoid running checks on the local player.
}