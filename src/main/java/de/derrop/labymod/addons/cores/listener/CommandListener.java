package de.derrop.labymod.addons.cores.listener;
/*
 * Created by derrop on 22.09.2019
 */

import de.derrop.labymod.addons.cores.CoresAddon;
import de.derrop.labymod.addons.cores.regex.Patterns;
import de.derrop.labymod.addons.cores.statistics.PlayerStatistics;
import de.derrop.labymod.addons.cores.tag.TagType;
import net.labymod.api.events.MessageSendEvent;
import net.labymod.core.LabyModCore;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Stream;

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
            if (args.length >= 4 && args[1].equalsIgnoreCase("addt")) {
                if (this.coresAddon.getTagProvider().isUseable()) {
                    String[] tags = String.join(" ", Arrays.copyOfRange(args, 3, args.length)).split(";");
                    String[] users = this.findUsersOrClansInTeam(tagType, args[2]);
                    if (users.length != 0) {
                        this.addTags(tagType, users, tags);
                    } else {
                        LabyModCore.getMinecraft().displayMessageInChat("§cDieses Team wurde nicht gefunden");
                    }
                } else {
                    LabyModCore.getMinecraft().displayMessageInChat("§cDu bist nicht mit dem Server verbunden");
                }
            } else if (args.length >= 4 && args[1].equalsIgnoreCase("removet")) {
                if (this.coresAddon.getTagProvider().isUseable()) {

                    String[] tags = String.join(" ", Arrays.copyOfRange(args, 3, args.length)).split(";");
                    String[] users = this.findUsersOrClansInTeam(tagType, args[2]);
                    if (users.length != 0) {
                        this.removeTags(tagType, users, tags);
                    } else {
                        LabyModCore.getMinecraft().displayMessageInChat("§cDieses Team wurde nicht gefunden");
                    }
                } else {
                    LabyModCore.getMinecraft().displayMessageInChat("§cDu bist nicht mit dem Server verbunden");
                }
            } else if (args.length >= 4 && args[1].equalsIgnoreCase("add")) {
                if (this.coresAddon.getTagProvider().isUseable()) {
                    String[] tags = String.join(" ", Arrays.copyOfRange(args, 3, args.length)).split(";");
                    String[] users = args[2].split(";");
                    this.addTags(tagType, users, tags);
                } else {
                    LabyModCore.getMinecraft().displayMessageInChat("§cDu bist nicht mit dem Server verbunden");
                }
            } else if (args.length >= 4 && args[1].equalsIgnoreCase("remove")) {
                if (this.coresAddon.getTagProvider().isUseable()) {
                    String[] users = args[2].split(";");
                    String[] tags = String.join(" ", Arrays.copyOfRange(args, 3, args.length)).split(";");
                    this.removeTags(tagType, users, tags);
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
            LabyModCore.getMinecraft().displayMessageInChat("§e!ctag addt/removet <team> <tag> §8| §7add/remove the tags of every clan in a team");
            LabyModCore.getMinecraft().displayMessageInChat("§e!utag addt/removet <team> <tag> §8| §7add/remove the tags of every player in a team");
        }
        return true;
    }

    private String[] findUsersOrClansInTeam(TagType tagType, String team) {
        Stream<String> users = this.coresAddon.getScoreboardTagDetector().getPlayersWithPrefix(s -> Patterns.getPossibleTeamPrefixes(team).contains(s))
                .stream()
                .filter(userName -> this.coresAddon.isPlayerOnline(userName));

        if (tagType == TagType.PLAYER) {
            return users.toArray(String[]::new);
        } else {
            Collection<String> clans = new HashSet<>();
            users.forEach(userName -> {
                String tag = this.coresAddon.getScoreboardTagDetector().getScoreboardTag(userName);
                if (tag == null || this.coresAddon.getScoreboardTagDetector().isParty(tag)) {
                    return;
                }

                clans.add(tag);
            });
            return clans.toArray(new String[0]);
        }
    }

    private void removeTags(TagType tagType, String[] users, String[] tags) {
        for (String user : users) {
            for (String tag : tags) {
                this.coresAddon.getTagProvider().removeTag(tagType, user, tag);
                LabyModCore.getMinecraft().displayMessageInChat("§aDer Tag §e\"" + tag + "\" §awurde dem User §e\"" + user + "\" §aerfolgreich entfernt");
            }
        }
    }

    private void addTags(TagType tagType, String[] users, String[] tags) {
        for (String user : users) {
            for (String tag : tags) {
                this.coresAddon.getTagProvider().addTag(tagType, user, tag).thenAccept(success -> {
                    if (success) {
                        LabyModCore.getMinecraft().displayMessageInChat("§aDer Tag §e\"" + tag + "\" §awurde dem User §e\"" + user + "\" §aerfolgreich hinzugefügt");
                    } else {
                        LabyModCore.getMinecraft().displayMessageInChat("§cDer User §e\"" + user + "\" §cbesitzt den Tag §e\"" + tag + "\" §cbereits");
                    }
                });
            }
        }
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
