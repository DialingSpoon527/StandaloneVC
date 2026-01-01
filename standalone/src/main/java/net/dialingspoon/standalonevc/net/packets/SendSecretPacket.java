package net.dialingspoon.standalonevc.net.packets;

import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.intercompatibility.CommonCompatibilityManager;
import de.maxhenkel.voicechat.voice.common.BufferUtils;
import de.maxhenkel.voicechat.voice.common.ResourceLocation;
import de.maxhenkel.voicechat.voice.common.Secret;
import de.maxhenkel.voicechat.voice.server.Server;
import io.netty.buffer.ByteBuf;
import net.dialingspoon.standalonevc.StandaloneVoiceChat;
import net.dialingspoon.standalonevc.intercompatibility.StandaloneCompatibilityManager;
import net.dialingspoon.standalonevc.net.StandalonePacket;
import net.dialingspoon.standalonevc.plugins.impl.MinecraftServerImpl;

import java.util.Map;
import java.util.UUID;

public class SendSecretPacket implements StandalonePacket<SendSecretPacket> {

    public static final ResourceLocation PLAYER_SECRET = new ResourceLocation(StandaloneVoiceChat.MOD_ID, "player_secret");

    private UUID uuid;
    private Secret secret;

    public SendSecretPacket() {

    }

    public SendSecretPacket(UUID uuid, Secret secret) {
        this.uuid = uuid;
        this.secret = secret;
    }

    public UUID getPlayer() {
        return uuid;
    }

    public Secret getSecret() {
        return secret;
    }

    @Override
    public ResourceLocation getIdentifier() {
        return PLAYER_SECRET;
    }

    @Override
    public SendSecretPacket fromBytes(ByteBuf buf) {
        uuid = BufferUtils.readUUID(buf);
        secret = Secret.fromBytes(buf);
        return this;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        BufferUtils.writeUUID(buf, uuid);
        secret.toBytes(buf);
    }

    @Override
    public void handle(StandaloneCompatibilityManager compatibilityManager, MinecraftServerImpl minecraftServer) {
        Server voiceServer = Voicechat.SERVER.getServer();
        CommonCompatibilityManager.INSTANCE.emitPlayerCompatibilityCheckSucceeded(voiceServer.getServer().getPlayer(getPlayer()));
        try {
            ((Map<UUID, Secret>) StandaloneVoiceChat.getServerSecretsField().get(voiceServer)).put(getPlayer(), getSecret());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}

