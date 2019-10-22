package de.derrop.labymod.addons.cores.detector;
/*
 * Created by derrop on 16.10.2019
 */

import com.mojang.authlib.GameProfile;
import de.derrop.labymod.addons.cores.CoresAddon;
import de.derrop.labymod.addons.cores.gametypes.GameType;
import de.derrop.labymod.addons.cores.player.OnlinePlayer;
import de.derrop.labymod.addons.cores.player.PlayerDataProviders;
import de.derrop.labymod.addons.cores.player.PlayerProvider;
import de.derrop.labymod.addons.cores.statistics.PlayerStatistics;
import net.labymod.api.events.MessageReceiveEvent;
import net.labymod.core.LabyModCore;
import net.labymod.main.LabyMod;
import net.labymod.utils.Consumer;
import net.minecraft.client.network.NetworkPlayerInfo;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PlayerLoginLogoutDetector implements Consumer<Object>, MessageReceiveEvent, PlayerProvider {

    private CoresAddon coresAddon;
    private PlayerDataProviders playerDataProviders;

    private Map<UUID, OnlinePlayer> onlinePlayers = new ConcurrentHashMap<>();

    public PlayerLoginLogoutDetector(CoresAddon coresAddon, PlayerDataProviders playerDataProviders) {
        this.coresAddon = coresAddon;
        this.playerDataProviders = playerDataProviders;
    }

    @Override
    public void accept(Object packet) {
        if (!this.coresAddon.isCurrentServerTypeSupported() || !this.coresAddon.isConnectedWithGommeServer()) {
            return;
        }

        GameType gameType = this.coresAddon.getCurrentServerType();
        if (gameType.usesPacketsToCheckPlayerLeave()) {
            gameType.getUUIDOfLeftPlayer(this.coresAddon, null, packet, this::handleLeave);
        }
        if (gameType.usesPacketsToCheckPlayerJoin()) {
            gameType.getProfileOfJoinedPlayer(this.coresAddon, null, packet, this::initPlayer);
        }
    }

    @Override
    public boolean onReceive(String coloredMessage, String message) {
        if (!this.coresAddon.isCurrentServerTypeSupported()) {
            return false;
        }

        GameType gameType = this.coresAddon.getCurrentServerType();

        if (!gameType.usesPacketsToCheckPlayerLeave()) {
            gameType.getUUIDOfLeftPlayer(this.coresAddon, message, null, this::handleLeave);
        }
        if (!gameType.usesPacketsToCheckPlayerJoin()) {
            gameType.getProfileOfJoinedPlayer(this.coresAddon, message, null, this::initPlayer);
        }

        return false;
    }

    private void handleLeave(UUID uniqueId) {
        OnlinePlayer player = this.getOnlinePlayer(uniqueId);
        System.out.println("Detected player left: " + uniqueId);
        if (player != null) {
            this.onlinePlayers.remove(uniqueId);
            this.coresAddon.getMatchDetector().removePlayerFromMatch(player.getName());
            this.coresAddon.getDisplay().handleStatsUpdate(this.onlinePlayers.values());
        }
    }

    public void handleServerSwitch() {
        for (NetworkPlayerInfo playerInfo : LabyModCore.getMinecraft().getConnection().getPlayerInfoMap()) {
            GameProfile profile = playerInfo.getGameProfile();
            if (!this.onlinePlayers.containsKey(profile.getId())) {
                this.initPlayer(profile);
            }
        }
    }

    private void initPlayer(GameProfile profile) { //todo tags are shown on match end
        if (this.onlinePlayers.containsKey(profile.getId())) {
            return;
        }
        this.coresAddon.getExecutorService().execute(() -> {
            System.out.println("Registered join for " + profile.getId() + "#" + profile.getName());
            OnlinePlayer player = new OnlinePlayer(this.coresAddon, this.playerDataProviders, profile);
            this.onlinePlayers.put(player.getUniqueId(), player);
            try {
                PlayerStatistics statistics = player.loadStatistics().get(30, TimeUnit.SECONDS);
                player.updateCachedStats(statistics);
                this.coresAddon.getDisplay().handleStatsUpdate(this.onlinePlayers.values());
                if (!player.isSelf()) {
                    this.coresAddon.warnForTags(player);
                    this.coresAddon.timedWarnOnGoodStats(player);
                }
                System.out.println(this.onlinePlayers);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
            }
        });
    }

    public void reset() {
        System.out.println("Reset players");
        this.onlinePlayers.clear();
    }

    @Override
    public Collection<OnlinePlayer> getOnlinePlayers() {
        return this.onlinePlayers.values();
    }

    @Override
    public OnlinePlayer getOnlinePlayer(UUID uniqueId) {
        return this.onlinePlayers.get(uniqueId);
    }

    @Override
    public OnlinePlayer getOnlinePlayer(String name) {
        return this.onlinePlayers.values().stream().filter(player -> player.getProfile().getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    @Override
    public OnlinePlayer getSelf() {
        return this.getOnlinePlayer(LabyMod.getInstance().getPlayerUUID());
    }
}
