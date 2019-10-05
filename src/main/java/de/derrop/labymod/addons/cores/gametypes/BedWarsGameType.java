package de.derrop.labymod.addons.cores.gametypes;
/*
 * Created by derrop on 30.09.2019
 */

import de.derrop.labymod.addons.cores.regex.Patterns;
import de.derrop.labymod.addons.cores.statistics.types.BedWarsStatistics;
import net.labymod.settings.elements.ControlElement;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BedWarsGameType extends GameType {
    private static final Pattern MATCH_BEGIN_PATTERN = Pattern.compile("\\[.*] Das Spiel beginnt!"); //todo english
    private static final Pattern WINNER_PATTERN = Pattern.compile("\\[*] Team (.*) hat .* gewonnen"); //todo english
    private static final Pattern MAP_PATTERN = Pattern.compile("\\[*] Map: (.*) von: .*|\\[Cores] Map (.*) by: .*");
    private static final Pattern MATCH_END_PATTERN = Pattern.compile("\\[.*] Du bist nun Zuschauer!"); //todo english

    public BedWarsGameType(ControlElement.IconData iconData, boolean defaultEnabled) {
        super("BW", BedWarsStatistics::new, iconData, "items/bed", defaultEnabled);
    }

    @Override
    public boolean matchesBeginMessage(String message) {
        return MATCH_BEGIN_PATTERN.matcher(message).matches();
    }

    @Override
    public boolean matchesEndMessage(String message) {
        return MATCH_END_PATTERN.matcher(message).matches();
    }

    @Override
    public String parseWinnerFromMessage(String message) {
        Matcher matcher = WINNER_PATTERN.matcher(message);
        return matcher.find() ? Patterns.matcherGroup(matcher) : null;
    }

    @Override
    public String parseMapFromMessage(String message) {
        Matcher matcher = MAP_PATTERN.matcher(message);
        return matcher.find() ? Patterns.matcherGroup(matcher) : null;
    }
}
