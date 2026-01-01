package net.dialingspoon.standalonevc.net.client;

import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.logging.VoicechatLogger;
import de.maxhenkel.voicechat.net.Packet;
import de.maxhenkel.voicechat.voice.common.BufferUtils;
import de.maxhenkel.voicechat.voice.common.ResourceLocation;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.concurrent.ScheduledFuture;
import net.dialingspoon.standalonevc.StandaloneVoiceChat;
import net.dialingspoon.standalonevc.intercompatibility.StandaloneCompatibilityManager;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class MinimalMcClient {

    private static final int PROTOCOL = 763;
    public static final VoicechatLogger LOGGER = StandaloneVoiceChat.LOGGER;
    private static PacketHandler packetHandler;

    private static volatile Mode mode = Mode.STATUS;
    private static final AtomicInteger reconnectAttempts = new AtomicInteger();
    private static EventLoopGroup eventLoopGroup;
    private static Bootstrap bootstrap;

    private static ScheduledFuture<?> shutdownTimer;
    private static final long SESSION_TIMEOUT_MINUTES = 15;

    public static void start(String ip, int port) throws InterruptedException {
        if (bootstrap != null) {
            LOGGER.warn("Client already started");
            return;
        }

        eventLoopGroup = new NioEventLoopGroup();

        bootstrap = new Bootstrap()
                .group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline().addLast("length-decoder", new LengthDecoder());
                        ch.pipeline().addLast("length-encoder", new LengthEncoder());
                        ch.pipeline().addLast(packetHandler = new PacketHandler(ip, port));
                    }
                });

        connect(ip, port);
    }

    private static void connect(String ip, int port) {
        bootstrap.connect(ip, port).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                return;
            }

            int attempt = reconnectAttempts.get();
            if (attempt <= 5) {
                LOGGER.warn("Connection failed: {}", future.cause().getMessage());
            }

            scheduleReconnect(ip, port);
        });
    }

    private static void scheduleReconnect(String ip, int port) {
        int attempt = reconnectAttempts.incrementAndGet();
        if (attempt <= 5) {
            LOGGER.info("Reconnecting in {} seconds...", StandaloneVoiceChat.PROXY_CONFIG.reconnectDelay.get());
        } else if (attempt == 6) {
            LOGGER.warn("Further reconnect failures will be suppressed");
        }

        eventLoopGroup.schedule(
                () -> connect(ip, port),
                StandaloneVoiceChat.PROXY_CONFIG.reconnectDelay.get(),
                java.util.concurrent.TimeUnit.SECONDS
        );
    }

    public static void sendPacket(Packet packet) {
        PacketHandler handler = packetHandler;
        if (handler == null || handler.channel == null || !handler.channel.isActive()) {
            LOGGER.debug("Dropping packet {}, not connected", packet.getIdentifier());
            return;
        }

        packetHandler.channel.eventLoop().execute(() -> {
            packetHandler.sendPlayCustomPayload(packet);
        });
    }

    public static void send(ResourceLocation id, ByteBuf buffer) {
        PacketHandler handler = packetHandler;
        if (handler == null || handler.channel == null || !handler.channel.isActive()) {
            LOGGER.debug("Dropping packet, not connected");
            return;
        }

        packetHandler.channel.eventLoop().execute(() -> {
            packetHandler.sendPayload(id, buffer);
        });
    }

    public static void startShutdownTimer() {
        cancelShutdownTimer();

        shutdownTimer = eventLoopGroup.schedule(
                () -> {
                    LOGGER.warn("Session timed out after {} minutes without players, shutting down", SESSION_TIMEOUT_MINUTES);
                    cancelShutdownTimer();

                    if (packetHandler != null && packetHandler.channel != null) {
                        packetHandler.channel.eventLoop().execute(() -> {
                            if (packetHandler.channel.isOpen()) {
                                packetHandler.channel.close();
                            }
                        });
                    }

                    mode = Mode.STATUS;
                },
                SESSION_TIMEOUT_MINUTES,
                TimeUnit.MINUTES
        );
    }

    public static void cancelShutdownTimer() {
        if (shutdownTimer != null) {
            shutdownTimer.cancel(false);
            shutdownTimer = null;
        }
    }

    // ================= PIPELINE =================

    private static class PacketHandler extends SimpleChannelInboundHandler<ByteBuf> {

        private final String ip;
        private final int port;
        private volatile Channel channel;

        PacketHandler(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            channel = ctx.channel();
            if (mode == Mode.STATUS) {
                getStatus(ctx);
            } else {
                sendHandshake(ctx);
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            channel = null;
            if (mode != Mode.LOGIN_HANDSHAKE) {
                mode = Mode.STATUS;

                if (reconnectAttempts.get() <= 5) {
                    LOGGER.warn("Connection lost");
                }

                if (Voicechat.SERVER.getServer() != null) {
                    ((StandaloneCompatibilityManager) StandaloneCompatibilityManager.INSTANCE).serverStopping(Voicechat.SERVER.getServer().getServer());
                }

                MinimalMcClient.scheduleReconnect(ip, port);
            }
        }

        private void sendPayload(ResourceLocation id, ByteBuf buffer) {
            if (mode != Mode.PLAY) {
                LOGGER.warn("Tried to send packet before PLAY");
                return;
            }

            ByteBuf payload = channel.alloc().buffer();
            BufferUtils.writeVarInt(payload, 0x0D);
            BufferUtils.writeUtf(payload, id.key() + ':' + id.value());
            payload.writeBytes(buffer);
            channel.writeAndFlush(payload);
        }

        private void sendPlayCustomPayload(Packet packet) {
            if (mode != Mode.PLAY) {
                LOGGER.warn("Tried to send packet {} before PLAY", packet.getIdentifier());
                return;
            }

            ByteBuf payload = channel.alloc().buffer();
            BufferUtils.writeVarInt(payload, 0x0D);
            BufferUtils.writeUtf(payload, packet.getIdentifier().key() + ':' + packet.getIdentifier().value());
            packet.toBytes(payload);
            channel.writeAndFlush(payload);
        }

        private void getStatus(ChannelHandlerContext ctx) {
            ByteBuf buf = ctx.alloc().buffer();
            BufferUtils.writeVarInt(buf, 0x00);
            BufferUtils.writeVarInt(buf, PROTOCOL);
            BufferUtils.writeUtf(buf, ip + "\u0000FML3\u0000");
            buf.writeShort(port);
            BufferUtils.writeVarInt(buf, 1);
            ctx.write(buf);

            buf = ctx.alloc().buffer();
            BufferUtils.writeVarInt(buf, 0x00);
            ctx.writeAndFlush(buf);
        }

        private void sendHandshake(ChannelHandlerContext ctx) {
            ByteBuf buf = ctx.alloc().buffer();
            BufferUtils.writeVarInt(buf, 0x00);
            BufferUtils.writeVarInt(buf, PROTOCOL);
            BufferUtils.writeUtf(buf, ip + "\u0000FML3\u0000"); //TODO fabric and vanill
            buf.writeShort(port);
            BufferUtils.writeVarInt(buf, 2);
            ctx.write(buf);

            buf = ctx.alloc().buffer();
            BufferUtils.writeVarInt(buf, 0x00);
            BufferUtils.writeUtf(buf, "VoicechatBOT", 16);
            buf.writeBoolean(false);
            ctx.writeAndFlush(buf);
            LOGGER.info("Connecting to {}:{}", ip, port);
            reconnectAttempts.set(0);
            mode = Mode.LOGIN;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf in) {
            int packetId = BufferUtils.readVarInt(in);

            if (mode == Mode.STATUS)
                handleStatusPacket(ctx, packetId, in);
            else if (mode == Mode.PLAY)
                handlePlayPacket(ctx, packetId, in);
            else
                handleLoginPacket(ctx, packetId, in);
        }

        private void handleStatusPacket(ChannelHandlerContext ctx, int id, ByteBuf in) {
            if (id != 0x00) {
                LOGGER.warn("Unexpected STATUS packet {}", id);
                ctx.close();
                return;
            }

            boolean log = reconnectAttempts.get() <= 5;

            String json = BufferUtils.readUtf(in);
            if (log)
                LOGGER.debug("Status JSON: {}", json);

            int online = extractOnlinePlayers(json);
            boolean allowConnect = online > 0;

            ctx.close();

            if (allowConnect) {
                mode = Mode.LOGIN_HANDSHAKE;
                connect(ip, port);
            } else if (log) {
                LOGGER.warn("Server rejected by status response");
            }
        }

        private static int extractOnlinePlayers(String json) {
            int playersIdx = json.indexOf("\"players\"");
            if (playersIdx == -1) return -1;

            int onlineIdx = json.indexOf("\"online\"", playersIdx);
            if (onlineIdx == -1) return -1;

            int colon = json.indexOf(':', onlineIdx);
            if (colon == -1) return -1;

            int end = colon + 1;
            while (end < json.length() && Character.isWhitespace(json.charAt(end))) end++;

            int start = end;
            while (end < json.length() && Character.isDigit(json.charAt(end))) end++;

            if (start == end) return -1;

            return Integer.parseInt(json.substring(start, end));
        }

        private void handlePlayPacket(ChannelHandlerContext ctx, int id, ByteBuf in) {
            switch (id) {
                case 0x17 -> {
                    ServerHandler.readPacket(in);
                }

                case 0x1a -> {
                    LOGGER.error("Server Disconnect {}", BufferUtils.readUtf(in));
                    ctx.close();
                }

                case 0x23 -> {
                    long thisID = in.readLong();

                    ByteBuf out = ctx.alloc().buffer();
                    BufferUtils.writeVarInt(out, 0x12);
                    out.writeLong(thisID);
                    ctx.writeAndFlush(out);
                }
            }
        }

        private void handleLoginPacket(ChannelHandlerContext ctx, int id, ByteBuf in) {
            switch (id) {
                case 0x00 -> {
                    LOGGER.error("Server Disconnect {}", BufferUtils.readUtf(in));
                    ctx.close();
                }

                case 0x01 -> {
                    BufferUtils.readUtf(in);
                    byte[] pub = BufferUtils.readByteArray(in);
                    byte[] token = BufferUtils.readByteArray(in);
                    enableEncryption(pub, token, ctx);
                }

                case 0x03 -> {
                    int compressionThreshold = BufferUtils.readVarInt(in);
                    ctx.pipeline().addAfter("length-decoder", "decompress", new CompressionDecoder(compressionThreshold));
                    ctx.pipeline().addAfter("length-encoder", "compress", new CompressionEncoder(compressionThreshold));
                }

                case 0x02 -> {
                    LOGGER.info("Entered PLAY state");
                    mode = Mode.PLAY;
                }
            }
        }

        private void enableEncryption(byte[] pub, byte[] token, ChannelHandlerContext ctx) {
            try {
                KeyGenerator keygenerator = KeyGenerator.getInstance("AES");
                keygenerator.init(128);
                SecretKey secretKey = keygenerator.generateKey();

                EncodedKeySpec encodedkeyspec = new X509EncodedKeySpec(pub);
                KeyFactory keyfactory = KeyFactory.getInstance("RSA");
                PublicKey publickey = keyfactory.generatePublic(encodedkeyspec);

                Cipher encryptCipher = Cipher.getInstance("AES/CFB8/NoPadding");
                encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(secretKey.getEncoded()));
                Cipher decryptCipher = Cipher.getInstance("AES/CFB8/NoPadding");
                decryptCipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(secretKey.getEncoded()));

                ByteBuf out = ctx.alloc().buffer();
                BufferUtils.writeVarInt(out, 0x01);

                Cipher cipher = Cipher.getInstance(publickey.getAlgorithm());
                cipher.init(1, publickey);
                BufferUtils.writeByteArray(out, cipher.doFinal(secretKey.getEncoded()));

                cipher = Cipher.getInstance(publickey.getAlgorithm());
                cipher.init(1, publickey);
                BufferUtils.writeByteArray(out, cipher.doFinal(token));

                ctx.writeAndFlush(out);

                ctx.pipeline().addBefore("length-encoder", "encrypt", new Encrypter(encryptCipher));
                ctx.pipeline().addBefore("length-decoder", "decrypt", new Decrypter(decryptCipher));
            } catch (GeneralSecurityException e) {
                LOGGER.error("Failed to create encryption key", e);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            LOGGER.error("Minecraft connection exception: ", cause);
            ctx.close();
        }
    }

    // =============== COMPRESSION ================

    private static class CompressionDecoder extends ByteToMessageDecoder {

        private final Inflater inflater = new Inflater();
        private final int threshold;

        public CompressionDecoder(int threshold) {
            this.threshold = threshold;
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws DataFormatException {
            if (in.readableBytes() == 0)
                return;

            int i = BufferUtils.readVarInt(in);
            if (i == 0) {
                out.add(in.readBytes(in.readableBytes()));
                return;
            }

            if (i < threshold || i > 8388608) {
                throw new DecoderException("Badly compressed packet - size of " + i + " is below server threshold of " + threshold);
            }

            byte[] bs = new byte[in.readableBytes()];
            in.readBytes(bs);
            byte[] cs = new byte[i];

            inflater.setInput(bs);
            inflater.inflate(cs);
            inflater.reset();
            out.add(Unpooled.wrappedBuffer(cs));
        }

        @Override
        public void handlerRemoved0(ChannelHandlerContext ctx) {
            inflater.end();
        }
    }

    private static class CompressionEncoder extends MessageToByteEncoder<ByteBuf> {

        private final byte[] encodeBuf = new byte[8192];
        private final Deflater deflater = new Deflater();
        private final int threshold;

        public CompressionEncoder(int threshold) {
            this.threshold = threshold;
        }

        @Override
        protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) {
            int i = msg.readableBytes();

            if (i < threshold) {
                BufferUtils.writeVarInt(out, 0);
                out.writeBytes(msg);
                return;
            }

            byte[] bs = new byte[i];
            msg.readBytes(bs);

            BufferUtils.writeVarInt(out, bs.length);
            deflater.setInput(bs, 0, i);
            deflater.finish();

            while (!deflater.finished()) {
                int j = deflater.deflate(encodeBuf);
                out.writeBytes(encodeBuf, 0, j);
            }
            deflater.reset();

        }

        @Override
        public void handlerRemoved(ChannelHandlerContext ctx) {
            deflater.end();
        }
    }

    // ================= Encryption =================

    private static class Decrypter extends ByteToMessageDecoder {
        Cipher decryptCipher;

        public Decrypter(Cipher decryptCipher) {
            this.decryptCipher = decryptCipher;
        }

        protected void decode(ChannelHandlerContext c, ByteBuf msg, java.util.List<Object> out) throws ShortBufferException {
            int i = msg.readableBytes();
            byte[] bs = bufToByte(msg);
            ByteBuf byteBuf2 = c.alloc().heapBuffer(decryptCipher.getOutputSize(i));
            byteBuf2.writerIndex(decryptCipher.update(bs, 0, i, byteBuf2.array(), byteBuf2.arrayOffset()));
            out.add(byteBuf2);
        }
    }

    private static class Encrypter extends MessageToByteEncoder<ByteBuf> {
        Cipher encryptCipher;
        byte[] heapOut = new byte[0];

        public Encrypter(Cipher encryptCipher) {
            this.encryptCipher = encryptCipher;
        }

        protected void encode(ChannelHandlerContext c, ByteBuf msg, ByteBuf out) throws ShortBufferException {
            int i = msg.readableBytes();
            byte[] bs = bufToByte(msg);
            int j = encryptCipher.getOutputSize(i);
            if (heapOut.length < j) {
                heapOut = new byte[j];
            }

            out.writeBytes(heapOut, 0, encryptCipher.update(bs, 0, i, heapOut));
        }
    }

    private static byte[] bufToByte(ByteBuf byteBuf) {
        int i = byteBuf.readableBytes();
        byte[] heapIn = new byte[i];

        byteBuf.readBytes(heapIn, 0, i);
        return heapIn;
    }

    // ================= FRAMING =================

    private static class LengthDecoder extends ByteToMessageDecoder {
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, java.util.List<Object> out) {
            in.markReaderIndex();
            try {
                int len = BufferUtils.readVarInt(in);
                if (in.readableBytes() < len) {
                    in.resetReaderIndex();
                    return;
                }
                out.add(in.readRetainedSlice(len));
            } catch (IndexOutOfBoundsException e) {
                in.resetReaderIndex();
            }
        }
    }

    private static class LengthEncoder extends MessageToByteEncoder<ByteBuf> {
        protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) {
            BufferUtils.writeVarInt(out, msg.readableBytes());
            out.writeBytes(msg);
        }
    }

    private enum Mode {
        STATUS,
        LOGIN_HANDSHAKE,
        LOGIN,
        PLAY
    }
}
