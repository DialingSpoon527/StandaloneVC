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

public class PlayerSpecPlayerPacket implements StandalonePacket<PlayerSpecPlayerPacket> {

    public static final ResourceLocation PLAYER_SPEC_PLAYER = new ResourceLocation(StandaloneVoiceChat.MOD_ID, "player_spec_player");

    private UUID spectator;
    private UUID victim;

    public PlayerSpecPlayerPacket() {

    }

    public PlayerSpecPlayerPacket(UUID spectator, UUID victim) {
        this.spectator = spectator;
        this.victim = victim;
    }

    public UUID getSpectator() {
        return spectator;
    }

    public UUID getVictim() {
        return victim;
    }

    @Override
    public ResourceLocation getIdentifier() {
        return PLAYER_SPEC_PLAYER;
    }

    @Override
    public PlayerSpecPlayerPacket fromBytes(ByteBuf buf) {
        spectator = BufferUtils.readUUID(buf);
        victim = BufferUtils.readUUID(buf);
        return this;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        BufferUtils.writeUUID(buf, spectator);
        BufferUtils.writeUUID(buf, victim);
    }

    @Override
    public void handle(StandaloneCompatibilityManager compatibilityManager, MinecraftServerImpl minecraftServer) {
        ServerPlayerImpl spec = (ServerPlayerImpl) minecraftServer.getPlayer(getSpectator());
        ServerPlayerImpl vic = (ServerPlayerImpl) minecraftServer.getPlayer(getVictim());
        spec.setCameraPlayer(vic);
    }

}
