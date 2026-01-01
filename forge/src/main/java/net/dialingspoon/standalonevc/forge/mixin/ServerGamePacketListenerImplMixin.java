package net.dialingspoon.standalonevc.forge.mixin;

import net.dialingspoon.standalonevc.StandaloneVCCommon;
import net.dialingspoon.standalonevc.net.packets.PlayerPosPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {

    @Shadow public ServerPlayer player;

    @Inject(method = "handleMovePlayer", at = @At("RETURN"))
    private void forwardPlayerMovePackets(ServerboundMovePlayerPacket arg, CallbackInfo ci) {
        StandaloneVCCommon.send(new PlayerPosPacket(player.getUUID(), player.getX(), player.getY(), player.getZ(), player.getEyePosition().x, player.getEyePosition().y, player.getEyePosition().z));
    }
}
