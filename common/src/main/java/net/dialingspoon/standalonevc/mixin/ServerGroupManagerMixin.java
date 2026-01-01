package net.dialingspoon.standalonevc.mixin;

import de.maxhenkel.voicechat.voice.server.Group;
import de.maxhenkel.voicechat.voice.server.ServerGroupManager;
import net.dialingspoon.standalonevc.StandaloneVCCommon;
import net.dialingspoon.standalonevc.net.packets.SendGroupPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGroupManager.class)
public class ServerGroupManagerMixin {

    @Inject(method = "addGroup", at = @At("HEAD"))
    private void forwardGroup(Group group, de.maxhenkel.voicechat.api.ServerPlayer player, CallbackInfo ci) {
        StandaloneVCCommon.send(new SendGroupPacket(group, player.getUuid()));
    }
}
