package de.derrop.labymod.addons.cores.gametypes;
/*
 * Created by derrop on 30.09.2019
 */

import de.derrop.labymod.addons.cores.regex.Patterns;
import de.derrop.labymod.addons.cores.statistics.types.CoresStatistics;
import net.labymod.settings.elements.ControlElement;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoresGameType extends GameType {

    private static final Pattern WINNER_PATTERN = Pattern.compile("\\[*] Team (.*) hat .* gewonnen"); //todo english
    private static final Pattern MATCH_BEGIN_PATTERN = Pattern.compile("\\[.*] Alle Spieler werden auf die Map teleportiert!"); //todo english
    private static final Pattern MAP_PATTERN = Pattern.compile("\\[*] Map: (.*) von: .*|\\[Cores] Map (.*) by: .*");
    private static final Pattern MATCH_END_PATTERN = Pattern.compile("\\[.*] Du bist nun Zuschauer!"); //todo english

    public CoresGameType(ControlElement.IconData iconData, boolean defaultEnabled) {
        super("CORES", CoresStatistics::new, iconData, "blocks/beacon", defaultEnabled);
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
