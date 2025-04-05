/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 *
 * Edited by HWU Group (2b2t).
 */

package HWU.group.addon.modules;

import meteordevelopment.meteorclient.events.entity.player.BlockBreakingCooldownEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import HWU.group.addon.HWU_HWBuilder;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.*;

import static HWU.group.addon.helpers.Utils.switchToBestTool;
import static HWU.group.addon.modules.HWUHighwayBuilder.*;
import static HWU.group.addon.modules.HWUKillAura.isAttacking;
import static HWU.group.addon.modules.LavaSourceRemover.isRemovingLavaSources;
import static meteordevelopment.meteorclient.utils.world.BlockUtils.getPlaceSide;

// TODO:Add diagonal highway support
//  Maybe type out the individual block positions in BlockPosHelper or look at Meteor's code for some inspiration

public class HWUNuker extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgWhitelist = settings.createGroup("Whitelist");
    private final MinecraftClient mc = MinecraftClient.getInstance();
    static boolean isBreaking;

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay in ticks between breaking blocks.")
        .defaultValue(1)
        .build()
    );

    private final Setting<Integer> maxBlocksPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("max-blocks-per-tick")
        .description("Maximum blocks to try to break per tick. Useful when insta mining.")
        .defaultValue(1)
        .min(1)
        .sliderRange(1, 6)
        .build()
    );

    private final Setting<Boolean> swingHand = sgGeneral.add(new BoolSetting.Builder()
        .name("swing-hand")
        .description("Swing hand client side.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> packetMine = sgGeneral.add(new BoolSetting.Builder()
        .name("packet-mine")
        .description("Attempt to instamine everything at once.")
        .defaultValue(false)
        .build()
    );

    private final Setting<ListMode> listMode = sgWhitelist.add(new EnumSetting.Builder<ListMode>()
        .name("list-mode")
        .description("Selection mode.")
        .defaultValue(ListMode.Blacklist)
        .build()
    );

    private final Setting<List<Block>> blacklist = sgWhitelist.add(new BlockListSetting.Builder()
            .name("blacklist")
            .description("The blocks you don't want to mine.")
            .defaultValue(List.of(
                    Blocks.BEDROCK,
                    Blocks.END_PORTAL_FRAME,
                    Blocks.NETHER_PORTAL,
                    Blocks.END_PORTAL,
                    Blocks.OAK_SIGN,

                    // Signs
                    Blocks.SPRUCE_SIGN,
                    Blocks.BIRCH_SIGN,
                    Blocks.JUNGLE_SIGN,
                    Blocks.ACACIA_SIGN,
                    Blocks.DARK_OAK_SIGN,
                    Blocks.CRIMSON_SIGN,
                    Blocks.WARPED_SIGN,
                    Blocks.OAK_WALL_SIGN,
                    Blocks.SPRUCE_WALL_SIGN,
                    Blocks.BIRCH_WALL_SIGN,
                    Blocks.JUNGLE_WALL_SIGN,
                    Blocks.ACACIA_WALL_SIGN,
                    Blocks.DARK_OAK_WALL_SIGN,
                    Blocks.CRIMSON_WALL_SIGN,
                    Blocks.WARPED_WALL_SIGN
            ))
            .visible(() -> listMode.get() == ListMode.Blacklist)
            .build()
    );

    private final Setting<List<Block>> whitelist = sgWhitelist.add(new BlockListSetting.Builder()
        .name("whitelist")
        .description("The blocks you want to mine.")
        .visible(() -> listMode.get() == ListMode.Whitelist)
        .build()
    );

    private final List<BlockPos> blocks = new ArrayList<>();
    private boolean firstBlock;
    private final BlockPos.Mutable lastBlockPos = new BlockPos.Mutable();
    private int timer;
    private int noBlockTimer;
    private final ShapeMode shapeModeBreak = getRenderMineShape();
    private final Color lineColor = getRenderMineLineColor();
    private final Color sideColor = getRenderMineSideColor();

    public static boolean getIsBreaking() {
        return isBreaking;
    }

    public static void setIsBreaking(boolean value) {
        isBreaking = value;
    }

    Module HWUHighwayBuilder = Modules.get().get("HWU-highway-builder");
    BoolSetting rotate = (BoolSetting) HWUHighwayBuilder.settings.get("rotate");
    BoolSetting removeY119 = (BoolSetting) HWUHighwayBuilder.settings.get("remove-y119");


    public HWUNuker() {
        super(HWU_HWBuilder.HELPER_MODULES_CATEGORY, "HWU-nuker", "A helper module that cleans/digs a highway.");
    }

    private BlockPos getRegion1Start() {
        if (getPlayerDirection() == null) return new BlockPos(0, 64, 0);
        if (mc.player == null) return null;

        return switch (getPlayerDirection()) {
            case NORTH -> new BlockPos(playerX + 1, playerY, mc.player.getBlockZ() - 1);
            case SOUTH -> new BlockPos(playerX - 1, playerY, mc.player.getBlockZ() + 1);
            case EAST -> new BlockPos(mc.player.getBlockX() + 1, playerY, playerZ + 1);
            case WEST -> new BlockPos(mc.player.getBlockX() - 1, playerY, playerZ - 1);
            default -> null; // This shouldn't happen
        };
    }

    private BlockPos getRegion1End() {
        if (getPlayerDirection() == null) return new BlockPos(0, 64, 0);
        if (mc.player == null) return null;

        return switch (getPlayerDirection()) {
            case NORTH -> new BlockPos(playerX - 2, playerY, mc.player.getBlockZ() + 1);
            case SOUTH -> new BlockPos(playerX + 2, playerY, mc.player.getBlockZ() - 1);
            case EAST -> new BlockPos(mc.player.getBlockX() - 1, playerY, playerZ - 2);
            case WEST -> new BlockPos(mc.player.getBlockX() + 1, playerY, playerZ + 2);
            default -> null; // This shouldn't happen
        };
    }

    private BlockPos getRegion2Start() {
        if (getPlayerDirection() == null) return new BlockPos(0, 64, 0);
        if (mc.player == null) return null;

        return switch (getPlayerDirection()) {
            case NORTH -> new BlockPos(playerX + 2, playerY + 1, mc.player.getBlockZ() - 1);
            case SOUTH -> new BlockPos(playerX - 2, playerY + 1, mc.player.getBlockZ() + 1);
            case EAST -> new BlockPos(mc.player.getBlockX() + 1, playerY + 1, playerZ + 2);
            case WEST -> new BlockPos(mc.player.getBlockX() - 1, playerY + 1, playerZ - 2);
            default -> null; // This shouldn't happen
        };
    }

    private BlockPos getRegion2End() {
        if (getPlayerDirection() == null) return new BlockPos(0, 64, 0);
        if (mc.player == null) return null;

        int offset;
        if (isPaveHighway())
            offset = 2;
        else
            offset = 3;

        return switch (getPlayerDirection()) {
            case NORTH -> new BlockPos(playerX - 3, playerY + offset, mc.player.getBlockZ() + 1);
            case SOUTH -> new BlockPos(playerX + 3, playerY + offset, mc.player.getBlockZ() - 1);
            case EAST -> new BlockPos(mc.player.getBlockX() - 1, playerY + offset, playerZ - 3);
            case WEST -> new BlockPos(mc.player.getBlockX() + 1, playerY + offset, playerZ + 3);
            default -> null; // This shouldn't happen
        };
    }


    private boolean isInRegion(BlockPos pos, BlockPos regionStart, BlockPos regionEnd) {
        if (regionStart == null || regionEnd == null) return false;
        int minX = Math.min(regionStart.getX(), regionEnd.getX());
        int maxX = Math.max(regionStart.getX(), regionEnd.getX());
        int minY = Math.min(regionStart.getY(), regionEnd.getY());
        int maxY = Math.max(regionStart.getY(), regionEnd.getY());
        int minZ = Math.min(regionStart.getZ(), regionEnd.getZ());
        int maxZ = Math.max(regionStart.getZ(), regionEnd.getZ());

        return pos.getX() >= minX && pos.getX() <= maxX &&
            pos.getY() >= minY && pos.getY() <= maxY &&
            pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    private boolean isInAnyRegion(BlockPos pos) {
        return isInRegion(pos, getRegion1Start(), getRegion1End()) ||
            isInRegion(pos, getRegion2Start(), getRegion2End());
    }

    @Override
    public void onActivate() {
        firstBlock = true;
        timer = 0;
        noBlockTimer = 0;
    }

    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        if (getIsRestocking()
                || getWasRestocking()
                || getIsPostRestocking()
                || getIsPlacingShulker()
                || getTest()
                || Modules.get().get("better-EChest-farmer").isActive()
                || isAttacking()
                || isRemovingLavaSources()) return;

        if (playerX == null || playerY == null || playerZ == null) return;

        if (timer > 0) {
            timer--;
            return;
        }

        if (mc.player == null) return;

        BlockPos pos1 = getRegion1Start();
        BlockPos pos2 = getRegion1End();
        BlockPos pos3 = getRegion2Start();
        BlockPos pos4 = getRegion2End();

        if (pos1 == null || pos2 == null || pos3 == null || pos4 == null) return;

        int maxWidth = Math.max(
            Math.abs(pos2.getX() - pos1.getX()),
            Math.abs(pos4.getX() - pos3.getX())
        ) + 1;

        int maxHeight = Math.max(
            Math.abs(pos2.getY() - pos1.getY()),
            Math.abs(pos4.getY() - pos3.getY())
        ) + 1;

        BlockIterator.register(maxWidth, maxHeight, (blockPos, blockState) -> {
            if (!isInAnyRegion(blockPos)) return;
            if (!BlockUtils.canBreak(blockPos, blockState)) return;

            if (listMode.get() == ListMode.Whitelist && !whitelist.get().contains(blockState.getBlock())) return;
            if (listMode.get() == ListMode.Blacklist && blacklist.get().contains(blockState.getBlock())) return;

            blocks.add(blockPos.toImmutable());
        });

        BlockIterator.after(this::processBlocks);
    }

    private void processBlocks() {
        if (blocks.isEmpty()) {
            if (noBlockTimer++ >= delay.get()) {
                firstBlock = true;
                setIsBreaking(false);
            }
            return;
        } else {
            noBlockTimer = 0;
        }

        if (!firstBlock && !lastBlockPos.equals(blocks.getFirst())) {
            timer = delay.get();
            firstBlock = false;
            lastBlockPos.set(blocks.getFirst());
            if (timer > 0) return;
        }

        int count = 0;
        for (BlockPos block : blocks) {
            if (count >= maxBlocksPerTick.get()) break;
            boolean canInstaMine = BlockUtils.canInstaBreak(block);

            if (rotate.get()) Rotations.rotate(Rotations.getYaw(block), Rotations.getPitch(block), () -> breakBlock(block));
            else breakBlock(block);
            // TODO:Add a configurable delay (I think 1 tick should be enough) before continuing to the next block so the player doesn't rubberband

            if (getRenderBreaking())
                RenderUtils.renderTickingBlock(block, sideColor, lineColor, shapeModeBreak, 0, 8, true, false);

            lastBlockPos.set(block);

            count++;
            if (!canInstaMine && !packetMine.get()) break;
        }

        firstBlock = false;
        blocks.clear();
    }

    private void breakBlock(BlockPos blockPos) {
        assert mc.interactionManager != null;
        switchToBestTool(blockPos);

        // performs a left click on a block to update it ensuring its not a ghost block...
        Vec3d hitPos = Vec3d.ofCenter(blockPos);
        Direction face = Direction.UP;
        BlockHitResult hitResult = new BlockHitResult(hitPos, face, blockPos, false);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);

        setIsBreaking(true);
        if (packetMine.get()) {
            Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, BlockUtils.getDirection(blockPos)));
            if (swingHand.get()) {
                assert mc.player != null;
                mc.player.swingHand(Hand.MAIN_HAND);
            }

            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, BlockUtils.getDirection(blockPos)));
        } else {
            BlockUtils.breakBlock(blockPos, swingHand.get());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onBlockBreakingCooldown(BlockBreakingCooldownEvent event) {
        event.cooldown = 0;
    }

    public enum ListMode {
        Whitelist,
        Blacklist
    }
}
