package HWU.group.addon.helpers;

import HWU.group.addon.modules.HWUHighwayBuilder;
import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.RichPresence;

import static HWU.group.addon.helpers.Utils.debug;

public class DiscordRPC {
    private static final long CLIENT_ID = 1346947991059300434L;
    private static final RichPresence rpc = new RichPresence();

    public static void startRPC() {
        DiscordIPC.start(CLIENT_ID, null);

        rpc.setStart(System.currentTimeMillis() / 1000L);
        rpc.setLargeImage("https://cdn.discordapp.com/icons/741022089988931706/a_f1bc37c3995a7e6142a78abf5c529c66.gif?size=512", "HWU Highway Builder");

        updateRPC();
    }

    public static void stopRPC() {
        DiscordIPC.stop();
    }

    public static void updateRPC() {
        if (HWUHighwayBuilder.getTicksPassed() <= 20) return;

        rpc.setDetails(getLine1());
        rpc.setState(getLine2());

        DiscordIPC.setActivity(rpc);
        debug("Updated RPC.");
    }

    private static String getLine1() {
        return String.format(
                "Distance: %s | Progress: %s",
                StatsHandler.getDistanceTravelled(),
                StatsHandler.formatPercentage(StatsHandler.calculatePercentage())
        );
    }

    private static String getLine2() {
        return String.format(
                "%d blocks placed | %.2f/s | ETA: %s",
                HWUHighwayBuilder.getObsidianPlaced(),
                formatPlacementsPerSecond(StatsHandler.getPlacementsPerSecond()),
                StatsHandler.formatTime(StatsHandler.getETA())
        );
    }

    private static double placementsPerSecond = 0;

    private static double formatPlacementsPerSecond(double x) {
        if (x > 0) {
            placementsPerSecond = StatsHandler.getPlacementsPerSecond(); // Update placementsPerSecond
        }

        return placementsPerSecond; // Return the last saved distance
    }
}
