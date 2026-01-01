package net.dialingspoon.standalonevc.mixin;

import com.mojang.authlib.GameProfile;
import net.dialingspoon.standalonevc.PlatformSpecific;
import net.dialingspoon.standalonevc.ServerGamePacketListenerNoPlayer;
import net.dialingspoon.standalonevc.StandaloneVCCommon;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.SocketAddress;

@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginPacketListenerImplMixin {

    @Shadow @Nullable
    public GameProfile gameProfile;

    @Shadow
    @Final MinecraftServer server;

    @Shadow
    @Final Connection connection;

    @Redirect(method = "handleAcceptedLogin", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;canPlayerLogin(Ljava/net/SocketAddress;Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/network/chat/Component;"))
    private Component dontMakePlayer(PlayerList instance, SocketAddress mutablecomponent1, GameProfile ipbanlistentry) {
        if (gameProfile.getName().equals("VoicechatBOT")) {
            return null;
        }
        return instance.canPlayerLogin(mutablecomponent1, ipbanlistentry);
    }

    @Inject(method = "handleAcceptedLogin", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;getPlayer(Ljava/util/UUID;)Lnet/minecraft/server/level/ServerPlayer;"), cancellable = true)
    private void dontMakePlayer(CallbackInfo ci) {
        if (gameProfile.getName().equals("VoicechatBOT")) {
            StandaloneVCCommon.listener = new ServerGamePacketListenerNoPlayer(server, connection);
            PlatformSpecific.sendMCRegistryPackets(connection, "PLAY_TO_CLIENT");
            StandaloneVCCommon.listener.sendAuthPacket();
            ci.cancel();
        }
    }

}
