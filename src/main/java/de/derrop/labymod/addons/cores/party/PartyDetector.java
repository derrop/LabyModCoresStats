package de.derrop.labymod.addons.cores.party;
/*
 * Created by derrop on 22.09.2019
 */

import net.labymod.api.events.MessageReceiveEvent;
import net.labymod.core.LabyModCore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PartyDetector implements MessageReceiveEvent {

    private static final Pattern PARTY_JOIN_PATTERN = Pattern.compile("\\[Party\\] (.*) hat die Party betreten");
    private static final Pattern PARTY_LEAVE_PATTERN = Pattern.compile("\\[Party\\] (.*) hat die Party verlassen");
    private static final Pattern PARTY_LIST_PATTERN = Pattern.compile("\\[Party\\] Mitglieder der Party von (.*) \\((.*)\\)");

    private Collection<String> currentPartyMembers = new ArrayList<>();
    private boolean handleList = false;

    public void handleLeaveParty() {
        this.currentPartyMembers.clear();
    }

    public Collection<String> getCurrentPartyMembers() {
        return currentPartyMembers;
    }

    @Override
    public boolean onReceive(String coloredMsg, String msg) {
        if (this.handleList) {
            this.handleList = false;
            String[] members = msg.substring(1).split(", "); //substring(1) because the message begins with a space
            this.currentPartyMembers.addAll(Arrays.asList(members));
            return false;
        }

        {
            Matcher matcher = PARTY_JOIN_PATTERN.matcher(msg);
            if (matcher.find()) {
                String name = matcher.group(1);
                if (name.equals(LabyModCore.getMinecraft().getPlayer().getName())) { // you joined the party of someone
                    LabyModCore.getMinecraft().getPlayer().sendChatMessage("/party list");
                } else {
                    this.currentPartyMembers.add(name);
                }
                return false;
            }
        }
        {
            Matcher matcher = PARTY_LEAVE_PATTERN.matcher(msg);
            if (matcher.find()) {
                String name = matcher.group(1);
                this.currentPartyMembers.remove(name);
                if (name.equals(LabyModCore.getMinecraft().getPlayer().getName())) { //you left the party of someone
                    this.currentPartyMembers.clear();
                }
                return false;
            }
        }
        {
            Matcher matcher = PARTY_LIST_PATTERN.matcher(msg);
            if (matcher.matches()) {
                String leader = matcher.group(1);
                this.currentPartyMembers.clear();
                this.currentPartyMembers.add(leader);
                this.handleList = true;
                return false;
            }
        }
        return false;
    }

}
