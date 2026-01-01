package net.dialingspoon.standalonevc;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.network.Connection;

public class PlatformSpecific {
    @ExpectPlatform
    public static void sendMCRegistryPackets(Connection connection, String direction) {
        throw new AssertionError();
    }
}
