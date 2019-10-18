package de.derrop.labymod.addons.cores.gametypes;
/*
 * Created by derrop on 17.10.2019
 */

import com.mojang.authlib.GameProfile;
import de.derrop.labymod.addons.cores.CoresAddon;
import de.derrop.labymod.addons.cores.player.OnlinePlayer;
import de.derrop.labymod.addons.cores.regex.Patterns;
import de.derrop.labymod.addons.cores.statistics.PlayerStatistics;
import net.labymod.core.LabyModCore;
import net.labymod.settings.elements.ControlElement;
import net.minecraft.client.network.NetworkPlayerInfo;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractMessageJoinLeaveDetectingGameType extends GameType {

    public AbstractMessageJoinLeaveDetectingGameType(String name, Function<String, PlayerStatistics> statisticsProvider, ControlElement.IconData iconData, String minecraftTexturePath, boolean defaultEnabled) {
        super(name, statisticsProvider, iconData, minecraftTexturePath, defaultEnabled);
    }

    protected abstract Pattern[] getLeavePatterns();

    protected abstract Pattern[] getJoinPatterns();

    @Override
    public void getUUIDOfLeftPlayer(CoresAddon coresAddon, String message, Object packet, Consumer<UUID> resultHandler) {
        String name = null;

        for (Pattern pattern : this.getLeavePatterns()) {
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                name = Patterns.matcherGroup(matcher);
                break;
            }
        }

        if (name != null) {
            OnlinePlayer player = coresAddon.getPlayerProvider().getOnlinePlayer(name);
            if (player != null) {
                resultHandler.accept(player.getUniqueId());
            }
        }
    }

    @Override
    public void getProfileOfJoinedPlayer(CoresAddon coresAddon, String message, Object packet, Consumer<GameProfile> resultHandler) {
        String name = null;

        for (Pattern pattern : this.getJoinPatterns()) {
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                name = Patterns.matcherGroup(matcher);
                break;
            }
        }

        if (name != null) {
            for (NetworkPlayerInfo playerInfo : LabyModCore.getMinecraft().getConnection().getPlayerInfoMap()) {
                if (playerInfo.getGameProfile().getName().equals(name)) {
                    resultHandler.accept(playerInfo.getGameProfile());
                    break;
                }
            }
        }
    }

    @Override
    public boolean usesPacketsToCheckPlayerLeave() {
        return false;
    }

    @Override
    public boolean usesPacketsToCheckPlayerJoin() {
        return false;
    }
}
