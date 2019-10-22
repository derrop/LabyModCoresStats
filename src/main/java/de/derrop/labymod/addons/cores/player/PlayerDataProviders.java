package de.derrop.labymod.addons.cores.player;
/*
 * Created by derrop on 16.10.2019
 */

import de.derrop.labymod.addons.cores.CoresAddon;
import de.derrop.labymod.addons.cores.detector.PlayerLoginLogoutDetector;
import de.derrop.labymod.addons.cores.detector.ScoreboardTagDetector;
import de.derrop.labymod.addons.cores.statistics.StatsParser;
import de.derrop.labymod.addons.cores.tag.TagProvider;
import de.derrop.labymod.addons.cores.tag.TagRenderListener;

public class PlayerDataProviders {

    private ScoreboardTagDetector scoreboardTagDetector;
    private TagProvider tagProvider;
    private StatsParser statsParser;
    private PlayerLoginLogoutDetector playerLoginLogoutDetector;

    public PlayerDataProviders(CoresAddon coresAddon) {
        this.scoreboardTagDetector = new ScoreboardTagDetector();
        this.tagProvider = new TagProvider(coresAddon);
        this.statsParser = new StatsParser(coresAddon, coresAddon.getExecutorService());
        this.playerLoginLogoutDetector = new PlayerLoginLogoutDetector(coresAddon, this);

        coresAddon.getApi().getEventManager().register(new TagRenderListener(coresAddon, this.tagProvider));
        coresAddon.getApi().getEventManager().register(this.statsParser);
        coresAddon.getApi().getEventManager().register(this.playerLoginLogoutDetector);
        coresAddon.getApi().getEventManager().registerOnIncomingPacket(this.playerLoginLogoutDetector);
    }

    public void reset() {
        this.playerLoginLogoutDetector.reset();
    }

    public void handleServerSwitch() {
        this.playerLoginLogoutDetector.handleServerSwitch();
    }

    public TagProvider getTagProvider() {
        return tagProvider;
    }

    public StatsParser getStatsParser() {
        return statsParser;
    }

    public ScoreboardTagDetector getScoreboardTagDetector() {
        return scoreboardTagDetector;
    }

    public PlayerProvider getPlayerProvider() {
        return this.playerLoginLogoutDetector;
    }
}
