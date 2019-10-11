package de.derrop.labymod.addons.cores.listener;
/*
 * Created by derrop on 22.09.2019
 */

import de.derrop.labymod.addons.cores.CoresAddon;
import de.derrop.labymod.addons.cores.statistics.PlayerStatistics;
import de.derrop.labymod.addons.cores.tag.TagType;
import net.labymod.api.events.MessageSendEvent;
import net.labymod.core.LabyModCore;

public class CommandListener implements MessageSendEvent {

    private CoresAddon coresAddon;

    public CommandListener(CoresAddon coresAddon) {
        this.coresAddon = coresAddon;
    }

    @Override
    public boolean onSend(String message) {
        if (message.isEmpty()) {
            return false;
        }
        if (message.charAt(0) != '!') {
            return false;
        }
        String commandLine = message.substring(1);
        String[] args = commandLine.split(" ");
        if (args[0].equalsIgnoreCase("bestStats")) {
            this.displayStatistics(this.coresAddon.getBestPlayer(), true);
        } else if (args[0].equalsIgnoreCase("worstStats")) {
            this.displayStatistics(this.coresAddon.getWorstPlayer(), false);
        } else if (args[0].equalsIgnoreCase("ctag") || args[0].equalsIgnoreCase("utag")) {
            TagType tagType = args[0].equalsIgnoreCase("ctag") ? TagType.CLAN : TagType.PLAYER;
            if (args.length == 4 && args[1].equalsIgnoreCase("add")) {
                if (this.coresAddon.getTagProvider().isUseable()) {
                    this.coresAddon.getTagProvider().addTag(tagType, args[2], args[3]).thenAccept(success -> {
                        if (success) {
                            LabyModCore.getMinecraft().displayMessageInChat("§aDer Tag §e\"" + args[3] + "\" §awurde dem User §e\"" + args[2] + "\" §aerfolgreich hinzugefügt");
                        } else {
                            LabyModCore.getMinecraft().displayMessageInChat("§cDieser User besitzt den Tag §e\"" + args[3] + "\" §cbereits");
                        }
                    });
                } else {
                    LabyModCore.getMinecraft().displayMessageInChat("§cDu bist nicht mit dem Server verbunden");
                }
            } else if (args.length == 4 && args[1].equalsIgnoreCase("remove")) {
                if (this.coresAddon.getTagProvider().isUseable()) {
                    this.coresAddon.getTagProvider().removeTag(tagType, args[2], args[3]);
                    LabyModCore.getMinecraft().displayMessageInChat("§aDer Tag §e\"" + args[3] + "\" §awurde dem User §e\"" + args[2] + "\" §aerfolgreich entfernt");
                } else {
                    LabyModCore.getMinecraft().displayMessageInChat("§cDu bist nicht mit dem Server verbunden");
                }
            } else if (args.length == 3 && args[1].equalsIgnoreCase("list")) {
                if (this.coresAddon.getTagProvider().isUseable()) {
                    this.coresAddon.getTagProvider().listTags(tagType, args[2]).thenAccept(tags -> {
                        if (tags == null || tags.isEmpty()) {
                            LabyModCore.getMinecraft().displayMessageInChat("§cKeine Tags für den User §e\"" + args[2] + "\" §cgefunden");
                        } else {
                            LabyModCore.getMinecraft().displayMessageInChat("§aTags des Users §e\"" + args[2] + "\"§a: §e" + String.join(", ", tags));
                        }
                    });
                } else {
                    LabyModCore.getMinecraft().displayMessageInChat("§cDu bist nicht mit dem Server verbunden");
                }
            } else {
                LabyModCore.getMinecraft().displayMessageInChat("§e!" + args[0] + " add <name> <tag>");
                LabyModCore.getMinecraft().displayMessageInChat("§e!" + args[0] + " remove <name> <tag>");
                LabyModCore.getMinecraft().displayMessageInChat("§e!" + args[0] + " list <name>");
            }
        } else if (args[0].equalsIgnoreCase("help")) {
            LabyModCore.getMinecraft().displayMessageInChat("§e!bestStats §8| §7display the statistics of the player with the highest rank in the current round");
            LabyModCore.getMinecraft().displayMessageInChat("§e!worstStats §8| §7display the statistics of the player with the lowest rank in the current round");
            LabyModCore.getMinecraft().displayMessageInChat("§e!ctag add/remove/list <clanTag> <tag> §8| §7add/remove/list the tags of a clan");
            LabyModCore.getMinecraft().displayMessageInChat("§e!utag add/remove/list <name> <tag> §8| §7add/remove/list the tags of a player");
        }
        return true;
    }

    private void displayStatistics(PlayerStatistics stats, boolean best) {
        if (stats != null) {
            if (best) {
                LabyModCore.getMinecraft().displayMessageInChat("§7Ranghöchster Spieler auf dem Server: §e" + stats.getName());
            } else {
                LabyModCore.getMinecraft().displayMessageInChat("§7Rangniedrigster Spieler auf dem Server: §e" + stats.getName());
            }
            for (String entry : stats.getHumanReadableEntries()) {
                LabyModCore.getMinecraft().displayMessageInChat("§7" + entry);
            }
        } else {
            LabyModCore.getMinecraft().displayMessageInChat("§7Es sind keine Statistiken geladen");
        }
    }
}
