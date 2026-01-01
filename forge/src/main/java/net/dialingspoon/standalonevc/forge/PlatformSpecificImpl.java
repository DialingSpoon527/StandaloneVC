package net.dialingspoon.standalonevc.forge;

import net.minecraft.network.Connection;
import net.minecraftforge.network.NetworkHooks;

public class PlatformSpecificImpl {
    public static void sendMCRegistryPackets(Connection connection, String direction) {
        NetworkHooks.sendMCRegistryPackets(connection, direction);
    }
}
