package net.dialingspoon.standalonevc.net.client;

import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.net.Channel;
import de.maxhenkel.voicechat.net.Packet;
import de.maxhenkel.voicechat.voice.common.BufferUtils;
import io.netty.buffer.ByteBuf;
import net.dialingspoon.standalonevc.intercompatibility.StandaloneCompatibilityManager;
import net.dialingspoon.standalonevc.net.StandalonePacket;
import net.dialingspoon.standalonevc.plugins.impl.MinecraftServerImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ServerHandler {

    public static final Logger LOGGER = LogManager.getLogger();

    public static Map<String, PacketChannel<? extends Packet<?>>> packets = new ConcurrentHashMap<>();

    public static void readPacket(ByteBuf msg) throws RuntimeException {
        String id = BufferUtils.readUtf(msg);
        PacketChannel<?> packetType = packets.get(id);
        if (packetType == null) {
            LOGGER.error("Unknown packet {}", id);
            return;
        }

        Packet<?> packet;
        try {
            packet = packetType.packet().getDeclaredConstructor().newInstance().fromBytes(msg);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        if (packet instanceof StandalonePacket<?> standalonePacket) {
            StandaloneCompatibilityManager compatibilityManager = (StandaloneCompatibilityManager) StandaloneCompatibilityManager.INSTANCE;

            MinecraftServerImpl minecraftServer = null;
            if (Voicechat.SERVER != null && Voicechat.SERVER.getServer() != null) {
                minecraftServer = (MinecraftServerImpl) Voicechat.SERVER.getServer().getServer();
            }

            standalonePacket.handle(compatibilityManager, minecraftServer);
        } else {
            UUID uuid = BufferUtils.readUUID(msg);

            ServerPlayer player = Voicechat.SERVER.getServer().getServer().getPlayer(uuid);
            LOGGER.info("Received packet: {} from {}", packetType.packet().getName(), player);

            packetType.channel().onServerPacket(Voicechat.SERVER.getServer().getServer(), player, null, packet);
        }
    }

    public static byte[] hmacSha256(String key, byte[] message) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        return mac.doFinal(message);
    }

    public record PacketChannel<T extends Packet<T>>(Class<T> packet, Channel channel) {}
}
