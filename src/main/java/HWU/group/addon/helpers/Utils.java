package HWU.group.addon.helpers;

import HWU.group.addon.config.ProxyConfig;

import static HWU.group.addon.modules.HWUPaver.*;
import static HWU.group.addon.modules.GatherItem.setPaveAfterwards;
import static HWU.group.addon.modules.HWUHighwayBuilder.*;
import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.process.IBuilderProcess;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.FreeLook;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.command.CommandSource;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.World;

import java.io.File;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static HWU.group.addon.modules.HWUAutoWalk.isStopping;
import static meteordevelopment.meteorclient.utils.Utils.*;
import static meteordevelopment.meteorclient.utils.world.BlockUtils.*;

public class Utils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final ProxyConfig proxyConfig = new ProxyConfig();

    public static Item[] shulkerBoxes = {
            Items.SHULKER_BOX,
            Items.BLACK_SHULKER_BOX,
            Items.LIME_SHULKER_BOX,
            Items.WHITE_SHULKER_BOX,
            Items.ORANGE_SHULKER_BOX,
            Items.MAGENTA_SHULKER_BOX,
            Items.LIGHT_BLUE_SHULKER_BOX,
            Items.YELLOW_SHULKER_BOX,
            Items.PINK_SHULKER_BOX,
            Items.GRAY_SHULKER_BOX,
            Items.LIGHT_GRAY_SHULKER_BOX,
            Items.CYAN_SHULKER_BOX,
            Items.PURPLE_SHULKER_BOX,
            Items.BLUE_SHULKER_BOX,
            Items.BROWN_SHULKER_BOX,
            Items.GREEN_SHULKER_BOX,
            Items.RED_SHULKER_BOX,
    };

    public static int countItems(Item item) {
        int count = 0;
        if (mc.player != null) {
            // Main inventory, hotbar, and offhand
            for (int i = 0; i <= 40; i++) {
                ItemStack itemStack = mc.player.getInventory().getStack(i);
                if (!itemStack.isEmpty() && itemStack.getItem() == item) {
                    count += itemStack.getCount();
                }
            }
        }
        return count;
    }

    public static void togglePaver(boolean activate) {
        if (!isPaveHighway()) return;

        Module HWUPaver = Modules.get().get("HWU-Paver");

        if (activate != HWUPaver.isActive())
            HWUPaver.toggle();
    }

    public static void toggleAutoWalk(boolean activate) {
        isStopping = !activate;

        Module HWUAutoWalk = Modules.get().get("HWU-auto-walk");

        if (activate != HWUAutoWalk.isActive())
            HWUAutoWalk.toggle();
    }

    public static void toggleFreeLook(boolean activate) {
        Module HWUFreeLook = Modules.get().get("free-look");

        if (activate != HWUFreeLook.isActive())
            HWUFreeLook.toggle();
    }

    public static void startFreeLook() {
        if(mc.player != null){
            setModuleSetting("free-look", "mode", FreeLook.Mode.Camera);
            setModuleSetting("free-look", "toggle-perspective", true);
            setModuleSetting("free-look", "camera-sensitivity", 8.0);
            setModuleSetting("free-look", "toggle-perspective", true);
            setModuleSetting("free-look", "arrows-control-opposite", false);

            // To get a good look at the player in FreeLook
            lockRotation();
            mc.player.setPitch(15);

            toggleFreeLook(true);
        }
    }

    public static void restoreFreeLookSettings() {
        setModuleSetting("free-look", "mode", mode ? FreeLook.Mode.Camera : FreeLook.Mode.Player);
        setModuleSetting("free-look", "toggle-perspective", togglePerspective);
        setModuleSetting("free-look", "camera-sensitivity", cameraSensitivity);
        setModuleSetting("free-look", "arrows-control-opposite", arrowsControlOpposite);
    }

    public static int countEmptySlots() {
        int emptyCount = 0;
        if (mc.player != null) {

            for (ItemStack itemStack : mc.player.getInventory().main) {
                if (itemStack.isEmpty()) {
                    emptyCount++;
                }
            }
        }
        return emptyCount;
    }

    public static boolean hasReachedLocation(ClientPlayerEntity player, BlockPos location) {
        return player.getBlockPos().equals(location);
    }

    public static void disconnect(String reason) {
        assert mc.player != null;
        MutableText text = Text.literal("[HWU Highway Builder] " + reason);

        mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(text));
    }

    // TODO:Implement this
    public static boolean checkForDangerousBlocks(ClientPlayerEntity player, int search_radius, int max_y) {
        Box searchArea = new Box(
                player.getX() - search_radius,
                player.getY() - search_radius,
                player.getZ() - search_radius,
                player.getX() + search_radius,
                max_y,
                player.getZ() + search_radius
        );

        World world = player.getWorld();
        BlockPos.Mutable pos = new BlockPos.Mutable();

        int minX = (int) Math.floor(searchArea.minX);
        int minY = (int) Math.floor(searchArea.minY);
        int minZ = (int) Math.floor(searchArea.minZ);
        int maxX = (int) Math.ceil(searchArea.maxX);
        int maxY = (int) Math.ceil(searchArea.maxY);
        int maxZ = (int) Math.ceil(searchArea.maxZ);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    pos.set(x, y, z);
                    if (isDangerous(world.getBlockState(pos).getBlock())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isDangerous(Block block) {
        return block == Blocks.LAVA
                || block == Blocks.FIRE
                || block == Blocks.CACTUS;
    }

    public static void switchToBestTool(BlockPos blockPos) {
        if (mc.world == null || mc.player == null || blockPos == null) return;

        BlockState blockState = mc.world.getBlockState(blockPos);
        double bestSpeed = 0;
        int bestSlot = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            if (itemStack.getItem() instanceof MiningToolItem) {
                double speed = itemStack.getMiningSpeedMultiplier(blockState);

                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestSlot = i;
                }
            }
        }

        if (bestSlot != -1) {
            mc.player.getInventory().selectedSlot = bestSlot;
        }
    }

    public static int countUsablePickaxes() {
        int count = 0;

        if (mc.player == null) return count;

        for (int i = 0; i < mc.player.getInventory().main.size(); i++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);

            if (itemStack.getItem() instanceof PickaxeItem) {
                if (!hasEnchantments(itemStack, Enchantments.SILK_TOUCH)) { // TODO:Don't check for non-silkTouch picks if the player is not paving the highway
                    if (itemStack.getMaxDamage() - itemStack.getDamage() > 50) {
                        count++;
                    }
                }
            }
        }

        ItemStack offhandStack = mc.player.getOffHandStack();
        if (offhandStack.getItem() instanceof PickaxeItem) {
            if (!hasEnchantments(offhandStack, Enchantments.SILK_TOUCH)) { // TODO:Don't check for non-silkTouch picks if the player is not paving the highway
                if (offhandStack.getMaxDamage() - offhandStack.getDamage() > 50) {
                    count++;
                }
            }
        }

        return count;
    }

    public static void setModuleSetting(String moduleName, String settingName, Object value) {
        Module module = Modules.get().get(moduleName);
        if (module == null) {
            ChatUtils.warning("Module '" + moduleName + "' not found.");
            return;
        }

        Setting<?> setting = module.settings.get(settingName);
        if (setting == null) {
            ChatUtils.warning("Setting '" + settingName + "' not found in module '" + moduleName + "'.");
            return;
        }

        try {
            switch (value) {
                case Integer i -> ((IntSetting) setting).set(i);
                case Double v -> ((DoubleSetting) setting).set(v);
                case String s -> ((StringSetting) setting).set(s);
                case Boolean b -> ((BoolSetting) setting).set(b);
                case Item item -> ((ItemSetting) setting).set(item);
                case null, default ->
                        System.out.println("Utils.java setModuleSetting: Incompatible value type for setting '" + settingName + "'.");
            }

        } catch (Exception e) {
            System.out.println("Utils.java setModuleSetting: Error setting value: " + e.getMessage());
        }
    }

    private static final IBuilderProcess builderProcess = BaritoneAPI.getProvider().getPrimaryBaritone().getBuilderProcess();

    public static void resumeBaritone() {
        if (builderProcess.isPaused())
            builderProcess.resume();
    }

    public static void cancelCurrentProcessBaritone() {
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
    }

    public static void gatherItem(Item item, boolean paveAfterwards) {
        setModuleSetting("gather-item", "item", item);
        setPaveAfterwards(paveAfterwards);
        Module gatherItem = Modules.get().get("gather-item");
        if (!isGatheringItems()) gatherItem.toggle();
    }

    public static boolean isEnderChest(BlockPos pos) {
        assert mc.world != null;
        Block block = mc.world.getBlockState(pos).getBlock();
        return block == Blocks.ENDER_CHEST;
    }

    // TODO:Add diagonal highway support
    public static void goToHighwayCoords(boolean paveAfterwards) {
        if (getTest() || Modules.get().get("better-EChest-farmer").isActive()) return;
        assert mc.player != null;
        BlockPos target;
        togglePaver(false);
        Direction direction = getPlayerDirection();
        if (getTargetDestination() != null)
            target = getTargetDestination();
        else {
            switch (direction) {
                case NORTH -> target = new BlockPos(playerX, playerY, mc.player.getBlockZ() + 1);
                case SOUTH -> target = new BlockPos(playerX, playerY, mc.player.getBlockZ() - 1);
                case EAST -> target = new BlockPos(mc.player.getBlockX() - 1, playerY, playerZ);
                case WEST -> target = new BlockPos(mc.player.getBlockX() + 1, playerY, playerZ);
                default -> target = null;
            }
        }

        if (target != null) {
            if ((mc.player.getBlockPos() != target) && !isGatheringItems()) {
                System.out.printf("Utils.java: %s%n", target);
                BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(target));
                resumeBaritone();
                debug("Baritone call number 4.");
            }
        }

        BlockPos finalTarget = target;
        mc.execute(() -> {
            if (hasReachedLocation(mc.player, finalTarget)) {
                if (paveAfterwards)
                    togglePaver(true);
            }
        });
    }

    public static boolean isStandingOnObsidian() {
        if (mc.player == null || mc.world == null) return false;

        BlockPos posBelow = mc.player.getBlockPos().down();
        return mc.world.getBlockState(posBelow).getBlock() == Blocks.OBSIDIAN;
    }

    public static File getGameDirectory() {
        return MinecraftClient.getInstance().runDirectory.toPath().toFile();
    }

    public static CompletableFuture<Suggestions> suggestUsernames(SuggestionsBuilder builder) {
        List<String> proxies = proxyConfig.listProxies();
        String remainingInput = builder.getRemaining();

        for (String proxy : proxies) {
            String[] parts = proxy.split(":");
            if (parts.length > 0 && CommandSource.shouldSuggest(remainingInput, parts[0].toLowerCase())) {
                builder.suggest(parts[0]);
            }
        }

        return builder.buildFuture();
    }

    public static List<BlockPos> getBlocksEntityIsStandingOn(World world, Entity entity) {
        List<BlockPos> blockPositions = new ArrayList<>();
        int minY;

        Box box = entity.getBoundingBox();
        int minX = MathHelper.floor(box.minX);
        int maxX = MathHelper.floor(box.maxX);
        if (entity.isOnGround())
            minY = MathHelper.floor(box.minY - 0.01); // Check just below the entity
        else
            minY = MathHelper.floor(box.minY - 1 - 0.01);
        int maxY = MathHelper.floor(box.maxY);
        int minZ = MathHelper.floor(box.minZ);
        int maxZ = MathHelper.floor(box.maxZ);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!world.getBlockState(pos).isAir()) {
                        blockPositions.add(pos);
                    }
                }
            }
        }

        return blockPositions;
    }

    public static FindItemResult getItemSlot(Item item) {
        return InvUtils.findInHotbar(itemStack -> itemStack.getItem() == item);
    }

    public static void switchToItem(Item item) {
        if (mc.player != null) {
            FindItemResult result = getItemSlot(item);
            if (result.found()) {
                InvUtils.swap(result.slot(), false);
            }
        }
    }

    public static Entity getEntitiesAtBlockPos(World world, BlockPos pos) {
        Box box = new Box(pos);

        List<Entity> entities = world.getEntitiesByClass(Entity.class, box, e -> true);

        return entities.isEmpty() ? null : entities.getFirst();
    }

    public static void setKeyPressed(KeyBinding key, boolean pressed) {
        key.setPressed(pressed);
        Input.setKeyState(key, pressed);
    }

    public static boolean airPlace(BlockPos pos, Direction direction) {
        if (mc.player == null || mc.getNetworkHandler() == null || mc.interactionManager == null) return false;

        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, direction));

        Hand hand = Hand.OFF_HAND;

        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos), direction.getOpposite(), pos, true);

        mc.interactionManager.interactBlock(mc.player, hand, hit);

        mc.player.swingHand(hand, false);

        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, direction));

        return true;
    }

    public static boolean airPlace(Item item, BlockPos pos, Direction direction) {
        if (!canPlaceBlock(pos, true, Block.getBlockFromItem(item))) return false;
        switchToItem(item);

        return airPlace(pos, direction);
    }

    public static boolean isGatheringItems() {
        return Modules.get().get("gather-item").isActive();
    }

    public static void swapToFortunePickaxe() {
        double bestScore = -1;
        int bestSlot = -1;

        for (int i = 0; i < 9; i++) {
            assert mc.player != null;
            ItemStack itemStack = mc.player.getInventory().getStack(i);

            if (hasEnchantments(itemStack, Enchantments.SILK_TOUCH)) continue;

            double score = itemStack.getMiningSpeedMultiplier(Blocks.ENDER_CHEST.getDefaultState());
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }

        if (bestSlot == -1) return;

        InvUtils.swap(bestSlot, false);
    }

    public static void toggleSneak(boolean flag) {
        mc.options.sneakKey.setPressed(flag);
    }

    public static void debug(String format, Object... args) {
        if (format == null || args == null) return;
        for (Object arg : args) {
            if (arg == null) return;
        }

        String formattedMessage = String.format(format, args);

        LocalTime now = LocalTime.now();
        String timestamp = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        String callerFileName = "Unknown";
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace.length > 2) {
            StackTraceElement caller = stackTrace[2];
            callerFileName = caller.getFileName(); // Retrieves the file name
        }

        String messageWithTimestamp = String.format("[%s] [%s] %s", timestamp, callerFileName, formattedMessage);

        final BoolSetting enableDebugMessages = (BoolSetting) Modules.get().get("HWU-highway-builder").settings.get("debug-messages");
        if (enableDebugMessages.get() && messageWithTimestamp.length() < 255)
            ChatUtils.info(messageWithTimestamp);
        else
            System.out.println(messageWithTimestamp);
    }

    public static void place(BlockPos blockPos, Hand hand, int slot, boolean swingHand, boolean checkEntities, boolean swapBack) {
        if (slot < 0 || slot > 8 || mc.player == null) return;

        Block toPlace = Blocks.BEDROCK;
        ItemStack i = hand == Hand.MAIN_HAND ? mc.player.getInventory().getStack(slot) : mc.player.getInventory().getStack(SlotUtils.OFFHAND);
        if (i.getItem() instanceof BlockItem blockItem) toPlace = blockItem.getBlock();
        if (!canPlaceBlock(blockPos, checkEntities, toPlace)) return;

        Vec3d hitPos = Vec3d.ofCenter(blockPos);

        BlockPos neighbour;
        Direction side = Direction.DOWN;

        neighbour = blockPos.offset(side);
        hitPos = hitPos.add(side.getOffsetX() * 0.5, side.getOffsetY() * 0.5, side.getOffsetZ() * 0.5);

        BlockHitResult bhr = new BlockHitResult(hitPos, side.getOpposite(), neighbour, false);

        InvUtils.swap(slot, swapBack);

        BlockUtils.interact(bhr, hand, swingHand);

        if (swapBack) InvUtils.swapBack();
    }


    static float[] getNeededRotations(ClientPlayerEntity player, Vec3d vec) {
        Vec3d eyesPos = player.getEyePos();

        double diffX = vec.x - eyesPos.x;
        double diffY = vec.y - eyesPos.y;
        double diffZ = vec.z - eyesPos.z;

        double r = Math.sqrt(diffX * diffX + diffY * diffY + diffZ * diffZ);
        double yaw = -Math.atan2(diffX, diffZ) / Math.PI * 180;

        double pitch = -Math.asin(diffY / r) / Math.PI * 180;

        return new float[]{(float) yaw, (float) pitch};
    }

    public static void lookAtBlock(BlockPos blockPos) {
        Vec3d hitPos = Vec3d.ofCenter(blockPos);

        Direction side = getPlaceSide(blockPos);

        if (side != null) {
            blockPos.offset(side);
            hitPos = hitPos.add(side.getOffsetX(), side.getOffsetY(), side.getOffsetZ());
        }

        assert mc.player != null;

        float[] rotation = getNeededRotations(mc.player, hitPos);
        mc.player.setYaw(rotation[0]);
        mc.player.setPitch(rotation[1]);
    }

    public static boolean checkItemsOnGround(List<BlockPos> locations) {
        if (locations.isEmpty()) return false;

        assert mc.player != null;
        for (BlockPos pos : locations) {
            boolean hasGroundBelow = false;

            for (int x = -1; x <= 1 && !hasGroundBelow; x++) {
                for (int z = -1; z <= 1 && !hasGroundBelow; z++) {
                    BlockPos checkPos = pos.add(x, -1, z);
                    if (!mc.player.getWorld().getBlockState(checkPos).isAir()) {
                        hasGroundBelow = true;
                    }
                }
            }

            if (!hasGroundBelow) return false;
        }

        return true;
    }

    // This probably doesn't work, but it may work in the future

//    public static List<Item> getShulkerBoxesNearby() {
//        if (mc.player == null || mc.player.getWorld() == null) {
//            return new ArrayList<>();
//        }
//
//        World world = mc.player.getWorld();
//        Box searchArea = new Box(
//                mc.player.getX() - SEARCH_RADIUS,
//                mc.player.getY() - SEARCH_RADIUS,
//                mc.player.getZ() - SEARCH_RADIUS,
//                mc.player.getX() + SEARCH_RADIUS,
//                MAX_Y,
//                mc.player.getZ() + SEARCH_RADIUS
//        );
//
//        List<Item> shulkerBoxItems = new ArrayList<>();
//
//        world.getEntitiesByClass(ItemEntity.class, searchArea,
//                        itemEntity -> Arrays.asList(shulkerBoxes).contains(itemEntity.getStack().getItem()))
//                .forEach(itemEntity -> {
//                    Item item = itemEntity.getStack().getItem();
//                    if (!shulkerBoxItems.contains(item)) {
//                        shulkerBoxItems.add(item);
//                    }
//                });
//
//        return shulkerBoxItems;
//    }
//
//    public static boolean areShulkerBoxesNearby() {
//        return !getShulkerBoxesNearby().isEmpty();
//    }

    // TODO:Add diagonal highway support
    public static void lockRotation() {
        assert mc.player != null;
        if (getPlayerDirection() == null) return;

        mc.player.setPitch(90);

        switch (getPlayerDirection()) {
            case Direction.NORTH:
                mc.player.setYaw(180);
                break;
            case Direction.SOUTH:
                mc.player.setYaw(0);
                break;
            case Direction.WEST:
                mc.player.setYaw(90);
                break;
            case Direction.EAST:
                mc.player.setYaw(-90);
                break;
        }
    }
}
