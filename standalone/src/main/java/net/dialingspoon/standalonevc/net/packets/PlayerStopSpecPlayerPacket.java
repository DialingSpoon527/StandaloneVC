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

public class PlayerStopSpecPlayerPacket implements StandalonePacket<PlayerStopSpecPlayerPacket> {

    public static final ResourceLocation PLAYER_STOP_SPEC_PLAYER = new ResourceLocation(StandaloneVoiceChat.MOD_ID, "player_stop_spec_player");

    private UUID uuid;

    public PlayerStopSpecPlayerPacket() {

    }

    public PlayerStopSpecPlayerPacket(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getPlayer() {
        return uuid;
    }

    @Override
    public ResourceLocation getIdentifier() {
        return PLAYER_STOP_SPEC_PLAYER;
    }

    @Override
    public PlayerStopSpecPlayerPacket fromBytes(ByteBuf buf) {
        uuid = BufferUtils.readUUID(buf);
        return this;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        BufferUtils.writeUUID(buf, uuid);
    }

    @Override
    public void handle(StandaloneCompatibilityManager compatibilityManager, MinecraftServerImpl minecraftServer) {
        ServerPlayerImpl player = (ServerPlayerImpl) minecraftServer.getPlayer(getPlayer());
        player.setCameraPlayer(player);
    }

}
