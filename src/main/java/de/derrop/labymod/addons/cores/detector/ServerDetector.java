package de.derrop.labymod.addons.cores.detector;
/*
 * Created by derrop on 23.09.2019
 */

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.derrop.labymod.addons.cores.CoresAddon;
import de.derrop.labymod.addons.cores.gametypes.GameType;
import io.netty.handler.codec.DecoderException;
import net.labymod.api.events.PluginMessageEvent;
import net.minecraft.network.PacketBuffer;

import java.nio.charset.StandardCharsets;

public class ServerDetector implements PluginMessageEvent {

    private final JsonParser jsonParser = new JsonParser();

    private CoresAddon coresAddon;

    private String currentServer;
    private String currentServerId;
    private GameType currentServerType;

    public ServerDetector(CoresAddon coresAddon) {
        this.coresAddon = coresAddon;
    }

    public GameType getCurrentServerType() {
        return currentServerType;
    }

    public String getCurrentServer() {
        return currentServer;
    }

    public String getCurrentServerId() {
        return currentServerId;
    }

    public void reset() {
        this.currentServer = null;
        this.currentServerId = null;
        this.currentServerType = null;
    }

    private String readStringFromBuffer(int maxLength, PacketBuffer packetBuffer) {
        int i = readVarIntFromBuffer(packetBuffer);
        if (i > maxLength * 4) {
            throw new DecoderException("The received encoded string buffer length is longer than maximum allowed (" + i + " > " + maxLength * 4 + ")");
        }
        if (i < 0) {
            throw new DecoderException("The received encoded string buffer length is less than zero! Weird string!");
        }
        String s = new String(packetBuffer.readBytes(i).array(), StandardCharsets.UTF_8);
        if (s.length() > maxLength) {
            throw new DecoderException("The received string length is longer than maximum allowed (" + i + " > " + maxLength + ")");
        }
        return s;
    }

    private int readVarIntFromBuffer(PacketBuffer packetBuffer) {
        int i = 0;
        int j = 0;
        for (; ; ) {
            byte b0 = packetBuffer.readByte();
            i |= (b0 & 0x7F) << j++ * 7;
            if (j > 5) {
                throw new RuntimeException("VarInt too big");
            }
            if ((b0 & 0x80) != 128) {
                break;
            }
        }
        return i;
    }

    @Override
    public void receiveMessage(String channelName, PacketBuffer packetBuffer) {
        if (!channelName.equals("GoMod")) {
            return;
        }
        try {
            packetBuffer = new PacketBuffer(packetBuffer.copy(0, packetBuffer.capacity()));
            if (packetBuffer.readableBytes() > 0) {
                String json = readStringFromBuffer(32767, packetBuffer);
                JsonElement jsonElement = this.jsonParser.parse(json);
                if (!jsonElement.isJsonObject()) {
                    return;
                }
                JsonObject jsonObject = jsonElement.getAsJsonObject();

                String action = jsonObject.get("action").getAsString().toUpperCase();
                JsonObject data = jsonObject.has("data") ? jsonObject.get("data").getAsJsonObject() : null;
                if (data != null && action.equals("JOIN_SERVER")) {
                    String serverType = data.get("cloud_type").getAsString().toUpperCase();
                    String serverId = data.get("id").getAsString();
                    System.out.println("registered server switch from " + this.currentServer + " to " + serverType);
                    this.currentServer = serverType;
                    this.currentServerId = serverId;
                    this.currentServerType = this.coresAddon.getSupportedGameType(this.currentServer);
                    this.coresAddon.handleServerSwitch(serverType, serverId);
                }
            }
        } catch (Exception error) {
            error.printStackTrace();
        }
    }
}
