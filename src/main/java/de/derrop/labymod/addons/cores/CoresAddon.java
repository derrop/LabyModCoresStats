package de.derrop.labymod.addons.cores;
/*
 * Created by derrop on 22.09.2019
 */

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import de.derrop.labymod.addons.cores.detector.MatchDetector;
import de.derrop.labymod.addons.cores.detector.ScoreboardTagDetector;
import de.derrop.labymod.addons.cores.detector.ServerDetector;
import de.derrop.labymod.addons.cores.display.StatisticsDisplay;
import de.derrop.labymod.addons.cores.gametypes.BedWarsGameType;
import de.derrop.labymod.addons.cores.gametypes.CoresGameType;
import de.derrop.labymod.addons.cores.gametypes.GameType;
import de.derrop.labymod.addons.cores.listener.CommandListener;
import de.derrop.labymod.addons.cores.listener.PlayerLoginLogoutListener;
import de.derrop.labymod.addons.cores.listener.PlayerStatsListener;
import de.derrop.labymod.addons.cores.listener.PlayerStatsLoginListener;
import de.derrop.labymod.addons.cores.module.BestPlayerModule;
import de.derrop.labymod.addons.cores.module.TimerModule;
import de.derrop.labymod.addons.cores.module.WorstPlayerModule;
import de.derrop.labymod.addons.cores.statistics.PlayerStatistics;
import de.derrop.labymod.addons.cores.statistics.StatsParser;
import de.derrop.labymod.addons.cores.network.sync.SyncClient;
import de.derrop.labymod.addons.cores.tag.TagProvider;
import de.derrop.labymod.addons.cores.tag.TagType;
import net.labymod.api.LabyModAddon;
import net.labymod.core.LabyModCore;
import net.labymod.ingamegui.ModuleCategory;
import net.labymod.ingamegui.ModuleCategoryRegistry;
import net.labymod.main.LabyMod;
import net.labymod.settings.elements.BooleanElement;
import net.labymod.settings.elements.ControlElement;
import net.labymod.settings.elements.SettingsElement;
import net.labymod.settings.elements.StringElement;
import net.labymod.utils.Material;

import javax.swing.*;
import java.awt.*;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

public class CoresAddon extends LabyModAddon {

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(3);
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Random random = new Random();

    private Map<UUID, GameProfile> onlinePlayers = new HashMap<>();

    private ServerDetector serverDetector = new ServerDetector(this);
    private StatsParser statsParser;
    private ScoreboardTagDetector scoreboardTagDetector = new ScoreboardTagDetector();
    private MatchDetector matchDetector = new MatchDetector(this);

    private Map<String, GameType> supportedGameTypes = new HashMap<>();

    private boolean externalDisplayEnabled;
    private StatisticsDisplay display;

    private long lastRoundBeginTimestamp = -1;

    private ModuleCategory coresCategory;

    private SyncClient syncClient = new SyncClient();
    private TagProvider tagProvider = new TagProvider(this);

    private String authToken;

    public TagProvider getTagProvider() {
        return tagProvider;
    }

    public StatsParser getStatsParser() {
        return this.statsParser;
    }

    public ScoreboardTagDetector getScoreboardTagDetector() {
        return scoreboardTagDetector;
    }

    public ScheduledExecutorService getExecutorService() {
        return this.executorService;
    }

    public Map<UUID, GameProfile> getOnlinePlayers() {
        return onlinePlayers;
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

        this.getApi().getEventManager().register(new PlayerStatsListener(this));
        this.getApi().getEventManager().register(new PlayerStatsLoginListener(this));
        this.getApi().getEventManager().register(new CommandListener(this));
        this.getApi().getEventManager().register(this.matchDetector);
        this.getApi().getEventManager().registerOnIncomingPacket(new PlayerLoginLogoutListener(this));

        this.getApi().registerModule(new BestPlayerModule(this));
        this.getApi().registerModule(new WorstPlayerModule(this));
        this.getApi().registerModule(new TimerModule(this));

        this.getApi().getEventManager().registerOnQuit(serverData -> {
            this.statsParser.reset();
            this.scoreboardTagDetector.clearCache();
            this.onlinePlayers.clear();
            this.serverDetector.reset();
            this.display.setVisible(false);

            if (this.matchDetector.isInMatch()) {
                this.matchDetector.handleMatchEnd(null);
            }
        });

        this.getApi().getEventManager().register(this.serverDetector);

        this.statsParser = new StatsParser(this, this.executorService);

        System.out.println("[GommeStats] Successfully enabled the addon!");
    }

    @Override
    public void onDisable() {
        if (this.syncClient != null && this.syncClient.isConnected()) {
            this.syncClient.close();
        }
    }

    private void connectToSyncServer() {
        if (this.syncClient.connect(
                new InetSocketAddress("internal.gomme.derrop.gq", 1510),
                this.authToken,
                error -> LabyMod.getInstance().notifyMessageRaw("Stats", "§c" + error))) {
            LabyMod.getInstance().notifyMessageRaw("Stats", "§aSuccessfully connected");
        }
    }

    public void handleServerSwitch(String serverType, String serverId) {
        this.statsParser.reset();
        this.lastRoundBeginTimestamp = -1;
        if (this.matchDetector.isInMatch()) {
            this.matchDetector.handleMatchEnd(null);
        }

        if (this.isCurrentServerTypeSupported()) {
            this.executorService.schedule(() -> { //wait for the tablist packets to arrive
                for (GameProfile profile : this.onlinePlayers.values()) {
                    this.requestPlayerStatsAndWarn(profile.getName());
                }
            }, 500, TimeUnit.MILLISECONDS);
            if (this.externalDisplayEnabled) {
                this.display.setVisible(true);
            }
        } else {
            this.display.setVisible(false);
        }
    }

    public void requestPlayerStatsAndWarn(String name) {
        this.executorService.schedule(
                () -> {
                    try {
                        this.warnOnGoodStats(this.getStatsParser().requestStats(name).get(6, TimeUnit.SECONDS));
                    } catch (InterruptedException | ExecutionException | TimeoutException exception) {
                        exception.printStackTrace();
                    }
                },
                this.random.nextInt(250) + 50, TimeUnit.MILLISECONDS
        );
    }

    private void warnOnGoodStats(PlayerStatistics statistics) {
        if (statistics == null) {
            return;
        }
        System.out.println("PlayerStatistics for " + statistics.getName() + ": " + statistics.getStats());
        if (LabyModCore.getMinecraft().getPlayer().getName().equals(statistics.getName())) { //not warning for my good stats
            return;
        }

        String selfTag = this.scoreboardTagDetector.getSelfScoreboardTag();
        String otherTag = this.scoreboardTagDetector.getScoreboardTag(statistics.getName());
        if (selfTag != null && selfTag.equals(otherTag)) { //don't warn when the player is in my clan/party
            System.out.println("Clan/Party contains " + statistics.getName() + ", not warning the player!");
            return;
        }

        if (!this.scoreboardTagDetector.isParty(otherTag)) {
            this.tagProvider.listTags(TagType.CLAN, otherTag.substring(2) /* Remove the color code at the beginning */).thenAccept(tags -> {
                if (tags != null && !tags.isEmpty()) {
                    LabyModCore.getMinecraft().displayMessageInChat("§4WARNUNG: §7Clan §e" + otherTag + " §7hat die Tags §e" + String.join(", ", tags));
                }
            });
        }
        this.tagProvider.listTags(TagType.PLAYER, statistics.getName()).thenAccept(tags -> {
            if (tags != null && !tags.isEmpty()) {
                LabyModCore.getMinecraft().displayMessageInChat("§4WARNUNG: §7Spieler §e" + statistics.getName() + " §7hat die Tags §e" + String.join(", ", tags));
            }
        });

        statistics.warnOnGoodStats(LabyModCore.getMinecraft()::displayMessageInChat, () -> {
            /*for (int i = 0; i < 5; i++) { todo this probably caused the ConcurrentModificationException
                Minecraft.getMinecraft().thePlayer.playSound("note.pling", 1000F, 100F); //https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/mapping-and-modding-tutorials/2213619-1-8-all-playsound-sound-arguments
            }*/
        });
    }

    public PlayerStatistics getBestPlayer() {
        return this.statsParser.getCachedStats().values().stream()
                .filter(stats -> stats.hasRank() && stats.getRank() > 0)
                .min(Comparator.comparingInt(PlayerStatistics::getRank))
                .orElse(null);
    }

    //todo #1 some players are sometimes not recognized when they join into a cores round (pretty rare now, if not fixed)
    //      (maybe this is because of the TimeOut Exceptions in the console? I think, that the stats command is sent but the result not parsed correctly)
    //      if you then execute /stats, you will not get any chat messages but the player will be added to the list of statistics
    //      if you execute /stats a second time, you will get your chat messages normally
    //todo #2 higher delay because sometimes we get kicked with "disconnect.spam"
    //todo #3 (maybe) sync stats between clients with a server to not reach the request limit so fast
    //todo #4 icon for the addon (addon.json)
    //todo #5 when not in party, automatically join the team with the best stats (can be enabled/disabled)

    //todo bug: winners contain the nick if a player is nicked
    //todo bug: players are recognized as "disconnected" when they die because they are removed from the tab list after their death and directly added after it in BEDWARS

    public PlayerStatistics getWorstPlayer() {
        return this.statsParser.getCachedStats().values().stream()
                .filter(stats -> stats.hasRank() && stats.getRank() > 0)
                .max(Comparator.comparingInt(PlayerStatistics::getRank))
                .orElse(null);
    }

    public Stream<PlayerStatistics> sortStatsStream(Stream<PlayerStatistics> stream) {
        return stream
                .filter(PlayerStatistics::hasRank)
                .sorted(Comparator.comparingInt(PlayerStatistics::getRank));
    }

    @Override
    public void loadConfig() {
        this.externalDisplayEnabled = getConfig().has("externalDisplayEnabled") && getConfig().get("externalDisplayEnabled").getAsBoolean();
        if (getConfig().has("externalDisplay")) {
            Rectangle rectangle = this.gson.fromJson(
                    getConfig().get("externalDisplay"),
                    Rectangle.class
            );
            int extendedState = getConfig().has("externalDisplayExtendedState") ?
                    getConfig().get("externalDisplayExtendedState").getAsInt() :
                    JFrame.NORMAL;
            if (rectangle != null) {
                this.display.setBounds(rectangle);
                if (this.display.isVisible()) {
                    this.display.repaint();
                }
                this.display.setExtendedState(extendedState);
            }
        }
        for (GameType gameType : this.supportedGameTypes.values()) {
            if (getConfig().has(gameType.getName() + "Enabled")) {
                gameType.setEnabled(getConfig().get(gameType.getName() + "Enabled").getAsBoolean());
            } else {
                gameType.setEnabled(gameType.isDefaultEnabled());
            }
        }
        this.authToken = getConfig().has("token") ? getConfig().get("token").getAsString() : null;

        if (this.authToken != null && !this.authToken.isEmpty()) {
            this.connectToSyncServer();
        }
    }

    @Override
    protected void fillSettings(List<SettingsElement> subSettings) {
        subSettings.add(
                new StringElement("Token", this, new ControlElement.IconData(Material.DIAMOND_SWORD), "token", this.authToken)
                .addCallback(token -> {
                    this.authToken = token;
                    if (token != null && !token.isEmpty()) {
                        if (this.syncClient.isConnected()) {
                            this.syncClient.close();
                        }
                        this.connectToSyncServer();
                    } else if (this.syncClient.isConnected()) {
                        this.syncClient.close();
                    }
                })
        );

        subSettings.add(
                new BooleanElement("External Display", this, new ControlElement.IconData(Material.SIGN), "externalDisplayEnabled", false)
                        .addCallback(externalDisplay -> {
                            this.externalDisplayEnabled = externalDisplay;
                            if (!this.externalDisplayEnabled) {
                                this.display.setVisible(false);
                            } else if (this.isCurrentServerTypeSupported()) {
                                this.display.setVisible(true);
                            }
                        })
        );
        for (GameType gameType : this.supportedGameTypes.values()) {
            subSettings.add(
                    new BooleanElement(gameType.getName() + " Stats", this, gameType.getIconData(), gameType.getName() + "Enabled", gameType.isDefaultEnabled())
                            .addCallback(gameType::setEnabled)
            );
        }
    }
}
