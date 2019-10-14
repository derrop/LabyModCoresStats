package de.derrop.labymod.addons.cores.network.sync;
/*
 * Created by derrop on 30.09.2019
 */

import com.google.gson.JsonElement;

import java.util.function.Consumer;

public interface PacketHandler {
    void handlePacket(JsonElement payload, Consumer<JsonElement> responseConsumer);
}
