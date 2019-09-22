package de.derrop.labymod.addons.cores.statistics;
/*
 * Created by derrop on 22.09.2019
 */

import java.util.Map;

public class PlayerStatistics {

    private String name;
    private Map<String, String> stats;

    public PlayerStatistics(String name, Map<String, String> stats) {
        this.name = name;
        this.stats = stats;
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

    @Override
    public String toString() {
        return "PlayerStatistics(name=" + this.name + ";stats=" + this.stats + ")";
    }
}
