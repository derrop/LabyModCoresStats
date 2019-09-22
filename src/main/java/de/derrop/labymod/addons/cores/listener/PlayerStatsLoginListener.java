package de.derrop.labymod.addons.cores.listener;
/*
 * Created by derrop on 22.09.2019
 */

import com.mojang.authlib.GameProfile;
import de.derrop.labymod.addons.cores.CoresAddon;
import net.labymod.api.events.MessageReceiveEvent;
import net.labymod.core.LabyModCore;

import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerStatsLoginListener implements MessageReceiveEvent {
    private static final Pattern PLAYER_JOIN_PATTERN = Pattern.compile("» (.*) hat das Spiel betreten");

    private CoresAddon coresAddon;
    private final Random random = new Random();

    public PlayerStatsLoginListener(CoresAddon coresAddon) {
        this.coresAddon = coresAddon;
    }

    @Override
    public boolean onReceive(String coloredMsg, String msg) {
        {
            Matcher matcher = PLAYER_JOIN_PATTERN.matcher(msg);
            if (matcher.find()) { //player joined the match
                String name = matcher.group(1);

                if (name.equals(LabyModCore.getMinecraft().getPlayer().getName())) { //todo check if this is a cores server (Gomme sends information like this for LabyMod's DiscordRPC)
                    for (GameProfile profile : this.coresAddon.getOnlinePlayers().values()) {
                        this.requestPlayerStatsAndWarn(profile.getName());
                    }
                } else {
                    this.requestPlayerStatsAndWarn(name);
                }
                return false;
            }
        }
        return false;
    }

    private void requestPlayerStatsAndWarn(String name) {
        this.coresAddon.getExecutorService().schedule(
                () -> {
                    try {
                        this.coresAddon.warnOnGoodStats(this.coresAddon.getStatsParser().requestStats(name).get(6, TimeUnit.SECONDS));
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                        e.printStackTrace();
                    }
                },
                this.random.nextInt(150) + 50, TimeUnit.MILLISECONDS
        ); //min: 50; max: 200
    }

}
