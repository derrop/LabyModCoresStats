package de.derrop.labymod.addons.cores.statistics.types;
/*
 * Created by derrop on 28.09.2019
 */

import de.derrop.labymod.addons.cores.statistics.SimplePatternPlayerStatistics;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class BedWarsStatistics extends SimplePatternPlayerStatistics {
    private static final Map<Pattern, String> PATTERNS = new HashMap<>();

    static {
        PATTERNS.put(Pattern.compile(" Position im Ranking: (.*)| Ranking: (.*)"), "rank");
        PATTERNS.put(Pattern.compile(" Kills: (.*)"), "kills");
        PATTERNS.put(Pattern.compile(" Deaths: (.*)"), "deaths");
        PATTERNS.put(Pattern.compile(" K/D: (.*)"), "kd");
        PATTERNS.put(Pattern.compile(" Gespielte Spiele: (.*)| Games played: (.*)"), "playedGames");
        PATTERNS.put(Pattern.compile(" Gewonnene Spiele: (.*)| Games won: (.*)"), "wonGames");
        PATTERNS.put(Pattern.compile(" Zerstörte Betten: (.*)| Beds destroyed: (.*)"), "destroyedBeds");
    }

    public BedWarsStatistics(String name) {
        super(name, "BW");
        super.setPatterns(PATTERNS);
    }

    private int rank;
    private int kills;
    private int deaths;
    private double kd;
    private int playedGames;
    private int wonGames;
    private int destroyedBeds;

    public int getWonGames() {
        return wonGames;
    }

    public int getPlayedGames() {
        return playedGames;
    }

    public int getKills() {
        return kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public double getKd() {
        return kd;
    }

    public int getDestroyedBeds() {
        return destroyedBeds;
    }

    public double getWinRate() {
        return ((double) this.wonGames / (double) this.playedGames) * 100D;
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
            case "destroyedBeds":
                this.destroyedBeds = Integer.parseInt(value);
                break;
            case "playedGames":
                this.playedGames = Integer.parseInt(value);
                break;
            case "wonGames":
                this.wonGames = Integer.parseInt(value);
                break;
        }
    }

    @Override
    public boolean hasRank() {
        return this.rank > 0;
    }

    @Override
    public int getRank() {
        return this.rank;
    }

    @Override
    public Collection<String> getHumanReadableEntries() {
        return Arrays.asList(
                "Rang: " + this.rank,
                "Siegwahrscheinlichkeit: " + String.format("%.2f", this.getWinRate()) + " %",
                "Gespielte Spiele: " + this.playedGames,
                "Gewonnene Spiele: " + this.wonGames,
                "K/D: " + this.kd,
                "Kills: " + this.kills,
                "Deaths: " + this.deaths,
                "Zerstörte Betten: " + this.destroyedBeds
        );
    }

    @Override
    public boolean isStatsEnd(String message) {
        return "----------------------".equals(message);
    }

    @Override
    public void warnOnGoodStats(Consumer<String> outputConsumer, Runnable warnSoundHandler) {
        double winRate = this.getWinRate();

        if (this.rank > 0 && this.rank <= 300) {
            outputConsumer.accept("§4WARNUNG: §7Spieler §e" + super.getName() + " §7ist Platz §e" + rank);
        }
        if ((winRate) >= 75 && this.playedGames >= 30) {
            outputConsumer.accept("§4WARNUNG: §7Spieler §e" + super.getName() + " §7hat eine Siegwahrscheinlichkeit von §e" + String.format("%.2f", winRate) + " %" +
                    " §8(Gespielt: §e" + this.playedGames + "§8; Gewonnen: §e" + this.wonGames + "§8)");
        }
        if ((this.rank > 0 && this.rank <= 100) || (winRate >= 85 && this.playedGames >= 30) || (this.playedGames >= 500)) {
            warnSoundHandler.run();
        }
    }
}
