package net.dialingspoon.standalonevc.config;

import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.configbuilder.ConfigBuilder;
import de.maxhenkel.voicechat.configbuilder.entry.ConfigEntry;
import de.maxhenkel.voicechat.intercompatibility.CommonCompatibilityManager;

import java.util.UUID;

public class ProxyConfig {
    public ConfigEntry<String> proxyPassword;
    public ConfigEntry<String> minecraftServerIP;
    public ConfigEntry<Integer> minecraftServerPort;
    public ConfigEntry<Integer> reconnectDelay;

    public ProxyConfig(ConfigBuilder builder) {
        builder.header(new String[]{String.format("%s server config v%s", CommonCompatibilityManager.INSTANCE.getModName(), CommonCompatibilityManager.INSTANCE.getModVersion())});
        this.proxyPassword = builder.stringEntry("proxy_password", UUID.randomUUID().toString(), new String[]{"The password to ensure that someone else doesn't try to take over your proxy. Copy this into your other config, or replace with any password you like."});
        this.minecraftServerIP = builder.stringEntry("server_ip", "", new String[]{"The server IP address of the minecraft server to proxy voicechat for.", "Leave blank for localhost"});
        this.minecraftServerPort = builder.integerEntry("server_port", 25565, -1, 65535, new String[]{"The port number of the minecraft server to proxy voicechat for.", "if the ip doesnt include port, simply leave as '25565'",});
        Voicechat.SERVER_CONFIG.voiceChatPort = builder.integerEntry("port", 24455, -1, 65535, new String[]{"The port number to use for the voice chat communication.", "Audio packets are always transmitted via the UDP protocol on the port number", "specified here, independently of other networking used for the game server."});
        Voicechat.SERVER_CONFIG.voiceChatBindAddress = builder.stringEntry("bind_address", "127.0.0.1", new String[]{"The server IP address to bind the voice chat to", "To bind to the wildcard IP address, use '*'"});
        if (Voicechat.SERVER_CONFIG.voiceChatBindAddress.get().isEmpty())
            Voicechat.SERVER_CONFIG.voiceChatBindAddress.set("127.0.0.1");
        this.reconnectDelay = builder.integerEntry("reconnect_delay", 60, new String[]{"The delay, in seconds, between reconnect attempts when the minecraft server is down."});
        Voicechat.SERVER_CONFIG.voiceHost = builder.stringEntry("voice_host", "", new String[]{"The hostname that clients should use to connect to the voice chat", "This may also include a port, e.g. 'example.com:24454' or just a port, e.g. '24454'"});
    }
}
