package de.derrop.labymod.addons.cores.detector;
/*
 * Created by derrop on 30.09.2019
 */

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.authlib.GameProfile;
import de.derrop.labymod.addons.cores.CoresAddon;
import de.derrop.labymod.addons.cores.gametypes.GameType;
import de.derrop.labymod.addons.cores.regex.Patterns;
import net.labymod.api.events.MessageReceiveEvent;

import java.util.Collection;

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

        if (this.coresAddon.getSyncClient().isConnected()) {
            JsonObject payload = new JsonObject();
            JsonArray players = new JsonArray();
            for (GameProfile value : this.coresAddon.getOnlinePlayers().values()) {
                players.add(new JsonPrimitive(value.getName()));
            }
            payload.add("players", players);
            payload.addProperty("map", this.currentMap);
            payload.addProperty("serverType", this.coresAddon.getCurrentServer());
            payload.addProperty("serverId", this.coresAddon.getCurrentServerId());
            this.coresAddon.getSyncClient().sendPacket((short) 1, payload);
        }
    }

    public void handleMatchEnd(String winnerTeam) {
        System.out.println("Detected match end");
        System.out.println("This match (map: \"" + this.currentMap + "\") took " + (System.currentTimeMillis() - this.coresAddon.getLastRoundBeginTimestamp()) + " ms");
        this.inMatch = false;

        Collection<String> winners = null;

        if (winnerTeam != null) {
            String prefix = Patterns.getScoreboardTeamPrefix(winnerTeam);
            if (prefix == null) {
                System.err.println("Failed to parse team \"" + winnerTeam + "\", language not supported!");
            }
            winners = this.coresAddon.getScoreboardTagDetector().getPlayersWithPrefix(s -> s.equals(prefix));
            System.out.println("Winners: " + winners + " [Team " + winnerTeam + "]");
        }

        if (this.coresAddon.getSyncClient().isConnected()) {
            JsonObject payload = new JsonObject();
            if (winnerTeam != null) {
                JsonArray winnersArray = new JsonArray();
                for (String winner : winners) {
                    winnersArray.add(new JsonPrimitive(winner));
                }
                payload.add("winners", winnersArray);
            }
            this.coresAddon.getSyncClient().sendPacket((short) 2, payload);
        }
    }

    public void removePlayerFromMatch(String player) {
        if (this.coresAddon.getCurrentServerId() == null || !this.inMatch) {
            return;
        }

        if (this.coresAddon.getSyncClient().isConnected()) {
            this.coresAddon.getSyncClient().sendPacket((short) 3, new JsonPrimitive(player));
        }
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
