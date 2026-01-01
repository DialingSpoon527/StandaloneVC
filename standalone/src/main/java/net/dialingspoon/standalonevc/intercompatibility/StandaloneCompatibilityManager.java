package net.dialingspoon.standalonevc.intercompatibility;

import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.intercompatibility.CommonCompatibilityManager;
import de.maxhenkel.voicechat.net.NetManager;
import de.maxhenkel.voicechat.permission.PermissionManager;
import de.maxhenkel.voicechat.plugins.impl.VoicechatServerApiImpl;
import de.maxhenkel.voicechat.voice.common.BufferUtils;
import de.maxhenkel.voicechat.voice.common.ResourceLocation;
import io.netty.buffer.ByteBuf;
import net.dialingspoon.standalonevc.StandaloneVoiceChat;
import net.dialingspoon.standalonevc.net.StandaloneNetManager;
import net.dialingspoon.standalonevc.net.client.MinimalMcClient;
import net.dialingspoon.standalonevc.permission.StandalonePermissionManager;
import net.dialingspoon.standalonevc.plugins.impl.PositionImpl;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class StandaloneCompatibilityManager extends CommonCompatibilityManager {

    private final List<Consumer<MinecraftServer>> serverStartingEvents = new CopyOnWriteArrayList<>();
    private final List<Consumer<MinecraftServer>> serverStoppingEvents = new CopyOnWriteArrayList<>();
    private final List<Consumer<ServerPlayer>> playerLoggedInEvents = new CopyOnWriteArrayList<>();
    private final List<Consumer<ServerPlayer>> playerLoggedOutEvents = new CopyOnWriteArrayList<>();
    private final List<Consumer<ServerPlayer>> voicechatConnectEvents = new CopyOnWriteArrayList<>();
    private final List<Consumer<ServerPlayer>> voicechatCompatibilityCheckSucceededEvents = new CopyOnWriteArrayList<>();
    private final List<Consumer<UUID>> voicechatDisconnectEvents = new CopyOnWriteArrayList<>();

    public void serverStarting(MinecraftServer server) {
        serverStartingEvents.forEach(consumer -> consumer.accept(server));
    }

    public void serverStopping(MinecraftServer server) {
        serverStoppingEvents.forEach(consumer -> consumer.accept(server));
    }

    public void playerLoggedIn(ServerPlayer player) {
        playerLoggedInEvents.forEach(consumer -> consumer.accept(player));
    }

    public void playerLoggedOut(ServerPlayer player) {
        playerLoggedOutEvents.forEach(consumer -> consumer.accept(player));
    }

    @Override
    public void emitServerVoiceChatConnectedEvent(ServerPlayer player) {
        voicechatConnectEvents.forEach(consumer -> consumer.accept(player));
    }

    @Override
    public void emitServerVoiceChatDisconnectedEvent(UUID clientID) {
        voicechatDisconnectEvents.forEach(consumer -> consumer.accept(clientID));
    }

    @Override
    public void emitPlayerCompatibilityCheckSucceeded(ServerPlayer player) {
        voicechatCompatibilityCheckSucceededEvents.forEach(consumer -> consumer.accept(player));
    }

    @Override
    public void onServerVoiceChatConnected(Consumer<ServerPlayer> onVoiceChatConnected) {
        voicechatConnectEvents.add(onVoiceChatConnected);
    }

    @Override
    public void onServerVoiceChatDisconnected(Consumer<UUID> onVoiceChatDisconnected) {
        voicechatDisconnectEvents.add(onVoiceChatDisconnected);
    }

    @Override
    public void onServerStarting(Consumer<MinecraftServer> onServerStarting) {
        serverStartingEvents.add(onServerStarting);
    }

    @Override
    public void onServerStopping(Consumer<MinecraftServer> onServerStopping) {
        serverStoppingEvents.add(onServerStopping);
    }

    @Override
    public void onPlayerLoggedIn(Consumer<ServerPlayer> onPlayerLoggedIn) {
        playerLoggedInEvents.add(onPlayerLoggedIn);
    }

    @Override
    public void onPlayerLoggedOut(Consumer<ServerPlayer> onPlayerLoggedOut) {
        playerLoggedOutEvents.add(onPlayerLoggedOut);
    }

    @Override
    public void onPlayerCompatibilityCheckSucceeded(Consumer<ServerPlayer> onPlayerCompatibilityCheckSucceeded) {
        voicechatCompatibilityCheckSucceededEvents.add(onPlayerCompatibilityCheckSucceeded);
    }

    @Override
    public String getModVersion() {
        return "1.20.1-2.6.7";
    }

    @Override
    public String getModName() {
        return "standalonevc";
    }

    @Override
    public Path getGameDirectory() {
        return StandaloneVoiceChat.PATH;
    }

    private StandaloneNetManager netManager;

    @Override
    public NetManager getNetManager() {
        if (netManager == null) {
            netManager = new StandaloneNetManager();
        }
        return netManager;
    }

    @Override
    public boolean isDevEnvironment() {
        return true;
    }

    @Override
    public PermissionManager createPermissionManager() {
        return new StandalonePermissionManager();
    }

    @Override
    public VoicechatServerApi getServerApi() {
        return VoicechatServerApiImpl.INSTANCE;
    }

    @Override
    public void executeOnMinecraftServer(MinecraftServer server, Runnable runnable) {
        runnable.run();
    }

    @Override
    public Position createPosition(double x, double y, double z) {
        return new PositionImpl(x, y, z);
    }

    @Override
    public void sendMinecraftPacket(ServerPlayer player, ResourceLocation id, ByteBuf buffer) {
        if (id.value().equals("state")) {
            MinimalMcClient.send(id, buffer);
        }
        if (id.value().equals("states")) {
            BufferUtils.writeUUID(buffer, player.getUuid());
            MinimalMcClient.send(id, buffer);
        }
    }




    // Unused by me
    @Override public void registerCommands() {}
    @Override public boolean isDedicatedServer() {
        return true;
    }
    @Override public void displayClientMessage(ServerPlayer player, String message, boolean overlay) {}
    @Override public void createTimeoutTimer(ServerPlayer player) {}
    @Override public void sendIncompatibleMessage(ServerPlayer serverPlayer, int compatibilityVersion) {}




    // should use maybeeventually
    @Override public void onPlayerHide(BiConsumer<ServerPlayer, ServerPlayer> onPlayerHide) {}
    @Override public void onPlayerShow(BiConsumer<ServerPlayer, ServerPlayer> onPlayerShow) {}
    @Override public boolean isModLoaded(String modId) {
        return false;
    }
    @Override public List<VoicechatPlugin> loadPlugins() {
        return List.of();
    }
    @Override public boolean canSee(ServerPlayer player, ServerPlayer other) {
        return true;
    }
}
