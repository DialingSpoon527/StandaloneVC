package net.dialingspoon.standalonevc.plugins.impl;

import de.maxhenkel.voicechat.api.Entity;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.ServerPlayer;

import java.util.UUID;

public class EntityImpl implements Entity {

    private final UUID uuid;
    private Position pos;
    private Position eyePos;
    private ServerLevel serverLevel;

    public EntityImpl(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public Object getEntity() {
        return this;
    }

    @Override
    public Position getPosition() {
        return pos;
    }

    public void setPos(Position pos) {
        this.pos = pos;
    }

    @Override
    public Position getEyePosition() {
        return eyePos;
    }

    public void setEyePos(Position pos) {
        this.eyePos = pos;
    }

    @Override
    public ServerLevel getLevel() {
        return serverLevel;
    }

    public void setServerLevel(ServerLevel serverLevel) {
        if (serverLevel instanceof ServerLevelImpl serverLevelImpl && this instanceof ServerPlayer serverPlayer) {
            if (this.serverLevel != null)
                ((ServerLevelImpl) this.serverLevel).removePlayer(serverPlayer);
            serverLevelImpl.addPlayer(serverPlayer);
        }

        this.serverLevel = serverLevel;
    }

}
