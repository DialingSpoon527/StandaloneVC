package net.dialingspoon.standalonevc.net;

import de.maxhenkel.voicechat.net.Packet;
import net.dialingspoon.standalonevc.intercompatibility.StandaloneCompatibilityManager;
import net.dialingspoon.standalonevc.plugins.impl.MinecraftServerImpl;
import org.jetbrains.annotations.Nullable;

public interface StandalonePacket<T extends StandalonePacket<T>> extends Packet<T> {
    void handle(StandaloneCompatibilityManager compatibilityManager, @Nullable MinecraftServerImpl minecraftServer);
}
