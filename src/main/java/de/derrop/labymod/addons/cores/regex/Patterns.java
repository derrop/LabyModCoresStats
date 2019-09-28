package de.derrop.labymod.addons.cores.regex;
/*
 * Created by derrop on 25.09.2019
 */

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Patterns {

    // -------------- german|english --------------

    //statistics
    public static final Pattern BEGIN_STATS_PATTERN = Pattern.compile("-= Statistiken von (.*) \\(30 Tage\\) =-|-= (.*)'s statistics \\(30 days\\) =-");

    //scoreboard
    public static final Pattern SCOREBOARD_SUFFIX_PATTERN = Pattern.compile("\\[§e(.*)§7]|\\[§5(.*)§7]"); //clan|party

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

}
