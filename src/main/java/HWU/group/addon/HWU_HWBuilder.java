package HWU.group.addon;

import HWU.group.addon.commands.ProxyCommand;
import HWU.group.addon.commands.SwitchCommand;
import HWU.group.addon.hud.StatsViewer;
import HWU.group.addon.modules.*;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class HWU_HWBuilder extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("HWU");
    public static final Category HELPER_MODULES_CATEGORY = new Category("HWU Helper Modules");
    public static final HudGroup HUD_GROUP = new HudGroup("HWU");

    // TODO:Re-configure the default values of all modules to the current working config
    @Override
    public void onInitialize() {
        LOG.info("Initializing HWU Highway Builder.");

        // Modules
        Modules.get().add(new HWUHighwayBuilder());
        Modules.get().add(new BetterEChestFarmer());
        Modules.get().add(new GatherItem());
        Modules.get().add(new HWUAutoWalk());
        Modules.get().add(new BetterAutoReplenish());
        Modules.get().add(new HWUNuker());
        Modules.get().add(new HWUAutoEat());
        Modules.get().add(new ProxySwitcher());
        Modules.get().add(new CoordsHider());
        Modules.get().add(new HWUPaver());
        Modules.get().add(new GrimAirPlace());
        Modules.get().add(new HWUKillAura());
        Modules.get().add(new LavaSourceRemover());
        // TODO:Maybe add a "Highway Maintainer" module that uses an elytra to travel and maintain a highway faster

        // Commands
        Commands.add(new ProxyCommand());
        Commands.add(new SwitchCommand());

        // HUD
        Hud.get().register(StatsViewer.INFO);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
        Modules.registerCategory(HELPER_MODULES_CATEGORY);
    }

    @Override
    public String getPackage() {
        return "HWU.group.addon";
    }

    // TODO:Put the actual GitHub repository
    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("username", "HWU-Highway-Builder");
    }
}
