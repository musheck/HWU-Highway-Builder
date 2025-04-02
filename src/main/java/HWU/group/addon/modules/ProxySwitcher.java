package HWU.group.addon.modules;

import HWU.group.addon.config.ProxyConfig;
import meteordevelopment.meteorclient.events.entity.player.InteractEntityEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import HWU.group.addon.HWU_HWBuilder;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;

import java.util.List;

import static meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerManager.getFakePlayers;

public class ProxySwitcher extends Module {
    private static final ProxyConfig proxyConfig = new ProxyConfig();
    private long lastClickTime = 0;
    private static final long DOUBLE_CLICK_DELAY = 500;

    public ProxySwitcher() {
        super(HWU_HWBuilder.CATEGORY, "proxy-switcher", "Switch between accounts by shifting and holding right-click on a player.");
    }

    @EventHandler
    private void onInteractEntity(InteractEntityEvent event) {
        ClientPlayerEntity player = mc.player;
        if (event.hand == Hand.MAIN_HAND) {
            assert player != null;
            if (player.isSneaking() && event.entity instanceof PlayerEntity targetPlayer) {

                List<FakePlayerEntity> fakePlayers = getFakePlayers();
                for (FakePlayerEntity fakePlayer : fakePlayers) {
                    if (fakePlayer.getName().getString().equals(targetPlayer.getName().getString())) {
                        return;
                    }
                }

                long currentTime = System.currentTimeMillis();

                ChatUtils.info("Keep holding right click to switch proxies!");

                if (currentTime - lastClickTime <= DOUBLE_CLICK_DELAY) {
                    String targetPlayerName = targetPlayer.getName().getString();

                    System.out.println("Attempting to switch to player " + targetPlayerName);

                    handleAccountSwitch(targetPlayerName);
                }

                lastClickTime = currentTime;
            }
        }
    }

    public static void handleAccountSwitch(String playerName) {
        List<String> proxies = proxyConfig.listProxies();
        System.out.println(proxies);

        for (String proxyInfo : proxies) {
            if (proxyInfo.startsWith(playerName + ":")) {
                String[] parts = proxyInfo.split(": ");
                String ipPort = parts[1];

                String[] ipPortParts = ipPort.split(":");
                String ip = ipPortParts[0];

                String port = ipPortParts.length > 1 ? ipPortParts[1] : "25565";

                ChatUtils.sendPlayerMsg("/transfer " + ip + " " + port);
                System.out.println("Switched to " + playerName + ": " + ip + ":" + port);
                return;
            }
        }

        ChatUtils.errorPrefix("Proxy Switcher", "No proxy found for player: " + playerName);
    }
}
