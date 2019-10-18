package de.derrop.labymod.addons.cores.gametypes;
/*
 * Created by derrop on 30.09.2019
 */

import com.mojang.authlib.GameProfile;
import de.derrop.labymod.addons.cores.CoresAddon;
import de.derrop.labymod.addons.cores.regex.Patterns;
import de.derrop.labymod.addons.cores.statistics.types.CoresStatistics;
import net.labymod.settings.elements.ControlElement;
import net.minecraft.network.play.server.S38PacketPlayerListItem;

import java.util.UUID;
import java.util.function.Consumer;
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

    /*@Override
    protected Pattern[] getLeavePatterns() {
        return new Pattern[]{Patterns.PLAYER_INGAME_LEAVE_PATTERN, Patterns.PLAYER_LEAVE_PATTERN};
    }

    @Override
    protected Pattern[] getJoinPatterns() {
        return new Pattern[]{Patterns.PLAYER_JOIN_PATTERN};
    }*/

    @Override
    public void getProfileOfJoinedPlayer(CoresAddon coresAddon, String message, Object packet, Consumer<GameProfile> resultHandler) {
        if (packet instanceof S38PacketPlayerListItem) {
            S38PacketPlayerListItem packetPlayerListItem = ((S38PacketPlayerListItem) packet);
            if (packetPlayerListItem.func_179768_b() == S38PacketPlayerListItem.Action.ADD_PLAYER) {
                for (S38PacketPlayerListItem.AddPlayerData addPlayerData : packetPlayerListItem.func_179767_a()) {
                    resultHandler.accept(addPlayerData.getProfile());
                }
            }
        }
    }

    @Override
    public void getUUIDOfLeftPlayer(CoresAddon coresAddon, String message, Object packet, Consumer<UUID> resultHandler) {
        if (packet instanceof S38PacketPlayerListItem) {
            S38PacketPlayerListItem packetPlayerListItem = (S38PacketPlayerListItem) packet;
            if (packetPlayerListItem.func_179768_b() == S38PacketPlayerListItem.Action.REMOVE_PLAYER) {
                for (S38PacketPlayerListItem.AddPlayerData addPlayerData : packetPlayerListItem.func_179767_a()) {
                    if (addPlayerData.getProfile().getId() != null) {
                        resultHandler.accept(addPlayerData.getProfile().getId());
                    }
                }
            }
        }
    }

    @Override
    public boolean usesPacketsToCheckPlayerLeave() {
        return true;
    }

    @Override
    public boolean usesPacketsToCheckPlayerJoin() {
        return true;
    }
}
