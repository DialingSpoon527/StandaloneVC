package net.dialingspoon.standalonevc.plugins.impl;

import de.maxhenkel.voicechat.api.MinecraftServer;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.ServerPlayer;
import net.dialingspoon.standalonevc.StandaloneVoiceChat;

import java.util.*;
import java.util.stream.Collectors;

public class MinecraftServerImpl implements MinecraftServer {

    private int opLevel;
    private final Map<UUID, ServerPlayer> players = new HashMap<>();
    private final Map<String, ServerLevel> serverLevels = new HashMap<>();

    @Override
    public Object getMinecraftServer() {
        return this;
    }

    @Override
    public int getPort() {
        int configPort = StandaloneVoiceChat.PROXY_CONFIG.minecraftServerPort.get();
        if (configPort < 0) {
            StandaloneVoiceChat.LOGGER.warn("Unable to read port config, defaulting to '24455'.");
            return 24455;
        } else {
            return configPort;
        }
    }

    @Override
    public String getIp() {
        String bindAddress = StandaloneVoiceChat.PROXY_CONFIG.minecraftServerIP.get();

        if (bindAddress.trim().equals("*") || bindAddress.trim().isEmpty()) {
            bindAddress = "";
        }
        return bindAddress;
    }

    @Override
    public ServerPlayer getPlayer(UUID playerUUID) {
        return players.computeIfAbsent(playerUUID, ServerPlayerImpl::new);
    }

    @Override
    public List<ServerPlayer> getPlayers() {
        return new ArrayList<>(players.values());
    }

    @Override
    public boolean isDedicated() {
        return true;
    }

    @Override
    public boolean usesAuthentication() {
        return true;
    }

    @Override
    public int getOperatorUserPermissionLevel() {
        return opLevel;
    }

    public ServerLevel getServerLevel(String id) {
        return serverLevels.get(id);
    }

    public void setServerLevels(Collection<ServerLevelImpl> levels) {
        serverLevels.putAll(levels.stream().collect(Collectors.toMap(ServerLevelImpl::getResourceLocation, level -> level)));
    }

    public void setOpLevel(int opPermissionLevel) {
        this.opLevel = opPermissionLevel;
    }
}
