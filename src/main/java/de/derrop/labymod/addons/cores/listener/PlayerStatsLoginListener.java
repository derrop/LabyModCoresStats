package de.derrop.labymod.addons.cores.listener;
/*
 * Created by derrop on 22.09.2019
 */

import de.derrop.labymod.addons.cores.CoresAddon;
import net.labymod.api.events.MessageReceiveEvent;
import net.labymod.core.LabyModCore;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerStatsLoginListener implements MessageReceiveEvent {
    private static final Pattern PLAYER_JOIN_PATTERN = Pattern.compile("» (.*) hat das Spiel betreten");

    private CoresAddon coresAddon;

    public PlayerStatsLoginListener(CoresAddon coresAddon) {
        this.coresAddon = coresAddon;
    }

    @Override
    public boolean onReceive(String coloredMsg, String msg) {
        {
            Matcher matcher = PLAYER_JOIN_PATTERN.matcher(msg);
            if (matcher.find()) { //player joined the match
                String name = matcher.group(1);

                if (!name.equals(LabyModCore.getMinecraft().getPlayer().getName()) &&
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
