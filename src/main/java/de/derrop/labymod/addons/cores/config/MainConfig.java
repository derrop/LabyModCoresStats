package de.derrop.labymod.addons.cores.config;
/*
 * Created by derrop on 16.10.2019
 */

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.derrop.labymod.addons.cores.CoresAddon;
import de.derrop.labymod.addons.cores.gametypes.GameType;
import net.labymod.settings.elements.BooleanElement;
import net.labymod.settings.elements.ControlElement;
import net.labymod.settings.elements.SettingsElement;
import net.labymod.settings.elements.StringElement;
import net.labymod.utils.Material;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class MainConfig {

    private Gson gson = new Gson();
    
    public boolean externalDisplayEnabled;
    public String authToken;
    public boolean showTagsAboveName;

    public void loadConfig(JsonObject config, CoresAddon addon) {
        this.externalDisplayEnabled = config.has("externalDisplayEnabled") && config.get("externalDisplayEnabled").getAsBoolean();
        this.showTagsAboveName = config.has("showTagsAboveName") && config.get("showTagsAboveName").getAsBoolean();

        if (config.has("externalDisplay")) {
            Rectangle rectangle = this.gson.fromJson(
                    config.get("externalDisplay"),
                    Rectangle.class
            );
            int extendedState = config.has("externalDisplayExtendedState") ?
                    config.get("externalDisplayExtendedState").getAsInt() :
                    JFrame.NORMAL;
            if (rectangle != null) {
                addon.getDisplay().setBounds(rectangle);
                if (addon.getDisplay().isVisible()) {
                    addon.getDisplay().repaint();
                }
                addon.getDisplay().setExtendedState(extendedState);
            }
        }
        for (GameType gameType : addon.getSupportedGameTypes().values()) {
            if (config.has(gameType.getName() + "Enabled")) {
                gameType.setEnabled(config.get(gameType.getName() + "Enabled").getAsBoolean());
            } else {
                gameType.setEnabled(gameType.isDefaultEnabled());
            }
        }
        this.authToken = config.has("token") ? config.get("token").getAsString() : null;

        if (this.authToken != null && !this.authToken.isEmpty() && !addon.getSyncClient().isConnectedWithToken(this.authToken)) {
            addon.connectToSyncServer();
        }
    }

    public void fillSettings(List<SettingsElement> subSettings, CoresAddon addon) {
        subSettings.add(
                new StringElement("Token", addon, new ControlElement.IconData(Material.DIAMOND_SWORD), "token", this.authToken)
                        .addCallback(token -> {
                            this.authToken = token;
                            if (token != null && !token.isEmpty()) {
                                if (addon.getSyncClient().isConnected()) {
                                    addon.getSyncClient().close();
                                }
                                addon.connectToSyncServer();
                            } else if (addon.getSyncClient().isConnected()) {
                                addon.getSyncClient().close();
                            }
                        })
        );

        subSettings.add(
                new BooleanElement("External Display", addon, new ControlElement.IconData(Material.SIGN), "externalDisplayEnabled", false)
                        .addCallback(externalDisplay -> {
                            this.externalDisplayEnabled = externalDisplay;
                            if (!this.externalDisplayEnabled) {
                                addon.getDisplay().setVisible(false);
                            } else if (addon.isCurrentServerTypeSupported()) {
                                addon.getDisplay().setVisible(true);
                            }
                        })
        );
        for (GameType gameType : addon.getSupportedGameTypes().values()) {
            subSettings.add(
                    new BooleanElement(gameType.getName() + " Stats", addon, gameType.getIconData(), gameType.getName() + "Enabled", gameType.isDefaultEnabled())
                            .addCallback(gameType::setEnabled)
            );
        }

        subSettings.add(
                new BooleanElement("Tags above name", addon, new ControlElement.IconData(Material.PAPER), "showTagsAboveName", true)
                        .addCallback(showTagsAboveName -> this.showTagsAboveName = showTagsAboveName)
        );
    }
    
}
