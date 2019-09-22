package de.derrop.labymod.addons.cores.listener;
/*
 * Created by derrop on 22.09.2019
 */

import de.derrop.labymod.addons.cores.CoresAddon;
import de.derrop.labymod.addons.cores.statistics.PlayerStatistics;
import de.derrop.labymod.addons.cores.statistics.StatsParseResult;
import net.labymod.api.events.MessageReceiveEvent;

import java.util.concurrent.CompletableFuture;

public class PlayerStatsListener implements MessageReceiveEvent {

    private CoresAddon coresAddon;

    public PlayerStatsListener(CoresAddon coresAddon) {
        this.coresAddon = coresAddon;
    }

    @Override
    public boolean onReceive(String coloredMsg, String msg) {
        StatsParseResult result = this.coresAddon.getStatsParser().handleChatMessage(msg);
        if (result == StatsParseResult.END) {
            PlayerStatistics stats = this.coresAddon.getStatsParser().getReadingStats();
            this.coresAddon.getStatsParser().setReadingStats(null);
            CompletableFuture<PlayerStatistics> future = this.coresAddon.getStatsParser().getStatsRequests().get(stats.getName());
            if (future != null) {
                this.coresAddon.getStatsParser().getStatsRequests().remove(stats.getName());
                future.complete(stats);
                return true;
            }
        } else if (result == StatsParseResult.BEGIN || result == StatsParseResult.ENTRY) {
            String name = this.coresAddon.getStatsParser().getReadingStats().getName();
            if (this.coresAddon.getStatsParser().getStatsRequests().containsKey(name)) {
                return true;
            }
        }
        return false;
    }
}
