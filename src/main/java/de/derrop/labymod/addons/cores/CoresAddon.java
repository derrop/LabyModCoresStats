package de.derrop.labymod.addons.cores;
/*
 * Created by derrop on 22.09.2019
 */

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import de.derrop.labymod.addons.cores.detector.ScoreboardTagDetector;
import de.derrop.labymod.addons.cores.detector.ServerDetector;
import de.derrop.labymod.addons.cores.display.StatisticsDisplay;
import de.derrop.labymod.addons.cores.gametypes.GameType;
import de.derrop.labymod.addons.cores.listener.CommandListener;
import de.derrop.labymod.addons.cores.listener.PlayerLoginLogoutListener;
import de.derrop.labymod.addons.cores.listener.PlayerStatsListener;
import de.derrop.labymod.addons.cores.listener.PlayerStatsLoginListener;
import de.derrop.labymod.addons.cores.module.BestPlayerModule;
import de.derrop.labymod.addons.cores.module.WorstPlayerModule;
import de.derrop.labymod.addons.cores.statistics.PlayerStatistics;
import de.derrop.labymod.addons.cores.statistics.StatsParser;
import de.derrop.labymod.addons.cores.statistics.types.BedWarsStatistics;
import de.derrop.labymod.addons.cores.statistics.types.CoresStatistics;
import net.labymod.api.LabyModAddon;
import net.labymod.core.LabyModCore;
import net.labymod.ingamegui.ModuleCategory;
import net.labymod.ingamegui.ModuleCategoryRegistry;
import net.labymod.settings.elements.BooleanElement;
import net.labymod.settings.elements.ControlElement;
import net.labymod.settings.elements.SettingsElement;
import net.labymod.utils.Material;
import net.minecraft.client.Minecraft;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

public class CoresAddon extends LabyModAddon {

    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(3);
    private Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private Map<UUID, GameProfile> onlinePlayers = new HashMap<>();

    private ServerDetector serverDetector = new ServerDetector(this);
    private StatsParser statsParser;
    private ScoreboardTagDetector scoreboardTagDetector = new ScoreboardTagDetector();
    private String currentServer;
    private GameType currentServerType;

    private Map<String, GameType> supportedGameTypes = new HashMap<>();

    private boolean externalDisplayEnabled;

    private StatisticsDisplay display;

    private ModuleCategory coresCategory;

    private final Random random = new Random();

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
        return currentServer;
    }

    public GameType getCurrentServerType() {
        return currentServerType;
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
        return this.currentServerType != null && this.currentServerType.isEnabled();
    }

    public Map<String, GameType> getSupportedGameTypes() {
        return supportedGameTypes;
    }

    @Override
    public void onEnable() {
        System.out.println("[GommeStats] Enabling addon...");

        this.addSupportedGameType(new GameType("CORES", CoresStatistics::new, new ControlElement.IconData(Material.BEACON), true));
        this.addSupportedGameType(new GameType("BW", BedWarsStatistics::new, new ControlElement.IconData(Material.BED), false));

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
        this.getApi().getEventManager().registerOnIncomingPacket(new PlayerLoginLogoutListener(this));

        this.getApi().registerModule(new BestPlayerModule(this));
        this.getApi().registerModule(new WorstPlayerModule(this));
        this.getApi().getEventManager().registerOnQuit(serverData -> {
            this.statsParser.reset();
            this.scoreboardTagDetector.clearCache();
            this.onlinePlayers.clear();
            this.currentServer = null;
            this.display.setVisible(false);
        });

        this.getApi().getEventManager().register(this.serverDetector);

        this.statsParser = new StatsParser(this, this.executorService);

        System.out.println("[GommeStats] Successfully enabled the addon!");
    }

    public void handleServerSwitch(String serverType) {
        System.out.println("registered server switch from " + this.currentServer + " to " + serverType);

        this.statsParser.reset();
        this.currentServer = serverType;

        if (this.isGameTypeSupported(serverType)) {
            this.currentServerType = this.getSupportedGameType(serverType);
            this.executorService.schedule(() -> { //wait for the tablist packets to arrive
                for (GameProfile profile : this.onlinePlayers.values()) {
                    this.requestPlayerStatsAndWarn(profile.getName());
                }
            }, 500, TimeUnit.MILLISECONDS);
            if (this.externalDisplayEnabled) {
                this.display.setVisible(true);
            }
        } else {
            this.currentServerType = null;
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
                this.random.nextInt(150) + 50, TimeUnit.MILLISECONDS
        ); //min: 50; max: 200
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

        statistics.warnOnGoodStats(LabyModCore.getMinecraft()::displayMessageInChat, () -> {
            for (int i = 0; i < 5; i++) {
                Minecraft.getMinecraft().thePlayer.playSound("note.pling", 1000F, 100F); //https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/mapping-and-modding-tutorials/2213619-1-8-all-playsound-sound-arguments
            }
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
    }

    @Override
    protected void fillSettings(List<SettingsElement> subSettings) {
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
