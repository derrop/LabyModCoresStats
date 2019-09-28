package de.derrop.labymod.addons.cores.statistics.types;
/*
 * Created by derrop on 28.09.2019
 */

import de.derrop.labymod.addons.cores.statistics.SimplePatternPlayerStatistics;
import net.labymod.core.LabyModCore;
import net.minecraft.client.Minecraft;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class CoresStatistics extends SimplePatternPlayerStatistics {

    private static final Map<Pattern, String> PATTERNS = new HashMap<>();

    static {
        PATTERNS.put(Pattern.compile(" Position im Ranking: (.*)| Ranking: (.*)"), "rank");
        PATTERNS.put(Pattern.compile(" Kills: (.*)"), "kills");
        PATTERNS.put(Pattern.compile(" Deaths: (.*)"), "deaths");
        PATTERNS.put(Pattern.compile(" K/D: (.*)"), "kd");
        PATTERNS.put(Pattern.compile(" Zerstörte Cores: (.*)| Cores destroyed: (.*)"), "destroyedCores");
        PATTERNS.put(Pattern.compile(" Gespielte Spiele: (.*)| Games played: (.*)"), "playedGames");
        PATTERNS.put(Pattern.compile(" Gewonnene Spiele: (.*)| Games won: (.*)"), "wonGames");
        PATTERNS.put(Pattern.compile(" Siegwahrscheinlichkeit: (.*) Prozent| Probability of winning: (.*)%"), "winRate");
    }
    
    public CoresStatistics(String name) {
        super(name, "CORES");
        super.setPatterns(PATTERNS);
    }

    private int kills = 0;
    private int deaths = 0;
    private double kd = 0;
    private int rank = 0;
    private double winRate = 0;
    private int playedGames = 0;
    private int wonGames = 0;

    public double getKd() {
        return kd;
    }

    public double getWinRate() {
        return winRate;
    }

    public int getDeaths() {
        return deaths;
    }

    public int getKills() {
        return kills;
    }

    public int getPlayedGames() {
        return playedGames;
    }

    public int getRank() {
        return rank;
    }

    @Override
    public Collection<String> getHumanReadableEntries() {
        return Arrays.asList(
                "Rang: " + this.rank,
                "Siegwahrscheinlichkeit: " + this.winRate + " %",
                "Gespielte Spiele: " + this.playedGames,
                "Gewonnene Spiele: " + this.wonGames,
                "K/D: " + this.kd,
                "Kills: " + this.kills,
                "Deaths: " + this.deaths
        );
    }

    @Override
    public boolean isStatsEnd(String message) {
        return "------------------------------".equals(message);
    }

    @Override
    public void warnOnGoodStats(Consumer<String> outputConsumer, Runnable warnSoundHandler) {
        if (this.rank > 0 && this.rank <= 300) {
            outputConsumer.accept("§4WARNUNG: §7Spieler §e" + super.getName() + " §7ist Platz §e" + rank);
        }
        if (this.winRate >= 75 && this.playedGames >= 30) {
            outputConsumer.accept("§4WARNUNG: §7Spieler §e" + super.getName() + " §7hat eine Siegwahrscheinlichkeit von §e" + winRate + " %" +
                    " §8(Gespielt: §e" + playedGames + "§8; Gewonnen: §e" + wonGames + "§8)");
        }
        if ((this.rank > 0 && this.rank <= 100) || (this.winRate >= 85 && this.playedGames >= 30) || (this.playedGames >= 500)) {
            warnSoundHandler.run();
        }
    }

    public int getWonGames() {
        return wonGames;
    }

    @Override
    public boolean hasRank() {
        return this.rank > 0;
    }

    @Override
    protected void handleParse(String key, String value) {
        switch (key) {
            case "kills":
                this.kills = Integer.parseInt(value);
                break;
            case "deaths":
                this.deaths = Integer.parseInt(value);
                break;
            case "kd":
                this.kd = Double.parseDouble(value);
                break;
            case "rank":
                this.rank = Integer.parseInt(value);
                break;
            case "winRate":
                this.winRate = Double.parseDouble(value);
                break;
            case "playedGames":
                this.playedGames = Integer.parseInt(value);
                break;
            case "wonGames":
                this.wonGames = Integer.parseInt(value);
                break;
        }
    }
}
