package net.dialingspoon.standalonevc.net.packets;

import de.maxhenkel.voicechat.voice.common.ResourceLocation;
import io.netty.buffer.ByteBuf;
import net.dialingspoon.standalonevc.StandaloneVoiceChat;
import net.dialingspoon.standalonevc.intercompatibility.StandaloneCompatibilityManager;
import net.dialingspoon.standalonevc.net.StandalonePacket;
import net.dialingspoon.standalonevc.plugins.impl.MinecraftServerImpl;

public class ServerStopPacket implements StandalonePacket<ServerStopPacket> {

    public static final ResourceLocation SERVER_CHANGE = new ResourceLocation(StandaloneVoiceChat.MOD_ID, "server_stop");

    public ServerStopPacket() {

    }

    @Override
    public ResourceLocation getIdentifier() {
        return SERVER_CHANGE;
    }

    @Override
    public ServerStopPacket fromBytes(ByteBuf buf) {
        return this;
    }

    @Override
    public void toBytes(ByteBuf buf) {

    }

    @Override
    public void handle(StandaloneCompatibilityManager compatibilityManager, MinecraftServerImpl minecraftServer) {
        compatibilityManager.serverStopping(minecraftServer);
    }

}
