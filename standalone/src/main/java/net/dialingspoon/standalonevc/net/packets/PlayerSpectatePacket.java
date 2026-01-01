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

public class PlayerSpectatePacket implements StandalonePacket<PlayerSpectatePacket> {

    public static final ResourceLocation PLAYER_SPECTATE = new ResourceLocation(StandaloneVoiceChat.MOD_ID, "player_spectate");

    private UUID uuid;
    private boolean isSpectating;

    public PlayerSpectatePacket() {

    }

    public PlayerSpectatePacket(UUID uuid, boolean isSpectating) {
        this.uuid = uuid;
        this.isSpectating = isSpectating;
    }

    public UUID getPlayer() {
        return uuid;
    }

    public boolean isSpectating() {
        return isSpectating;
    }

    @Override
    public ResourceLocation getIdentifier() {
        return PLAYER_SPECTATE;
    }

    @Override
    public PlayerSpectatePacket fromBytes(ByteBuf buf) {
        uuid = BufferUtils.readUUID(buf);
        isSpectating = buf.readBoolean();
        return this;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        BufferUtils.writeUUID(buf, uuid);
        buf.writeBoolean(isSpectating);
    }

    @Override
    public void handle(StandaloneCompatibilityManager compatibilityManager, MinecraftServerImpl minecraftServer) {
        ServerPlayerImpl player = (ServerPlayerImpl) minecraftServer.getPlayer(getPlayer());
        player.setSpectator(isSpectating());
    }

}
