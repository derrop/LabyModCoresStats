package de.derrop.labymod.addons.cores.clan;
/*
 * Created by derrop on 22.09.2019
 */

import net.labymod.core.LabyModCore;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.Map;

public class ClanDetector {

    private Map<String, String> cachedClanShortcuts = new HashMap<>();

    /**
     * Clears the player-clan cache
     */
    public void clearCache() {
        this.cachedClanShortcuts.clear();
    }

    /**
     * Gets the clan of the player from the cache or if the player is not cached, this detects the clan shortcut of the given player from the scoreboard
     *
     * @param playerName the name of the player
     * @return the shortcut or null if the player is in no clan
     */
    public String getClanShortcut(String playerName) {
        if (this.cachedClanShortcuts.containsKey(playerName))
            return this.cachedClanShortcuts.get(playerName);
        String shortcut = this.detectClanShortcut(playerName);
        if (shortcut == null)
            return null;
        this.cachedClanShortcuts.put(playerName, shortcut);
        return shortcut;
    }

    /**
     * Gets the clan shortcut of the self player
     *
     * @return the shortcut or null if the player is in no clan
     * @see ClanDetector#getClanShortcut(String)
     */
    public String getSelfClanShortcut() {
        return this.getClanShortcut(LabyModCore.getMinecraft().getPlayer().getName());
    }

    /**
     * Detects the clan shortcut of the given player from the scoreboard
     *
     * @param playerName the name of the player
     * @return the shortcut or null if the player is in no clan
     */
    public String detectClanShortcut(String playerName) {
        Scoreboard scoreboard = LabyModCore.getMinecraft().getWorld().getScoreboard();
        if (scoreboard != null) {
            for (ScorePlayerTeam team : scoreboard.getTeams()) {
                if (team.getMembershipCollection().contains(playerName)) {
                    if (team.getColorSuffix().length() < 9) //player is in no clan
                        return null;
                    String shortcut = team.getColorSuffix().substring(6, team.getColorSuffix().length() - 3);
                    this.cachedClanShortcuts.put(playerName, shortcut);
                    return shortcut;
                }
            }
        }
        return null;
    }

}
