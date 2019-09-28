package de.derrop.labymod.addons.cores.gametypes;
/*
 * Created by derrop on 28.09.2019
 */

import de.derrop.labymod.addons.cores.statistics.PlayerStatistics;
import net.labymod.settings.elements.ControlElement;

import java.util.function.Function;

public class GameType {

    private String name;
    private Function<String, PlayerStatistics> statisticsProvider;
    private ControlElement.IconData iconData;
    private boolean defaultEnabled;
    private boolean enabled;

    public GameType(String name, Function<String, PlayerStatistics> statisticsProvider, ControlElement.IconData iconData, boolean defaultEnabled) {
        this.name = name;
        this.statisticsProvider = statisticsProvider;
        this.iconData = iconData;
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

    public boolean isDefaultEnabled() {
        return defaultEnabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
