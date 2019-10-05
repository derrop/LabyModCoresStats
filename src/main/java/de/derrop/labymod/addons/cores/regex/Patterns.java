package de.derrop.labymod.addons.cores.regex;
/*
 * Created by derrop on 25.09.2019
 */

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Patterns {

    private static final Map<String, String> TEAM_NAME_TAG_MAP = new HashMap<>();

    static {
        TEAM_NAME_TAG_MAP.put("Rot", "§cRot §7| §c");
        TEAM_NAME_TAG_MAP.put("Red", "§cRed §7| §c");

        TEAM_NAME_TAG_MAP.put("Blau", "§9Blau §7| §9");
        TEAM_NAME_TAG_MAP.put("Blue", "§9Blau §7| §9");
        //todo support for more teams
    }

    // -------------- german|english --------------

    //statistics
    public static final Pattern BEGIN_STATS_PATTERN = Pattern.compile("-= Statistiken von (.*) \\(30 Tage\\) =-|-= (.*)'s statistics \\(30 days\\) =-");

    //scoreboard
    public static final Pattern SCOREBOARD_SUFFIX_PATTERN = Pattern.compile("§7 \\[(.*)§7]");

    //general
    public static final Pattern PLAYER_JOIN_PATTERN = Pattern.compile("» (.*) hat das Spiel betreten|» (.*) joined the game");

    public static String matcherGroup(Matcher matcher) {
        String firstGroup = matcher.group(1);  //1|2
        if (firstGroup != null) {
            return firstGroup;
        }

        String secondGroup = matcher.group(2); //1|2
        if (secondGroup != null) {
            return secondGroup;
        }
        return null;
    }

    public static String getScoreboardTeamPrefix(String teamName) {
        return TEAM_NAME_TAG_MAP.get(teamName);
    }

}
