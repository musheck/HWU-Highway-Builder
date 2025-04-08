package HWU.group.addon.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import HWU.group.addon.HWU_HWBuilder;
import org.jetbrains.annotations.NotNull;
import static HWU.group.addon.helpers.Utils.isGatheringItems;
import static HWU.group.addon.modules.HWUHighwayBuilder.*;
import static HWU.group.addon.modules.HWUAutoEat.getIsEating;
import static HWU.group.addon.modules.HWUKillAura.isAttacking;
import static HWU.group.addon.modules.HWUNuker.getIsBreaking;
import static HWU.group.addon.modules.LavaSourceRemover.isRemovingLavaSources;

public class HWUAutoWalk extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public static boolean isStopping = false;

    public HWUAutoWalk() {
        super(HWU_HWBuilder.HELPER_MODULES_CATEGORY, "HWU-auto-walk", "Automatically walks forward.");
    }

    @Override
    public void onDeactivate() {
        releaseForward();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        assert mc.player != null;
        Module betterEChestFarmer = Modules.get().get("better-EChest-farmer");
        Module HWUPaver = Modules.get().get("HWU-Paver");

        if(betterEChestFarmer.isActive()
                || isGatheringItems()
                || getIsBreaking()
                || getIsEating()
                || getTest()
                || isAttacking()
                || (isRemovingLavaSources() && Modules.get().get("lava-source-remover").isActive())
                || isStopping // Some clients deal with pressForward() as a toggleable function, not a function that needs to be called on every tick
                              // Without this, some clients may face issues like the player not stopping when it needs to
        ) {
            releaseForward();
            return;
        }

        if (HWUPaver.isActive() && getIsPausing() && isPaveHighway()) return;
        if (!HWUPaver.isActive() && isPaveHighway()) return;

        // TODO:Stop if there's a gap in front of the player so the player doesn't fall
        //  Maybe take inspiration from the distanceBetweenPlayerAndEndOfPavement() function in the "HWUPaver" class.

        // TODO:Add Grim Scaffold or regular scaffold to scaffold over air

        pressForward();
    }

    private void pressForward() {
        setKeyPressed(mc.options.forwardKey, true);
    }

    public static void releaseForward() {
        setKeyPressed(mc.options.forwardKey, false);
    }

    private static void setKeyPressed(@NotNull KeyBinding key, boolean pressed) {
        key.setPressed(pressed);
        Input.setKeyState(key, pressed);
    }
}
