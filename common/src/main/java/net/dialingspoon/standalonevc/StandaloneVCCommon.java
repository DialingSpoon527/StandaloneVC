package net.dialingspoon.standalonevc;

import de.maxhenkel.voicechat.configbuilder.entry.ConfigEntry;
import de.maxhenkel.voicechat.net.Packet;
import io.netty.buffer.Unpooled;
import net.dialingspoon.standalonevc.net.StandalonePacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public final class StandaloneVCCommon {

    public static ServerGamePacketListenerNoPlayer listener;
    public static ConfigEntry<String> proxyPassword;

    public static void init() {}

    public static void send(StandalonePacket packet) {
        if (listener != null) {
            FriendlyByteBuf out = new FriendlyByteBuf(Unpooled.buffer());
            packet.toBytes(out);
            listener.send(new ClientboundCustomPayloadPacket(new ResourceLocation(packet.getIdentifier().key(), packet.getIdentifier().value()), out));
        }
    }

    public static void send(Packet packet, UUID sender) {
        if (listener != null) {
            FriendlyByteBuf out = new FriendlyByteBuf(Unpooled.buffer());
            packet.toBytes(out);
            out.writeUUID(sender);
            listener.send(new ClientboundCustomPayloadPacket(new ResourceLocation(packet.getIdentifier().key(), packet.getIdentifier().value()), out));
        }
    }
}