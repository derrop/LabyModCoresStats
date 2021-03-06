package de.derrop.labymod.addons.cores.detector;
/*
 * Created by derrop on 22.09.2019
 */

import de.derrop.labymod.addons.cores.player.PlayerProvider;
import de.derrop.labymod.addons.cores.regex.Patterns;
import net.labymod.core.LabyModCore;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.regex.Matcher;

public class ScoreboardTagDetector {

    /**
     * Detects the scoreboard tag of the given player from the scoreboard
     *
     * @param playerName the name of the player
     * @return the tag or null if the player is in no clan/party
     */
    public String detectScoreboardTag(String playerName) {
        Scoreboard scoreboard = LabyModCore.getMinecraft().getWorld().getScoreboard();
        if (scoreboard != null) {
            for (ScorePlayerTeam team : scoreboard.getTeams()) {
                if (team.getMembershipCollection().contains(playerName)) {
                    String suffix = team.getColorSuffix();
                    Matcher matcher = Patterns.SCOREBOARD_SUFFIX_PATTERN.matcher(suffix);
                    if (matcher.find()) {
                        String tag = Patterns.matcherGroup(matcher);

                        if (tag != null) {
                            return tag;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Detects the players that have the given prefix
     *
     * @param prefixTester the prefix to check for
     * @return a collection containing all player names that have the given prefix in our scoreboard
     */
    public Collection<String> getPlayersWithPrefix(Predicate<String> prefixTester) {
        Collection<String> players = new ArrayList<>();
        Scoreboard scoreboard = LabyModCore.getMinecraft().getWorld().getScoreboard();
        if (scoreboard != null) {
            for (ScorePlayerTeam team : scoreboard.getTeams()) {
                if (prefixTester.test(team.getColorPrefix())) {
                    players.addAll(team.getMembershipCollection());
                }
            }
        }
        return players;
    }

    /**
     * Checks whether the given string is the tag for parties or not
     *
     * @param scoreboardTag the tag to check for from {@link ScoreboardTagDetector#detectScoreboardTag(String)}
     * @return {@code true} if it is a party or {@code false} if it is a clan or nothing
     */
    public boolean isParty(String scoreboardTag) {
        return scoreboardTag != null && scoreboardTag.equals("§5Party");
    }

}
