package net.dialingspoon.standalonevc.plugins.impl;

import de.maxhenkel.voicechat.api.Player;

import java.util.UUID;

public class PlayerImpl extends EntityImpl implements Player {

    public PlayerImpl(UUID uuid) {
        super(uuid);
    }
}
