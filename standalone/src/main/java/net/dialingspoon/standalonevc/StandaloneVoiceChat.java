package net.dialingspoon.standalonevc;

import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.config.ServerConfig;
import de.maxhenkel.voicechat.config.Translations;
import de.maxhenkel.voicechat.configbuilder.ConfigBuilder;
import de.maxhenkel.voicechat.intercompatibility.CommonCompatibilityManager;
import de.maxhenkel.voicechat.permission.PermissionType;
import de.maxhenkel.voicechat.voice.server.Server;
import net.dialingspoon.standalonevc.config.ProxyConfig;
import net.dialingspoon.standalonevc.net.client.MinimalMcClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;

public class StandaloneVoiceChat extends Voicechat {

    public static final String MOD_ID = "standalonevc";

    private static final Field SERVER_SECRETS_FIELD;
    private static final Method HAS_PERMISSION_METHOD;

    public static StandaloneVoiceChat INSTANCE;
    public static Path PATH;

    public static ProxyConfig PROXY_CONFIG;

    private StandaloneVoiceChat() {
        initialize();
        CommonCompatibilityManager.INSTANCE.onPlayerLoggedIn((player) -> MinimalMcClient.cancelShutdownTimer());
        CommonCompatibilityManager.INSTANCE.onPlayerLoggedOut(player -> {
            var mcServer = StandaloneVoiceChat.SERVER.getServer();
            if (mcServer != null) {
                var players = mcServer.getServer().getPlayers();
                if (players.isEmpty() || (players.size() == 1 && players.contains(player))) {
                    MinimalMcClient.startShutdownTimer();
                }
            }
        });

        try {
            MinimalMcClient.start(StandaloneVoiceChat.PROXY_CONFIG.minecraftServerIP.get(), StandaloneVoiceChat.PROXY_CONFIG.minecraftServerPort.get());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    public static void init(Path path) {
        if (INSTANCE != null) {
            LOGGER.warn("StandaloneVoiceChat.init() called multiple times");
            return;
        }

        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        configureLogging(context.getRootLogger());

        PATH = path;
        INSTANCE = new StandaloneVoiceChat();
    }

    private static void configureLogging(Logger logger) {
        PatternLayout layout = PatternLayout.newBuilder()
                .withPattern("%d [%t] %-5level: %msg%n")
                .build();

        ConsoleAppender consoleAppender = ConsoleAppender.newBuilder()
                .setTarget(ConsoleAppender.Target.SYSTEM_OUT)
                .setLayout(layout)
                .setName("Console")
                .build();
        consoleAppender.start();

        logger.addAppender(consoleAppender);
    }

    @Override
    public void initializeConfigs() {
        SERVER_CONFIG = ConfigBuilder.builder(ServerConfig::new).build();
        TRANSLATIONS = ConfigBuilder.builder(Translations::new).build();
        PROXY_CONFIG = ConfigBuilder.builder(ProxyConfig::new).path(this.getVoicechatConfigFolderInternal().resolve("voicechat-proxy.properties")).build();
    }

    @Override
    protected Path getVoicechatConfigFolderInternal() {
        if (PATH == null)
            throw new IllegalStateException("StandaloneVoiceChat PATH not initialized");
        return PATH.resolve("config").resolve(MODID);
    }


    static {
        try {
            SERVER_SECRETS_FIELD = Server.class.getDeclaredField("secrets");
            HAS_PERMISSION_METHOD = PermissionType.class.getDeclaredMethod("hasPermission", ServerPlayer.class);

            SERVER_SECRETS_FIELD.setAccessible(true);
            HAS_PERMISSION_METHOD.setAccessible(true);
        } catch (NoSuchFieldException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static Field getServerSecretsField() {
        return SERVER_SECRETS_FIELD;
    }

    public static Method getHasPermissionMethod() {
        return HAS_PERMISSION_METHOD;
    }
}
