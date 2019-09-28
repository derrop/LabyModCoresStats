package de.derrop.labymod.addons.cores.statistics;
/*
 * Created by derrop on 28.09.2019
 */

import de.derrop.labymod.addons.cores.regex.Patterns;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class SimplePatternPlayerStatistics extends PlayerStatistics {

    private Map<Pattern, String> patterns = new HashMap<>();

    public SimplePatternPlayerStatistics(String name, String gameType) {
        super(name, gameType);
    }

    public void setPatterns(Map<Pattern, String> patterns) {
        this.patterns = patterns;
    }

    public void addPattern(Pattern pattern, String key) {
        this.patterns.put(pattern, key);
    }

    protected abstract void handleParse(String key, String value);

    @Override
    public void parseLine(String message) {
        for (Map.Entry<Pattern, String> entry : this.patterns.entrySet()) {
            Matcher matcher = entry.getKey().matcher(message);
            if (matcher.find()) {
                String group = Patterns.matcherGroup(matcher);
                if (group != null) {
                    group = group.replace(",", ""); //Gomme uses "," to split numbers (e.g. 1,000,000)
                    this.handleParse(entry.getValue(), group);
                    this.getStats().put(entry.getValue(), group);
                    break;
                }
            }
        }
    }
}
