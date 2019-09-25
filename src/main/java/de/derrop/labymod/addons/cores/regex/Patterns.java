package de.derrop.labymod.addons.cores.regex;
/*
 * Created by derrop on 25.09.2019
 */

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Patterns {

    // -------------- german|english --------------

    //statistics
    public static final Pattern BEGIN_STATS_PATTERN = Pattern.compile("-= Statistiken von (.*) \\(30 Tage\\) =-|-= (.*)'s statistics \\(30 days\\) =-");

    public static final Map<Pattern, String> STATS_ENTRIES = new HashMap<>();

    static {
        STATS_ENTRIES.put(Pattern.compile(" Position im Ranking: (.*)| Ranking: (.*)"), "rank");
        STATS_ENTRIES.put(Pattern.compile(" Kills: (.*)"), "kills");
        STATS_ENTRIES.put(Pattern.compile(" Deaths: (.*)"), "deaths");
        STATS_ENTRIES.put(Pattern.compile(" K/D: (.*)"), "kd");
        STATS_ENTRIES.put(Pattern.compile(" Zerstörte Cores: (.*)| Cores destroyed: (.*)"), "destroyedCores");
        STATS_ENTRIES.put(Pattern.compile(" Gespielte Spiele: (.*)| Games played: (.*)"), "playedGames");
        STATS_ENTRIES.put(Pattern.compile(" Gewonnene Spiele: (.*)| Games won: (.*)"), "wonGames");
        STATS_ENTRIES.put(Pattern.compile(" Siegwahrscheinlichkeit: (.*) Prozent| Probability of winning: (.*)%"), "winRate");
    }
    //statistics

    //parties
    public static final Pattern PARTY_JOIN_PATTERN = Pattern.compile("\\[Party\\] (.*) hat die Party betreten|\\[Party\\] (.*) joined the party");
    public static final Pattern PARTY_LEAVE_PATTERN = Pattern.compile("\\[Party\\] (.*) hat die Party verlassen|\\[Party\\] (.*) left the party");
    public static final Pattern PARTY_LIST_PATTERN = Pattern.compile("\\[Party\\] Mitglieder der Party von (.*) \\(*\\)|\\[Party\\] Members of (.*)'s party \\(*\\)");
    //parties

    //general
    public static final Pattern PLAYER_JOIN_PATTERN = Pattern.compile("» (.*) hat das Spiel betreten|» (.*) joined the game");
    //general

    public static String matcherGroup(Matcher matcher) {
        return matcher.group(1) != null ? matcher.group(1) : //german
                matcher.group(2);                            //english
    }

}
