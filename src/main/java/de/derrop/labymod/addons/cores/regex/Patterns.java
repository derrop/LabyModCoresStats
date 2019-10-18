package de.derrop.labymod.addons.cores.regex;
/*
 * Created by derrop on 25.09.2019
 */

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Patterns {

    private static final Map<String, Collection<String>> TEAM_NAME_TAG_MAP = new HashMap<>();

    static {
        registerScoreboardTeamPrefix(
                new String[]{
                        "red",
                        "rot"
                },
                new String[]{
                        "§cRed §7| §c",
                        "§cRot §7| §c"
                }
        );
        registerScoreboardTeamPrefix(
                new String[]{
                        "blue",
                        "blau"
                },
                new String[]{
                        "§9Blau §7| §9",
                        "§9Blue §7| §9"
                });
        //todo support for more teams
    }

    // -------------- german|english --------------

    //statistics
    public static final Pattern BEGIN_STATS_PATTERN = Pattern.compile("-= Statistiken von (.*) \\(30 Tage\\) =-|-= (.*)'s statistics \\(30 days\\) =-");

    //scoreboard
    public static final Pattern SCOREBOARD_SUFFIX_PATTERN = Pattern.compile("§7 \\[(.*)§7]");

    //general
    public static final Pattern PLAYER_JOIN_PATTERN = Pattern.compile("» (.*) hat das Spiel betreten|» (.*) joined the game");
    public static final Pattern PLAYER_LEAVE_PATTERN = Pattern.compile("« (.*) hat das Spiel verlassen|« (.*) left the game");
    public static final Pattern PLAYER_INGAME_LEAVE_PATTERN = Pattern.compile("\\[BedWars] (.*) hat das Spiel verlassen. Team .* hat noch .* Spieler.|\\[BedWars] (.*) left the game. Team .* has .* remaining players.");

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

    public static void registerScoreboardTeamPrefix(String[] names, String[] prefixes) {
        for (String name : names) {
            TEAM_NAME_TAG_MAP.put(name, Arrays.asList(prefixes));
        }
    }

    public static Collection<String> getPossibleTeamPrefixes(String teamName) {
        return TEAM_NAME_TAG_MAP.get(teamName.toLowerCase());
    }

}
