package net.dialingspoon.standalonevc;

import com.mojang.logging.LogUtils;
import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.intercompatibility.CommonCompatibilityManager;
import de.maxhenkel.voicechat.net.*;
import de.maxhenkel.voicechat.plugins.PluginManager;
import de.maxhenkel.voicechat.voice.common.BufferUtils;
import de.maxhenkel.voicechat.voice.common.Secret;
import de.maxhenkel.voicechat.voice.server.Server;
import io.netty.buffer.Unpooled;
import net.dialingspoon.standalonevc.net.client.ServerHandler;
import net.dialingspoon.standalonevc.net.packets.*;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import org.slf4j.Logger;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ServerGamePacketListenerNoPlayer implements ServerPlayerConnection, TickablePacketListener, ServerGamePacketListener {
    static final Logger LOGGER = LogUtils.getLogger();
    private final Connection connection;
    private final MinecraftServer server;
    private long keepAliveTime;
    private boolean keepAlivePending;
    private long keepAliveChallenge;
    private volatile boolean isAuthed;
    private final byte[] nonce = new byte[32];

    public ServerGamePacketListenerNoPlayer(MinecraftServer minecraftServer, Connection connection) {
        this.server = minecraftServer;
        this.connection = connection;
        connection.setListener(this);
        this.keepAliveTime = Util.getMillis();
    }

    public void tick() {
        this.server.getProfiler().push("keepAlive");
        long l = Util.getMillis();
        if (l - this.keepAliveTime >= 15000L) {
            if (this.keepAlivePending) {
                this.disconnect(Component.translatable("disconnect.timeout"));
            } else {
                this.keepAlivePending = true;
                this.keepAliveTime = l;
                this.keepAliveChallenge = l;
                this.innerSend(new ClientboundKeepAlivePacket(this.keepAliveChallenge));
            }
        }

        this.server.getProfiler().pop();
    }

    public boolean isAcceptingMessages() {
        return this.connection.isConnected();
    }

    public void disconnect(Component component) {
        this.connection.send(new ClientboundDisconnectPacket(component), PacketSendListener.thenRun(() -> this.connection.disconnect(component)));
        this.connection.setReadOnly();
        Objects.requireNonNull(connection);
        this.server.executeBlocking(connection::handleDisconnection);
    }

    public void onDisconnect(Component component) {
        LOGGER.info("voicechat lost connection: {}", component.getString());
        StandaloneVCCommon.listener = null;
    }

    public void sendAuthPacket(){
        SecureRandom random = new SecureRandom();
        random.nextBytes(nonce);
        ExternalAuthPacket packet = new ExternalAuthPacket(nonce);
        FriendlyByteBuf out = new FriendlyByteBuf(Unpooled.buffer());
        packet.toBytes(out);
        connection.send(new ClientboundCustomPayloadPacket(new ResourceLocation(packet.getIdentifier().key(), packet.getIdentifier().value()), out));
    }

    public void send(Packet<?> packet) {
        if (isAuthed) {
            innerSend(packet);
        } else LOGGER.warn("Trying to send packet to unauthenticated external server");
    }

    private void innerSend(Packet<?> packet) {
        try {
            this.connection.send(packet);
        } catch (Throwable throwable) {
            CrashReport crashReport = CrashReport.forThrowable(throwable, "Sending packet");
            CrashReportCategory crashReportCategory = crashReport.addCategory("Packet being sent");
            crashReportCategory.setDetail("Packet class", () -> packet.getClass().getCanonicalName());
            throw new ReportedException(crashReport);
        }
    }

    public void handleKeepAlive(ServerboundKeepAlivePacket serverboundKeepAlivePacket) {
        if (this.keepAlivePending && serverboundKeepAlivePacket.getId() == this.keepAliveChallenge) {
            this.keepAlivePending = false;
        }
    }

    public void handleCustomPayload(ServerboundCustomPayloadPacket serverboundCustomPayloadPacket) {
        if (serverboundCustomPayloadPacket.getIdentifier().equals(new ResourceLocation(ExternalAuthPacket.External_Auth.key(), ExternalAuthPacket.External_Auth.value()))) {
            handleAuthResponse(serverboundCustomPayloadPacket.getData());
        }
        if (serverboundCustomPayloadPacket.getIdentifier().equals(new ResourceLocation(ProxyLocationPacket.PROXY_LOCATION.key(), ProxyLocationPacket.PROXY_LOCATION.value()))) {
            try {
                handleProxyLocation(serverboundCustomPayloadPacket.getData());
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        if (serverboundCustomPayloadPacket.getIdentifier().equals(new ResourceLocation(PlayerStatePacket.PLAYER_STATE.key(), PlayerStatePacket.PLAYER_STATE.value()))) {
            PlayerStatePacket packet = new PlayerStatePacket().fromBytes(serverboundCustomPayloadPacket.getData());
            Voicechat.SERVER.getServer().getPlayerStateManager().broadcastState(Voicechat.SERVER.getServer().getServer().getPlayer(packet.getPlayerState().getUuid()), packet.getPlayerState());
        }
        if (serverboundCustomPayloadPacket.getIdentifier().equals(new ResourceLocation(PlayerStatesPacket.PLAYER_STATES.key(), PlayerStatesPacket.PLAYER_STATES.value()))) {
            FriendlyByteBuf byteBuf = serverboundCustomPayloadPacket.getData();
            PlayerStatesPacket packet = new PlayerStatesPacket().fromBytes(byteBuf);
            NetManager.sendToClient(Voicechat.SERVER.getServer().getServer().getPlayer(byteBuf.readUUID()), packet);
        }
    }

    private void handleAuthResponse(FriendlyByteBuf buf) {
        if (isAuthed) {
            LOGGER.warn("Received auth packet after authentication");
            return;
        }

        int readable = buf.readableBytes();
        if (readable != 32) {
            LOGGER.warn("Invalid auth response length: {}", readable);
            disconnect(Component.literal("Invalid auth payload"));
            return;
        }

        byte[] response = new byte[32];
        buf.readBytes(response);

        try {
            byte[] expected = ServerHandler.hmacSha256(StandaloneVCCommon.proxyPassword.get(), nonce);
            Arrays.fill(nonce, (byte) 0);

            if (!Arrays.equals(expected, response)) {
                LOGGER.warn("Invalid auth from voicechat bot");
                disconnect(Component.literal("Invalid auth"));
                return;
            }

            isAuthed = true;
            LOGGER.info("Voicechat proxy authenticated successfully");

            StandaloneVCCommon.send(new ServerStartPacket(
                    server.getOperatorUserPermissionLevel(),
                    StreamSupport.stream(server.getAllLevels().spliterator(), false)
                            .map(level -> level.dimension().location().toString())
                            .collect(Collectors.toSet())
            ));
        } catch (Exception e) {
            LOGGER.error("Auth verification failed", e);
            disconnect(Component.literal("Authentication error"));
        }
    }

    private void handleProxyLocation(FriendlyByteBuf buf) throws IllegalAccessException {
        String hostName = BufferUtils.readUtf(buf);
        Voicechat.SERVER_CONFIG.voiceHost.set(hostName);
        Server vcserver = Voicechat.SERVER.getServer();

        Voicechat.LOGGER.info("Voicechat proxy connected, reconnecting players");

        Set<UUID> sentGroups = new HashSet<>();
        ((Map<UUID, Secret>)StandaloneVoiceChat.getServerSecretsField().get(vcserver)).forEach((uuid, secret) -> {
            de.maxhenkel.voicechat.api.ServerPlayer player = vcserver.getServer().getPlayer(uuid);

            if (player == null) {
                Voicechat.LOGGER.warn("Reconnecting player {} failed (Could not find player)", uuid);
            } else {
                Voicechat.SERVER.initializePlayerConnection(player);

                NetManager.sendToClient(player, new SecretPacket(player, secret, vcserver.getPort(), Voicechat.SERVER_CONFIG));
                StandaloneVCCommon.send(new PlayerJoinPacket(player.getUuid(), player.getName(), ((ServerLevel)player.getLevel().getServerLevel()).dimension().location().toString()));
                StandaloneVCCommon.send(new SendSecretPacket(player.getUuid(), vcserver.getSecret(player.getUuid())));

                UUID group = vcserver.getPlayerStateManager().getState(player.getUuid()).getGroup();
                if (group != null) {
                    if (sentGroups.add(group)) {
                        StandaloneVCCommon.send(new SendGroupPacket(vcserver.getGroupManager().getGroup(group), player.getUuid()));
                    } else {
                        StandaloneVCCommon.send(new JoinGroupPacket(group, vcserver.getGroupManager().getGroup(group).getPassword()), player.getUuid());
                    }
                }

            }

            vcserver.getConnections().remove(uuid);
            CommonCompatibilityManager.INSTANCE.emitServerVoiceChatDisconnectedEvent(uuid);
            PluginManager.instance().onPlayerDisconnected(uuid);
        });
    }

    public void handlePlayerInput(ServerboundPlayerInputPacket serverboundPlayerInputPacket) {}
    public void handleMoveVehicle(ServerboundMoveVehiclePacket serverboundMoveVehiclePacket) {}
    public void handleAcceptTeleportPacket(ServerboundAcceptTeleportationPacket serverboundAcceptTeleportationPacket) {}
    public void handleRecipeBookSeenRecipePacket(ServerboundRecipeBookSeenRecipePacket serverboundRecipeBookSeenRecipePacket) {}
    public void handleRecipeBookChangeSettingsPacket(ServerboundRecipeBookChangeSettingsPacket serverboundRecipeBookChangeSettingsPacket) {}
    public void handleSeenAdvancements(ServerboundSeenAdvancementsPacket serverboundSeenAdvancementsPacket) {}
    public void handleCustomCommandSuggestions(ServerboundCommandSuggestionPacket serverboundCommandSuggestionPacket) {}
    public void handleSetCommandBlock(ServerboundSetCommandBlockPacket serverboundSetCommandBlockPacket) {}
    public void handleSetCommandMinecart(ServerboundSetCommandMinecartPacket serverboundSetCommandMinecartPacket) {}
    public void handlePickItem(ServerboundPickItemPacket serverboundPickItemPacket) {}
    public void handleRenameItem(ServerboundRenameItemPacket serverboundRenameItemPacket) {}
    public void handleSetBeaconPacket(ServerboundSetBeaconPacket serverboundSetBeaconPacket) {}
    public void handleSetStructureBlock(ServerboundSetStructureBlockPacket serverboundSetStructureBlockPacket) {}
    public void handleSetJigsawBlock(ServerboundSetJigsawBlockPacket serverboundSetJigsawBlockPacket) {}
    public void handleJigsawGenerate(ServerboundJigsawGeneratePacket serverboundJigsawGeneratePacket) {}
    public void handleSelectTrade(ServerboundSelectTradePacket serverboundSelectTradePacket) {}
    public void handleEditBook(ServerboundEditBookPacket serverboundEditBookPacket) {}
    public void handleEntityTagQuery(ServerboundEntityTagQuery serverboundEntityTagQuery) {}
    public void handleBlockEntityTagQuery(ServerboundBlockEntityTagQuery serverboundBlockEntityTagQuery) {}
    public void handleMovePlayer(ServerboundMovePlayerPacket serverboundMovePlayerPacket) {}
    public void handlePlayerAction(ServerboundPlayerActionPacket serverboundPlayerActionPacket) {}
    public void handleUseItemOn(ServerboundUseItemOnPacket serverboundUseItemOnPacket) {}
    public void handleUseItem(ServerboundUseItemPacket serverboundUseItemPacket) {}
    public void handleTeleportToEntityPacket(ServerboundTeleportToEntityPacket serverboundTeleportToEntityPacket) {}
    public void handleResourcePackResponse(ServerboundResourcePackPacket serverboundResourcePackPacket) {}
    public void handlePaddleBoat(ServerboundPaddleBoatPacket serverboundPaddleBoatPacket) {}
    public void handlePong(ServerboundPongPacket serverboundPongPacket) {}
    public void handleSetCarriedItem(ServerboundSetCarriedItemPacket serverboundSetCarriedItemPacket) {}
    public void handleChat(ServerboundChatPacket serverboundChatPacket) {}
    public void handleChatCommand(ServerboundChatCommandPacket serverboundChatCommandPacket) {}
    public void handleChatAck(ServerboundChatAckPacket serverboundChatAckPacket) {}
    public void handleAnimate(ServerboundSwingPacket serverboundSwingPacket) {}
    public void handlePlayerCommand(ServerboundPlayerCommandPacket serverboundPlayerCommandPacket) {}
    public void handleInteract(ServerboundInteractPacket serverboundInteractPacket) {}
    public void handleClientCommand(ServerboundClientCommandPacket serverboundClientCommandPacket) {}
    public void handleContainerClose(ServerboundContainerClosePacket serverboundContainerClosePacket) {}
    public void handleContainerClick(ServerboundContainerClickPacket serverboundContainerClickPacket) {}
    public void handlePlaceRecipe(ServerboundPlaceRecipePacket serverboundPlaceRecipePacket) {}
    public void handleContainerButtonClick(ServerboundContainerButtonClickPacket serverboundContainerButtonClickPacket) {}
    public void handleSetCreativeModeSlot(ServerboundSetCreativeModeSlotPacket serverboundSetCreativeModeSlotPacket) {}
    public void handleSignUpdate(ServerboundSignUpdatePacket serverboundSignUpdatePacket) {}
    public void handlePlayerAbilities(ServerboundPlayerAbilitiesPacket serverboundPlayerAbilitiesPacket) {}
    public void handleClientInformation(ServerboundClientInformationPacket serverboundClientInformationPacket) {}
    public void handleChangeDifficulty(ServerboundChangeDifficultyPacket serverboundChangeDifficultyPacket) {}
    public void handleLockDifficulty(ServerboundLockDifficultyPacket serverboundLockDifficultyPacket) {}
    public void handleChatSessionUpdate(ServerboundChatSessionUpdatePacket serverboundChatSessionUpdatePacket) {}
    public ServerPlayer getPlayer() {
        return null;
    }
}
