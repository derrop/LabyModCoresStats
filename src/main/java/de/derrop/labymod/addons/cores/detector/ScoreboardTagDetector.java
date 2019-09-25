package de.derrop.labymod.addons.cores.detector;
/*
 * Created by derrop on 22.09.2019
 */

import de.derrop.labymod.addons.cores.regex.Patterns;
import net.labymod.core.LabyModCore;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.Map;

public class ScoreboardTagDetector {

    private Map<String, String> cachedTags = new HashMap<>();

    /**
     * Clears the player-tag cache
     */
    public void clearCache() {
        this.cachedTags.clear();
    }

    /**
     * Gets the tag of the player from the cache or if the player is not cached, this detects the scoreboard tag of the given player from the scoreboard
     *
     * @param playerName the name of the player
     * @return the tag or null if the player is in no clan/party
     */
    public String getScoreboardTag(String playerName) {
        if (this.cachedTags.containsKey(playerName)) {
            return this.cachedTags.get(playerName);
        }
        String shortcut = this.detectScoreboardTag(playerName);
        if (shortcut == null) {
            return null;
        }
        this.cachedTags.put(playerName, shortcut);
        return shortcut;
    }

    /**
     * Gets the scoreboard tag of the self player
     *
     * @return the tag or null if the player is in no clan/party
     * @see ScoreboardTagDetector#getScoreboardTag(String)
     */
    public String getSelfScoreboardTag() {
        return this.getScoreboardTag(LabyModCore.getMinecraft().getPlayer().getName());
    }

    /**
     * Detects the scoreboard tag of the given player from the scoreboard
     *
     * @param playerName the name of the player
     * @return the tag or null if the player is in no clan/party
     */
    private String detectScoreboardTag(String playerName) {
        Scoreboard scoreboard = LabyModCore.getMinecraft().getWorld().getScoreboard();
        if (scoreboard != null) {
            for (ScorePlayerTeam team : scoreboard.getTeams()) {
                if (team.getMembershipCollection().contains(playerName)) {
                    String suffix = team.getColorSuffix();
                    String tag = Patterns.matcherGroup(Patterns.SCOREBOARD_CLAN_PATTERN.matcher(suffix), Patterns.SCOREBOARD_PARTY_PATTERN.matcher(suffix));

                    if (tag != null) {
                        this.cachedTags.put(playerName, tag);
                        return tag;
                    }
                }
            }
        }
        return null;
    }

}
