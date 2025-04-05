package HWU.group.addon.helpers;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.starscript.compiler.Expr;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static HWU.group.addon.modules.HWUHighwayBuilder.*;
import static meteordevelopment.meteorclient.MeteorClient.mc;

// TODO:Rewrite the first 2 functions to be coded in the same way that the 3rd function works.
//  Maybe even merge them all into one big function because they all have the same block positions.
// TODO:Add diagonal highway support

public class BlockPosHelper {

    @NotNull
    public static BlockPos[] getBlockPositions() {
        Module HWUHighwayPaver = Modules.get().get("HWU-highway-builder");
        BoolSetting placeRails = (BoolSetting) HWUHighwayPaver.settings.get("place-rails");
        BoolSetting removeY119 = (BoolSetting) HWUHighwayPaver.settings.get("remove-y119");

        if (getPlayerDirection() == null) return new BlockPos[0];
        assert mc.player != null;

        if (playerX == null) playerX = mc.player.getBlockX();
        if (playerY == null) playerY = mc.player.getBlockY();

        Direction direction = getPlayerDirection();

        return switch (direction) {
            case NORTH -> {
                List<BlockPos> positions = new ArrayList<>();

                positions.add(new BlockPos(playerX, playerY - 1, mc.player.getBlockZ() + 3));
                if (placeRails.get()) {
                    positions.add(new BlockPos(playerX - 3, playerY, mc.player.getBlockZ() + 3));
                    positions.add(new BlockPos(playerX + 2, playerY, mc.player.getBlockZ() + 3));
                }
                positions.add(new BlockPos(playerX - 2, playerY - 1, mc.player.getBlockZ() + 3));
                positions.add(new BlockPos(playerX + 1, playerY - 1, mc.player.getBlockZ() + 3));
                positions.add(new BlockPos(playerX - 1, playerY - 1, mc.player.getBlockZ() + 3));

                positions.add(new BlockPos(playerX, playerY - 1, mc.player.getBlockZ() + 2));
                if (placeRails.get()) {
                    positions.add(new BlockPos(playerX - 3, playerY, mc.player.getBlockZ() + 2));
                    positions.add(new BlockPos(playerX + 2, playerY, mc.player.getBlockZ() + 2));
                }
                positions.add(new BlockPos(playerX - 2, playerY - 1, mc.player.getBlockZ() + 2));
                positions.add(new BlockPos(playerX + 1, playerY - 1, mc.player.getBlockZ() + 2));
                positions.add(new BlockPos(playerX - 1, playerY - 1, mc.player.getBlockZ() + 2));

                positions.add(new BlockPos(playerX, playerY - 1, mc.player.getBlockZ() + 1));
                if (placeRails.get()) {
                    positions.add(new BlockPos(playerX - 3, playerY, mc.player.getBlockZ() + 1));
                    positions.add(new BlockPos(playerX + 2, playerY, mc.player.getBlockZ() + 1));
                }
                positions.add(new BlockPos(playerX - 2, playerY - 1, mc.player.getBlockZ() + 1));
                positions.add(new BlockPos(playerX + 1, playerY - 1, mc.player.getBlockZ() + 1));
                positions.add(new BlockPos(playerX - 1, playerY - 1, mc.player.getBlockZ() + 1));

                positions.add(new BlockPos(playerX, playerY - 1, mc.player.getBlockZ()));
                if (placeRails.get()) {
                    positions.add(new BlockPos(playerX - 3, playerY, mc.player.getBlockZ()));
                    positions.add(new BlockPos(playerX + 2, playerY, mc.player.getBlockZ()));
                }
                positions.add(new BlockPos(playerX - 2, playerY - 1, mc.player.getBlockZ()));
                positions.add(new BlockPos(playerX + 1, playerY - 1, mc.player.getBlockZ()));
                positions.add(new BlockPos(playerX - 1, playerY - 1, mc.player.getBlockZ()));

                positions.add(new BlockPos(playerX, playerY - 1, mc.player.getBlockZ() + 3));
                if (placeRails.get()) {
                    positions.add(new BlockPos(playerX - 3, playerY, mc.player.getBlockZ() + 1));
                    positions.add(new BlockPos(playerX + 2, playerY, mc.player.getBlockZ() + 1));
                }
                positions.add(new BlockPos(playerX - 2, playerY - 1, mc.player.getBlockZ() + 1));
                positions.add(new BlockPos(playerX + 1, playerY - 1, mc.player.getBlockZ() + 1));
                positions.add(new BlockPos(playerX - 1, playerY - 1, mc.player.getBlockZ() + 1));

                yield positions.toArray(new BlockPos[0]); // Convert list back to array
            }
            case EAST -> {
                List<BlockPos> positions = new ArrayList<>();

                positions.add(new BlockPos(mc.player.getBlockX() - 3, playerY - 1, playerZ));
                if (placeRails.get()) {
                    positions.add(new BlockPos(mc.player.getBlockX() - 3, playerY, playerZ + 2));
                    positions.add(new BlockPos(mc.player.getBlockX() - 3, playerY, playerZ - 3));
                }
                positions.add(new BlockPos(mc.player.getBlockX() - 3, playerY - 1, playerZ - 2));
                positions.add(new BlockPos(mc.player.getBlockX() - 3, playerY - 1, playerZ - 1));
                positions.add(new BlockPos(mc.player.getBlockX() - 3, playerY - 1, playerZ + 1));

                positions.add(new BlockPos(mc.player.getBlockX() - 2, playerY - 1, playerZ));
                if (placeRails.get()) {
                    positions.add(new BlockPos(mc.player.getBlockX() - 2, playerY, playerZ + 2));
                    positions.add(new BlockPos(mc.player.getBlockX() - 2, playerY, playerZ - 3));
                }
                positions.add(new BlockPos(mc.player.getBlockX() - 2, playerY - 1, playerZ - 2));
                positions.add(new BlockPos(mc.player.getBlockX() - 2, playerY - 1, playerZ - 1));
                positions.add(new BlockPos(mc.player.getBlockX() - 2, playerY - 1, playerZ + 1));

                positions.add(new BlockPos(mc.player.getBlockX() - 1, playerY - 1, playerZ));
                if (placeRails.get()) {
                    positions.add(new BlockPos(mc.player.getBlockX() - 1, playerY, playerZ + 2));
                    positions.add(new BlockPos(mc.player.getBlockX() - 1, playerY, playerZ - 3));
                }
                positions.add(new BlockPos(mc.player.getBlockX() - 1, playerY - 1, playerZ - 2));
                positions.add(new BlockPos(mc.player.getBlockX() - 1, playerY - 1, playerZ - 1));
                positions.add(new BlockPos(mc.player.getBlockX() - 1, playerY - 1, playerZ + 1));

                positions.add(new BlockPos(mc.player.getBlockX(), playerY - 1, playerZ));
                if (placeRails.get()) {
                    positions.add(new BlockPos(mc.player.getBlockX(), playerY, playerZ + 2));
                    positions.add(new BlockPos(mc.player.getBlockX(), playerY, playerZ - 3));
                }
                positions.add(new BlockPos(mc.player.getBlockX(), playerY - 1, playerZ - 2));
                positions.add(new BlockPos(mc.player.getBlockX(), playerY - 1, playerZ - 1));
                positions.add(new BlockPos(mc.player.getBlockX(), playerY - 1, playerZ + 1));

                positions.add(new BlockPos(mc.player.getBlockX() + 1, playerY - 1, playerZ));
                if (placeRails.get()) {
                    positions.add(new BlockPos(mc.player.getBlockX() + 1, playerY, playerZ + 2));
                    positions.add(new BlockPos(mc.player.getBlockX() + 1, playerY, playerZ - 3));
                }
                positions.add(new BlockPos(mc.player.getBlockX() + 1, playerY - 1, playerZ - 2));
                positions.add(new BlockPos(mc.player.getBlockX() + 1, playerY - 1, playerZ - 1));
                positions.add(new BlockPos(mc.player.getBlockX() + 1, playerY - 1, playerZ + 1));

                yield positions.toArray(new BlockPos[0]); // Convert list back to array
            }
            case SOUTH -> {
                List<BlockPos> positions = new ArrayList<>();
                positions.add(new BlockPos(playerX, playerY - 1, mc.player.getBlockZ() - 3));
                if (placeRails.get()) {
                    positions.add(new BlockPos(playerX - 2, playerY, mc.player.getBlockZ() - 3));
                    positions.add(new BlockPos(playerX + 3, playerY, mc.player.getBlockZ() - 3));
                }
                positions.add(new BlockPos(playerX + 2, playerY - 1, mc.player.getBlockZ() - 3));
                positions.add(new BlockPos(playerX + 1, playerY - 1, mc.player.getBlockZ() - 3));
                positions.add(new BlockPos(playerX - 1, playerY - 1, mc.player.getBlockZ() - 3));

                positions.add(new BlockPos(playerX, playerY - 1, mc.player.getBlockZ() - 2));
                if (placeRails.get()) {
                    positions.add(new BlockPos(playerX - 2, playerY, mc.player.getBlockZ() - 2));
                    positions.add(new BlockPos(playerX + 3, playerY, mc.player.getBlockZ() - 2));
                }
                positions.add(new BlockPos(playerX + 2, playerY - 1, mc.player.getBlockZ() - 2));
                positions.add(new BlockPos(playerX + 1, playerY - 1, mc.player.getBlockZ() - 2));
                positions.add(new BlockPos(playerX - 1, playerY - 1, mc.player.getBlockZ() - 2));

                positions.add(new BlockPos(playerX, playerY - 1, mc.player.getBlockZ() - 1));
                if (placeRails.get()) {
                    positions.add(new BlockPos(playerX - 2, playerY, mc.player.getBlockZ() - 1));
                    positions.add(new BlockPos(playerX + 3, playerY, mc.player.getBlockZ() - 1));
                }
                positions.add(new BlockPos(playerX + 2, playerY - 1, mc.player.getBlockZ() - 1));
                positions.add(new BlockPos(playerX + 1, playerY - 1, mc.player.getBlockZ() - 1));
                positions.add(new BlockPos(playerX - 1, playerY - 1, mc.player.getBlockZ() - 1));

                positions.add(new BlockPos(playerX, playerY - 1, mc.player.getBlockZ()));
                if (placeRails.get()) {
                    positions.add(new BlockPos(playerX - 2, playerY, mc.player.getBlockZ()));
                    positions.add(new BlockPos(playerX + 3, playerY, mc.player.getBlockZ()));
                }
                positions.add(new BlockPos(playerX + 2, playerY - 1, mc.player.getBlockZ()));
                positions.add(new BlockPos(playerX + 1, playerY - 1, mc.player.getBlockZ()));
                positions.add(new BlockPos(playerX - 1, playerY - 1, mc.player.getBlockZ()));

                positions.add(new BlockPos(playerX, playerY - 1, mc.player.getBlockZ() + 1));
                if (placeRails.get()) {
                    positions.add(new BlockPos(playerX - 2, playerY, mc.player.getBlockZ() + 1));
                    positions.add(new BlockPos(playerX + 3, playerY, mc.player.getBlockZ() + 1));
                }
                positions.add(new BlockPos(playerX + 2, playerY - 1, mc.player.getBlockZ() + 1));
                positions.add(new BlockPos(playerX + 1, playerY - 1, mc.player.getBlockZ() + 1));
                positions.add(new BlockPos(playerX - 1, playerY - 1, mc.player.getBlockZ() + 1));

                positions.add(new BlockPos(playerX, playerY - 1, mc.player.getBlockZ() + 2));
                if (placeRails.get()) {
                    positions.add(new BlockPos(playerX - 2, playerY, mc.player.getBlockZ() + 2));
                    positions.add(new BlockPos(playerX + 3, playerY, mc.player.getBlockZ() + 2));
                }
                positions.add(new BlockPos(playerX + 2, playerY - 1, mc.player.getBlockZ() + 2));
                positions.add(new BlockPos(playerX + 1, playerY - 1, mc.player.getBlockZ() + 2));
                positions.add(new BlockPos(playerX - 1, playerY - 1, mc.player.getBlockZ() + 2));

                yield positions.toArray(new BlockPos[0]); // Convert list back to array
            }
            case WEST -> {
                List<BlockPos> positions = new ArrayList<>();

                positions.add(new BlockPos(mc.player.getBlockX() + 3, playerY - 1, playerZ));
                if (placeRails.get()) {
                    positions.add(new BlockPos(mc.player.getBlockX() + 3, playerY, playerZ - 2));
                    positions.add(new BlockPos(mc.player.getBlockX() + 3, playerY, playerZ + 3));
                }
                positions.add(new BlockPos(mc.player.getBlockX() + 3, playerY - 1, playerZ + 2));
                positions.add(new BlockPos(mc.player.getBlockX() + 3, playerY - 1, playerZ + 1));
                positions.add(new BlockPos(mc.player.getBlockX() + 3, playerY - 1, playerZ - 1));

                positions.add(new BlockPos(mc.player.getBlockX() + 2, playerY - 1, playerZ));
                if (placeRails.get()) {
                    positions.add(new BlockPos(mc.player.getBlockX() + 2, playerY, playerZ - 2));
                    positions.add(new BlockPos(mc.player.getBlockX() + 2, playerY, playerZ + 3));
                }
                positions.add(new BlockPos(mc.player.getBlockX() + 2, playerY - 1, playerZ + 2));
                positions.add(new BlockPos(mc.player.getBlockX() + 2, playerY - 1, playerZ + 1));
                positions.add(new BlockPos(mc.player.getBlockX() + 2, playerY - 1, playerZ - 1));

                positions.add(new BlockPos(mc.player.getBlockX() + 1, playerY - 1, playerZ));
                if (placeRails.get()) {
                    positions.add(new BlockPos(mc.player.getBlockX() + 1, playerY, playerZ - 2));
                    positions.add(new BlockPos(mc.player.getBlockX() + 1, playerY, playerZ + 3));
                }
                positions.add(new BlockPos(mc.player.getBlockX() + 1, playerY - 1, playerZ + 2));
                positions.add(new BlockPos(mc.player.getBlockX() + 1, playerY - 1, playerZ + 1));
                positions.add(new BlockPos(mc.player.getBlockX() + 1, playerY - 1, playerZ - 1));

                positions.add(new BlockPos(mc.player.getBlockX(), playerY - 1, playerZ));
                if (placeRails.get()) {
                    positions.add(new BlockPos(mc.player.getBlockX(), playerY, playerZ - 2));
                    positions.add(new BlockPos(mc.player.getBlockX(), playerY, playerZ + 3));
                }
                positions.add(new BlockPos(mc.player.getBlockX(), playerY - 1, playerZ + 2));
                positions.add(new BlockPos(mc.player.getBlockX(), playerY - 1, playerZ + 1));
                positions.add(new BlockPos(mc.player.getBlockX(), playerY - 1, playerZ - 1));

                positions.add(new BlockPos(mc.player.getBlockX() - 1, playerY - 1, playerZ));
                if (placeRails.get()) {
                    positions.add(new BlockPos(mc.player.getBlockX() - 1, playerY, playerZ - 2));
                    positions.add(new BlockPos(mc.player.getBlockX() - 1, playerY, playerZ + 3));
                }
                positions.add(new BlockPos(mc.player.getBlockX() - 1, playerY - 1, playerZ + 2));
                positions.add(new BlockPos(mc.player.getBlockX() - 1, playerY - 1, playerZ + 1));
                positions.add(new BlockPos(mc.player.getBlockX() - 1, playerY - 1, playerZ - 1));

                yield positions.toArray(new BlockPos[0]); // Convert list back to array
            }
            default -> new BlockPos[0]; // This shouldn't happen
        };
    }

    @NotNull
    public static BlockPos[] getBlockPositions2() {
        Module HWUHighwayPaver = Modules.get().get("HWU-highway-builder");
        BoolSetting placeRails = (BoolSetting) HWUHighwayPaver.settings.get("place-rails");

        if (getPlayerDirection() == null) return new BlockPos[0];
        assert mc.player != null;

        int ZCoords = mc.player.getBlockZ();
        int XCoords = mc.player.getBlockX();
        if (playerX == null) playerX = mc.player.getBlockX();
        if (playerY == null) playerY = mc.player.getBlockY();

        return switch (getPlayerDirection()) {
            case NORTH -> {
                List<BlockPos> positions = new ArrayList<>();
                for (int i = 4; i <= 7; i++) {
                    int z = ZCoords + i;
                    positions.add(new BlockPos(playerX, playerY - 1, z));
                    positions.add(new BlockPos(playerX - 1, playerY - 1, z));
                    positions.add(new BlockPos(playerX + 1, playerY - 1, z));
                    positions.add(new BlockPos(playerX - 2, playerY - 1, z));
                    if (placeRails.get()) {
                        positions.add(new BlockPos(playerX + 2, playerY, z));
                        positions.add(new BlockPos(playerX - 3, playerY, z));
                    }
                }
                yield positions.toArray(new BlockPos[0]);
            }
            case SOUTH -> {
                List<BlockPos> positions = new ArrayList<>();
                for (int i = 4; i <= 7; i++) {
                    int z = ZCoords - i;
                    positions.add(new BlockPos(playerX, playerY - 1, z));
                    positions.add(new BlockPos(playerX - 1, playerY - 1, z));
                    positions.add(new BlockPos(playerX + 1, playerY - 1, z));
                    positions.add(new BlockPos(playerX + 2, playerY - 1, z));
                    if (placeRails.get()) {
                        positions.add(new BlockPos(playerX - 2, playerY, z));
                        positions.add(new BlockPos(playerX + 3, playerY, z));
                    }
                }
                yield positions.toArray(new BlockPos[0]);
            }
            case EAST -> {
                List<BlockPos> positions = new ArrayList<>();
                for (int i = 4; i <= 7; i++) {
                    positions.add(new BlockPos(mc.player.getBlockX() - 3, playerY - 1, playerZ));
                    positions.add(new BlockPos(mc.player.getBlockX() - 3, playerY - 1, playerZ + 1));
                    positions.add(new BlockPos(mc.player.getBlockX() - 3, playerY - 1, playerZ - 1));
                    positions.add(new BlockPos(mc.player.getBlockX() - 3, playerY - 1, playerZ - 2));
                    if (placeRails.get()) {
                        positions.add(new BlockPos(mc.player.getBlockX() - 3, playerY, playerZ + 2));
                        positions.add(new BlockPos(mc.player.getBlockX() - 3, playerY, playerZ - 3));
                    }
                }
                yield positions.toArray(new BlockPos[0]);
            }
            case WEST -> {
                List<BlockPos> positions = new ArrayList<>();
                for (int i = 4; i <= 7; i++) {
                    int x = XCoords - i;
                    positions.add(new BlockPos(mc.player.getBlockX() - x, playerY - 1, playerZ));
                    positions.add(new BlockPos(mc.player.getBlockX() - x, playerY - 1, playerZ - 1));
                    positions.add(new BlockPos(mc.player.getBlockX() - x, playerY - 1, playerZ + 1));
                    positions.add(new BlockPos(mc.player.getBlockX() - x, playerY - 1, playerZ + 2));
                    if (placeRails.get()) {
                        positions.add(new BlockPos(mc.player.getBlockX() - x, playerY, playerZ - 2));
                        positions.add(new BlockPos(mc.player.getBlockX() - x, playerY, playerZ + 3));
                    }
                }
                yield positions.toArray(new BlockPos[0]);
            }
            default -> new BlockPos[0];
        };
    }

    @NotNull
    public static List<BlockPos> getBlockPositions3() {
        Module HWUHighwayPaver = Modules.get().get("HWU-highway-builder");
        BoolSetting placeRails = (BoolSetting) HWUHighwayPaver.settings.get("place-rails");

        if (getPlayerDirection() == null) return new ArrayList<>();
        assert mc.player != null;

        if (playerX == null) playerX = mc.player.getBlockX();
        if (playerY == null) playerY = mc.player.getBlockY();

        Direction direction = getPlayerDirection();
        List<BlockPos> blockPositions = new ArrayList<>();

        switch (direction) {
            case NORTH :
                for (int i=30; i<=128; i++) {
                    blockPositions.add(new BlockPos(playerX, playerY - 1, mc.player.getBlockZ() - i));
                    blockPositions.add(new BlockPos(playerX - 1, playerY - 1, mc.player.getBlockZ() - i));
                    blockPositions.add(new BlockPos(playerX + 1, playerY - 1, mc.player.getBlockZ() - i));
                    blockPositions.add(new BlockPos(playerX - 2, playerY - 1, mc.player.getBlockZ() - i));
                    if (placeRails.get()) {
                        blockPositions.add(new BlockPos(playerX + 2, playerY, mc.player.getBlockZ() - i));
                        blockPositions.add(new BlockPos(playerX - 3, playerY, mc.player.getBlockZ() - i));
                    }
                }
                break;
            case EAST:
                for (int i=30; i<=128; i++) {
                    blockPositions.add(new BlockPos(mc.player.getBlockX() + i, playerY - 1, playerZ));
                    blockPositions.add(new BlockPos(mc.player.getBlockX() + i, playerY - 1, playerZ + 1));
                    blockPositions.add(new BlockPos(mc.player.getBlockX() + i, playerY - 1, playerZ - 1));
                    blockPositions.add(new BlockPos(mc.player.getBlockX() + i, playerY - 1, playerZ - 2));
                    if (placeRails.get()) {
                        blockPositions.add(new BlockPos(mc.player.getBlockX() + i, playerY, playerZ + 2));
                        blockPositions.add(new BlockPos(mc.player.getBlockX() + i, playerY, playerZ - 3));
                    }
                }
                break;
            case SOUTH:
                for (int i=30; i<=128; i++) {
                    blockPositions.add(new BlockPos(playerX, playerY - 1, mc.player.getBlockZ() + i));
                    blockPositions.add(new BlockPos(playerX - 1, playerY - 1, mc.player.getBlockZ() + i));
                    blockPositions.add(new BlockPos(playerX + 1, playerY - 1, mc.player.getBlockZ() + i));
                    blockPositions.add(new BlockPos(playerX + 2, playerY - 1, mc.player.getBlockZ() + i));
                    if (placeRails.get()) {
                        blockPositions.add(new BlockPos(playerX - 2, playerY, mc.player.getBlockZ() + i));
                        blockPositions.add(new BlockPos(playerX + 3, playerY, mc.player.getBlockZ() + i));
                    }
                }
                break;

            case WEST:
                for (int i=30; i<=128; i++) {
                    blockPositions.add(new BlockPos(mc.player.getBlockX() - i, playerY - 1, playerZ));
                    blockPositions.add(new BlockPos(mc.player.getBlockX() - i, playerY - 1, playerZ - 1));
                    blockPositions.add(new BlockPos(mc.player.getBlockX() - i, playerY - 1, playerZ + 1));
                    blockPositions.add(new BlockPos(mc.player.getBlockX() - i, playerY - 1, playerZ + 2));
                    if (placeRails.get()) {
                        blockPositions.add(new BlockPos(mc.player.getBlockX() - i, playerY, playerZ - 2));
                        blockPositions.add(new BlockPos(mc.player.getBlockX() - i, playerY, playerZ + 3));
                    }
                }
                break;
        }

        return blockPositions;
    }
}
