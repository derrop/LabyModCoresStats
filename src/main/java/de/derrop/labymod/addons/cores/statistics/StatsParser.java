package de.derrop.labymod.addons.cores.statistics;
/*
 * Created by derrop on 22.09.2019
 */

import de.derrop.labymod.addons.cores.CoresAddon;
import net.labymod.core.LabyModCore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StatsParser {

    private static final Pattern BEGIN_STATS_PATTERN = Pattern.compile("-= Statistiken von (.*) \\(30 Tage\\) =-");

    private static final Map<Pattern, String> STATS_ENTRIES = new HashMap<>();

    static {
        STATS_ENTRIES.put(Pattern.compile(" Position im Ranking: (.*)"), "rank");
        STATS_ENTRIES.put(Pattern.compile(" Kills: (.*)"), "kills");
        STATS_ENTRIES.put(Pattern.compile(" Deaths: (.*)"), "deaths");
        STATS_ENTRIES.put(Pattern.compile(" K/D: (.*)"), "kd");
        STATS_ENTRIES.put(Pattern.compile(" Zerstörte Cores: (.*)"), "destroyedCores");
        STATS_ENTRIES.put(Pattern.compile(" Gespielte Spiele: (.*)"), "playedGames");
        STATS_ENTRIES.put(Pattern.compile(" Gewonnene Spiele: (.*)"), "wonGames");
        STATS_ENTRIES.put(Pattern.compile(" Siegwahrscheinlichkeit: (.*) Prozent"), "winRate");
    }


    private Map<String, PlayerStatistics> readStatistics = new HashMap<>();
    private Map<String, CompletableFuture<PlayerStatistics>> statsRequests = new HashMap<>();
    private long lastBlock = -1;

    private BlockingQueue<String> requestQueue = new LinkedBlockingQueue<>();

    private PlayerStatistics readingStats;

    private CoresAddon coresAddon;

    public StatsParser(CoresAddon coresAddon, ExecutorService executorService) {
        this.coresAddon = coresAddon;
        executorService.execute(() -> {
            while (!Thread.interrupted()) {
                try {
                    if (LabyModCore.getMinecraft().getPlayer() != null) {
                        LabyModCore.getMinecraft().getPlayer().sendChatMessage(this.requestQueue.take());
                        Thread.sleep(300);
                    } else { //not connected to any server
                        if (!this.requestQueue.isEmpty()) {
                            this.requestQueue.clear();
                        }
                        Thread.sleep(5000);
                    }
                } catch (InterruptedException exception) {
                    exception.printStackTrace();
                }
            }
        });
    }

    private String getStatsPLayerName(String msg) {
        Matcher matcher = BEGIN_STATS_PATTERN.matcher(msg);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private boolean isStatsEnd(String msg) {
        return msg.equals("------------------------------");
    }

    private void handleStats(String msg) {
        for (Map.Entry<Pattern, String> entry : STATS_ENTRIES.entrySet()) {
            Matcher matcher = entry.getKey().matcher(msg);
            if (matcher.find()) {
                this.readingStats.getStats().put(entry.getValue(), matcher.group(1).replace(",", "")); //Gomme uses "," to split numbers (e.g. 1,000,000)
                break;
            }
        }
    }

    /**
     * Gets all read statistics in the current session
     *
     * @return
     */
    public Map<String, PlayerStatistics> getCachedStats() {
        return readStatistics;
    }

    public Map<String, CompletableFuture<PlayerStatistics>> getStatsRequests() {
        return statsRequests;
    }

    /**
     * Gets the currently reading stats from the server
     *
     * @return
     */
    public PlayerStatistics getReadingStats() {
        return readingStats;
    }

    public void setReadingStats(PlayerStatistics readingStats) {
        this.readingStats = readingStats;
    }

    /**
     * Called on chat message from the server
     *
     * @param msg the message including color codes
     * @return
     */
    public StatsParseResult handleChatMessage(String msg) {
        if (this.coresAddon.getCurrentServer() == null || !this.coresAddon.getCurrentServer().equals("CORES")) {
            return StatsParseResult.NONE;
        }
        if (msg.equals("Du hast zu viele Statistiken abgerufen, bitte versuche es in einer anderen Runde erneut.")) { //Gomme hates us :peepoCry:
            for (CompletableFuture<PlayerStatistics> value : this.statsRequests.values()) {
                value.complete(null);
            }
            this.lastBlock = System.currentTimeMillis();
            return StatsParseResult.NONE;
        }
        if (this.readingStats != null) {
            if (this.isStatsEnd(msg)) {
                this.readStatistics.put(this.readingStats.getName(), this.readingStats);
                return StatsParseResult.END;
            }
            this.handleStats(msg);
            return StatsParseResult.ENTRY;
        }
        String name = this.getStatsPLayerName(msg);
        if (name != null) {
            this.readingStats = new PlayerStatistics(name, new HashMap<>());
            return StatsParseResult.BEGIN;
        }
        return StatsParseResult.NONE;
    }

    /**
     * Removes the statistics by the name out of the stats cache
     *
     * @param name the name of the player
     */
    public void removeFromCache(String name) {
        this.readStatistics.remove(name);
    }

    /**
     * Clears the cache of statistics and removes the last blocked request
     */
    public void reset() {
        this.readStatistics.clear();
        this.lastBlock = -1;
    }

    /**
     * Gets the stats of the given player from the cache if they exist
     *
     * @param name the name of the player
     * @return the {@link PlayerStatistics} object or null, if the player is not cached
     */
    public PlayerStatistics getCached(String name) {
        return this.readStatistics.get(name);
    }

    /**
     * Requests the stats of the given player from the server and caches them
     *
     * @param name the name of the player
     * @return the future with the {@link PlayerStatistics} object
     */
    public CompletableFuture<PlayerStatistics> requestStats(String name) {
        if (this.statsRequests.containsKey(name))
            return this.statsRequests.get(name);

        if (this.lastBlock != -1 && System.currentTimeMillis() - this.lastBlock <= 20000) { //after the last blocked request, we wait 20 seconds to not get a blocked request directly after the other
            //but it also seems like if you get a blocked request (after something around 10 - 15 requests), that you're not getting unblocked on this server until it restarts
            //the requests have to be from different players, you could request the stats of a player as often as you want
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<PlayerStatistics> future = new CompletableFuture<>();
        this.statsRequests.put(name, future);
        this.requestQueue.offer("/stats " + name);
        return future;
    }

}
