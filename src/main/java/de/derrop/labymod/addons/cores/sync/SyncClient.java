package de.derrop.labymod.addons.cores.sync;
/*
 * Created by derrop on 30.09.2019
 */

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import net.minecraft.client.Minecraft;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class SyncClient {

    private final JsonParser jsonParser = new JsonParser();

    private EventLoopGroup eventLoopGroup = Epoll.isAvailable() ? new EpollEventLoopGroup() : new NioEventLoopGroup();

    private Channel channel;

    private Map<Short, Collection<PacketHandler>> packetHandlers = new HashMap<>();
    private Map<Short, CompletableFuture<JsonElement>> pendingQueries = new HashMap<>();

    public boolean isConnected() {
        return this.channel != null && this.channel.isOpen();
    }

    public void sendPacket(short packetId, JsonElement payload) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", packetId);
        jsonObject.add("payload", payload);
        this.channel.writeAndFlush(jsonObject.toString()).syncUninterruptibly();
    }

    public boolean connect(InetSocketAddress host) {
        if (this.channel != null) {
            this.channel.close().syncUninterruptibly();
        }

        this.channel = new Bootstrap()
                .group(this.eventLoopGroup)
                .channel(Epoll.isAvailable() ? EpollSocketChannel.class : NioSocketChannel.class)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        channel.pipeline()
                                .addLast(new StringEncoder(), new StringDecoder())
                                .addLast(new PacketReader());
                    }
                })
                .connect(host).syncUninterruptibly()
                .channel();

        JsonObject authPayload = new JsonObject();
        authPayload.addProperty("uniqueId", Minecraft.getMinecraft().getSession().getProfile().getId().toString());
        authPayload.addProperty("name", Minecraft.getMinecraft().getSession().getProfile().getName());
        JsonElement response = this.sendQuery((short) 0, authPayload, null); //todo add good auth
        return response.getAsBoolean();
    }

    public JsonElement sendQuery(short packetId, JsonElement payload, JsonElement defResponse) {
        try {
            return this.sendQuery(packetId, payload).get(6, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
            return defResponse;
        }
    }

    public CompletableFuture<JsonElement> sendQuery(short packetId, JsonElement payload) {
        short queryId;
        do {
            queryId = (short) ThreadLocalRandom.current().nextInt(Short.MAX_VALUE);
        } while (this.pendingQueries.containsKey(queryId));

        CompletableFuture<JsonElement> future = new CompletableFuture<>();
        this.pendingQueries.put(queryId, future);

        JsonObject packet = new JsonObject();
        packet.addProperty("id", packetId);
        packet.addProperty("queryId", queryId);
        packet.add("payload", payload);
        this.channel.writeAndFlush(packet.toString());

        return future;
    }

    public void registerHandler(short packetId, PacketHandler packetHandler) {
        if (!this.packetHandlers.containsKey(packetId))
            this.packetHandlers.put(packetId, new ArrayList<>());
        this.packetHandlers.get(packetId).add(packetHandler);
    }

    private final class PacketReader extends SimpleChannelInboundHandler<String> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String message) throws Exception {
            JsonObject jsonObject = SyncClient.this.jsonParser.parse(message).getAsJsonObject();
            JsonElement payload = jsonObject.get("payload");

            Consumer<JsonElement> responseConsumer = null;
            if (jsonObject.has("queryId")) {
                short queryId = jsonObject.get("queryId").getAsShort();
                if (SyncClient.this.pendingQueries.containsKey(queryId)) {
                    SyncClient.this.pendingQueries.remove(queryId).complete(payload);
                    return;
                }
                responseConsumer = element -> {
                    JsonObject response = new JsonObject();
                    response.addProperty("queryId", queryId);
                    response.add("payload", element);
                    ctx.channel().writeAndFlush(response.toString());
                };
            }

            if (!jsonObject.has("id")) {
                return;
            }
            short id = jsonObject.get("id").getAsShort();

            Collection<PacketHandler> packetHandlers = SyncClient.this.packetHandlers.get(id);
            if (packetHandlers != null && !packetHandlers.isEmpty()) {
                for (PacketHandler packetHandler : packetHandlers) {
                    packetHandler.handlePacket(payload, responseConsumer);
                }
            }
        }
    }

}
