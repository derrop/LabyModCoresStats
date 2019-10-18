package de.derrop.labymod.addons.cores.statistics;
/*
 * Created by derrop on 22.09.2019
 */

import com.google.gson.JsonObject;
import de.derrop.labymod.addons.cores.CoresAddon;
import de.derrop.labymod.addons.cores.gametypes.GameType;
import de.derrop.labymod.addons.cores.player.OnlinePlayer;
import de.derrop.labymod.addons.cores.regex.Patterns;
import net.labymod.api.events.MessageReceiveEvent;
import net.labymod.core.LabyModCore;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;

public class StatsParser implements MessageReceiveEvent {

    private Map<String, CompletableFuture<PlayerStatistics>> statsRequests = new HashMap<>();
    private long lastBlock = -1;

    private BlockingQueue<Runnable> requestQueue = new LinkedBlockingQueue<>();

    private PlayerStatistics readingStats;

    private CoresAddon coresAddon;

    public StatsParser(CoresAddon coresAddon, ExecutorService executorService) {
        this.coresAddon = coresAddon;
        executorService.execute(() -> {
            while (!Thread.interrupted()) {
                try {
                    Runnable runnable = this.requestQueue.take();
                    if (LabyModCore.getMinecraft().getPlayer() != null) {
                        runnable.run();
                        Thread.sleep(600);
                    } else { //not connected to any server
                        for (CompletableFuture<PlayerStatistics> value : this.statsRequests.values()) {
                            value.complete(null);
                        }
                        this.statsRequests.clear();
                        Thread.sleep(5000);
                    }
                } catch (InterruptedException exception) {
                    exception.printStackTrace();
                }
            }
        });
    }

    private String getStatsPlayerName(String msg) {
        Matcher matcher = Patterns.BEGIN_STATS_PATTERN.matcher(msg);
        if (matcher.find()) {
            return Patterns.matcherGroup(matcher);
        }
        return null;
    }

    /**
     * Clears the cache of statistics and removes the last blocked request
     */
    public void reset() {
        this.lastBlock = -1;
        for (CompletableFuture<PlayerStatistics> value : this.statsRequests.values()) {
            value.complete(null);
        }
        this.statsRequests.clear();
        this.requestQueue.clear();
    }

    /**
     * Requests the stats of the given player from the server and caches them
     *
     * @param name the name of the player
     * @return the future with the {@link PlayerStatistics} object
     */
    public CompletableFuture<PlayerStatistics> requestStats(String name) {
        if (name.contains("§")) {
            return CompletableFuture.completedFuture(null);
        }
        if (this.statsRequests.containsKey(name)) {
            return this.statsRequests.get(name);
        }

        CompletableFuture<PlayerStatistics> future = new CompletableFuture<>();

        this.requestQueue.offer(() -> this.executeRequest(name, future));

        return future;
    }

    /**
     * Requests the stats of the given player (if the player is online) from the server and caches them
     *
     * @param name the name of the player
     * @return the future with the {@link PlayerStatistics} object
     */
    public CompletableFuture<PlayerStatistics> requestStatsByOnlinePlayer(String name) {
        if (name.contains("§")) {
            return CompletableFuture.completedFuture(null);
        }
        if (this.statsRequests.containsKey(name)) {
            return this.statsRequests.get(name);
        }

        CompletableFuture<PlayerStatistics> future = new CompletableFuture<>();
        this.requestQueue.offer(() -> {
            if (!this.coresAddon.getPlayerProvider().isPlayerOnline(name)) {
                future.complete(null);
                return;
            }

            this.executeRequest(name, future);
        });
        return future;
    }

    private void executeRequest(String name, CompletableFuture<PlayerStatistics> future) {
        if (this.lastBlock != -1 && System.currentTimeMillis() - this.lastBlock <= 20000) { //after the last blocked request, we wait 20 seconds to not get a blocked request directly after the other
            //but it also seems like if you get a blocked request (after something around 10 - 15 requests), that you're not getting unblocked on this server until it restarts
            //the requests have to be from different players, you could request the stats of a player as often as you want
            future.complete(null);
            return;
        }

        System.out.println("Parsing stats: " + name);
        this.statsRequests.put(name, future);
        LabyModCore.getMinecraft().getPlayer().sendChatMessage("/stats " + name);
    }

    /**
     * Called on chat message from the server
     *
     * @param message the message including color codes
     * @return the result of the message
     */
    public StatsParseResult handleChatMessage(String message) {
        if (this.statsRequests.isEmpty()) {
            return StatsParseResult.NONE;
        }
        if (message.equals("Du hast zu viele Statistiken abgerufen, bitte versuche es in einer anderen Runde erneut.") || //german
                message.equals("You have retrieved too many statistics, please try again in another game.")) { //english
            //Gomme hates us :peepoCry:
            for (CompletableFuture<PlayerStatistics> value : this.statsRequests.values()) {
                value.complete(null);
            }
            this.lastBlock = System.currentTimeMillis();
            return StatsParseResult.NONE;
        }
        if (this.readingStats != null) {
            if (this.readingStats.isStatsEnd(message)) {
                return StatsParseResult.END;
            }
            this.readingStats.parseLine(message);
            return StatsParseResult.ENTRY;
        }
        String name = this.getStatsPlayerName(message);
        if (name != null) {
            GameType gameType = this.coresAddon.getCurrentServerType();
            if (gameType != null) {
                this.readingStats = gameType.getStatisticsProvider().apply(name);
                if (this.readingStats != null) {
                    return StatsParseResult.BEGIN;
                }
            }
        }
        return StatsParseResult.NONE;
    }

    @Override
    public boolean onReceive(String coloredMessage, String message) {
        StatsParseResult result = this.handleChatMessage(message);
        if (result == StatsParseResult.END) {
            PlayerStatistics stats = this.readingStats;
            this.readingStats = null;

            OnlinePlayer player = this.coresAddon.getPlayerProvider().getOnlinePlayer(stats.getName());
            System.out.println("Stats parsed successfully: " + stats + "; player available: " + (player != null));
            if (player != null) {
                player.updateCachedStats(stats);
            }

            if (this.coresAddon.getSyncClient().isConnected()) {
                JsonObject jsonObject = new JsonObject();

                if (player != null) {
                    jsonObject.addProperty("uniqueId", player.getUniqueId().toString());
                }

                jsonObject.addProperty("name", stats.getName());
                jsonObject.addProperty("gamemode", stats.getGameType());
                jsonObject.add("stats", this.coresAddon.getGson().toJsonTree(stats.getStats()));
                this.coresAddon.getSyncClient().sendPacket((short) 4, jsonObject);
            }

            CompletableFuture<PlayerStatistics> future = this.statsRequests.remove(stats.getName());
            if (future != null) {
                future.complete(stats);
                return true;
            }
        } else if (result == StatsParseResult.BEGIN || result == StatsParseResult.ENTRY) {
            return this.statsRequests.containsKey(this.readingStats.getName());
        }
        return false;
    }
}
