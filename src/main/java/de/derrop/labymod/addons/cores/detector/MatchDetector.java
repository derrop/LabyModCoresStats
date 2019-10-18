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
import net.labymod.core.LabyModCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;

import java.util.Collection;

public class MatchDetector implements MessageReceiveEvent {

    private CoresAddon coresAddon;

    private String currentMap;
    private boolean inMatch;
    private GameType currentMatchGameType;

    public MatchDetector(CoresAddon coresAddon) {
        this.coresAddon = coresAddon;
    }

    public boolean isInMatch() {
        return inMatch;
    }

    public String getCurrentMap() {
        return currentMap;
    }

    public void handleMatchBegin() {
        System.out.println("Detected match begin on map \"" + this.currentMap + "\"");
        this.inMatch = true;
        this.currentMatchGameType = this.coresAddon.getCurrentServerType();

        this.coresAddon.setLastRoundBeginTimestamp(System.currentTimeMillis());

        if (this.coresAddon.getSyncClient().isConnected()) {
            JsonObject payload = new JsonObject();
            JsonArray players = new JsonArray();
            for (NetworkPlayerInfo value : LabyModCore.getMinecraft().getConnection().getPlayerInfoMap()) {
                if (value.getGameProfile() != null) {
                    players.add(new JsonPrimitive(value.getGameProfile().getName()));
                }
            }
            payload.add("players", players);
            payload.addProperty("map", this.currentMap);
            payload.addProperty("serverType", this.coresAddon.getCurrentServer());
            payload.addProperty("serverId", this.coresAddon.getCurrentServerId());
            payload.addProperty("texturePath", this.coresAddon.getCurrentServerType().getMinecraftTexturePath());
            this.coresAddon.getSyncClient().sendPacket((short) 1, payload);
        }
    }

    public void handleMatchEnd(String winnerTeam) {
        System.out.println("Detected match end");
        System.out.println("This match (map: \"" + this.currentMap + "\") took " + (System.currentTimeMillis() - this.coresAddon.getLastRoundBeginTimestamp()) + " ms");
        this.inMatch = false;

        Collection<String> winners = null;

        if (winnerTeam != null) {
            Collection<String> possibleTeamPrefixes = Patterns.getPossibleTeamPrefixes(winnerTeam);
            if (possibleTeamPrefixes == null || possibleTeamPrefixes.isEmpty()) {
                System.err.println("Failed to parse team \"" + winnerTeam + "\", language not supported!");
            } else {
                winners = this.coresAddon.getPlayersWithPrefix(possibleTeamPrefixes::contains);
                System.out.println("Winners: " + winners + " [Team " + winnerTeam + "]");
            }
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
            JsonArray players = new JsonArray();
            for (NetworkPlayerInfo value : LabyModCore.getMinecraft().getConnection().getPlayerInfoMap()) {
                if (value.getGameProfile() != null) {
                    players.add(new JsonPrimitive(value.getGameProfile().getName()));
                }
            }
            payload.add("players", players);
            payload.addProperty("texturePath", this.currentMatchGameType.getMinecraftTexturePath());
            this.coresAddon.getSyncClient().sendPacket((short) 2, payload);
        }

        this.currentMatchGameType = null;
    }

    public void removePlayerFromMatch(String player) { //todo bug: this is called when a player dies (should be fixed now)
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
