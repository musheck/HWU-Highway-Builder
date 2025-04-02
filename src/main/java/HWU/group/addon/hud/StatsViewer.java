package HWU.group.addon.hud;

import HWU.group.addon.HWU_HWBuilder;
import HWU.group.addon.helpers.StatsHandler;
import HWU.group.addon.modules.HWUHighwayBuilder;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class StatsViewer extends HudElement {
    public static final HudElementInfo<StatsViewer> INFO = new HudElementInfo<>(HWU_HWBuilder.HUD_GROUP, "stats-viewer", "View your stats while paving.", StatsViewer::new);

    public StatsViewer() {
        super(INFO);
    }

    private String[][] getLines() {
        return new String[][]{
                {"Time passed", StatsHandler.formatTime(HWUHighwayBuilder.getTicksPassed())},
                {"Distance travelled", String.valueOf(StatsHandler.getDistanceTravelled())},
                {"Distance left", String.valueOf(StatsHandler.getDistanceLeft())},
                {"Blocks placed", String.valueOf(HWUHighwayBuilder.getObsidianPlaced())},
                {"Placements per second", StatsHandler.formatPlacementsPerSecond(StatsHandler.getPlacementsPerSecond())},
                {"Progress", StatsHandler.formatPercentage(StatsHandler.calculatePercentage())},
                {"ETA", StatsHandler.formatTime(StatsHandler.getETA())}
        };
    }

    @Override
    public void render(HudRenderer renderer) {
        if (!HWUHighwayBuilder.getEnableHUD()) return;

        String[][] lines = getLines();

        double lineHeight = renderer.textHeight(true);
        double width = 0;

        // Calculate maximum width of title-value pairs
        for (String[] line : lines) {
            double lineWidth = renderer.textWidth(line[0], true) + renderer.textWidth(": ", true) + renderer.textWidth(line[1], true);
            width = Math.max(width, lineWidth);
        }

        setSize(width, lineHeight * lines.length);

        // Render each line
        double currentY = y;
        for (String[] line : lines) {
            double currentX = x;

            renderer.text(line[0], currentX, currentY, Color.WHITE, true);
            currentX += renderer.textWidth(line[0], true);

            renderer.text(": ", currentX, currentY, Color.WHITE, true);
            currentX += renderer.textWidth(": ", true);

            // Handle the progress line differently
            if (line[0].equals("Progress")) {
                String percentageText = line[1];
                String doneText = " done";

                double percentageWidth = renderer.textWidth(percentageText, true);
                renderer.textWidth(doneText, true);

                renderer.text(percentageText, currentX, currentY, Color.RED, true);
                currentX += percentageWidth;

                renderer.text(doneText, currentX, currentY, Color.WHITE, true);
            } else {
                renderer.text(line[1], currentX, currentY, Color.RED, true);
            }

            currentY += lineHeight;  // Move to the next line
        }
    }
}
