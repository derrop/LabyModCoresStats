package de.derrop.labymod.addons.cores.listener;
/*
 * Created by derrop on 22.09.2019
 */

import de.derrop.labymod.addons.cores.CoresAddon;
import de.derrop.labymod.addons.cores.regex.Patterns;
import net.labymod.api.events.MessageReceiveEvent;
import net.labymod.core.LabyModCore;

import java.util.regex.Matcher;

public class PlayerStatsLoginListener implements MessageReceiveEvent {
    private CoresAddon coresAddon;

    public PlayerStatsLoginListener(CoresAddon coresAddon) {
        this.coresAddon = coresAddon;
    }

    @Override
    public boolean onReceive(String coloredMsg, String msg) {
        {
            Matcher matcher = Patterns.PLAYER_JOIN_PATTERN.matcher(msg);
            if (matcher.find()) { //player joined the match
                String name = Patterns.matcherGroup(matcher);

                if (name != null && !name.equals(LabyModCore.getMinecraft().getPlayer().getName()) &&
                        this.coresAddon.getCurrentServer() != null &&
                        this.coresAddon.getCurrentServer().equals("CORES")) {
                    this.coresAddon.requestPlayerStatsAndWarn(name);
                }
                return false;
            }
        }
        return false;
    }

}
