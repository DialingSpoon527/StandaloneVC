package net.dialingspoon.standalonevc.net.packets;

import de.maxhenkel.voicechat.voice.common.BufferUtils;
import de.maxhenkel.voicechat.voice.common.ResourceLocation;
import io.netty.buffer.ByteBuf;
import net.dialingspoon.standalonevc.StandaloneVoiceChat;
import net.dialingspoon.standalonevc.intercompatibility.StandaloneCompatibilityManager;
import net.dialingspoon.standalonevc.net.StandalonePacket;
import net.dialingspoon.standalonevc.plugins.impl.MinecraftServerImpl;

import java.util.UUID;

public class PlayerLeavePacket implements StandalonePacket<PlayerLeavePacket> {

    public static final ResourceLocation PLAYER_LEAVE = new ResourceLocation(StandaloneVoiceChat.MOD_ID, "player_leave");

    private UUID uuid;

    public PlayerLeavePacket() {

    }

    public PlayerLeavePacket(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getPlayer() {
        return uuid;
    }

    @Override
    public ResourceLocation getIdentifier() {
        return PLAYER_LEAVE;
    }

    @Override
    public PlayerLeavePacket fromBytes(ByteBuf buf) {
        uuid = BufferUtils.readUUID(buf);
        return this;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        BufferUtils.writeUUID(buf, uuid);
    }

    @Override
    public void handle(StandaloneCompatibilityManager compatibilityManager, MinecraftServerImpl minecraftServer) {
        compatibilityManager.playerLoggedOut(minecraftServer.getPlayer(getPlayer()));
    }

}
