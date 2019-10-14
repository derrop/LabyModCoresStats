package de.derrop.labymod.addons.cores.network.sync.codec;
/*
 * Created by derrop on 14.10.2019
 */

import com.google.gson.JsonElement;
import de.derrop.labymod.addons.cores.network.NetworkUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class PacketEncoder extends MessageToByteEncoder<JsonElement> {
    @Override
    protected void encode(ChannelHandlerContext ctx, JsonElement msg, ByteBuf out) throws Exception {
        NetworkUtils.writeString(out, msg.toString());
    }
}
