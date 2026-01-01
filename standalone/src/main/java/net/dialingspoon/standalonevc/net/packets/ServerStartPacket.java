package net.dialingspoon.standalonevc.net.packets;

import de.maxhenkel.voicechat.voice.common.BufferUtils;
import de.maxhenkel.voicechat.voice.common.ResourceLocation;
import io.netty.buffer.ByteBuf;
import net.dialingspoon.standalonevc.StandaloneVoiceChat;
import net.dialingspoon.standalonevc.intercompatibility.StandaloneCompatibilityManager;
import net.dialingspoon.standalonevc.net.StandalonePacket;
import net.dialingspoon.standalonevc.plugins.impl.MinecraftServerImpl;
import net.dialingspoon.standalonevc.plugins.impl.ServerLevelImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ServerStartPacket implements StandalonePacket<ServerStartPacket> {

    public static final ResourceLocation SERVER_START = new ResourceLocation(StandaloneVoiceChat.MOD_ID, "server_start");

    private int opPermissionLevel;
    private final List<String> serverLevelIDs = new ArrayList<>();

    public ServerStartPacket() {

    }

    public ServerStartPacket(int opPermissionLevel, Collection<String> serverLevelIDs) {
        this.opPermissionLevel = opPermissionLevel;
        this.serverLevelIDs.addAll(serverLevelIDs);
    }

    public int getOpPermissionLevel() {
        return opPermissionLevel;
    }

    public List<String> getServerLevels() {
        return serverLevelIDs;
    }

    @Override
    public ResourceLocation getIdentifier() {
        return SERVER_START;
    }

    @Override
    public ServerStartPacket fromBytes(ByteBuf buf) {
        opPermissionLevel = buf.readInt();
        int count = buf.readInt();
        serverLevelIDs.clear();
        for (int i = 0; i < count; i++) {
            serverLevelIDs.add(BufferUtils.readUtf(buf));
        }
        return this;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(opPermissionLevel);
        buf.writeInt(serverLevelIDs.size());
        serverLevelIDs.forEach(id -> BufferUtils.writeUtf(buf, id));
    }

    @Override
    public void handle(StandaloneCompatibilityManager compatibilityManager, MinecraftServerImpl minecraftServer) {
        minecraftServer = new MinecraftServerImpl();
        minecraftServer.setOpLevel(getOpPermissionLevel());
        minecraftServer.setServerLevels(getServerLevels().stream().map(ServerLevelImpl::new).collect(Collectors.toSet()));
        compatibilityManager.serverStarting(minecraftServer);
    }

}
