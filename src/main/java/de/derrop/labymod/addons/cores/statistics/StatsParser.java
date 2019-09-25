package de.derrop.labymod.addons.cores.statistics;
/*
 * Created by derrop on 22.09.2019
 */

import de.derrop.labymod.addons.cores.CoresAddon;
import de.derrop.labymod.addons.cores.regex.Patterns;
import net.labymod.core.LabyModCore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StatsParser {

    private Map<String, PlayerStatistics> readStatistics = new ConcurrentHashMap<>();
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
                    String name = this.requestQueue.take();
                    if (LabyModCore.getMinecraft().getPlayer() != null) {
                        LabyModCore.getMinecraft().getPlayer().sendChatMessage("/stats " + name);
                        Thread.sleep(300);
                    } else { //not connected to any server
                        CompletableFuture<PlayerStatistics> future = this.statsRequests.get(name);
                        if (future != null) {
                            future.complete(null);
                        }
                        while (!this.requestQueue.isEmpty()) {
                            future = this.statsRequests.remove(this.requestQueue.poll());
                            if (future != null) {
                                future.complete(null);
                            }
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
        Matcher matcher = Patterns.BEGIN_STATS_PATTERN.matcher(msg);
        if (matcher.find()) {
            return Patterns.matcherGroup(matcher);
        }
        return null;
    }

    private boolean isStatsEnd(String msg) {
        return msg.equals("------------------------------");
    }

    private void handleStats(String msg) {
        for (Map.Entry<Pattern, String> entry : Patterns.STATS_ENTRIES.entrySet()) {
            Matcher matcher = entry.getKey().matcher(msg);
            if (matcher.find()) {
                this.readingStats.getStats().put(entry.getValue(), Patterns.matcherGroup(matcher).replace(",", "")); //Gomme uses "," to split numbers (e.g. 1,000,000)
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
        for (CompletableFuture<PlayerStatistics> value : this.statsRequests.values()) {
            value.complete(null);
        }
        this.statsRequests.clear();
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
        this.requestQueue.offer(name);
        return future;
    }

}
