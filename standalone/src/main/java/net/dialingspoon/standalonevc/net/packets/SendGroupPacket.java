package net.dialingspoon.standalonevc.net.packets;

import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.plugins.impl.GroupImpl;
import de.maxhenkel.voicechat.voice.common.BufferUtils;
import de.maxhenkel.voicechat.voice.common.ResourceLocation;
import de.maxhenkel.voicechat.voice.server.Group;
import io.netty.buffer.ByteBuf;
import net.dialingspoon.standalonevc.StandaloneVoiceChat;
import net.dialingspoon.standalonevc.intercompatibility.StandaloneCompatibilityManager;
import net.dialingspoon.standalonevc.net.StandalonePacket;
import net.dialingspoon.standalonevc.plugins.impl.MinecraftServerImpl;

import java.util.UUID;

public class SendGroupPacket implements StandalonePacket<SendGroupPacket> {

    public static final ResourceLocation SEND_GROUP = new ResourceLocation(StandaloneVoiceChat.MOD_ID, "send_group");

    private Group group;
    private UUID player;

    public SendGroupPacket() {

    }

    public SendGroupPacket(Group group, UUID player) {
        this.group = group;
        this.player = player;
    }

    public Group getGroup() {
        return group;
    }

    public UUID getPlayer() {
        return player;
    }

    @Override
    public ResourceLocation getIdentifier() {
        return SEND_GROUP;
    }

    @Override
    public SendGroupPacket fromBytes(ByteBuf buf) {
        group = new Group(BufferUtils.readUUID(buf), BufferUtils.readUtf(buf, 512), buf.readBoolean() ? BufferUtils.readUtf(buf, 512) : null, buf.readBoolean(), buf.readBoolean(), GroupImpl.TypeImpl.fromInt(buf.readShort()));
        player = BufferUtils.readUUID(buf);
        return this;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        BufferUtils.writeUUID(buf, group.getId());
        BufferUtils.writeUtf(buf, group.getName(), 512);
        buf.writeBoolean(group.getPassword() != null);
        if (group.getPassword() != null) {
            BufferUtils.writeUtf(buf, group.getPassword(), 512);
        }
        buf.writeBoolean(group.isPersistent());
        buf.writeBoolean(group.isHidden());
        buf.writeShort(GroupImpl.TypeImpl.toInt(group.getType()));
        BufferUtils.writeUUID(buf, player);
    }

    @Override
    public void handle(StandaloneCompatibilityManager compatibilityManager, MinecraftServerImpl minecraftServer) {
        Voicechat.SERVER.getServer().getGroupManager().addGroup(getGroup(), minecraftServer.getPlayer(getPlayer()));
    }

}
