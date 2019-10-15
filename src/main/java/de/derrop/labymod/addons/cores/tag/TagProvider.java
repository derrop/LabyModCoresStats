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

    private Map<String, Collection<Tag>> cachedTags = new HashMap<>();

    public TagProvider(CoresAddon coresAddon) {
        this.coresAddon = coresAddon;
        /*this.cachedTags.put("derrop", new ArrayList<>(Arrays.asList(
                new Tag(TagType.PLAYER, "derrop", "bowspam"),
                new Tag(TagType.PLAYER, "derrop", "fulldia"),
                new Tag(TagType.PLAYER, "derrop", "bescht"),
                new Tag(TagType.PLAYER, "derrop", "only_fulliron")
        )));*/
    }

    public boolean isUseable() {
        return this.coresAddon.getSyncClient().isConnected();
    }

    public Collection<Tag> getCachedTags(String name) {
        return this.cachedTags.get(name);
    }

    public void cacheTag(Tag tag) {
        if (!this.cachedTags.containsKey(tag.getName())) {
            this.cachedTags.put(tag.getName(), new ArrayList<>());
        }
        this.cachedTags.get(tag.getName()).add(tag);
    }

    public void removeFromCache(String name, String tag) {
        if (this.cachedTags.containsKey(name)) {
            Collection<Tag> tags = this.cachedTags.get(name);
            tags.stream().filter(filterTag -> filterTag.getTag().equals(tag)).findFirst().ifPresent(tags::remove);
            if (tags.isEmpty()) {
                this.cachedTags.remove(name);
            }
        }
    }

    public void removeFromCache(String name) {
        this.cachedTags.remove(name);
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
                if (tagType == TagType.PLAYER) {
                    this.cachedTags.put(name, tags);
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
