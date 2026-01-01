package net.dialingspoon.standalonevc.net.packets;

import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.intercompatibility.CommonCompatibilityManager;
import de.maxhenkel.voicechat.voice.common.BufferUtils;
import de.maxhenkel.voicechat.voice.common.ResourceLocation;
import io.netty.buffer.ByteBuf;
import net.dialingspoon.standalonevc.StandaloneVoiceChat;
import net.dialingspoon.standalonevc.intercompatibility.StandaloneCompatibilityManager;
import net.dialingspoon.standalonevc.net.StandalonePacket;
import net.dialingspoon.standalonevc.plugins.impl.MinecraftServerImpl;
import net.dialingspoon.standalonevc.plugins.impl.ServerPlayerImpl;

import java.util.UUID;

public class PlayerPosPacket implements StandalonePacket<PlayerPosPacket> {

    public static final ResourceLocation PLAYER_POS = new ResourceLocation(StandaloneVoiceChat.MOD_ID, "player_pos");

    private UUID uuid;
    private double x, y, z;
    private double eyeX, eyeY, eyeZ;

    public PlayerPosPacket() {

    }

    public PlayerPosPacket(UUID uuid, Position pos, Position eyePos) {
        this(uuid, pos.getX(), pos.getY(), pos.getZ(), eyePos.getX(), eyePos.getY(), eyePos.getZ());
    }

    public PlayerPosPacket(UUID uuid, double x, double y, double z, double eyeX, double eyeY, double eyeZ) {
        this.uuid = uuid;
        this.x = x;
        this.y = y;
        this.z = z;
        this.eyeX = eyeX;
        this.eyeY = eyeY;
        this.eyeZ = eyeZ;
    }

    public UUID getPlayer() {
        return uuid;
    }

    public Position getPos() {
        return CommonCompatibilityManager.INSTANCE.createPosition(x, y, z);
    }

    public Position getEyePos() {
        return CommonCompatibilityManager.INSTANCE.createPosition(eyeX, eyeY, eyeZ);
    }

    @Override
    public ResourceLocation getIdentifier() {
        return PLAYER_POS;
    }

    @Override
    public PlayerPosPacket fromBytes(ByteBuf buf) {
        uuid = BufferUtils.readUUID(buf);
        x = buf.readDouble();
        y = buf.readDouble();
        z = buf.readDouble();
        eyeX = buf.readDouble();
        eyeY = buf.readDouble();
        eyeZ = buf.readDouble();
        return this;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        BufferUtils.writeUUID(buf, uuid);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeDouble(eyeX);
        buf.writeDouble(eyeY);
        buf.writeDouble(eyeZ);
    }

    @Override
    public void handle(StandaloneCompatibilityManager compatibilityManager, MinecraftServerImpl minecraftServer) {
        ServerPlayerImpl player = (ServerPlayerImpl) minecraftServer.getPlayer(getPlayer());
        player.setPos(getPos());
        player.setEyePos(getEyePos());
    }

}
