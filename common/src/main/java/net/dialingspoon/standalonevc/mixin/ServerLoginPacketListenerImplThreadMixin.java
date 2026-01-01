package net.dialingspoon.standalonevc.mixin;

import com.mojang.logging.LogUtils;
import net.dialingspoon.standalonevc.StandaloneVCCommon;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.server.network.ServerLoginPacketListenerImpl$1")
public class ServerLoginPacketListenerImplThreadMixin {

    @Unique
    private static Logger standalone_voice_chat$LOGGER = LogUtils.getLogger();

    @Shadow
    @Final ServerLoginPacketListenerImpl field_14176;

    @Inject(method = "run", at = @At("HEAD"), cancellable = true)
    private void acceptBotConnection(CallbackInfo ci) {
        if (field_14176.gameProfile.getName().equals("VoicechatBOT")) {
            if (StandaloneVCCommon.listener != null) {
                standalone_voice_chat$LOGGER.warn("Voicechat proxy server tried to connect when server already has one!");
                ci.cancel();
                return;
            }

            standalone_voice_chat$LOGGER.info("New voicechat proxy server connected");
            field_14176.state = ServerLoginPacketListenerImpl.State.READY_TO_ACCEPT;
            ci.cancel();
        }
    }
}
