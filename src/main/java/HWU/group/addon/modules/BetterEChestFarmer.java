/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 *
 * Edited by HWU Group (2b2t).
 */

package HWU.group.addon.modules;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalBlock;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.PacketMine;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import HWU.group.addon.HWU_HWBuilder;

import static HWU.group.addon.helpers.Utils.*;
import static HWU.group.addon.modules.HWUHighwayBuilder.*;
import static HWU.group.addon.modules.HWUAutoEat.getIsEating;
import static HWU.group.addon.modules.HWUKillAura.isAttacking;
import static HWU.group.addon.modules.LavaSourceRemover.isRemovingLavaSources;

public class BetterEChestFarmer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    BlockPos startPos = null;

    private final Setting<Boolean> selfToggle = sgGeneral.add(new BoolSetting.Builder()
        .name("self-toggle")
        .description("Disables when you reach the desired amount of ender chests.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> amount = sgGeneral.add(new IntSetting.Builder()
        .name("amount")
        .description("The amount of ender chests to farm.")
        .defaultValue(64)
        .range(1, 280)
        .sliderRange(1, 280)
        .visible(selfToggle::get)
        .build()
    );

    // Render settings
    private final Setting<Boolean> swingHand = sgRender.add(new BoolSetting.Builder()
        .name("swing-hand")
        .description("Swing hand client-side.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders a block overlay where the ender chest will be placed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The color of the sides of the blocks being rendered.")
        .defaultValue(new SettingColor(204, 0, 0, 50))
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The color of the lines of the blocks being rendered.")
        .defaultValue(new SettingColor(204, 0, 0, 255))
        .build()
    );

    private final VoxelShape SHAPE = Block.createCuboidShape(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D);
    public static BlockPos target;
    int tickCounter = 0;
    int resetTickCounter = 0;
    private int startCount;
    boolean isPause;
    boolean rebreakActivated;

    Module HWUHighwayBuilder = Modules.get().get("HWU-highway-builder");
    BoolSetting useInstantRebreak = (BoolSetting) HWUHighwayBuilder.settings.get("use-instant-rebreak");
    Module instantRebreak = Modules.get().get("instant-rebreak");

    public BetterEChestFarmer() {
        super(HWU_HWBuilder.HELPER_MODULES_CATEGORY, "better-EChest-farmer", "An ender-chest farmer that counts the broken ender chests instead of collected obsidian.");
    }

    @Override
    public void onDeactivate() {
        cancelCurrentProcessBaritone();
        InvUtils.swapBack();
        setTest(false);
        isPause = false;
        baritoneCalled = false;
    }

    private boolean initialSetupDone = false;

    @Override
    public void onActivate() {
        target = null;
        startCount = 0;
        initialSetupDone = false;
        tickCounter = 0;
        resetTickCounter = 0;
        startPos = null;
        rebreakActivated = false;
    }

    public static BlockPos getTarget() {
        return target;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {

        if (isRemovingLavaSources()) return;

        assert mc.player != null;
        assert mc.world != null;
        togglePaver(false);
        setKeyPressed(mc.options.forwardKey, false);
        setKeyPressed(mc.options.backKey, false);
        setKeyPressed(mc.options.rightKey, false);
        setKeyPressed(mc.options.leftKey, false);
        
        if (getIsEating()) {
            debug("Pausing -> Eating.");
            return;
        }

        if (isAttacking()) {
            debug("Pausing -> Attacking.");
            return;
        }

        if (!initialSetupDone) {
            startPos = mc.player.getBlockPos();

            if (startPos == null) {
                debug("Pausing because of invalid startPos.");
                return;
            }

            if (getPlayerDirection() == null) return;

            // TODO:Add diagonal highway support
            Direction playerDir = getPlayerDirection();

            switch (playerDir) {
                case NORTH -> target = new BlockPos(playerX, playerY, mc.player.getBlockZ() + 2);
                case SOUTH -> target = new BlockPos(playerX, playerY, mc.player.getBlockZ() - 2);
                case EAST -> target = new BlockPos(mc.player.getBlockX() - 2, playerY, playerZ);
                case WEST -> target = new BlockPos(mc.player.getBlockX() + 2, playerY, playerZ);
            }

            debug("Player direction: %s, current position: x: %s y: %s z: %s", playerDir.getName(), target.getX(), target.getY(), target.getZ());
            initialSetupDone = true;
        }

        switchToBestTool(target);

        if (!startPos.equals(mc.player.getBlockPos())) {
            if (!isPause) {
                System.out.println("Pausing because isPause=false.");
                isPause = true;
                return;
            }

            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(startPos));
            resumeBaritone();
            System.out.println("Baritone call number 3.");
        } else
            isPause = false;


        if (isPause) {
            System.out.println("Pausing because isPause=true.");
            return;
        }

        if (selfToggle.get() && startCount >= amount.get()) {
            debug("Mining last enderchest...");
            BlockUtils.breakBlock(target, swingHand.get());
            if (instantRebreak.isActive()) { instantRebreak.toggle(); }
            toggle();
            return;
        }

        if (target == null) {
            if (instantRebreak.isActive()) {instantRebreak.toggle(); }
            toggle();
            return;
        }

        if (mc.world.getBlockState(target).isReplaceable()) {
            FindItemResult echest = InvUtils.findInHotbar(Items.ENDER_CHEST);
            if (!echest.isMain()) { switchToItem(Items.ENDER_CHEST); }

            // Ensure Ender Chest is in the hotbar
            if (!echest.found()) {
                debug("No Ender Chests in hotbar, disabling.");
                if (instantRebreak.isActive()) {
                    instantRebreak.toggle();
                }
                toggle();
                return;
            }

            lookAtBlock(target);
            BlockUtils.place(target, echest, false, 0, true);
            startCount++;

            if (!rebreakActivated && startCount > 3 && useInstantRebreak.get()) { instantRebreak.toggle(); rebreakActivated = true; }

            debug("Echest count: %s", startCount);
            tickCounter = 0;
        }

        if (mc.world.getBlockState(target).getBlock() == Blocks.OBSIDIAN) {
            swapToFortunePickaxe();
            BlockUtils.breakBlock(target, swingHand.get());
            debug("Mining unwanted obsidian block...");
        }

        if (mc.world.getBlockState(target).getBlock() == Blocks.ENDER_CHEST) {
            tickCounter++;
            swapToFortunePickaxe();

            BlockUtils.breakBlock(target, swingHand.get());
            //debug("Breaking enderchest, ticks passed: %s", tickCounter);
            // if it hasn't mined an echest in 5 seconds (probably stuck because of instant rebreak -> toggle rebreak
            if (tickCounter / 20 > 5) { instantRebreak.toggle(); tickCounter = 0; }
        }
    }


    @EventHandler
    private void onRender(Render3DEvent event) {
        if (target == null || !render.get() || Modules.get().get(PacketMine.class).isMiningBlock(target)) return;

        Box box = SHAPE.getBoundingBoxes().getFirst();
        event.renderer.box(target.getX() + box.minX, target.getY() + box.minY, target.getZ() + box.minZ, target.getX() + box.maxX, target.getY() + box.maxY, target.getZ() + box.maxZ, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }
}
