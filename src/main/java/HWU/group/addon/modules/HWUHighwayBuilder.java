package HWU.group.addon.modules;

import static HWU.group.addon.helpers.Utils.*;
import static HWU.group.addon.modules.BetterAutoReplenish.findBestEnderChestShulker;
import static HWU.group.addon.modules.BetterEChestFarmer.getTarget;
import static HWU.group.addon.modules.HWUPaver.*;
import static HWU.group.addon.modules.HWUAutoWalk.isStopping;

import HWU.group.addon.helpers.DiscordRPC;
import HWU.group.addon.HWU_HWBuilder;

import HWU.group.addon.helpers.StatsHandler;
import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalBlock;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.AutoTotem.Mode;
import meteordevelopment.meteorclient.systems.modules.render.FreeLook;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HWUHighwayBuilder extends Module {
    private boolean isProcessing = false;
    public static boolean wasEchestFarmerActive = false;
    public static boolean isPausing;
    public static boolean baritoneCalled;
    public static Direction playerDirection;
    private float previousPitch;
    private float previousYaw;
    private int noPickaxeTickCount = 0;
    public static boolean mode;
    public static boolean togglePerspective;
    public static double cameraSensitivity;
    public static boolean arrowsControlOpposite;

    // Shulker Interactions
    private int stacksStolen;
    public static boolean isRestocking;
    private BlockPos restockingStartPosition;
    private boolean isPause;
    public static boolean isPlacingShulker;
    public static boolean isPlacingEchest;
    private int numberOfSlotsToSteal;
    private static boolean wasRestocking;
    private static BlockPos shulkerBlockPos;
    private static BlockPos echestBlockPos;
    public static boolean hasOpenedShulker;
    public static boolean hasOpenedEchest;
    private int slotNumber = 0;
    private boolean isBreakingShulker;
    public static boolean isPostRestocking;
    private boolean isProcessingTasks;
    private int hasLookedAtShulker = 0;
    private int hasLookedAtEchest = 0;
    private boolean hasSwitchedToBestTool;
    private int stealingDelay = 0;
    private boolean stackRecentlyStolen;
    public static Screen previousScreen;
    private boolean hasCreatedARestore;
    private int delayTicks = 15;
    private boolean hasSwitchedToFirstSlot;
    private int hasDroppedHand = 0;
    List<Item> items;

    // Stats
    private static int ticksPassed = 0;
    public static int obsidianPlaced = 0;
    public static int blocksBroken = 0;
    private int RPCTickCounter = 0;
    private static boolean enableHud; // So the HUD won't enable and disable if one of the requirements is not met

    // Constants
    private static final int MIN_ECHEST_COUNT = 8;
    private static final int REQUIRED_OBSIDIAN = 8;
    public static Integer playerX;
    public static Integer playerY;
    public static Integer playerZ;
    private static boolean test = false;
    private boolean miningLastEchest = false;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAutoEat = settings.createGroup("Auto Eat");
    private final SettingGroup sgNuker = settings.createGroup("Nuker");
    private final SettingGroup sgSafety = settings.createGroup("Safety");
    private final SettingGroup sgInventory = settings.createGroup("Inventory");
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgMisc = settings.createGroup("Misc");

    Module AutoTotem = Modules.get().get("auto-totem");
    Setting<Mode> totemMode = (Setting<Mode>) AutoTotem.settings.get("mode");
    Module betterEChestFarmer = Modules.get().get("better-EChest-farmer");

    // Settings
    private final Setting<Boolean> enableFreeLook = sgGeneral.add(new BoolSetting.Builder()
            .name("free-look")
            .description("Enable Free Look automatically when starting.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> useInstantRebreak = sgGeneral.add(new BoolSetting.Builder()
            .name("use-instant-rebreak")
            .description("Use instant block rebreaking. Disable if using rusherhack's fastbreak.")
            .defaultValue(false)
            .build()
    );

    @SuppressWarnings("unused")
    private final Setting<Boolean> enableDebugMessages = sgMisc.add(new BoolSetting.Builder()
            .name("debug-messages")
            .description("Enables debug messages in chat.")
            .defaultValue(false)
            .build()
    );

    @SuppressWarnings("unused")
    private final Setting<Boolean> paveHighway = sgGeneral.add(new BoolSetting.Builder()
            .name("pave-highway")
            .description("Pave the highway instead of just digging it.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> placeRails = sgGeneral.add(new BoolSetting.Builder()
            .name("place-rails")
            .description("Allow the paver to place railings on each side of the highway.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> enableAirPlace = sgGeneral.add(new BoolSetting.Builder()
            .name("enable-air-place")
            .description("Allow to airplace in case of messed up highway (slower than rotations).")
            .defaultValue(true)
            .build()
    );

    private final Setting<Integer> airPlaceDelay = sgGeneral.add(new IntSetting.Builder()
            .name("air-place-delay")
            .description("Delay between air place interactions, increase this if you are facing blocks being placed at the wrong place after air placing.")
            .defaultValue(5)
            .min(1)
            .sliderRange(1, 10)
            .visible(enableAirPlace::get)
            .build()
    );

    // Auto Eat Settings
    private final Setting<Boolean> enableAutoEat = sgAutoEat.add(new BoolSetting.Builder()
            .name("auto-eat")
            .description("Pauses the current task and automatically eats.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> eatEGaps = sgAutoEat.add(new BoolSetting.Builder()
            .name("eat-egap-when-burning")
            .description("Eats an enchanted golden apple if the player is burning.")
            .defaultValue(true)
            .visible(enableAutoEat::get)
            .build()
    );

    private final Setting<Boolean> disableAutoEatAfterDeactivating = sgAutoEat.add(new BoolSetting.Builder()
            .name("disable-auto-eat-after-deactivating")
            .description("Disables Auto Eat after deactivating.")
            .defaultValue(true)
            .visible(enableAutoEat::get)
            .build()
    );

    private final Setting<Boolean> enableAutoTotem = sgSafety.add(new BoolSetting.Builder()
            .name("auto-totem")
            .description("Enable Autototem when activating this module.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> disableAutoTotemAfterDeactivating = sgSafety.add(new BoolSetting.Builder()
            .name("disable-auto-totem-after-deactivating")
            .description("Enable Autototem when activating this module.")
            .defaultValue(true)
            .build()
    );

    // Blockage Remover (Nuker) Settings
    private final Setting<Boolean> enableNuker = sgNuker.add(new BoolSetting.Builder()
            .name("nuker")
            .description("Automatically remove blockages.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> rotate = sgNuker.add(new BoolSetting.Builder()
            .name("rotate")
            .description("Rotates server-side to the block being mined.")
            .defaultValue(true)
            .build()
    );

    // Safety Settings
    private final Setting<Boolean> isDisconnect = sgSafety.add(new BoolSetting.Builder()
            .name("disconnect-if-no-materials")
            .description("Automatically disconnect when materials are low (less than 8 Obsidian/E-Chests).")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> enableKillAura = sgSafety.add(new BoolSetting.Builder()
            .name("kill-aura")
            .description("Automatically attack nearby hostile entities.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> disableKillAuraAfterDeactivating = sgSafety.add(new BoolSetting.Builder()
            .name("disable-kill-aura-after-deactivating")
            .description("Disables Kill Aura after deactivating.")
            .defaultValue(true)
            .visible(enableKillAura::get)
            .build()
    );

    private final Setting<Boolean> enableSourceRemover = sgSafety.add(new BoolSetting.Builder()
            .name("source-remover")
            .description("Automatically remove lava sources when you can reach them.")
            .defaultValue(true)
            .build()
    );


    private final Setting<Boolean> RemoveY119 = sgNuker.add(new BoolSetting.Builder()
            .name("remove-y119")
            .description("If enabled, will bypass nuker blacklist and mine any block that isn't obsidian below the player's feet.")
            .defaultValue(true)
            .build()
    );

    // Inventory Management Settings
    private final Setting<Boolean> autoRefillHotbar = sgInventory.add(new BoolSetting.Builder()
            .name("auto-replenish")
            .description("Automatically move items from inventory to hotbar when slots are empty.")
            .defaultValue(true)
            .build()
    );

    // Rendering settings
    private final Setting<Boolean> renderBlocks = sgRender.add(new BoolSetting.Builder()
            .name("render-blocks")
            .description("Render blocks scheduled for placement/breaking.")
            .defaultValue(true)
            .build());

    @SuppressWarnings("unused")
    private final Setting<Boolean> renderPlacing = sgRender.add(new BoolSetting.Builder()
            .name("render-placing")
            .description("Render blocks scheduled for placement.")
            .defaultValue(true)
            .visible(renderBlocks::get)
            .build());

    @SuppressWarnings("unused")
    private final Setting<ShapeMode> renderPlacementsShape = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("render-placements-shape")
            .description("How the blocks scheduled for placement are rendered.")
            .defaultValue(ShapeMode.Both)
            .visible(renderPlacing::get)
            .build()
    );

    @SuppressWarnings("unused")
    private final Setting<SettingColor> renderPlacementsLineColor = sgRender.add(new ColorSetting.Builder()
            .name("render-placements-line-color")
            .description("Color of blocks scheduled for placement.")
            .defaultValue(new SettingColor(25, 25, 225, 255))
            .visible(renderPlacing::get)
            .build()
    );

    @SuppressWarnings("unused")
    private final Setting<SettingColor> renderPlacementsSideColor = sgRender.add(new ColorSetting.Builder()
            .name("render-placements-side-color")
            .description("Side color of blocks scheduled for placement.")
            .defaultValue(new SettingColor(25, 25, 225, 25))
            .visible(renderPlacing::get)
            .build()
    );

    @SuppressWarnings("unused")
    private final Setting<Boolean> renderBreaking = sgRender.add(new BoolSetting.Builder()
            .name("render-breaking")
            .description("Render blocks that have been broken.")
            .defaultValue(true)
            .visible(renderBlocks::get)
            .build());

    @SuppressWarnings("unused")
    private final Setting<ShapeMode> renderMineShape = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("render-mine-shape")
            .description("How the blocks that have been mined are rendered.")
            .defaultValue(ShapeMode.Both)
            .visible(renderBreaking::get)
            .build()
    );

    @SuppressWarnings("unused")
    private final Setting<SettingColor> renderMineLineColor = sgRender.add(new ColorSetting.Builder()
            .name("render-mine-line-color")
            .description("Color of blocks that are have been mined.")
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .visible(renderBreaking::get)
            .build()
    );

    @SuppressWarnings("unused")
    private final Setting<SettingColor> renderMineSideColor = sgRender.add(new ColorSetting.Builder()
            .name("render-mine-side-color")
            .description("Side color of blocks that have been mined.")
            .defaultValue(new SettingColor(255, 0, 0, 80))
            .visible(renderBreaking::get)
            .build()
    );

    // Misc
    private final Setting<Boolean> enableDiscordRPC = sgMisc.add(new BoolSetting.Builder()
            .name("discord-rpc")
            .description("Enable Discord Rich Presence")
            .defaultValue(true)
            .build()
    );

    public HWUHighwayBuilder() {
        super(HWU_HWBuilder.CATEGORY, "HWU-highway-builder", "Automated highway builder.");
    }

    @Override
    public void onActivate() {
        if (!validateInitialConditions()) {
            toggle();
            return;
        }

        initializeRequiredVariables();
        enableRequiredModules();
        previousPitch = mc.player.getPitch();
        previousYaw = mc.player.getYaw();
    }

    @Override
    public void onDeactivate() {
        cancelCurrentProcessBaritone();
        disableAllModules();
        resetState();
        DiscordRPC.stopRPC();
        if(AutoTotem.isActive() && disableAutoTotemAfterDeactivating.get()) AutoTotem.toggle();
    }

    private void resetState() {
        assert mc.player != null;
        isProcessing = false;
        wasEchestFarmerActive = false;
        baritoneCalled = false;
        miningLastEchest = false;
        test = false;
        playerX = null;
        playerY = null;
        playerZ = null;
        alignmentX = null;
        alignmentZ = null;
        playerDirection = null;
        noPickaxeTickCount = 0;
        setKeyPressed(mc.options.forwardKey, false);
        setKeyPressed(mc.options.backKey, false);
        setKeyPressed(mc.options.rightKey, false);
        setKeyPressed(mc.options.leftKey, false);
        mc.player.setPitch(previousPitch);
        mc.player.setYaw(previousYaw);

        // Free Look
        restoreFreeLookSettings();

        // Stats
        ticksPassed = 0;
        obsidianPlaced = 0;
        blocksBroken = 0;
        StatsHandler.distanceTravelled = 0;
        RPCTickCounter = 0;
        enableHud = false;

        // Shulker Interactions
        isRestocking = false;
        isPause = false;
        restockingStartPosition = null;
        isPlacingShulker = false;
        isPlacingEchest = false;
        stacksStolen = 0;
        hasOpenedShulker = false;
        hasOpenedEchest = false;
        slotNumber = 0;
        wasRestocking = false;
        shulkerBlockPos = null;
        echestBlockPos = null;
        isBreakingShulker = true;
        isPostRestocking = false;
        isProcessingTasks = false;
        hasLookedAtShulker = 0;
        hasLookedAtEchest = 0;
        stealingDelay = 0;
        hasSwitchedToBestTool = false;
        isStopping = false;
        stackRecentlyStolen = false;
        hasCreatedARestore = false;
        previousScreen = null;
        delayTicks = 15;
        hasSwitchedToFirstSlot = false;
        ticksTracker = 0;
        hasDroppedHand = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        enableHud = true;

        // TODO:Handle digging a highway and paving a highway differently
        // TODO:Add a highway coords tracker to the highway digger

        if (!paveHighway.get()) {
            lockRotation();
            toggleAutoWalk(true); // Handle walking in the "HWUAutoWalk" class
        }

        RPCTickCounter++;

        if (enableDiscordRPC.get()) {
            if (RPCTickCounter >= 310) {
                RPCTickCounter = 0;
                DiscordRPC.updateRPC();
            }
        } else
            DiscordRPC.stopRPC();

        ticksPassed += 1;

        int enderChestCount = countItems(Items.ENDER_CHEST);
        int obsidianCount = countItems(Items.OBSIDIAN);
        boolean isLowOnMaterials = enderChestCount <= 8 && obsidianCount <= 8;

        if (isLowOnMaterials && findBestEnderChestShulker() == null && !isPlacingShulker && !isGatheringItems() && isPaveHighway()) {
            String reason = String.format(
                    "Player low on materials. %s E-Chests. %s Obsidian.",
                    countItems(Items.ENDER_CHEST),
                    countItems(Items.OBSIDIAN)
            );

            if (isDisconnect.get())
                disconnect(reason);
            else
                error(reason);
            toggle();
            return;
        }

        if(totemMode.get() != Mode.Strict) {totemMode.set(Mode.Strict);}
        if (!AutoTotem.isActive() && enableAutoTotem.get()) { AutoTotem.toggle(); }

        Module HWUAutoEat = Modules.get().get("HWU-auto-eat");
        BoolSetting amountSetting = (BoolSetting) HWUAutoEat.settings.get("eat-egap-when-burning");
        if (amountSetting != null) amountSetting.set(eatEGaps.get());

        if (!isPlayerInValidPosition() && !isGatheringItems() && !isPausing) {
            handleInvalidPosition();
            return;
        }

        Module betterEChestFarmer = Modules.get().get("better-EChest-Farmer");

        if (countUsablePickaxes() == 0) {
            noPickaxeTickCount++;
            if (noPickaxeTickCount >= 5) { // Check for 5 ticks because it may rubberband and cause the module to falsely toggle off
                error("0 Usable pickaxes were found. \"Highway Builder\" will turn off.");
                toggle();
                return;
            }
        } else {
            // Reset the counter if we find any pickaxes
            noPickaxeTickCount = 0;
        }

        if (isPostRestocking) {
            handlePostRestocking();
            return;
        }

        if (isRestocking) {
            handleRestocking();
            return;
        }

        /*
        // Toggle off instant rebreak if the player isn't farming E-Chests
        if (useInstantRebreak.get()) {
            if ((!getTest()
                    || !betterEChestFarmer.isActive()) && instantRebreak.isActive()) instantRebreak.toggle();

            if ((getTest() || betterEChestFarmer.isActive()) && !instantRebreak.isActive()) instantRebreak.toggle();
        } else if (instantRebreak.isActive())
            warning("Please disable the \"Instant Rebreak\" module and use the built-in option in the \"Highway Builder\" module.");

         */

        if (isLowOnMaterials) {
            if (isGatheringItems()
                    || getTest()
                    || betterEChestFarmer.isActive()
                    || isRestocking) return;

            if (isGatheringItems() || Modules.get().get("HWU-Paver").isActive()) return;

            BlockPos target = getTarget();

            if (target != null) {
                if (!mc.world.getBlockState(target).isAir()) {
                    BlockUtils.breakBlock(target, true);
                    debug("Attempting to break block...");
                    return;
                }
            }

            restockingStartPosition = mc.player.getBlockPos();
            togglePaver(false); // No need to check for the "pave-highway" option as it is already being checked in the togglePaver(boolean) function
            debug("Toggled paver off");

            numberOfSlotsToSteal = countEmptySlots() / 2;
            items = new ArrayList<>(List.of(shulkerBoxes));

            // Initiate restocking
            debug("Running handleRestocking().");
            isRestocking = true;
        }

        handleMainProcess();
    }

    private void steal(ScreenHandler handler, int slotNumber) {
        MeteorExecutor.execute(() -> moveSlots(handler, slotNumber));
    }

    private void moveSlots(ScreenHandler handler, int i) {
        if (handler.getSlot(i).hasStack() && Utils.canUpdate()) {
            if (handler.getSlot(i).getStack().getItem() == Items.ENDER_CHEST) {
                InvUtils.shiftClick().slotId(i);
                stacksStolen++;
                stackRecentlyStolen = true; // Mark that a stack was stolen
            }
        }
    }

    private void moveShulkers(ScreenHandler handler, int i) {
        if (handler.getSlot(i).hasStack() && Utils.canUpdate()) {
            if (handler.getSlot(i).getStack().getItem() == Items.SHULKER_BOX) {
                InvUtils.shiftClick().slotId(i);
                stacksStolen++;
                stackRecentlyStolen = true; // Mark that a stack was stolen
            }
        }
    }

    private void handleInvalidPosition() {
        togglePaver(false); // No need to check for the "pave-highway" option as it is already being checked in the togglePaver(boolean) function
        goToHighwayCoords(false);
    }

    private void handleMainProcess() {
        Module betterEChestFarmer = Modules.get().get("better-EChest-farmer");

        int obsidianCount = countItems(Items.OBSIDIAN);
        int echestCount = countItems(Items.ENDER_CHEST);

        if (wasEchestFarmerActive && !betterEChestFarmer.isActive() && !test) {
            handlePostEchestFarming();
            return;
        }

        // Check if ready to pave
        if (obsidianCount > REQUIRED_OBSIDIAN && !betterEChestFarmer.isActive() &&
                !isGatheringItems() && !wasEchestFarmerActive) {
            togglePaver(true); // No need to check for the "pave-highway" option as it is already being checked in the togglePaver(boolean) function
            return;
        }

        if (shouldStartEchestFarming(echestCount))
            handleEchestFarming();
    }

    private void handleRestocking() {
        toggleAutoWalk(false);

        if (!hasCreatedARestore) {
            previousScreen = mc.currentScreen;
            hasCreatedARestore = true;
        }

        if (isGatheringItems()
                || getTest() || betterEChestFarmer.isActive() // If it is farming E-Chests
                || (!getTest() && wasEchestFarmerActive)      // If it is post E-Chest farming
        ) {
            isRestocking = false;
            return;
        }

        assert mc.player != null;
        assert mc.world != null;

        debug("handleRestocking() is on!");

        debug("Player Position: %s", mc.player.getBlockPos());
        debug("Restocking Start Position: %s", restockingStartPosition);

        if (!restockingStartPosition.equals(mc.player.getBlockPos())) {
            debug("Restocking Start Position doesn't match player position, pausing...");

            if (!isPause) {
                isPause = true;
                return;
            }

            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(restockingStartPosition));
            resumeBaritone();
            debug("Baritone call number 1.");
        } else {
            isPause = false;
        }

        if (isPause) {
            debug("Paused, returning...");
            return;
        }

        Direction playerDir = getPlayerDirection();

        // TODO:Add diagonal highway support
        switch (playerDir) {
            case NORTH -> shulkerBlockPos = new BlockPos(playerX, playerY, mc.player.getBlockZ() + 2);
            case SOUTH -> shulkerBlockPos = new BlockPos(playerX, playerY, mc.player.getBlockZ() - 2);
            case EAST -> shulkerBlockPos = new BlockPos(mc.player.getBlockX() - 2, playerY, playerZ);
            case WEST -> shulkerBlockPos = new BlockPos(mc.player.getBlockX() + 2, playerY, playerZ);
        }

        debug("Player direction: %s, current position: %s", playerDir, shulkerBlockPos);
        debug("Shulker block position: %s", shulkerBlockPos);

        if (hasLookedAtShulker < 15) { // To add a 15 tick delay
            toggleSneak(false);

            if (hasLookedAtShulker == 0) {
                InvUtils.swap(8, false);
                lookAtBlock(shulkerBlockPos.withY(playerY - 1)); // To minimize the chance of the shulker being placed upside down
            }

            hasLookedAtShulker++;
            isPlacingShulker = true;
            return;
        }

        if (mc.world.getBlockState(shulkerBlockPos).getBlock() instanceof ShulkerBoxBlock) {
            debug("Shulker box placed successfully at %s", shulkerBlockPos);
        } else {
            if (BlockUtils.canPlace(shulkerBlockPos, false) && !BlockUtils.canPlace(shulkerBlockPos, true)) return;
            debug("Placing %s at %s", Items.SHULKER_BOX, shulkerBlockPos);
            place(shulkerBlockPos, Hand.MAIN_HAND, 8, true, true, false);
            return;
        }

        /* Not sure if we need this anymore
        if (hasDroppedHand < 5) {
            if (hasDroppedHand == 0)
                InvUtils.dropHand(); // Drop the hand if the player is holding an item
            hasDroppedHand++;
            return;
        }*/

        if (!hasSwitchedToFirstSlot) { // Switch to the first slot so the shulker can open
            InvUtils.swap(0, false);
            hasSwitchedToFirstSlot = true;
            return;
        }

        if (!hasOpenedShulker) {
            toggleSneak(false);
            // Open the shulker
            mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND,
                    new BlockHitResult(Vec3d.ofCenter(shulkerBlockPos), Direction.DOWN,
                            shulkerBlockPos, false), 0));

            hasOpenedShulker = true;
            return;
        }

        if ((stacksStolen >= numberOfSlotsToSteal)
                || slotNumber > 27 // No more slots left (numberOfSlotsToSteal > stacksStolen because the shulker may not have the full amount)
        ) {
            debug("Stopping...");

            // Run post restocking
            isPostRestocking = true;

            stacksStolen = 0;
            slotNumber = 0;
            wasRestocking = true;

            isPause = false;
            isPlacingShulker = false;
            restockingStartPosition = null;
            hasLookedAtShulker = 0;
            hasDroppedHand = 0;
            stealingDelay = 0;
            hasOpenedShulker = false;
            isRestocking = false;
            delayTicks = 15;
            hasSwitchedToFirstSlot = false;
        } else {
            ScreenHandler handler = mc.player.currentScreenHandler;
            debug("Current Screen Handler = %s", handler);
            if (delayTicks > 0) {
                delayTicks--;
                return;
            }

            if (stackRecentlyStolen) {
                if (stealingDelay < 5) { // Delay after stealing a stack
                    stealingDelay++;
                    return;
                }
                stackRecentlyStolen = false; // Reset after the delay
            }

            debug("Auto stealing slot %s...", slotNumber);
            steal(handler, slotNumber);
            slotNumber++;
            stealingDelay = 0;
        }
    }

    private void handlePostRestocking() {
        Module betterEChestFarmer = Modules.get().get("better-EChest-farmer");
        if (betterEChestFarmer.isActive() || getTest() || isRestocking)
            return;

        assert mc.player != null;
        if (wasRestocking && !isPostRestocking) {
            mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.playerScreenHandler.syncId));
            isPostRestocking = true;
            return;
        }

        if (isPostRestocking) {
            debug("Post-restocking task running...");
            toggleAutoWalk(false);

            if (!hasSwitchedToBestTool) { // To add a 1 tick delay
                switchToBestTool(shulkerBlockPos);
                hasSwitchedToBestTool = true;
            }

            assert mc.world != null;
            isBreakingShulker = true;
            if (mc.world.getBlockState(shulkerBlockPos).getBlock() instanceof ShulkerBoxBlock)
                if (PlayerUtils.isWithinReach(shulkerBlockPos))
                    if (BlockUtils.breakBlock(shulkerBlockPos, true))
                        return;

            isBreakingShulker = false;

            for (Iterator<Item> iterator = items.iterator(); iterator.hasNext();) {
                Item item = iterator.next();
                debug("item=%s", item);

                if (!isGatheringItems()) {
                    gatherItem(item, false);
                    debug("Gathering item: %s...", item);
                    iterator.remove();
                }
            }

            if ((mc.world.getBlockState(shulkerBlockPos).getBlock().equals(Blocks.AIR))
                    && !isGatheringItems()
                    && !isRestocking
                    && !isBreakingShulker
                    && !isProcessingTasks) {
                if (previousScreen instanceof ChatScreen) // Some screens get stuck and require the player to quit the game forcefully
                    mc.setScreen(previousScreen);
                hasCreatedARestore = false;
                previousScreen = null;
                isPlacingShulker = false;
                wasRestocking = false;
                isPostRestocking = false;
                hasSwitchedToBestTool = false;
            }
        }
    }

    private void handleEchestFarming() {
        if (isRestocking || wasRestocking || isPostRestocking || isGatheringItems() || Modules.get().get("HWU-Paver").isActive())
            return;

        togglePaver(false); // No need to check for the "pave-highway" option as it is already being checked in the togglePaver(boolean) function

        setModuleSetting("better-EChest-farmer", "self-toggle", true);
        if (!isProcessing) {
            assert mc.player != null;

            int remainingEChests = countItems(Items.ENDER_CHEST) - MIN_ECHEST_COUNT;
            if (remainingEChests > 0)
                setModuleSetting("better-EChest-farmer", "amount", Math.min(remainingEChests, countEmptySlots() * 8));

            toggleAutoWalk(false);

            isProcessing = true;
            Module betterEChestFarmer = Modules.get().get("better-EChest-farmer");
            if (!betterEChestFarmer.isActive()) {
                wasEchestFarmerActive = true;
                setTest(true);
                betterEChestFarmer.toggle();
            }
        }
    }

    private void handlePostEchestFarming() {
        if (!test && wasEchestFarmerActive) {
            assert mc.world != null;
            assert mc.player != null;
            assert mc.interactionManager != null;

            BlockPos target = getTarget();

            if (target != null) {
                if (isEnderChest(getTarget())) {
                    debug("Attempting to break block...");
                    BlockUtils.breakBlock(target, true);
                    return;
                }
            }

            if (isGatheringItems())
                return;
            else
                gatherItem(Items.OBSIDIAN, false);

            goToHighwayCoords(true);
            isProcessing = false;
            wasEchestFarmerActive = false;
            miningLastEchest = false;
        }
    }

    private void handleShulkerRestocking() {
        assert mc.world != null;
        assert mc.player != null;
        assert mc.interactionManager != null;

        toggleAutoWalk(false);

        if (!hasCreatedARestore) {
            previousScreen = mc.currentScreen;
            hasCreatedARestore = true;
        }

        if (isGatheringItems()
                || getTest() || betterEChestFarmer.isActive() // If it is farming E-Chests
                || (!getTest() && wasEchestFarmerActive)      // If it is post E-Chest farming
        ) {
            isRestocking = false;
            return;
        }

        assert mc.player != null;
        assert mc.world != null;

        debug("handleShulkerRestocking() is on!");

        debug("Player Position: %s", mc.player.getBlockPos());
        debug("Restocking Start Position: %s", restockingStartPosition);

        if (!restockingStartPosition.equals(mc.player.getBlockPos())) {
            debug("Restocking Start Position doesn't match player position, pausing...");

            if (!isPause) {
                isPause = true;
                return;
            }

            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(restockingStartPosition));
            resumeBaritone();
            debug("Baritone call number 1.");
        } else {
            isPause = false;
        }

        if (isPause) {
            debug("Paused, returning...");
            return;
        }

        Direction playerDir = getPlayerDirection();

        // TODO:Add diagonal highway support
        switch (playerDir) {
            case NORTH -> echestBlockPos = new BlockPos(playerX, playerY, mc.player.getBlockZ() + 2);
            case SOUTH -> echestBlockPos = new BlockPos(playerX, playerY, mc.player.getBlockZ() - 2);
            case EAST -> echestBlockPos = new BlockPos(mc.player.getBlockX() - 2, playerY, playerZ);
            case WEST -> echestBlockPos = new BlockPos(mc.player.getBlockX() + 2, playerY, playerZ);
        }

        debug("Player direction: %s, current position: %s", playerDir, echestBlockPos);
        debug("Echest block position: %s", echestBlockPos);

        if (hasLookedAtEchest < 15) { // To add a 15 tick delay
            toggleSneak(false);

            if (hasLookedAtEchest == 0) {
                InvUtils.swap(8, false);
                lookAtBlock(echestBlockPos.withY(playerY - 1)); // To ensure the enderchest is being placed correctly...
            }

            hasLookedAtEchest++;
            isPlacingEchest = true;
            return;
        }

        if (mc.world.getBlockState(echestBlockPos).getBlock() instanceof EnderChestBlock) {
            debug("Enderchest placed successfully at %s", echestBlockPos);
        } else {
            if (BlockUtils.canPlace(echestBlockPos, false) && !BlockUtils.canPlace(echestBlockPos, true)) return;
            debug("Placing %s at %s", Items.ENDER_CHEST, echestBlockPos);
            FindItemResult echest = InvUtils.findInHotbar(Items.ENDER_CHEST);
            if (!echest.found()) {
                debug("No Enderchests in hotbar, disabling.");
                toggle();
                return;
            }
            BlockUtils.place(echestBlockPos, echest, true, 0, false);
            return;
        }

        if (!hasSwitchedToFirstSlot) { // Switch to the first slot so the enderchest can be opened
            InvUtils.swap(0, false);
            hasSwitchedToFirstSlot = true;
            return;
        }

        if (!hasOpenedEchest) {
            toggleSneak(false);
            // Open the shulker
            mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND,
                    new BlockHitResult(Vec3d.ofCenter(echestBlockPos), Direction.DOWN,
                            echestBlockPos, false), 0));

            hasOpenedEchest = true;
            return;
        }

        if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler echestHandler) {
            // Loop through second and third row (slots 9 to 26)
            for (int slot = 9; slot < 27; slot++) {
                ItemStack stack = echestHandler.getSlot(slot).getStack();

                // Check if the slot contains a Shulker Box
                if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
                    checkShulkerForEnderChests(stack);
                }
            }
        }

    }

    private void checkShulkerForEnderChests(ItemStack shulkerStack) {
        // to be implemented...
    }

    private boolean validateInitialConditions() {
        if (mc.player == null || mc.world == null) return false;

        boolean flag = true;

        if (countItems(Items.OBSIDIAN) < 8) {
            if (countItems(Items.ENDER_CHEST) < MIN_ECHEST_COUNT)
                if (findBestEnderChestShulker() == null)
                    flag = false;
                else {
                    error("Insufficient materials. Need: 8+ Ender Chests, 8+ obsidian or 1 E-Chest shulker.");
                }
        }

        if (countUsablePickaxes() == 0) {
            error("Insufficient materials. Need: 1 diamond/netherite, non-silk touch pickaxe.");
            flag = false;
        }

        if ((countEmptySlots() + countItems(Items.OBSIDIAN) / 64) <= 3) {
            error("Need: at least 4 empty slots.");
            flag = false;
        }

        int renderDistance = mc.options.getViewDistance().getValue();

        if (renderDistance <= 8) {
            error("Need: 9 or more render distance for ETA.");
            flag = false;
        }

        if (paveHighway.get() && mc.world.getBlockState(mc.player.getBlockPos().withY(mc.player.getBlockY() - 1)).getBlock() != Blocks.OBSIDIAN) { // So it doesn't spleef the player
            error("Need to stand on obsidian.");
            flag = false;
        }

        return flag;
    }

    private boolean isPlayerInValidPosition() {
        Module HWUPaver = Modules.get().get("HWU-Paver");
        if (HWUPaver.isActive()) return true; // "HWU Paver" has its own invalid position handler

        assert mc.player != null;
        Direction direction = getPlayerDirection();
        return ((direction == (Direction.NORTH) && mc.player.getBlockX() == playerX)
                || (direction == Direction.SOUTH && mc.player.getBlockX() == playerX)
                || (direction == Direction.EAST && mc.player.getBlockZ() == playerZ)
                || (direction == Direction.WEST && mc.player.getBlockZ() == playerZ)) && mc.player.getBlockY() == playerY;
    }

    private void enableRequiredModules() {
        Module HWUNuker = Modules.get().get("HWU-nuker");
        Module HWUAutoEat = Modules.get().get("HWU-auto-eat");
        Module HWUKillAura = Modules.get().get("HWU-kill-aura");
        Module lavaSourceRemover = Modules.get().get("lava-source-remover");

        if (enableNuker.get() && !HWUNuker.isActive()) HWUNuker.toggle();
        if (enableAutoEat.get() && !HWUAutoEat.isActive()) HWUAutoEat.toggle();
        if (enableKillAura.get() && !HWUKillAura.isActive()) HWUKillAura.toggle();

        Module betterAutoReplenish = Modules.get().get("better-auto-replenish");
        if (autoRefillHotbar.get())
            if (!betterAutoReplenish.isActive())
                betterAutoReplenish.toggle();

        if (enableDiscordRPC.get())
            DiscordRPC.startRPC();

        if (enableFreeLook.get()) {
            startFreeLook();
        }

        if (enableSourceRemover.get())
            if (!lavaSourceRemover.isActive())
                lavaSourceRemover.toggle();
    }

    private void disableAllModules() {
        String[] modulesToDisable = {
                "instant-rebreak",
                "HWU-Paver",
                "gather-item",
                "better-EChest-farmer",
                "HWU-auto-walk",
                "HWU-nuker",
                "better-auto-replenish",
                "free-look",
                "lava-source-remover"
        };

        for (String moduleName : modulesToDisable) {
            Module module = Modules.get().get(moduleName);
            if (module.isActive())
                module.toggle();
        }

        Module HWUAutoEat = Modules.get().get("HWU-auto-eat");
        Module HWUKillAura = Modules.get().get("HWU-kill-aura");

        if (disableAutoEatAfterDeactivating.get() && HWUAutoEat.isActive())
            HWUAutoEat.toggle();

        if (disableKillAuraAfterDeactivating.get() && HWUKillAura.isActive())
            HWUKillAura.toggle();
    }

    private boolean shouldStartEchestFarming(int echestCount) {
        if (isRestocking || wasRestocking || isPostRestocking || isGatheringItems() || Modules.get().get("HWU-Paver").isActive())
            return false;

        return echestCount > MIN_ECHEST_COUNT &&
                countEmptySlots() > 0 &&
                countItems(Items.OBSIDIAN) <= REQUIRED_OBSIDIAN;
    }

    // Getters and Setters

    public static ShapeMode getRenderPlacementsShape() {
        return (ShapeMode) Modules.get().get("HWU-highway-builder").settings.get("render-placements-shape").get();
    }

    public static SettingColor getRenderPlacementsLineColor() {
        return (SettingColor) Modules.get().get("HWU-highway-builder").settings.get("render-placements-line-color").get();
    }

    public static SettingColor getRenderPlacementsSideColor() {
        return (SettingColor) Modules.get().get("HWU-highway-builder").settings.get("render-placements-side-color").get();
    }

    public static boolean getRenderBreaking() {
        return (boolean) Modules.get().get("HWU-highway-builder").settings.get("render-breaking").get();
    }

    public static ShapeMode getRenderMineShape() {
        return (ShapeMode) Modules.get().get("HWU-highway-builder").settings.get("render-mine-shape").get();
    }

    public static SettingColor getRenderMineLineColor() {
        return (SettingColor) Modules.get().get("HWU-highway-builder").settings.get("render-mine-line-color").get();
    }

    public static SettingColor getRenderMineSideColor() {
        return (SettingColor) Modules.get().get("HWU-highway-builder").settings.get("render-mine-side-color").get();
    }

    // Stats
    public static int getObsidianPlaced() {
        return obsidianPlaced;
    }

    public static void addObsidianPlaced() {
        obsidianPlaced += 1;
    }

    public static int getTicksPassed() {
        return ticksPassed;
    }

    public static boolean getEnableHUD() {
        return enableHud;
    }

    // Restocking
    public static boolean getWasRestocking() {
        return wasRestocking;
    }

    public static boolean getIsPlacingShulker() {
        return isPlacingShulker;
    }

    public static boolean getIsRestocking() {
        return isRestocking;
    }

    public static boolean getIsPostRestocking() {
        return isPostRestocking;
    }

    // General
    public static Direction getPlayerDirection() {
        return playerDirection;
    }

    public static boolean getIsPausing() {
        return isPausing;
    }

    public static void setIsPausing(boolean isPausing) {
        HWUHighwayBuilder.isPausing = isPausing;
    }

    public static void setTest(boolean test) {
        HWUHighwayBuilder.test = test;
    }

    public static boolean getTest() {
        return HWUHighwayBuilder.test;
    }

    // TODO:Add diagonal highway support
    //  Use yaw values instead of mc.player.getHorizontalFacing()
    public Direction getPlayerCurrentDirection() {
        assert mc.player != null;
        return mc.player.getHorizontalFacing();
    }

    private void initializeRequiredVariables() {
        assert mc.player != null;
        playerX = mc.player.getBlockX();
        playerY = mc.player.getBlockY();
        playerZ = mc.player.getBlockZ();
        playerDirection = getPlayerCurrentDirection();
        alignmentX = mc.player.getBlockX() + 0.5;
        alignmentZ = mc.player.getBlockZ() + 0.5;

        // Free Look
        createAFreeLookRestore();
    }

    private void createAFreeLookRestore() {
        mode = ((EnumSetting<FreeLook.Mode>) Modules.get().get("free-look").settings.get("mode")).get() == FreeLook.Mode.Camera;
        togglePerspective = ((BoolSetting) Modules.get().get("free-look").settings.get("toggle-perspective")).get();
        cameraSensitivity = ((DoubleSetting) Modules.get().get("free-look").settings.get("camera-sensitivity")).get();
        arrowsControlOpposite = ((BoolSetting) Modules.get().get("free-look").settings.get("arrows-control-opposite")).get();
    }

    public static boolean isDebugMode() {
        return (boolean) Modules.get().get("HWU-highway-builder").settings.get("debug-messages").get();
    }

    public static boolean isPaveHighway() {
        return (boolean) Modules.get().get("HWU-highway-builder").settings.get("pave-highway").get();
    }
}
