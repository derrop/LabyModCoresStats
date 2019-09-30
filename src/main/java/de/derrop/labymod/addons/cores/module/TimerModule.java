package de.derrop.labymod.addons.cores.module;
/*
 * Created by derrop on 28.09.2019
 */

import de.derrop.labymod.addons.cores.CoresAddon;
import net.labymod.ingamegui.ModuleCategory;
import net.labymod.ingamegui.moduletypes.SimpleModule;
import net.labymod.settings.elements.ControlElement;
import net.labymod.utils.Material;

public class TimerModule extends SimpleModule {

    private CoresAddon coresAddon;

    public TimerModule(CoresAddon coresAddon) {
        this.coresAddon = coresAddon;
    }

    @Override
    public String getDisplayName() {
        return "Rundentimer";
    }

    @Override
    public String getDisplayValue() {
        if (this.coresAddon.getLastRoundBeginTimestamp() == -1)
            return null;
        long milliseconds = System.currentTimeMillis() - this.coresAddon.getLastRoundBeginTimestamp();
        long seconds = (milliseconds / 1000) % 60;
        long minutes = ((milliseconds / (1000 * 60)) % 60);
        long hours = ((milliseconds / (1000 * 60 * 60)) % 24);
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    @Override //this is only shown in the module editor
    public String getDefaultValue() {
        return "?";
    }

    @Override
    public ControlElement.IconData getIconData() {
        return new ControlElement.IconData(Material.WATCH);
    }

    @Override
    public void loadSettings() {
    }

    @Override
    public boolean isShown() {
        return super.isShown() && this.coresAddon.getLastRoundBeginTimestamp() != -1;
    }

    @Override
    public String getSettingName() {
        return "cores_round_timer";
    }

    @Override
    public String getControlName() {
        return "Timer";
    }

    @Override
    public String getDescription() {
        return "Shows the time the current round is running";
    }

    @Override
    public int getSortingId() {
        return 0;
    }

    @Override
    public ModuleCategory getCategory() {
        return this.coresAddon.getCoresCategory();
    }

}
