package de.derrop.labymod.addons.cores.player;
/*
 * Created by derrop on 16.10.2019
 */

import com.mojang.authlib.GameProfile;
import de.derrop.labymod.addons.cores.CoresAddon;
import de.derrop.labymod.addons.cores.statistics.PlayerStatistics;
import de.derrop.labymod.addons.cores.tag.Tag;
import de.derrop.labymod.addons.cores.tag.TagType;
import net.labymod.main.LabyMod;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class OnlinePlayer {
    private transient CoresAddon coresAddon;
    private transient PlayerDataProviders playerDataProviders;

    private GameProfile profile;
    private String scoreboardTag;
    private Collection<Tag> cachedTags;
    private PlayerStatistics lastStatistics;

    public OnlinePlayer(CoresAddon coresAddon, PlayerDataProviders playerDataProviders, GameProfile profile) {
        this.coresAddon = coresAddon;
        this.playerDataProviders = playerDataProviders;
        this.profile = profile;
    }

    public UUID getUniqueId() {
        return this.profile.getId();
    }

    public String getName() {
        return this.profile.getName();
    }

    public Collection<Tag> getCachedTags() {
        try {
            return this.cachedTags != null ? this.cachedTags :
                    (this.cachedTags = this.coresAddon.getTagProvider().listTags(TagType.PLAYER, this.profile.getName()).get());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public CompletableFuture<Collection<Tag>> loadTags() {
        return this.coresAddon.getTagProvider().listTags(TagType.PLAYER, this.profile.getName());
    }

    public boolean isSelf() {
        return this.getUniqueId().equals(LabyMod.getInstance().getPlayerUUID());
    }

    /**
     * @deprecated for internal use only
     */
    @Deprecated
    public void removeTagFromCache(String tag) {
        this.cachedTags.stream().filter(filterTag -> filterTag.getTag().equals(tag)).findFirst().ifPresent(this.cachedTags::remove);
    }

    public GameProfile getProfile() {
        return this.profile;
    }

    public PlayerStatistics getLastStatistics() {
        return this.lastStatistics;
    }

    public CompletableFuture<PlayerStatistics> loadStatistics() {
        return this.playerDataProviders.getStatsParser().requestStats(this.profile.getName());
    }

    public void updateCachedStats(PlayerStatistics statistics) {
        this.lastStatistics = statistics;
    }

    public String getScoreboardTag() {
        return this.scoreboardTag != null ? this.scoreboardTag :
                (this.scoreboardTag = this.playerDataProviders.getScoreboardTagDetector().detectScoreboardTag(this.profile.getName()));
    }

    public boolean isInParty() {
        return this.playerDataProviders.getScoreboardTagDetector().isParty(this.getScoreboardTag());
    }

}
