package de.derrop.labymod.addons.cores.statistics;
/*
 * Created by derrop on 28.09.2019
 */

public class StatisticsType<T extends PlayerStatistics> {

    public T map(PlayerStatistics statistics) {
        return (T) statistics;
    }

}
