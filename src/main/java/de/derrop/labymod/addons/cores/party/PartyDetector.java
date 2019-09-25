package de.derrop.labymod.addons.cores.party;
/*
 * Created by derrop on 22.09.2019
 */

import de.derrop.labymod.addons.cores.regex.Patterns;
import net.labymod.api.events.MessageReceiveEvent;
import net.labymod.core.LabyModCore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Matcher;

public class PartyDetector implements MessageReceiveEvent { //todo parties may not be detected correctly, we have to test this again

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
            Matcher matcher = Patterns.PARTY_JOIN_PATTERN.matcher(msg);
            if (matcher.find()) {
                String name = Patterns.matcherGroup(matcher);
                if (name.equals(LabyModCore.getMinecraft().getPlayer().getName())) { // you joined the party of someone
                    LabyModCore.getMinecraft().getPlayer().sendChatMessage("/party list");
                } else {
                    this.currentPartyMembers.add(name);
                }
                return false;
            }
        }
        {
            Matcher matcher = Patterns.PARTY_LEAVE_PATTERN.matcher(msg);
            if (matcher.find()) {
                String name = Patterns.matcherGroup(matcher);
                this.currentPartyMembers.remove(name);
                if (name.equals(LabyModCore.getMinecraft().getPlayer().getName())) { //you left the party of someone
                    this.currentPartyMembers.clear();
                }
                return false;
            }
        }
        {
            Matcher matcher = Patterns.PARTY_LIST_PATTERN.matcher(msg);
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
