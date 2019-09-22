package de.derrop.labymod.addons.cores.listener;
/*
 * Created by derrop on 22.09.2019
 */

import de.derrop.labymod.addons.cores.CoresAddon;
import de.derrop.labymod.addons.cores.statistics.PlayerStatistics;
import net.labymod.api.events.MessageSendEvent;
import net.labymod.core.LabyModCore;
import net.minecraft.client.Minecraft;

import java.util.Comparator;
import java.util.Optional;

public class CommandListener implements MessageSendEvent {

    private CoresAddon coresAddon;

    public CommandListener(CoresAddon coresAddon) {
        this.coresAddon = coresAddon;
    }

    @Override
    public boolean onSend(String message) {
        if (message.isEmpty())
            return false;
        if (message.charAt(0) != '!')
            return false;
        String commandLine = message.substring(1);
        String[] args = commandLine.split(" ");
        if (args[0].equalsIgnoreCase("bestStats")) {
            this.displayStatistics(this.coresAddon.getBestPlayer());
        } else if (args[0].equalsIgnoreCase("worstStats")) {
            this.displayStatistics(this.coresAddon.getWorstPlayer());
        }
        return true;
    }

    private void displayStatistics(PlayerStatistics stats) {
        if (stats != null) {
            LabyModCore.getMinecraft().displayMessageInChat("§7Bester Spieler auf dem Server: §e" + stats.getName());
            if (stats.getStats().containsKey("rank")) {
                LabyModCore.getMinecraft().displayMessageInChat("§7Rang: §e" + stats.getStats().get("rank"));
            }
            if (stats.getStats().containsKey("kills")) {
                LabyModCore.getMinecraft().displayMessageInChat("§7Kills: §e" + stats.getStats().get("kills"));
            }
            if (stats.getStats().containsKey("deaths")) {
                LabyModCore.getMinecraft().displayMessageInChat("§7Deaths: §e" + stats.getStats().get("deaths"));
            }
            if (stats.getStats().containsKey("kd")) {
                LabyModCore.getMinecraft().displayMessageInChat("§7K/D: §e" + stats.getStats().get("kd"));
            }
            if (stats.getStats().containsKey("destroyedCores")) {
                LabyModCore.getMinecraft().displayMessageInChat("§7Zerstörte Cores: §e" + stats.getStats().get("destroyedCores"));
            }
            if (stats.getStats().containsKey("playedGames")) {
                LabyModCore.getMinecraft().displayMessageInChat("§7Gespielte Spiele: §e" + stats.getStats().get("playedGames"));
            }
            if (stats.getStats().containsKey("wonGames")) {
                LabyModCore.getMinecraft().displayMessageInChat("§7Gewonnene Spiele: §e" + stats.getStats().get("wonGames"));
            }
            if (stats.getStats().containsKey("winRate")) {
                LabyModCore.getMinecraft().displayMessageInChat("§7Siegwahrscheinlichkeit: §e" + stats.getStats().get("winRate") + " %");
            }
        } else {
            LabyModCore.getMinecraft().displayMessageInChat("§7Es sind keine Statistiken geladen");
        }
    }
}
