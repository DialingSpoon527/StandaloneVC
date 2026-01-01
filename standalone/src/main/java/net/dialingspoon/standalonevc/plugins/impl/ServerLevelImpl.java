package net.dialingspoon.standalonevc.plugins.impl;

import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public class ServerLevelImpl implements ServerLevel {

    private final String resourceLocation;
    private static final List<ServerPlayer> players = new ArrayList<>();

    public ServerLevelImpl(String resourceLocation) {
        this.resourceLocation = resourceLocation;
    }

    @Override
    public Object getServerLevel() {
        return this;
    }

    @Override
    public List<ServerPlayer> players() {
        return List.copyOf(players);
    }

    public void addPlayer(ServerPlayer serverPlayer) {
        players.add(serverPlayer);
    }

    public void removePlayer(ServerPlayer serverPlayer) {
        players.remove(serverPlayer);
    }

    public String getResourceLocation() {
        return this.resourceLocation;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object instanceof ServerLevelImpl serverLevelImpl) {
            return resourceLocation.equals(serverLevelImpl.getResourceLocation());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return resourceLocation.hashCode();
    }
}
