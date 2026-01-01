package net.dialingspoon.standalonevc.net.packets;

import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.voice.common.BufferUtils;
import de.maxhenkel.voicechat.voice.common.ResourceLocation;
import io.netty.buffer.ByteBuf;
import net.dialingspoon.standalonevc.StandaloneVoiceChat;
import net.dialingspoon.standalonevc.intercompatibility.StandaloneCompatibilityManager;
import net.dialingspoon.standalonevc.net.StandalonePacket;
import net.dialingspoon.standalonevc.plugins.impl.MinecraftServerImpl;
import net.dialingspoon.standalonevc.plugins.impl.ServerPlayerImpl;

import java.util.UUID;

public class PlayerLevelPacket implements StandalonePacket<PlayerLevelPacket> {

    public static final ResourceLocation PLAYER_LEVEL = new ResourceLocation(StandaloneVoiceChat.MOD_ID, "player_level");

    private UUID uuid;
    private String levelID;

    public PlayerLevelPacket() {

    }

    public PlayerLevelPacket(UUID uuid, String levelID) {
        this.uuid = uuid;
        this.levelID = levelID;
    }

    public UUID getPlayer() {
        return uuid;
    }

    public String getLevelID() {
        return levelID;
    }

    @Override
    public ResourceLocation getIdentifier() {
        return PLAYER_LEVEL;
    }

    @Override
    public PlayerLevelPacket fromBytes(ByteBuf buf) {
        uuid = BufferUtils.readUUID(buf);
        levelID = BufferUtils.readUtf(buf);
        return this;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        BufferUtils.writeUUID(buf, uuid);
        BufferUtils.writeUtf(buf, levelID);
    }

    @Override
    public void handle(StandaloneCompatibilityManager compatibilityManager, MinecraftServerImpl minecraftServer) {
        ServerPlayerImpl player = (ServerPlayerImpl) minecraftServer.getPlayer(getPlayer());
        ServerLevel serverLevel = minecraftServer.getServerLevel(getLevelID());
        player.setServerLevel(serverLevel);
    }

}

