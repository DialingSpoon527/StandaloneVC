package net.dialingspoon.standalonevc.net;

import de.maxhenkel.voicechat.net.Channel;
import de.maxhenkel.voicechat.net.NetManager;
import de.maxhenkel.voicechat.net.Packet;
import net.dialingspoon.standalonevc.net.client.ServerHandler;
import net.dialingspoon.standalonevc.net.packets.*;

public class StandaloneNetManager extends NetManager {

    @Override
    public void init() {
        super.init();
        registerReceiver(ServerStartPacket.class, false, true);
        registerReceiver(ServerStopPacket.class, false, true);
        registerReceiver(PlayerJoinPacket.class, false, true);
        registerReceiver(PlayerLeavePacket.class, false, true);
        registerReceiver(PlayerPosPacket.class, false, true);
        registerReceiver(PlayerLevelPacket.class, false, true);
        registerReceiver(SendSecretPacket.class, false, true);
        registerReceiver(PlayerSpectatePacket.class, false, true);
        registerReceiver(PlayerPermsPacket.class, false, true);
        registerReceiver(SendGroupPacket.class, false, true);
        registerReceiver(ExternalAuthPacket.class, true, true);
    }

    @Override
    public <T extends Packet<T>> Channel<T> registerReceiver(Class<T> packetType, boolean toClient, boolean toServer) {
        Channel<T> channel = new Channel<>();
        T packet;
        try {
            packet = packetType.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Failed to instantiate packet: " + packetType.getName(), e
            );
        }
        ServerHandler.packets.put(packet.getIdentifier().key() + ":" + packet.getIdentifier().value(), new ServerHandler.PacketChannel<>(packetType, channel));
        return channel;
    }
}
