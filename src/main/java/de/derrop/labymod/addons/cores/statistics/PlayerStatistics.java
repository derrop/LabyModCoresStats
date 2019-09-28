package de.derrop.labymod.addons.cores.statistics;
/*
 * Created by derrop on 22.09.2019
 */

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class PlayerStatistics {

    private String name;
    private String gameType;
    private Map<String, String> stats;

    public PlayerStatistics(String name, String gameTYpe) {
        this.name = name;
        this.stats = new HashMap<>();
        this.gameType = gameTYpe;
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getStats() {
        return stats;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStats(Map<String, String> stats) {
        this.stats = stats;
    }

    public String getGameType() {
        return gameType;
    }

    public abstract void parseLine(String message);

    public abstract boolean hasRank();

    public abstract int getRank();

    public abstract Collection<String> getHumanReadableEntries();

    public abstract boolean isStatsEnd(String message);

    public abstract void warnOnGoodStats(Consumer<String> outputConsumer, Runnable warnSoundHandler);

    @Override
    public String toString() {
        return "PlayerStatistics(name=" + this.name + ";stats=" + this.stats + ")";
    }
}
