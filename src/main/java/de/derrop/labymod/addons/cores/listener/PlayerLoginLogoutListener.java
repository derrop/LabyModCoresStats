package de.derrop.labymod.addons.cores.listener;
/*
 * Created by derrop on 22.09.2019
 */

import com.mojang.authlib.GameProfile;
import de.derrop.labymod.addons.cores.CoresAddon;
import net.labymod.utils.Consumer;
import net.minecraft.network.play.server.S38PacketPlayerListItem;

public class PlayerLoginLogoutListener implements Consumer<Object> {

    private CoresAddon coresAddon;

    public PlayerLoginLogoutListener(CoresAddon coresAddon) {
        this.coresAddon = coresAddon;
    }

    @Override
    public void accept(Object object) {
        if (object instanceof S38PacketPlayerListItem) {
            S38PacketPlayerListItem packet = ((S38PacketPlayerListItem) object);
            if (packet.func_179768_b() == S38PacketPlayerListItem.Action.ADD_PLAYER) {
                for (S38PacketPlayerListItem.AddPlayerData addPlayerData : ((S38PacketPlayerListItem) object).func_179767_a()) {
                    this.coresAddon.getOnlinePlayers().put(addPlayerData.getProfile().getId(), addPlayerData.getProfile());
                }
            } else if (packet.func_179768_b() == S38PacketPlayerListItem.Action.REMOVE_PLAYER) {
                for (S38PacketPlayerListItem.AddPlayerData addPlayerData : packet.func_179767_a()) {
                    if (addPlayerData.getProfile().getId() != null) {
                        GameProfile profile = this.coresAddon.getOnlinePlayers().remove(addPlayerData.getProfile().getId());
                        if (profile != null) {
                            this.coresAddon.getStatsParser().removeFromCache(profile.getName());
                            this.coresAddon.getDisplay().handleStatsUpdate();
                        }
                    }
                }
            }
        }
    }
}
