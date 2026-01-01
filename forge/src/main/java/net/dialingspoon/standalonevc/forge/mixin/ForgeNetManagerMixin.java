package net.dialingspoon.standalonevc.forge.mixin;

import de.maxhenkel.voicechat.net.*;
import net.dialingspoon.standalonevc.StandaloneVCCommon;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ForgeNetManager.class)
public class ForgeNetManagerMixin {

    @Inject(method = "lambda$registerReceiver$1", at = @At(value = "INVOKE", target = "Lde/maxhenkel/voicechat/net/ClientServerChannel;onServerPacket(Lde/maxhenkel/voicechat/api/MinecraftServer;Lde/maxhenkel/voicechat/api/ServerPlayer;Ljava/lang/Object;Lde/maxhenkel/voicechat/net/Packet;)V", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD)
    private void forwardPlayerPackets(boolean toServer, Class packetType, ClientServerChannel c, NetworkEvent event, CallbackInfo ci, NetworkEvent.Context context, Packet packet) {
        if (RequestSecretPacket.class.isAssignableFrom(packetType) || CreateGroupPacket.class.isAssignableFrom(packetType) || PlayerStatePacket.class.isAssignableFrom(packetType)  || PlayerStatesPacket.class.isAssignableFrom(packetType)) {
            return;
        }
        StandaloneVCCommon.send(packet, event.getSource().get().getSender().getUUID());
    }
}
