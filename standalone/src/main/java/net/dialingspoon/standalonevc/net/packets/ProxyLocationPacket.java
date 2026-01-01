package net.dialingspoon.standalonevc.net.packets;

import de.maxhenkel.voicechat.voice.common.BufferUtils;
import de.maxhenkel.voicechat.voice.common.ResourceLocation;
import io.netty.buffer.ByteBuf;
import net.dialingspoon.standalonevc.StandaloneVoiceChat;
import net.dialingspoon.standalonevc.intercompatibility.StandaloneCompatibilityManager;
import net.dialingspoon.standalonevc.net.StandalonePacket;
import net.dialingspoon.standalonevc.plugins.impl.MinecraftServerImpl;

public class ProxyLocationPacket implements StandalonePacket<ProxyLocationPacket> {

    public static final ResourceLocation PROXY_LOCATION = new ResourceLocation(StandaloneVoiceChat.MOD_ID, "proxy_location");

    private String hostName;

    public ProxyLocationPacket() {

    }

    public ProxyLocationPacket(String hostName) {
        this.hostName = hostName;
    }

    public String getHostName() {
        return hostName;
    }

    @Override
    public ResourceLocation getIdentifier() {
        return PROXY_LOCATION;
    }

    @Override
    public ProxyLocationPacket fromBytes(ByteBuf buf) {
        hostName = BufferUtils.readUtf(buf);
        return this;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        BufferUtils.writeUtf(buf, hostName);
    }

    @Override
    public void handle(StandaloneCompatibilityManager compatibilityManager, MinecraftServerImpl minecraftServer) {
    }

}
