package de.derrop.labymod.addons.cores;
/*
 * Created by derrop on 22.09.2019
 */

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.derrop.labymod.addons.cores.config.MainConfig;
import de.derrop.labymod.addons.cores.detector.MatchDetector;
import de.derrop.labymod.addons.cores.detector.PlayerLoginLogoutDetector;
import de.derrop.labymod.addons.cores.detector.ServerDetector;
import de.derrop.labymod.addons.cores.display.StatisticsDisplay;
import de.derrop.labymod.addons.cores.gametypes.BedWarsGameType;
import de.derrop.labymod.addons.cores.gametypes.CoresGameType;
import de.derrop.labymod.addons.cores.gametypes.GameType;
import de.derrop.labymod.addons.cores.listener.CommandListener;
import de.derrop.labymod.addons.cores.module.BestPlayerModule;
import de.derrop.labymod.addons.cores.module.TimerModule;
import de.derrop.labymod.addons.cores.module.WorstPlayerModule;
import de.derrop.labymod.addons.cores.network.sync.SyncClient;
import de.derrop.labymod.addons.cores.network.sync.handler.TagHandler;
import de.derrop.labymod.addons.cores.player.OnlinePlayer;
import de.derrop.labymod.addons.cores.player.PlayerDataProviders;
import de.derrop.labymod.addons.cores.player.PlayerProvider;
import de.derrop.labymod.addons.cores.statistics.PlayerStatistics;
import de.derrop.labymod.addons.cores.tag.Tag;
import de.derrop.labymod.addons.cores.tag.TagProvider;
import de.derrop.labymod.addons.cores.tag.TagType;
import net.labymod.api.LabyModAddon;
import net.labymod.core.LabyModCore;
import net.labymod.ingamegui.ModuleCategory;
import net.labymod.ingamegui.ModuleCategoryRegistry;
import net.labymod.main.LabyMod;
import net.labymod.settings.elements.ControlElement;
import net.labymod.settings.elements.SettingsElement;
import net.labymod.utils.Material;
import net.labymod.utils.ServerData;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CoresAddon extends LabyModAddon {

    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Random random = new Random();

    private MainConfig config = new MainConfig();

    private ServerDetector serverDetector = new ServerDetector(this);
    private MatchDetector matchDetector = new MatchDetector(this);
    private PlayerLoginLogoutDetector playerLoginLogoutDetector;
    private PlayerDataProviders playerDataProviders;

    private Map<String, GameType> supportedGameTypes = new HashMap<>();

    private StatisticsDisplay display;

    private long lastRoundBeginTimestamp = -1;

    private ModuleCategory coresCategory;

    private SyncClient syncClient = new SyncClient();

    public ExecutorService getExecutorService() {
        return this.executorService;
    }

    public PlayerProvider getPlayerProvider() {
        return this.playerDataProviders.getPlayerProvider();
    }

    public TagProvider getTagProvider() {
        return this.playerDataProviders.getTagProvider();
    }

    public Collection<String> getPlayersWithPrefix(Predicate<String> prefixTester) {
        return this.playerDataProviders.getScoreboardTagDetector().getPlayersWithPrefix(prefixTester);
    }

    public String getCurrentServer() {
        return this.serverDetector.getCurrentServer();
    }

    public String getCurrentServerId() {
        return this.serverDetector.getCurrentServerId();
    }

    public GameType getCurrentServerType() {
        return this.serverDetector.getCurrentServerType();
    }

    public long getLastRoundBeginTimestamp() {
        return lastRoundBeginTimestamp;
    }

    public void setLastRoundBeginTimestamp(long lastRoundBeginTimestamp) {
        this.lastRoundBeginTimestamp = lastRoundBeginTimestamp;
    }

    public SyncClient getSyncClient() {
        return syncClient;
    }

    public MatchDetector getMatchDetector() {
        return matchDetector;
    }

    public ModuleCategory getCoresCategory() {
        return coresCategory;
    }

    public Gson getGson() {
        return gson;
    }

    public StatisticsDisplay getDisplay() {
        return display;
    }

    public void addSupportedGameType(GameType gameType) {
        this.supportedGameTypes.put(gameType.getName(), gameType);
    }

    public GameType getSupportedGameType(String name) {
        return this.supportedGameTypes.get(name);
    }

    public boolean isGameTypeSupported(String name) {
        return this.supportedGameTypes.containsKey(name) && this.supportedGameTypes.get(name).isEnabled();
    }

    public boolean isCurrentServerTypeSupported() {
        return this.serverDetector.getCurrentServerType() != null && this.serverDetector.getCurrentServerType().isEnabled();
    }

    public MainConfig getMainConfig() {
        return this.config;
    }

    public Map<String, GameType> getSupportedGameTypes() {
        return supportedGameTypes;
    }

    @Override
    public void onEnable() {
        System.out.println("[GommeStats] Enabling addon...");

        this.addSupportedGameType(new CoresGameType(new ControlElement.IconData(Material.BEACON), true));
        this.addSupportedGameType(new BedWarsGameType(new ControlElement.IconData(Material.BED), false));

        ModuleCategoryRegistry.loadCategory(
                this.coresCategory = new ModuleCategory(
                        "Stats",
                        true,
                        new ControlElement.IconData(Material.BEACON)
                )
        );

        this.display = new StatisticsDisplay(this);

        this.getApi().getEventManager().register(new CommandListener(this));
        this.getApi().getEventManager().register(this.matchDetector);
        this.getApi().getEventManager().register(this.serverDetector);

        this.getApi().registerModule(new BestPlayerModule(this));
        this.getApi().registerModule(new WorstPlayerModule(this));
        this.getApi().registerModule(new TimerModule(this));

        this.getApi().getEventManager().registerOnQuit(serverData -> {
            this.playerDataProviders.getStatsParser().reset();
            this.getPlayerProvider().getOnlinePlayers().clear();

            this.serverDetector.reset();
            this.display.setVisible(false);

            if (this.matchDetector.isInMatch()) {
                this.matchDetector.handleMatchEnd(null);
            }
        });

        this.playerDataProviders = new PlayerDataProviders(this);

        this.playerLoginLogoutDetector = new PlayerLoginLogoutDetector(this, this.playerDataProviders);

        this.syncClient.registerHandler((short) 1, new TagHandler(this));

        System.out.println("[GommeStats] Successfully enabled the addon!");
    }

    @Override
    public void onDisable() {
        if (this.syncClient != null && this.syncClient.isConnected()) {
            this.syncClient.close();
        }
    }

    public void connectToSyncServer() {
        if (this.syncClient.connect(
                new InetSocketAddress("internal.gomme.derrop.gq", 1510),
                this.config.authToken,
                error -> LabyMod.getInstance().notifyMessageRaw("Stats", "§c" + error))) {
            LabyMod.getInstance().notifyMessageRaw("Stats", "§aSuccessfully connected");
        }
    }

    public void handleServerSwitch(String serverType, String serverId) {
        this.playerDataProviders.getStatsParser().reset();
        this.lastRoundBeginTimestamp = -1;
        if (this.matchDetector.isInMatch()) {
            this.matchDetector.handleMatchEnd(null);
        }

        this.playerLoginLogoutDetector.reset();

        if (this.isCurrentServerTypeSupported()) {
            this.executorService.execute(() -> { //wait for the tablist packets to arrive
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                this.playerLoginLogoutDetector.handleServerSwitch();
            });

            if (this.config.externalDisplayEnabled) {
                this.display.setVisible(true);
            }
        } else {
            this.display.setVisible(false);
        }
    }

    public void timedWarnOnGoodStats(OnlinePlayer player) {
        this.executorService.execute(() -> {
            try {
                Thread.sleep(this.random.nextInt(250) + 50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            this.warnOnGoodStats(player);
        });
    }

    public void warnForTags(OnlinePlayer player) {
        if (!player.isInParty() && player.getScoreboardTag() != null) {
            this.getTagProvider().listTags(TagType.CLAN, player.getScoreboardTag().substring(2) /* Remove the color code at the beginning */).thenAccept(tags -> {
                if (tags != null && !tags.isEmpty()) {
                    LabyModCore.getMinecraft().displayMessageInChat("§4WARNUNG: §7Clan §e" + player.getScoreboardTag() + " §7hat die Tags §e" + tags.stream().map(Tag::getTag).collect(Collectors.joining(", ")));
                }
            });
        }
        player.loadTags().thenAccept(tags -> {
            if (tags != null && !tags.isEmpty()) {
                LabyModCore.getMinecraft().displayMessageInChat("§4WARNUNG: §7Spieler §e" + player.getName() + " §7hat die Tags §e" + tags.stream().map(Tag::getTag).collect(Collectors.joining(", ")));
            }
        });
    }

    private void warnOnGoodStats(OnlinePlayer player) {
        if (player.getLastStatistics() == null) {
            return;
        }
        PlayerStatistics statistics = player.getLastStatistics();
        System.out.println("PlayerStatistics for " + statistics.getName() + ": " + statistics.getStats());
        if (LabyModCore.getMinecraft().getPlayer().getName().equals(statistics.getName())) { //not warning for my good stats
            return;
        }

        String selfTag = this.playerDataProviders.getScoreboardTagDetector().detectScoreboardTag(LabyMod.getInstance().getPlayerName());
        String otherTag = player.getScoreboardTag();
        if (selfTag != null && selfTag.equals(otherTag)) { //don't warn when the player is in my clan/party
            System.out.println("Clan/Party contains " + statistics.getName() + ", not warning the player!");
            return;
        }

        statistics.warnOnGoodStats(LabyModCore.getMinecraft()::displayMessageInChat, () -> {
            /*for (int i = 0; i < 5; i++) { todo this probably caused the ConcurrentModificationException
                Minecraft.getMinecraft().thePlayer.playSound("note.pling", 1000F, 100F); //https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/mapping-and-modding-tutorials/2213619-1-8-all-playsound-sound-arguments
            }*/
        });
    }

    public PlayerStatistics getBestPlayer() {
        return this.getPlayerProvider().getOnlinePlayers().stream()
                .map(OnlinePlayer::getLastStatistics)
                .filter(Objects::nonNull)
                .filter(stats -> stats.hasRank() && stats.getRank() > 0)
                .min(Comparator.comparingInt(PlayerStatistics::getRank))
                .orElse(null);
    }

    public PlayerStatistics getWorstPlayer() {
        return this.getPlayerProvider().getOnlinePlayers().stream()
                .map(OnlinePlayer::getLastStatistics)
                .filter(Objects::nonNull)
                .filter(stats -> stats.hasRank() && stats.getRank() > 0)
                .max(Comparator.comparingInt(PlayerStatistics::getRank))
                .orElse(null);
    }

    //todo #3 (maybe) sync stats between clients with a server to not reach the request limit so fast
    //todo #4 icon for the addon (addon.json)
    //todo #5 when not in party, automatically join the team with the best stats (can be enabled/disabled)

    //todo bug: winners contain the nick if a player is nicked

    public boolean isConnectedWithGommeServer() {
        ServerData currentServerData = this.getApi().getCurrentServer();
        return currentServerData != null &&
                (LabyMod.getInstance().getCurrentServerData().getIp().toLowerCase().contains("gommehd.net") ||
                        LabyMod.getInstance().getCurrentServerData().getIp().toLowerCase().contains("mc.gommehd.com"));
    }

    public Stream<OnlinePlayer> sortStreamByStats(Stream<OnlinePlayer> stream) {
        return stream
                .filter(onlinePlayer -> onlinePlayer.getLastStatistics() != null && onlinePlayer.getLastStatistics().hasRank())
                .sorted(Comparator.comparingInt(value -> value.getLastStatistics().getRank()));
    }

    @Override
    public void loadConfig() {
        this.config.loadConfig(getConfig(), this);
    }

    @Override
    protected void fillSettings(List<SettingsElement> subSettings) {
        this.config.fillSettings(subSettings, this);
    }
}
