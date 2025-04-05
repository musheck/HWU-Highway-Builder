package HWU.group.addon.modules;

import HWU.group.addon.helpers.BlockPosHelper;
import HWU.group.addon.helpers.StatsHandler;
import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import HWU.group.addon.HWU_HWBuilder;

import java.util.*;

import static HWU.group.addon.helpers.Utils.*;
import static HWU.group.addon.helpers.Utils.toggleAutoWalk;
import static HWU.group.addon.modules.HWUHighwayBuilder.*;
import static HWU.group.addon.modules.HWUAutoEat.getIsEating;
import static HWU.group.addon.modules.HWUKillAura.isAttacking;
import static HWU.group.addon.modules.HWUNuker.getIsBreaking;

public class HWUPaver extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static int nonObsidianBlocksCount;

    Module HWUHighwayBuilder = Modules.get().get("HWU-highway-builder");
    BoolSetting enableAirPlace = (BoolSetting) HWUHighwayBuilder.settings.get("enable-air-place");
    IntSetting airPlaceDelay = (IntSetting) HWUHighwayBuilder.settings.get("air-place-delay");
    BoolSetting removeY119 = (BoolSetting) HWUHighwayBuilder.settings.get("remove-y119");

    Module HWUNuker = Modules.get().get("HWU-nuker");
    IntSetting maxBlocksPerTick = (IntSetting) HWUNuker.settings.get("max-blocks-per-tick");
    BoolSetting packetMine = (BoolSetting) HWUNuker.settings.get("packet-mine");

    private boolean baritoneCalled = false;
    public static BlockPos targetDestination;
    private int blockPlaceTicks = 0;
    public static boolean hasRotated;
    public static Double alignmentX;
    public static Double alignmentZ;
    private boolean needsToStop;
    public static int ticksTracker;
    private Direction direction;
    private final List<BlockPos> remainingBlocksToBreak = new ArrayList<>();
    List<BlockPos> placementAttempts = new ArrayList<>();

    private final ShapeMode shapeModeBreak = getRenderMineShape();
    private final Color lineColor = getRenderMineLineColor();
    private final Color sideColor = getRenderMineSideColor();
    private final ShapeMode shapeModePlacement = ShapeMode.Both;
    private final Color lineColorPlacement = new Color(125, 25, 225, 25);
    private final Color sideColorPlacement = new Color(125, 25, 225, 25);

    // Stats
    private int tickCounter = 0;
    public static int ticksPassed = 0;
    private static int obsidianPlacedThisSession = 0;
    private static int nextInterval = getRandomInterval();

    private static int getRandomInterval() {
        Random rand = new Random();
        return rand.nextInt(11) + 95; // Generate a random number between 95 and 105
    }

    public static BlockPos getTargetDestination() {
        return targetDestination;
    }

    public HWUPaver() {
        super(HWU_HWBuilder.HELPER_MODULES_CATEGORY, "HWU-Paver", "A helper module that paves a highway with obsidian.");
    }

    public static int getTicksPassed() {
        return ticksPassed;
    }

    public static int getObsidianPlacedThisSession() {
        return obsidianPlacedThisSession;
    }

    public static int getNonObsidianBlocksCount() {
        return nonObsidianBlocksCount;
    }

    Setting<List<Block>> blacklist = (Setting<List<Block>>) HWUNuker.settings.get("blacklist");

    List<Block> blacklistedBlocks = blacklist.get();

    public int countNonObsidianBlocks() {
        int nonObsidianCount = 0;
        assert mc.world != null;

        List<BlockPos> blockPositions = BlockPosHelper.getBlockPositions3();
        for (BlockPos pos : blockPositions) {
            Block block = mc.world.getBlockState(pos).getBlock();
            if (!mc.world.getBlockState(pos).getBlock().equals(Blocks.OBSIDIAN)) {
                if (!blacklistedBlocks.contains(block) || removeY119.get()) {
                    nonObsidianCount++;
                    if (isDebugMode())
                        RenderUtils.renderTickingBlock(pos, new Color(125, 25, 225, 25), new Color(125, 25, 225, 25), ShapeMode.Both, 0, 8, true, false);
                }
            }
        }

        return nonObsidianCount;
    }

    private boolean cannotPlaceOrBreak(BlockPos pos) {
        assert mc.world != null;
        return !mc.world.getBlockState(pos).isAir()
                && !mc.world.getBlockState(pos).isReplaceable()
                && mc.world.getBlockState(pos).getBlock() != Blocks.OBSIDIAN;
    }

    public void onActivate() {
        assert mc.player != null;
        direction = getPlayerDirection();
        lockRotation();
        toggleAutoWalk(true);
        nonObsidianBlocksCount = countNonObsidianBlocks();
        placementAttempts.add(mc.player.getBlockPos());
    }

    @Override
    public void onDeactivate() {
        toggleSneak(false);
        toggleAutoWalk(false);
        setIsPausing(false);
        blockPlaceTicks = 0;
        hasRotated = false;
        needsToStop = false;
        remainingBlocksToBreak.clear();
        ticksPassed = 0;
        obsidianPlacedThisSession = 0;
        nonObsidianBlocksCount = 0;
    }

    BlockPos target1;

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (direction == null) return;
        assert mc.player != null;
        if (playerX == null || playerY == null || playerZ == null) return;
        if (alignmentX == null || alignmentZ == null) return;

        ticksTracker++;
        ticksPassed++;
        if(placementAttempts.size() > 5000) placementAttempts.clear(); placementAttempts.add(mc.player.getBlockPos());


        if (tickCounter++ >= nextInterval) { // To reduce lag all of the time
            nonObsidianBlocksCount = countNonObsidianBlocks();
            tickCounter = 0; // Reset the counter

            nextInterval = getRandomInterval();
        }

        if (countItems(Items.OBSIDIAN) <= 8) {
            toggle();
            return;
        }

        lockRotation();

        blockPlaceTicks++;

        boolean pressD = false;
        boolean pressA = false;

        if (direction == Direction.NORTH || direction == Direction.SOUTH) {
            double currentX = mc.player.getX();
            if (direction == Direction.NORTH) {
                if (currentX > alignmentX + 0.13) {
                    pressA = true;
                } else if (currentX < alignmentX - 0.13) {
                    pressD = true;
                }
            } else {
                if (currentX < alignmentX - 0.13) {
                    pressA = true;
                } else if (currentX > alignmentX + 0.13) {
                    pressD = true;
                }
            }
        } else if (direction == Direction.EAST || direction == Direction.WEST) {
            double currentZ = mc.player.getZ();
            if (direction == Direction.EAST) {
                if (currentZ > alignmentZ + 0.13) {
                    pressA = true;
                } else if (currentZ < alignmentZ - 0.13) {
                    pressD = true;
                }
            } else {
                if (currentZ < alignmentZ - 0.13) {
                    pressA = true;
                } else if (currentZ > alignmentZ + 0.13) {
                    pressD = true;
                }
            }
        }

        setKeyPressed(mc.options.rightKey, pressD);
        setKeyPressed(mc.options.leftKey, pressA);

        boolean condition = ((direction == (Direction.NORTH) && mc.player.getBlockX() == playerX)
                         || (direction == Direction.SOUTH && mc.player.getBlockX() == playerX)
                         || (direction == Direction.EAST && mc.player.getBlockZ() == playerZ)
                         || (direction == Direction.WEST && mc.player.getBlockZ() == playerZ)) && mc.player.getBlockY() == playerY;

        Module betterEChestFarmer = Modules.get().get("better-EChest-Farmer");
        if (!condition && !betterEChestFarmer.isActive() && !isGatheringItems() && !getTest()) {
            if (!baritoneCalled) {
                switch (direction) {
                    case NORTH -> target1 = new BlockPos(playerX, playerY, mc.player.getBlockZ() + 1);
                    case SOUTH -> target1 = new BlockPos(playerX, playerY, mc.player.getBlockZ() - 1);
                    case EAST -> target1 = new BlockPos(mc.player.getBlockX() - 1, playerY, playerZ);
                    case WEST -> target1 = new BlockPos(mc.player.getBlockX() + 1, playerY, playerZ);
                }

                if (target1 == null) return;
                Goal target2 = new GoalBlock(target1);
                BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess()
                        .setGoalAndPath(target2);
                baritoneCalled = true;
            }
            return;
        }

        if (condition)
            baritoneCalled = false;

        BlockPos[] positions = BlockPosHelper.getBlockPositions();
        FindItemResult obsidianSlot = InvUtils.findInHotbar(
                stack -> stack.getItem() instanceof BlockItem &&
                        ((BlockItem) stack.getItem()).getBlock() == Blocks.OBSIDIAN
        );

        if (!obsidianSlot.found()) {
            toggleAutoWalk(false);
            return;
        }

        if (mc.player == null || mc.world == null) return;

        if (getIsBreaking() || getIsEating() || getIsPausing()) {
            // Stop walking if the player is currently breaking a block/eating
            toggleAutoWalk(false);
            return;
        }

        if (isAttacking()) return; // Don't do anything if KillAura is attacking

        if (isStandingOnObsidian() && !betterEChestFarmer.isActive() && !isGatheringItems()) {
            switch (direction) {
                case NORTH -> targetDestination = new BlockPos(playerX, playerY, mc.player.getBlockZ() + 1);
                case SOUTH -> targetDestination = new BlockPos(playerX, playerY, mc.player.getBlockZ() - 1);
                case EAST -> targetDestination = new BlockPos(mc.player.getBlockX() - 1, playerY, playerZ);
                case WEST -> targetDestination = new BlockPos(mc.player.getBlockX() + 1, playerY, playerZ);
            }
        }

        for (BlockPos currentPos : positions) {
            if (PlayerUtils.isWithinReach(currentPos)) {
                if (BlockUtils.canPlace(currentPos, false) && !BlockUtils.canPlace(currentPos, true)) {
                    toggleAutoWalk(false);

                    // Spleef
                    assert mc.world != null;
                    Entity entity = getEntitiesAtBlockPos(mc.world, currentPos);
                    assert entity != null;
                    if (!entity.isAlive()) return;
                    List<BlockPos> blocksToBreak = getBlocksEntityIsStandingOn(mc.world, entity); // Get blocks entity is standing on
                    for (BlockPos pos1 : blocksToBreak) {
                        if (!BlockUtils.canPlace(currentPos, true)) {
                            // Check if the current block position is a highway block
                            boolean found = false;
                            for (BlockPos pos : positions) {
                                if (pos.equals(pos1)) {
                                    found = true;
                                    break;
                                }
                            }

                            if (found) return;

                            if (countUsablePickaxes() <= 0) return;
                            // TODO:Add a configurable delay (I think 1 tick should be enough) before continuing to the next block so the player doesn't rubberband
                            breakBlock(pos1);
                            if (renderBreaking.get())
                                RenderUtils.renderTickingBlock(pos1, sideColor, lineColor, shapeModeBreak, 0, 8, true, false);
                            return;
                        }
                    }
                    return;
                }
            }
        }

        BlockPos[] positions2 = BlockPosHelper.getBlockPositions2();

        boolean foundNonObsidian = false;
        BlockPos nonObsidianBlock = null;
        for (BlockPos currentPos : positions2) {
            Block block = mc.world.getBlockState(currentPos).getBlock();

            if ((mc.world.getBlockState(currentPos).getBlock() != Blocks.OBSIDIAN)
                && StatsHandler.getDistanceTravelled() > 9 // Stop it from going back when the highway isn't paved behind, so it keeps moving forward
            ) {
                if (!blacklistedBlocks.contains(block)) {
                    foundNonObsidian = true;
                    nonObsidianBlock = currentPos;
                    break; // Stop checking further blocks
                }
            }
        }

        if (foundNonObsidian) {
            toggleAutoWalk(false);
            debug("auto walk stop 34553");

            Goal baritoneGoal = switch (direction) {
                case NORTH -> new GoalBlock(playerX, playerY, nonObsidianBlock.getZ() - 2);
                case EAST -> new GoalBlock(nonObsidianBlock.getX() + 2, playerY, playerZ);
                case SOUTH -> new GoalBlock(playerX, playerY, nonObsidianBlock.getZ() + 2);
                case WEST -> new GoalBlock(nonObsidianBlock.getX() - 2, playerY, playerZ);
                default -> new GoalBlock(playerX, playerY, playerZ);
            };

            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(baritoneGoal);
            resumeBaritone();
            debug("Baritone call number 2.");
        }


        boolean hasPlaceableBlocks = false;
        int count = 0;

        // First time or if we've finished the previous batch, refresh the list
        if (remainingBlocksToBreak.isEmpty()) {
            for (BlockPos pos : positions) {
                Block block = mc.world.getBlockState(pos).getBlock();

                if (block != Blocks.NETHER_PORTAL || block != Blocks.BEDROCK) break;

                if (cannotPlaceOrBreak(pos) && !blacklistedBlocks.contains(block) || removeY119.get()) remainingBlocksToBreak.add(pos);
                // will remove blacklisted blocks if in path of the player and if removeY119 is enabled
                if (direction == Direction.NORTH || direction == Direction.SOUTH && pos.getX() == playerX && removeY119.get() && blacklistedBlocks.contains(block)) remainingBlocksToBreak.add(pos);
                if (direction == Direction.EAST || direction == Direction.WEST && pos.getZ() == playerZ && removeY119.get() && blacklistedBlocks.contains(block)) remainingBlocksToBreak.add(pos);
            }
        }

        // If we have blocks to break, focus on that
        if (!remainingBlocksToBreak.isEmpty()) {
            toggleAutoWalk(false);

            Iterator<BlockPos> iterator = remainingBlocksToBreak.iterator();
            while (iterator.hasNext() && count < maxBlocksPerTick.get()) {
                BlockPos currentPos = iterator.next();
                // TODO:Add a configurable delay (I think 1 tick should be enough) before continuing to the next block so the player doesn't rubberband
                breakBlock(currentPos);

                if (renderBreaking.get()) {
                    RenderUtils.renderTickingBlock(
                            currentPos,
                            sideColor,
                            lineColor,
                            shapeModeBreak,
                            0,
                            8,
                            true,
                            false
                    );
                }
                iterator.remove();
                count++;
            }

            return; // Exit here if we're still breaking blocks
        }

        for (BlockPos currentPos : positions) {
            // Handle block placing
            if (BlockUtils.canPlace(currentPos, true)) {
                if (PlayerUtils.isWithinReach(currentPos)) {
                    hasPlaceableBlocks = true;
                    if (getIsPausing()) return;

                    double distance = distanceBetweenPlayerAndEndOfPavement();

                    //toggleSneak(distance <= 1.498);

                    if (distance <= 0.7) {
                        toggleAutoWalk(false);
                        needsToStop = true;
                    } else {
                        needsToStop = false;
                        toggleAutoWalk(true);
                    }

                    assert mc.world != null;
                    switchToItem(Items.OBSIDIAN);

                    if (placementAttempts.contains(currentPos) && enableAirPlace.get()) {
                        // airplace if the block below is air (needs rewriting to if no face to place against)
                        if (blockPlaceTicks % airPlaceDelay.get() != 0) return;

                        if (airPlace(Items.OBSIDIAN, currentPos, Direction.DOWN)) {
                            debug("Airplace attempt at: x: %s y: %s z: %s", currentPos.getX(), currentPos.getY(), currentPos.getZ());
                            addObsidianPlaced();
                            obsidianPlacedThisSession++;
                            placementAttempts.add(currentPos);

                            if (renderPlacing.get()) {
                                RenderUtils.renderTickingBlock(currentPos, sideColorPlacement, lineColorPlacement, shapeModePlacement, 0, 8, true, false);
                            }

                            blockPlaceTicks = 0;
                        }
                        hasRotated = false;
                    }

                    // Ensure rotation happens first
                    if (!hasRotated) {
                        lookAtBlock(currentPos);
                        hasRotated = true;
                        //return;
                    }

                    // Try placing normally
                    if (!placementAttempts.contains(currentPos)) {
                        if (BlockUtils.place(currentPos, obsidianSlot, false, 0, true, true, false)) {
                            debug("Placement attempt at: x: %s y: %s z: %s", currentPos.getX(), currentPos.getY(), currentPos.getZ());
                            addObsidianPlaced();
                            obsidianPlacedThisSession++;
                            placementAttempts.add(currentPos);

                            if (renderPlacing.get()) {
                                RenderUtils.renderTickingBlock(currentPos, sideColorPlacement, lineColorPlacement, shapeModePlacement, 0, 8, true, false);
                            }

                            hasRotated = false;
                            return;
                        }
                    }
                    hasRotated = false;
                }
            }
        }

        if (!hasPlaceableBlocks || !needsToStop
            || !foundNonObsidian || !getIsBreaking()) {
            lockRotation();
            debug("Auto walk 3.");
            debug("needsToStop: %s. hasPlaceableBlocks: %s.", needsToStop, hasPlaceableBlocks);
            debug("foundNonObsidian: %s.", foundNonObsidian);
            toggleAutoWalk(true);
        }
    }

    final BoolSetting renderBlocks = (BoolSetting) Modules.get().get("HWU-highway-builder").settings.get("render-blocks");
    final BoolSetting renderPlacing = (BoolSetting) Modules.get().get("HWU-highway-builder").settings.get("render-placing");
    final BoolSetting renderBreaking = (BoolSetting) Modules.get().get("HWU-highway-builder").settings.get("render-breaking");

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (direction == null) return;
        assert mc.player != null;
        if (playerX == null || playerY == null || playerZ == null) return;
        if (alignmentX == null || alignmentZ == null) return;
        if (!renderBlocks.get()) return;

        BlockPos[] positions = BlockPosHelper.getBlockPositions();

        // Render blocks placement
        if (renderPlacing.get()) {
            assert mc.player != null;
            for (BlockPos pos : positions) {
                if (BlockUtils.canPlace(pos, true)) {
                    event.renderer.box(
                            pos,
                            getRenderPlacementsSideColor(),
                            getRenderPlacementsLineColor(),
                            getRenderPlacementsShape(),
                            0
                    );
                }
            }
        }
    }

    private double distanceBetweenPlayerAndEndOfPavement() {
        int offsetX = 0, offsetZ = 0;
        Direction direction = getPlayerDirection();

        switch (direction) {
            case NORTH -> offsetZ = -1;
            case SOUTH -> offsetZ = 1;
            case EAST -> offsetX = 1;
            case WEST -> offsetX = -1;
        }

        assert mc.player != null;
        assert mc.world != null;
        Vec3d playerPos = mc.player.getPos();
        BlockPos blockPos = null;

        for (int i = 0; i <= 5; i++) {
            int blockX = mc.player.getBlockX() + offsetX * i;
            int blockZ = mc.player.getBlockZ() + offsetZ * i;
            BlockPos blockPosition = new BlockPos(blockX, mc.player.getBlockY() - 1, blockZ);

            if (mc.world.getBlockState(blockPosition).isAir()) {
                blockPos = blockPosition;
                break;
            }
        }

        if (blockPos == null) return 10;

        double coord = 0;
        int blockPosCoord = 0;
        switch (getPlayerDirection()) {
            case Direction.NORTH, Direction.SOUTH -> {
                coord = playerPos.getZ();
                blockPosCoord = blockPos.getZ();
            }

            case Direction.EAST, Direction.WEST -> {
                coord = playerPos.getX();
                blockPosCoord = blockPos.getX();
            }
        }
        
        return Math.abs(coord - blockPosCoord);
    }

    private void breakBlock(BlockPos blockPos) {
        assert mc.player != null;
        switchToBestTool(blockPos);
        if (packetMine.get()) {
            Objects.requireNonNull(mc.getNetworkHandler()).sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, BlockUtils.getDirection(blockPos)));
            mc.player.swingHand(Hand.MAIN_HAND);

            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, BlockUtils.getDirection(blockPos)));
        } else
            BlockUtils.breakBlock(blockPos, true);
    }
}
