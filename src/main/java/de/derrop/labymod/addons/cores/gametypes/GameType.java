package de.derrop.labymod.addons.cores.gametypes;
/*
 * Created by derrop on 28.09.2019
 */

import com.mojang.authlib.GameProfile;
import de.derrop.labymod.addons.cores.CoresAddon;
import de.derrop.labymod.addons.cores.statistics.PlayerStatistics;
import net.labymod.settings.elements.ControlElement;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class GameType {

    private String name;
    private Function<String, PlayerStatistics> statisticsProvider;
    private ControlElement.IconData iconData;
    private String minecraftTexturePath;
    private boolean defaultEnabled;
    private boolean enabled;

    public GameType(String name, Function<String, PlayerStatistics> statisticsProvider, ControlElement.IconData iconData, String minecraftTexturePath, boolean defaultEnabled) {
        this.name = name;
        this.statisticsProvider = statisticsProvider;
        this.iconData = iconData;
        this.minecraftTexturePath = minecraftTexturePath;
        this.defaultEnabled = defaultEnabled;
    }

    public String getName() {
        return name;
    }

    public Function<String, PlayerStatistics> getStatisticsProvider() {
        return statisticsProvider;
    }

    public ControlElement.IconData getIconData() {
        return iconData;
    }

    public String getMinecraftTexturePath() {
        return minecraftTexturePath;
    }

    public boolean isDefaultEnabled() {
        return defaultEnabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public abstract boolean matchesBeginMessage(String message);

    public abstract boolean matchesEndMessage(String message);

    public abstract String parseWinnerFromMessage(String message);

    public abstract String parseMapFromMessage(String message);

    public abstract void getUUIDOfLeftPlayer(CoresAddon coresAddon, String message, Object packet, Consumer<UUID> resultHandler);

    public abstract boolean usesPacketsToCheckPlayerLeave();

    public abstract void getProfileOfJoinedPlayer(CoresAddon coresAddon, String message, Object packet, Consumer<GameProfile> resultHandler);

    public abstract boolean usesPacketsToCheckPlayerJoin();

    @Override
    public String toString() {
        return this.name;
    }
}
