package de.derrop.labymod.addons.cores.module;
/*
 * Created by derrop on 22.09.2019
 */

import de.derrop.labymod.addons.cores.CoresAddon;
import de.derrop.labymod.addons.cores.statistics.PlayerStatistics;
import net.labymod.ingamegui.ModuleCategory;
import net.labymod.ingamegui.moduletypes.SimpleModule;
import net.labymod.settings.elements.ControlElement;
import net.labymod.utils.Material;

public class WorstPlayerModule extends SimpleModule {

    private CoresAddon coresAddon;

    public WorstPlayerModule(CoresAddon coresAddon) {
        this.coresAddon = coresAddon;
    }

    @Override
    public String getDisplayName() {
        return "Rangniedrigster";
    }

    @Override
    public String getDisplayValue() {
        PlayerStatistics statistics = this.coresAddon.getWorstPlayer();
        return statistics != null ? statistics.getName() : this.getDefaultValue();
    }

    @Override //this is only shown in the module editor
    public String getDefaultValue() {
        return "?";
    }

    @Override
    public ControlElement.IconData getIconData() {
        return new ControlElement.IconData(Material.DIAMOND_SWORD);
    }

    @Override
    public void loadSettings() {
    }

    @Override
    public boolean isShown() {
        return super.isShown() && !this.coresAddon.getStatsParser().getCachedStats().isEmpty();
    }

    @Override
    public String getSettingName() {
        return "cores_worst_player";
    }

    @Override
    public String getControlName() {
        return "Worst player";
    }

    @Override
    public String getDescription() {
        return "Shows the worst player in the current round (sorted by the ranking)";
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
