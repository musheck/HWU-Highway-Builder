package HWU.group.addon.modules;

import HWU.group.addon.HWU_HWBuilder;
import meteordevelopment.meteorclient.systems.modules.Module;

public class CoordsHider extends Module {
    static boolean isEnabled;

    public CoordsHider() {
        super(HWU_HWBuilder.HELPER_MODULES_CATEGORY, "coords-hider", "Hide coordinates in the chat! This can be helpful if you have a module that shows your coordinates in the chat.    This module does NOT stop you from posting coordinates in chat, use BetterChat by meteor for that purpose.");
    }

    @Override
    public void onActivate() {
        isEnabled = true;
    }

    @Override
    public void onDeactivate() {
        isEnabled = false;
    }

    public static boolean getIsEnabled() {
        return isEnabled;
    }
}
