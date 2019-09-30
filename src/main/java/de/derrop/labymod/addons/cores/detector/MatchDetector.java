package de.derrop.labymod.addons.cores.detector;
/*
 * Created by derrop on 30.09.2019
 */

import de.derrop.labymod.addons.cores.CoresAddon;
import de.derrop.labymod.addons.cores.gametypes.GameType;
import de.derrop.labymod.addons.cores.regex.Patterns;
import net.labymod.api.events.MessageReceiveEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;

public class MatchDetector implements MessageReceiveEvent {

    private CoresAddon coresAddon;

    private String currentMap;
    private boolean inMatch;

    public MatchDetector(CoresAddon coresAddon) {
        this.coresAddon = coresAddon;
    }

    public String getCurrentMap() {
        return currentMap;
    }

    public void handleMatchBegin() {
        System.out.println("Detected match begin on map \"" + this.currentMap + "\"");
        this.inMatch = true;

        this.coresAddon.setLastRoundBeginTimestamp(System.currentTimeMillis());

        //send packet to server
    }

    public void addPlayerToMatch(String player) {
        if (this.coresAddon.getCurrentServerId() == null) {
            return;
        }

        //send packet to server
    }

    public void handleMatchEnd(String winnerTeam) {
        System.out.println("Detected match end");
        System.out.println("This match (map: \"" + this.currentMap + "\") took " + (System.currentTimeMillis() - this.coresAddon.getLastRoundBeginTimestamp()) + " ms");
        this.inMatch = false;

        Collection<String> winners;

        if (winnerTeam != null) {
            String prefix = Patterns.getScoreboardTeamPrefix(winnerTeam);
            if (prefix == null) {
                System.err.println("Failed to parse team \"" + winnerTeam + "\", language not supported!");
            }
            winners = this.coresAddon.getScoreboardTagDetector().getPlayersWithPrefix(s -> s.equals(prefix));
        } else {
            winners = new ArrayList<>();
        }

        //send packet to server
    }

    @Override
    public boolean onReceive(String coloredMessage, String message) {
        GameType gameType = this.coresAddon.getCurrentServerType();
        if (gameType != null) {
            if (gameType.matchesBeginMessage(message)) {
                this.handleMatchBegin();
                return false;
            }

            String map = gameType.parseMapFromMessage(message);
            if (map != null) {
                this.currentMap = map;
                return false;
            }

            String winnerTeam = gameType.parseWinnerFromMessage(message);
            if (winnerTeam != null) {
                this.handleMatchEnd(winnerTeam);
                return false;
            }

            if (gameType.matchesEndMessage(message)) {
                this.handleMatchEnd(null);
                return false;
            }
        }
        return false;
    }
}
