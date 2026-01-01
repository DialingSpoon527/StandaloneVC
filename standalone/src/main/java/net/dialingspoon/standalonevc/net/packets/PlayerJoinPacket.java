package net.dialingspoon.standalonevc.net.packets;

import de.maxhenkel.voicechat.voice.common.BufferUtils;
import de.maxhenkel.voicechat.voice.common.ResourceLocation;
import io.netty.buffer.ByteBuf;
import net.dialingspoon.standalonevc.StandaloneVoiceChat;
import net.dialingspoon.standalonevc.intercompatibility.StandaloneCompatibilityManager;
import net.dialingspoon.standalonevc.net.StandalonePacket;
import net.dialingspoon.standalonevc.plugins.impl.MinecraftServerImpl;
import net.dialingspoon.standalonevc.plugins.impl.ServerPlayerImpl;

import java.util.UUID;

public class PlayerJoinPacket implements StandalonePacket<PlayerJoinPacket> {

    public static final ResourceLocation PLAYER_JOIN = new ResourceLocation(StandaloneVoiceChat.MOD_ID, "player_join");

    private UUID uuid;
    private String name;
    private String levelID;

    public PlayerJoinPacket() {

    }

    public PlayerJoinPacket(UUID uuid, String name, String levelID) {
        this.uuid = uuid;
        this.name = name;
        this.levelID = levelID;
    }

    public UUID getPlayer() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getLevelID() {
        return levelID;
    }

    @Override
    public ResourceLocation getIdentifier() {
        return PLAYER_JOIN;
    }

    @Override
    public PlayerJoinPacket fromBytes(ByteBuf buf) {
        uuid = BufferUtils.readUUID(buf);
        name = BufferUtils.readUtf(buf);
        levelID = BufferUtils.readUtf(buf);
        return this;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        BufferUtils.writeUUID(buf, uuid);
        BufferUtils.writeUtf(buf, name);
        BufferUtils.writeUtf(buf, levelID);
    }

    @Override
    public void handle(StandaloneCompatibilityManager compatibilityManager, MinecraftServerImpl minecraftServer) {
        ServerPlayerImpl player = (ServerPlayerImpl) minecraftServer.getPlayer(getPlayer());
        player.setName(getName());
        player.setServerLevel(minecraftServer.getServerLevel(getLevelID()));

        compatibilityManager.playerLoggedIn(player);
    }

}

