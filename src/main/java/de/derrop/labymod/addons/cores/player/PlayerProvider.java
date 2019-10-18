package de.derrop.labymod.addons.cores.player;
/*
 * Created by derrop on 16.10.2019
 */

import java.util.Collection;
import java.util.UUID;

public interface PlayerProvider {

    default boolean isPlayerOnline(UUID uniqueId) {
        return this.getOnlinePlayer(uniqueId) != null;
    }

    default boolean isPlayerOnline(String name) {
        return this.getOnlinePlayer(name) != null;
    }

    OnlinePlayer getOnlinePlayer(UUID uniqueId);

    OnlinePlayer getOnlinePlayer(String name);

    OnlinePlayer getSelf();

    Collection<OnlinePlayer> getOnlinePlayers();

}
