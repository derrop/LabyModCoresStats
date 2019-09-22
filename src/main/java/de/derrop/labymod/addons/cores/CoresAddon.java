package de.derrop.labymod.addons.cores;
/*
 * Created by derrop on 22.09.2019
 */

import de.derrop.labymod.addons.cores.clan.ClanDetector;
import de.derrop.labymod.addons.cores.listener.PlayerLoginLogoutListener;
import de.derrop.labymod.addons.cores.listener.PlayerStatsListener;
import de.derrop.labymod.addons.cores.party.PartyDetector;
import de.derrop.labymod.addons.cores.statistics.PlayerStatistics;
import de.derrop.labymod.addons.cores.statistics.StatsParser;
import net.labymod.api.LabyModAddon;
import net.labymod.core.LabyModCore;
import net.labymod.settings.elements.SettingsElement;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CoresAddon extends LabyModAddon {

    private StatsParser statsParser = new StatsParser();
    private PartyDetector partyDetector = new PartyDetector();
    private ClanDetector clanDetector = new ClanDetector();
    private ExecutorService executorService = Executors.newCachedThreadPool();

    public StatsParser getStatsParser() {
        return this.statsParser;
    }

    public ExecutorService getExecutorService() {
        return this.executorService;
    }

    @Override
    public void onEnable() {
        System.out.println("[CoresStats] Enabling addon...");
        this.getApi().getEventManager().register(new PlayerStatsListener(this));
        this.getApi().getEventManager().register(new PlayerLoginLogoutListener(this));
        this.getApi().getEventManager().register(this.partyDetector);
        this.getApi().getEventManager().registerOnJoin(serverData -> {
            //todo check for all online players stats and warn
        });
        this.getApi().getEventManager().registerOnQuit(serverData -> {
            this.partyDetector.handleLeaveParty();
            this.clanDetector.clearCache();
        });
        System.out.println("[CoresStats] Successfully enabled the addon!");
    }

    public void warnOnGoodStats(PlayerStatistics statistics) {
        if (LabyModCore.getMinecraft().getPlayer().getName().equals(statistics.getName())) { //not warning for my good stats
            return;
        }

        if (this.partyDetector.getCurrentPartyMembers().contains(statistics.getName())) { //don't warn when the player is in my party todo not tested if this works
            System.out.println("Party contains " + statistics.getName() + ", not warning the player!");
            return;
        }
        String selfClan = this.clanDetector.getSelfClanShortcut();
        String otherClan = this.clanDetector.getClanShortcut(statistics.getName());
        if (selfClan != null && selfClan.equals(otherClan)) { //don't warn when the player is in my clan todo not tested if this works
            System.out.println("Clan contains " + statistics.getName() + ", not warning the player!");
            return;
        }

        Map<String, String> stats = statistics.getStats();
        int kills = 0;
        int deaths = 0;
        double kd = 0;
        int rank = 0;
        double winRate = 0;
        int playedGames = 0;
        int wonGames = 0;
        if (stats.containsKey("kills")) {
            kills = Integer.parseInt(stats.get("kills"));
        }
        if (stats.containsKey("deaths")) {
            deaths = Integer.parseInt(stats.get("deaths"));
        }
        if (stats.containsKey("kd")) {
            kd = Double.parseDouble(stats.get("kd"));
        }
        if (stats.containsKey("rank")) {
            rank = Integer.parseInt(stats.get("rank"));
        }
        if (stats.containsKey("winRate")) {
            winRate = Double.parseDouble(stats.get("winRate"));
        }
        if (stats.containsKey("playedGames")) {
            playedGames = Integer.parseInt(stats.get("playedGames"));
        }
        if (stats.containsKey("wonGames")) {
            wonGames = Integer.parseInt(stats.get("wonGames"));
        }
        if (rank > 0 && rank <= 300) {
            LabyModCore.getMinecraft().displayMessageInChat("§4WARNUNG: §7Spieler §e" + statistics.getName() + " §7ist Platz §e" + rank);
        }
        if (winRate >= 75 && playedGames >= 30) {
            LabyModCore.getMinecraft().displayMessageInChat("§4WARNUNG: §7Spieler §e" + statistics.getName() + " §7hat eine Siegwahrscheinlichkeit von §e" + winRate + " %" +
                    " §8(Gespielt: §e" + playedGames + "§8; Gewonnen: §e" + wonGames + "§8)");
        }
    }

    @Override
    public void loadConfig() {
    }

    @Override
    protected void fillSettings(List<SettingsElement> subSettings) {
    }
}
