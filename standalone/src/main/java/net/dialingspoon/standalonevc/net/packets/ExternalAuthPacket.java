package net.dialingspoon.standalonevc.net.packets;

import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.voice.common.ResourceLocation;
import io.netty.buffer.ByteBuf;
import net.dialingspoon.standalonevc.StandaloneVoiceChat;
import net.dialingspoon.standalonevc.intercompatibility.StandaloneCompatibilityManager;
import net.dialingspoon.standalonevc.net.StandalonePacket;
import net.dialingspoon.standalonevc.net.client.MinimalMcClient;
import net.dialingspoon.standalonevc.net.client.ServerHandler;
import net.dialingspoon.standalonevc.plugins.impl.MinecraftServerImpl;

import java.net.URI;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class ExternalAuthPacket implements StandalonePacket<ExternalAuthPacket> {

    public static final ResourceLocation External_Auth = new ResourceLocation(StandaloneVoiceChat.MOD_ID, "external_auth");

    private byte[] data;

    public ExternalAuthPacket() {

    }

    public ExternalAuthPacket(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public ResourceLocation getIdentifier() {
        return External_Auth;
    }

    @Override
    public ExternalAuthPacket fromBytes(ByteBuf buf) {
        data = new byte[32];
        buf.readBytes(data);
        return this;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBytes(data);
    }

    @Override
    public void handle(StandaloneCompatibilityManager compatibilityManager, MinecraftServerImpl minecraftServer) {
        try {
            MinimalMcClient.sendPacket(new ExternalAuthPacket(ServerHandler.hmacSha256(StandaloneVoiceChat.PROXY_CONFIG.proxyPassword.get(), getData())));
            sendServerInfo();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendServerInfo() {
        String ip = Voicechat.SERVER_CONFIG.voiceChatBindAddress.get();
        int port = Voicechat.SERVER_CONFIG.voiceChatPort.get();
        String voiceHost = Voicechat.SERVER_CONFIG.voiceHost.get();

        if (!voiceHost.isEmpty()) {
            try {
                int parsedPort = Integer.parseInt(voiceHost);
                if (parsedPort > 0 && parsedPort <= 65535) {
                    port = parsedPort;
                } else {
                    StandaloneVoiceChat.LOGGER.warn("Invalid voice host port: {}", parsedPort);
                }
            } catch (NumberFormatException ignored) {
                try {
                    URI uri = new URI("voicechat://" + voiceHost);
                    if (uri.getHost() != null) {
                        ip = uri.getHost();
                    }
                    if (uri.getPort() > 0) {
                        port = uri.getPort();
                    }
                } catch (Exception e) {
                    StandaloneVoiceChat.LOGGER.warn("Failed to parse voice host", e);
                }
            }
        }

        String address = ip.isEmpty() ? String.valueOf(port) : ip + ":" + port;
        MinimalMcClient.sendPacket(new ProxyLocationPacket(address));
    }

}
