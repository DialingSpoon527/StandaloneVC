package net.dialingspoon.standalonevc.plugins.impl;

import de.maxhenkel.voicechat.api.ServerPlayer;

import java.util.UUID;

public class ServerPlayerImpl extends PlayerImpl implements ServerPlayer {

    private String name;
    private ServerPlayer cameraPlayer = this;
    private boolean spectator;
    private int permission;

    public ServerPlayerImpl(UUID uuid) {
        super(uuid);
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public ServerPlayer getCameraPlayer() {
        return cameraPlayer;
    }

    public void setCameraPlayer(ServerPlayer cameraPlayer) {
        this.cameraPlayer = cameraPlayer;
    }

    @Override
    public boolean isSpectator() {
        return spectator;
    }

    public void setSpectator(boolean spectator) {
        this.spectator = spectator;
    }

    @Override
    public boolean hasPermissions(int operatorUserPermissionLevel) {
        return permission >= operatorUserPermissionLevel;
    }

    public void setPermission(int level) {
        this.permission = level;
    }
}
