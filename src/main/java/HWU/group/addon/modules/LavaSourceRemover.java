/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 *
 * Edited by HWU Group (2b2t).
 */

package HWU.group.addon.modules;

import HWU.group.addon.HWU_HWBuilder;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluids;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.BlockItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static HWU.group.addon.helpers.Utils.airPlace;

public class LavaSourceRemover extends Module {
    private final SettingGroup sgGeneral  = settings.getDefaultGroup();
    private final SettingGroup sgWhitelist = settings.createGroup("Whitelist");

    public static boolean isRemovingLavaSources() {
        return isRemovingLavaSources;
    }

    public static boolean isRemovingLavaSources = false;

    private final Setting<Shape> shape = sgGeneral.add(new EnumSetting.Builder<Shape>()
            .name("shape")
            .description("The shape of placing algorithm.")
            .defaultValue(Shape.Sphere)
            .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("The place range.")
            .defaultValue(4.5)
            .min(0)
            .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("Delay between actions in ticks.")
            .defaultValue(0)
            .min(0)
            .build()
    );

    private final Setting<Integer> maxBlocksPerTick = sgGeneral.add(new IntSetting.Builder()
            .name("max-blocks-per-tick")
            .description("Maximum blocks to try to place per tick.")
            .defaultValue(1)
            .min(1)
            .sliderRange(1, 10)
            .build()
    );

    private final Setting<SortMode> sortMode = sgGeneral.add(new EnumSetting.Builder<SortMode>()
            .name("sort-mode")
            .description("The blocks you want to place first.")
            .defaultValue(SortMode.Closest)
            .build()
    );

    // Whitelist and blacklist
    private final Setting<ListMode> listMode = sgWhitelist.add(new EnumSetting.Builder<ListMode>()
            .name("list-mode")
            .description("Selection mode.")
            .defaultValue(ListMode.Whitelist)
            .build()
    );

    private final Setting<List<Block>> whitelist = sgWhitelist.add(new BlockListSetting.Builder()
            .name("whitelist")
            .description("The allowed blocks that it will use to fill up the lava.")
            .defaultValue(
                    Blocks.NETHERRACK,
                    Blocks.COBBLESTONE,
                    Blocks.DIRT,
                    Blocks.BLACKSTONE,
                    Blocks.OBSIDIAN
            )
            .visible(() -> listMode.get() == ListMode.Whitelist)
            .build()
    );

    private final Setting<List<Block>> blacklist = sgWhitelist.add(new BlockListSetting.Builder()
            .name("blacklist")
            .description("The denied blocks that it not will use to fill up the lava.")
            .visible(() -> listMode.get() == ListMode.Blacklist)
            .build()
    );

    private final List<BlockPos.Mutable> blocks = new ArrayList<>();
    private int timer;

    public LavaSourceRemover() {
        super(HWU_HWBuilder.HELPER_MODULES_CATEGORY, "lava-source-remover", "Places blocks inside of lava source blocks within range of you.");
    }

    @Override
    public void onActivate() {
        timer = 0;
        isRemovingLavaSources = false;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (HWUKillAura.isAttacking()) return;

        // Reset source removing flag at start of tick
        isRemovingLavaSources = false;

        // Update timer according to delay
        if (timer < delay.get()) {
            timer++;
            return;
        } else {
            timer = 0;
        }

        // Calculate some stuff
        double pX = mc.player.getX();
        double pY = mc.player.getY();
        double pZ = mc.player.getZ();

        double rangeSq = Math.pow(range.get(), 2);

        if (shape.get() == Shape.UniformCube) range.set((double) Math.round(range.get()));

        // Find slot with a block
        FindItemResult item;
        if (listMode.get() == ListMode.Whitelist) {
            item = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BlockItem && whitelist.get().contains(Block.getBlockFromItem(itemStack.getItem())));
        } else {
            item = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BlockItem && !blacklist.get().contains(Block.getBlockFromItem(itemStack.getItem())));
        }

        if (!item.found()) return;

        // Loop blocks around the player
        BlockIterator.register((int) Math.ceil(range.get()+1), (int) Math.ceil(range.get()), (blockPos, blockState) -> {
            boolean tooFarSphere = Utils.squaredDistance(pX, pY, pZ, blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) > rangeSq;
            boolean tooFarUniformCube = maxDist(Math.floor(pX), Math.floor(pY), Math.floor(pZ), blockPos.getX(), blockPos.getY(), blockPos.getZ()) >= range.get();

            // Check distance
            if ((tooFarSphere && shape.get() == Shape.Sphere) || (tooFarUniformCube && shape.get() == Shape.UniformCube)) return;

            // Check if the block is a lava source block
            FluidState fluidState = blockState.getFluidState();
            if (fluidState.getFluid() != Fluids.LAVA || !fluidState.isStill()) return;

            isRemovingLavaSources = true;

            // Add block
            blocks.add(blockPos.mutableCopy());
        });

        BlockIterator.after(() -> {
            // Sort blocks
            if (sortMode.get() == SortMode.TopDown || sortMode.get() == SortMode.BottomUp)
                blocks.sort(Comparator.comparingDouble(value -> value.getY() * (sortMode.get() == SortMode.BottomUp ? 1 : -1)));
            else if (sortMode.get() != SortMode.None)
                blocks.sort(Comparator.comparingDouble(value -> Utils.squaredDistance(pX, pY, pZ, value.getX() + 0.5, value.getY() + 0.5, value.getZ() + 0.5) * (sortMode.get() == SortMode.Closest ? 1 : -1)));

            // Place and clear place positions
            int count = 0;
            for (BlockPos pos : blocks) {
                if (count >= maxBlocksPerTick.get()) break;

                InvUtils.swap(item.slot(), false);
                airPlace(pos, Direction.DOWN);
                count++;
            }
            blocks.clear();
        });
    }

    public enum ListMode {
        Whitelist,
        Blacklist
    }

    public enum SortMode {
        None,
        Closest,
        TopDown,
        BottomUp
    }

    public enum Shape {
        Sphere,
        UniformCube
    }

    private static double maxDist(double x1, double y1, double z1, double x2, double y2, double z2) {
        // Gets the largest X, Y or Z difference, manhattan style
        double dX = Math.ceil(Math.abs(x2 - x1));
        double dY = Math.ceil(Math.abs(y2 - y1));
        double dZ = Math.ceil(Math.abs(z2 - z1));
        return Math.max(Math.max(dX, dY), dZ);
    }
}
