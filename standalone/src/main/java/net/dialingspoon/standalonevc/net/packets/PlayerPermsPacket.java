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

public class PlayerPermsPacket implements StandalonePacket<PlayerPermsPacket> {

    public static final ResourceLocation PLAYER_PERMS = new ResourceLocation(StandaloneVoiceChat.MOD_ID, "player_perms");

    private UUID uuid;
    private int level;

    public PlayerPermsPacket() {

    }

    public PlayerPermsPacket(UUID uuid, int level) {
        this.uuid = uuid;
        this.level = level;
    }

    public UUID getPlayer() {
        return uuid;
    }

    public int getLevel() {
        return level;
    }

    @Override
    public ResourceLocation getIdentifier() {
        return PLAYER_PERMS;
    }

    @Override
    public PlayerPermsPacket fromBytes(ByteBuf buf) {
        uuid = BufferUtils.readUUID(buf);
        level = buf.readInt();
        return this;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        BufferUtils.writeUUID(buf, uuid);
        buf.writeInt(level);
    }

    @Override
    public void handle(StandaloneCompatibilityManager compatibilityManager, MinecraftServerImpl minecraftServer) {
        ServerPlayerImpl player = (ServerPlayerImpl) minecraftServer.getPlayer(getPlayer());
        player.setPermission(getLevel());
    }

}
