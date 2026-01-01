package net.dialingspoon.standalonevc.mixin;

import de.maxhenkel.voicechat.voice.common.Secret;
import de.maxhenkel.voicechat.voice.server.Server;
import net.dialingspoon.standalonevc.StandaloneVCCommon;
import net.dialingspoon.standalonevc.net.packets.SendSecretPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.UUID;

@Mixin(Server.class)
public class VoicechatServerMixin {

    @Inject(method = "getSecret", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void forwardSecret(UUID playerUUID, CallbackInfoReturnable<Secret> cir, Secret secret) {
        StandaloneVCCommon.send(new SendSecretPacket(playerUUID, secret));
    }
}
