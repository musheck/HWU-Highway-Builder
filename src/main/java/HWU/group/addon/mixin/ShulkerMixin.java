package HWU.group.addon.mixin;

import HWU.group.addon.modules.HWUHighwayBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static HWU.group.addon.helpers.Utils.debug;

@Mixin(ClientPlayNetworkHandler.class)
public class ShulkerMixin {
    @Inject(method = "onOpenScreen", at = @At("HEAD"))
    private void onOpenScreen(OpenScreenS2CPacket packet, CallbackInfo ci) {
        if (!HWUHighwayBuilder.getIsRestocking()) return;
        MinecraftClient client = MinecraftClient.getInstance();

        client.execute(() -> {
            if (client.player != null) {
                ScreenHandler handler = client.player.currentScreenHandler;
                if (handler instanceof ShulkerBoxScreenHandler) {
                    debug("Opened shulker.");
                    client.setScreen(HWUHighwayBuilder.previousScreen);
                }
            }
        });
    }
}
