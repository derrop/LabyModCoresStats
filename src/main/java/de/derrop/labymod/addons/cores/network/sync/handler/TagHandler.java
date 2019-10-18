package de.derrop.labymod.addons.cores.network.sync.handler;
/*
 * Created by derrop on 15.10.2019
 */

import com.google.gson.JsonElement;
import de.derrop.labymod.addons.cores.CoresAddon;
import de.derrop.labymod.addons.cores.network.sync.PacketHandler;
import de.derrop.labymod.addons.cores.player.OnlinePlayer;
import de.derrop.labymod.addons.cores.tag.Tag;
import de.derrop.labymod.addons.cores.tag.TagType;

import java.util.function.Consumer;

public class TagHandler implements PacketHandler {

    private CoresAddon coresAddon;

    public TagHandler(CoresAddon coresAddon) {
        this.coresAddon = coresAddon;
    }

    @Override
    public void handlePacket(JsonElement payload, Consumer<JsonElement> responseConsumer) {
        if (!payload.isJsonObject()) {
            return;
        }
        String query = payload.getAsJsonObject().get("query").getAsString();
        TagType tagType = TagType.valueOf(payload.getAsJsonObject().get("type").getAsString());
        String name = payload.getAsJsonObject().get("name").getAsString();
        String tagName = payload.getAsJsonObject().get("tag").getAsString();
        OnlinePlayer player = this.coresAddon.getPlayerProvider().getOnlinePlayer(name);
        if (player != null) {
            if (query.equals("add")) {
                Tag tag = new Tag(tagType, name, tagName);
                player.getCachedTags().add(tag);
            } else if (query.equals("remove")) {
                player.removeTagFromCache(tagName);
            }
        }
    }
}
