package de.derrop.labymod.addons.cores.listener;
/*
 * Created by derrop on 22.09.2019
 */

import de.derrop.labymod.addons.cores.CoresAddon;
import net.labymod.api.events.MessageReceiveEvent;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerLoginLogoutListener implements MessageReceiveEvent {
    private static final Pattern PLAYER_JOIN_PATTERN = Pattern.compile("» (.*) hat das Spiel betreten");
    private static final Pattern PLAYER_LEAVE_LOBBY_PATTERN = Pattern.compile("« (.*) hat das Spiel verlassen");
    private static final Pattern PLAYER_LEAVE_INGAME_PATTERN = Pattern.compile("(.*) hat das Spiel verlassen. Team (.*) hat noch (.*) Spieler.");

    private CoresAddon coresAddon;
    private final Random random = new Random();

    public PlayerLoginLogoutListener(CoresAddon coresAddon) {
        this.coresAddon = coresAddon;
    }

    @Override
    public boolean onReceive(String coloredMsg, String msg) {
        {
            Matcher matcher = PLAYER_JOIN_PATTERN.matcher(msg);
            if (matcher.find()) { //player joined the match
                String name = matcher.group(1);

                this.coresAddon.getExecutorService().execute(() -> {
                    try {
                        Thread.sleep(this.random.nextInt(500) + 500); //min: 500; max: 1000
                    } catch (InterruptedException exception) {
                        exception.printStackTrace();
                    }
                    this.coresAddon.getStatsParser().requestStats(name).thenAccept(playerStatistics -> {
                        System.out.println("PlayerStatistics for " + name + ":" + playerStatistics.getStats());
                        this.coresAddon.warnOnGoodStats(playerStatistics);
                    });
                });
                return false;
            }
        }
        {
            Matcher matcher = PLAYER_LEAVE_LOBBY_PATTERN.matcher(msg);
            if (matcher.find()) { //player left the match in the lobby/end phase
                String name = matcher.group(1);
                this.coresAddon.getStatsParser().removeFromCache(name);
                return false;
            }
        }
        {
            Matcher matcher = PLAYER_LEAVE_INGAME_PATTERN.matcher(msg);
            if (matcher.find()) {//player left the match in the ingame phase
                String name = matcher.group(1);
                this.coresAddon.getStatsParser().removeFromCache(name);
                return false;
            }
        }
        return false;
    }
}
