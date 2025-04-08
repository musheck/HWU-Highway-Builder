package HWU.group.addon.helpers;

import HWU.group.addon.modules.HWUHighwayBuilder;
import HWU.group.addon.modules.HWUPaver;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.util.math.Direction;

import static HWU.group.addon.modules.HWUPaver.getNonObsidianBlocksCount;
import static HWU.group.addon.modules.HWUHighwayBuilder.*;
import static meteordevelopment.meteorclient.MeteorClient.mc;

// TODO:Rewrite stats to make it work for all directions, no matter where the player is at
// TODO:Add diagonal highway support

public class StatsHandler {
    public static double calculatePercentage() {
        return Math.abs(((double) (50000 - getDistanceLeft()) / 50000) * 100);
    }

    public static int distanceTravelled = 0;

    public static int getDistance() {
        Direction direction = getPlayerDirection();
        assert mc.player != null;
        int distanceTravelled = 0;

        if (direction == Direction.NORTH || direction == Direction.SOUTH) {
            distanceTravelled = Math.abs(mc.player.getBlockZ()) - Math.abs(playerZ);
        } else if (direction == Direction.EAST || direction == Direction.WEST) {
            distanceTravelled = Math.abs(mc.player.getBlockX()) - Math.abs(playerX);
        }
        return Math.abs(distanceTravelled);
    }

    public static int getDistanceTravelled() {
        if (Modules.get().get("HWU-Paver").isActive()) {
            distanceTravelled = getDistance(); // Update distanceTravelled
        }
        return distanceTravelled; // Return the last saved distance
    }

    public static double getPlacementsPerSecond() {
        if (!Modules.get().get("HWU-highway-builder").isActive()) return -1.0;

        double obsidianPlaced = HWUHighwayBuilder.getObsidianPlaced();
        double ticksPassed = HWUHighwayBuilder.getTicksPassed();

        if (obsidianPlaced == 0 || ticksPassed == 0) return 0.0; // Avoid division by zero

        return obsidianPlaced / (ticksPassed / 20.0);
    }

    public static long getETA() {
        long remainingBlocks = getDistanceLeft() * 6L;
        long placementsPerSecond = (long) getPlacementsPerSecond();

        if (placementsPerSecond <= 0) return (long) -2.0; // To display "Waiting..."
        if (HWUHighwayBuilder.getTicksPassed() <= 120 && placementsPerSecond <= 50) return (long) -1.0; // To display "Calculating..."

        return ((long) ((double) remainingBlocks / placementsPerSecond) * 20);
    }

    // TODO:Replace with distance left based on the materials the user has (only if paving).
    public static int getDistanceLeft() {
        assert mc.player != null;
        Direction direction = getPlayerDirection();
        int blockCoordinate;

        switch (direction) {
            case NORTH, SOUTH -> blockCoordinate = mc.player.getBlockZ();
            case EAST, WEST -> blockCoordinate = mc.player.getBlockX();
            default -> blockCoordinate = 0;
        }

        int sectionStart = Math.floorDiv(blockCoordinate, 50000) * 50000;
        int sectionEnd = sectionStart + 50000;

        // Reverse the sections for SOUTH and WEST
        if (direction == Direction.SOUTH || direction == Direction.WEST) {
            sectionStart = sectionEnd;
        }

        int blocksLeft = sectionStart - blockCoordinate;

        return Math.abs(blocksLeft);
    }

    public static String formatPercentage(double percentage) {
        return String.format("%.2f", percentage) + "%";
    }

    public static String formatPlacementsPerSecond(double placementsPerSecond) {
        if (placementsPerSecond == -1.0) return "Waiting...";

        return String.format("%.2f blocks / s", placementsPerSecond);
    }

    public static String formatTime(long ticks) {
        if (ticks == -1) return "Calculating...";
        if (ticks == -2) return "Waiting...";

        long totalSeconds = ticks / 20;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%d:%02d:%02d", hours, minutes, seconds);
    }
}
