package net.dialingspoon.standalonevc.mixin;

import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.voice.server.ServerVoiceEvents;
import net.dialingspoon.standalonevc.StandaloneVCCommon;
import net.dialingspoon.standalonevc.net.packets.PlayerJoinPacket;
import net.dialingspoon.standalonevc.net.packets.PlayerLeavePacket;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerVoiceEvents.class)
public class ServerVoiceEventsMixin {

    @Inject(method = "playerLoggedIn", at = @At("HEAD"))
    private void forwardLogIn(ServerPlayer serverPlayer, CallbackInfo ci) {
        StandaloneVCCommon.send(new PlayerJoinPacket(serverPlayer.getUuid(), serverPlayer.getName(), ((ServerLevel)serverPlayer.getLevel().getServerLevel()).dimension().location().toString()));
    }

    @Inject(method = "playerLoggedOut", at = @At("HEAD"))
    private void forwardLogOut(ServerPlayer serverPlayer, CallbackInfo ci) {
        StandaloneVCCommon.send(new PlayerLeavePacket(serverPlayer.getUuid()));
    }
}
