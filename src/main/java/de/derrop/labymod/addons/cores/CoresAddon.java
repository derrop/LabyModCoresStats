package de.derrop.labymod.addons.cores;
/*
 * Created by derrop on 22.09.2019
 */

import com.mojang.authlib.GameProfile;
import de.derrop.labymod.addons.cores.clan.ClanDetector;
import de.derrop.labymod.addons.cores.listener.CommandListener;
import de.derrop.labymod.addons.cores.listener.PlayerLoginLogoutListener;
import de.derrop.labymod.addons.cores.listener.PlayerStatsListener;
import de.derrop.labymod.addons.cores.listener.PlayerStatsLoginListener;
import de.derrop.labymod.addons.cores.module.BestPlayerModule;
import de.derrop.labymod.addons.cores.module.WorstPlayerModule;
import de.derrop.labymod.addons.cores.party.PartyDetector;
import de.derrop.labymod.addons.cores.server.ServerDetector;
import de.derrop.labymod.addons.cores.statistics.PlayerStatistics;
import de.derrop.labymod.addons.cores.statistics.StatsParser;
import net.labymod.api.LabyModAddon;
import net.labymod.api.events.PluginMessageEvent;
import net.labymod.core.LabyModCore;
import net.labymod.ingamegui.ModuleCategory;
import net.labymod.ingamegui.ModuleCategoryRegistry;
import net.labymod.settings.elements.ControlElement;
import net.labymod.settings.elements.SettingsElement;
import net.labymod.utils.Material;

import java.util.*;
import java.util.concurrent.*;

public class CoresAddon extends LabyModAddon {

    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(3);

    private Map<UUID, GameProfile> onlinePlayers = new HashMap<>();

    private ServerDetector serverDetector = new ServerDetector(this);
    private StatsParser statsParser;
    private PartyDetector partyDetector = new PartyDetector();
    private ClanDetector clanDetector = new ClanDetector();
    private String currentServer;

    private ModuleCategory coresCategory;

    private final Random random = new Random();

    public StatsParser getStatsParser() {
        return this.statsParser;
    }

    public ClanDetector getClanDetector() {
        return clanDetector;
    }

    public PartyDetector getPartyDetector() {
        return partyDetector;
    }

    public ScheduledExecutorService getExecutorService() {
        return this.executorService;
    }

    public Map<UUID, GameProfile> getOnlinePlayers() {
        return onlinePlayers;
    }

    public String getCurrentServer() {
        return currentServer;
    }

    public ModuleCategory getCoresCategory() {
        return coresCategory;
    }

    @Override
    public void onEnable() {
        System.out.println("[CoresStats] Enabling addon...");

        ModuleCategoryRegistry.loadCategory(
                this.coresCategory = new ModuleCategory(
                        "Cores",
                        true,
                        new ControlElement.IconData(Material.BEACON)
                )
        );

        this.getApi().getEventManager().register(new PlayerStatsListener(this));
        this.getApi().getEventManager().register(new PlayerStatsLoginListener(this));
        this.getApi().getEventManager().register(new CommandListener(this));
        this.getApi().getEventManager().register(this.partyDetector);
        this.getApi().getEventManager().registerOnIncomingPacket(new PlayerLoginLogoutListener(this));

        this.getApi().registerModule(new BestPlayerModule(this));
        this.getApi().registerModule(new WorstPlayerModule(this));
        //todo sometimes players are not removed from the stats cache when a round ends and you don't leave the server fully (could be fixed by just clearing when Gomme sends an update with a server change for DiscordRPC)
        //should be fixed now with #handleServerSwitch(String), but not tested yet
        this.getApi().getEventManager().registerOnQuit(serverData -> {
            this.partyDetector.handleLeaveParty();
            this.statsParser.reset();
            this.clanDetector.clearCache();
            this.onlinePlayers.clear();
        });

        this.getApi().getEventManager().register(this.serverDetector);

        this.statsParser = new StatsParser(this.executorService);

        System.out.println("[CoresStats] Successfully enabled the addon!");
    }

    public void handleServerSwitch(String serverType) {
        System.out.println("registered server switch from " + this.currentServer + " to " + serverType);

        this.statsParser.reset();
        this.onlinePlayers.clear();
        this.currentServer = serverType;

        if (serverType.equals("CORES")) {
            this.executorService.schedule(() -> { //wait for the tablist packets to arrive
                for (GameProfile profile : this.onlinePlayers.values()) {
                    this.requestPlayerStatsAndWarn(profile.getName());
                }
            }, 500, TimeUnit.MILLISECONDS);
        }
    }

    public void requestPlayerStatsAndWarn(String name) {
        this.executorService.schedule(
                () -> {
                    try {
                        this.warnOnGoodStats(this.getStatsParser().requestStats(name).get(6, TimeUnit.SECONDS));
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                        e.printStackTrace();
                    }
                },
                this.random.nextInt(150) + 50, TimeUnit.MILLISECONDS
        ); //min: 50; max: 200
    }

    public void warnOnGoodStats(PlayerStatistics statistics) {
        System.out.println("PlayerStatistics for " + statistics.getName() + ": " + statistics.getStats());
        if (LabyModCore.getMinecraft().getPlayer().getName().equals(statistics.getName())) { //not warning for my good stats
            return;
        }

        if (this.partyDetector.getCurrentPartyMembers().contains(statistics.getName())) { //don't warn when the player is in my party
            System.out.println("Party contains " + statistics.getName() + ", not warning the player!");
            return;
        }
        String selfClan = this.clanDetector.getSelfClanShortcut();
        String otherClan = this.clanDetector.getClanShortcut(statistics.getName());
        if (selfClan != null && selfClan.equals(otherClan)) { //don't warn when the player is in my clan
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

    public PlayerStatistics getBestPlayer() {
        return this.statsParser.getCachedStats().values().stream()
                .filter(stats -> stats.getStats().containsKey("rank"))
                .min(Comparator.comparingDouble(value -> Integer.parseInt(value.getStats().get("rank"))))
                .orElse(null);
    }

    public PlayerStatistics getWorstPlayer() {
        return this.statsParser.getCachedStats().values().stream()
                .filter(stats -> stats.getStats().containsKey("rank"))
                .max(Comparator.comparingDouble(value -> Integer.parseInt(value.getStats().get("rank"))))
                .orElse(null);
    }

    @Override
    public void loadConfig() {
    }

    @Override
    protected void fillSettings(List<SettingsElement> subSettings) {
    }
}
