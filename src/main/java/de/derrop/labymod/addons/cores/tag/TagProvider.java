package de.derrop.labymod.addons.cores.tag;
/*
 * Created by derrop on 10.10.2019
 */

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.derrop.labymod.addons.cores.CoresAddon;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class TagProvider {

    private CoresAddon coresAddon;

    public TagProvider(CoresAddon coresAddon) {
        this.coresAddon = coresAddon;
    }

    public boolean isUseable() {
        return this.coresAddon.getSyncClient().isConnected();
    }

    public CompletableFuture<Collection<Tag>> listTags(TagType tagType, String name) {
        if (!this.isUseable()) {
            return CompletableFuture.completedFuture(null);
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("type", tagType.toString());
        payload.addProperty("name", name);
        payload.addProperty("query", "list");
        CompletableFuture<Collection<Tag>> future = new CompletableFuture<>();
        this.coresAddon.getSyncClient().sendQuery((short) 5, payload).thenAccept(element -> {
            if (element.isJsonArray()) {
                Collection<Tag> tags = new ArrayList<>();
                for (JsonElement content : element.getAsJsonArray()) {
                    tags.add(this.coresAddon.getGson().fromJson(content, Tag.class));
                }
                future.complete(tags);
            } else {
                future.complete(null);
            }
        });
        return future;
    }

    public CompletableFuture<Boolean> addTag(TagType tagType, String name, String tag) {
        if (!this.isUseable()) {
            return CompletableFuture.completedFuture(false);
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("type", tagType.toString());
        payload.addProperty("name", name);
        payload.addProperty("tag", tag);
        payload.addProperty("query", "add");
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        this.coresAddon.getSyncClient().sendQuery((short) 5, payload).thenAccept(element -> future.complete(element.getAsBoolean()));
        return future;
    }

    public void removeTag(TagType tagType, String name, String tag) {
        if (!this.isUseable()) {
            return;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("type", tagType.toString());
        payload.addProperty("name", name);
        payload.addProperty("tag", tag);
        payload.addProperty("query", "remove");
        this.coresAddon.getSyncClient().sendPacket((short) 5, payload);
    }
}
