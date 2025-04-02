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
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.*;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;

import java.util.List;
import java.util.Objects;

import static meteordevelopment.meteorclient.utils.Utils.hasEnchantments;

public class BetterAutoReplenish extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    /* not used (example)
    private final Setting<List<Item>> block_list = sgGeneral.add(new ItemListSetting.Builder()
            .name("Blocks")
            .description("List of blocks paver can mine.")
            .defaultValue(Items.AIR)
            .build()
    );*/

    private final Setting<Integer> threshold = sgGeneral.add(new IntSetting.Builder()
            .name("threshold")
            .description("The threshold of items left to trigger replenishment.")
            .defaultValue(16)
            .min(1)
            .sliderRange(1, 63)
            .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("Delay in ticks between operations.")
            .defaultValue(2)
            .min(0)
            .sliderRange(0, 20)
            .build()
    );

    private final Setting<Item> slot2Item = sgGeneral.add(new ItemSetting.Builder()
            .name("slot-2-item")
            .description("Item to maintain in the second hotbar slot.")
            .defaultValue(Items.AIR)
            .build()
    );

    private final Setting<Item> slot3Item = sgGeneral.add(new ItemSetting.Builder()
            .name("slot-3-item")
            .description("Item to maintain in the third hotbar slot.")
            .defaultValue(Items.AIR)
            .build()
    );

    private final Setting<Item> slot4Item = sgGeneral.add(new ItemSetting.Builder()
            .name("slot-4-item")
            .description("Item to maintain in the fourth hotbar slot.")
            .defaultValue(Items.AIR)
            .build()
    );

    private final Setting<Item> slot5Item = sgGeneral.add(new ItemSetting.Builder()
            .name("slot-5-item")
            .description("Item to maintain in the fifth hotbar slot.")
            .defaultValue(Items.AIR)
            .build()
    );

    private final Setting<Item> slot6Item = sgGeneral.add(new ItemSetting.Builder()
            .name("slot-6-item")
            .description("Item to maintain in the sixth hotbar slot.")
            .defaultValue(Items.AIR)
            .build()
    );

    private final Setting<Item> slot7Item = sgGeneral.add(new ItemSetting.Builder()
            .name("slot-7-item")
            .description("Item to maintain in the seventh hotbar slot.")
            .defaultValue(Items.AIR)
            .build()
    );

    private final Setting<Item> slot8Item = sgGeneral.add(new ItemSetting.Builder()
            .name("slot-8-item")
            .description("Item to maintain in the eighth hotbar slot.")
            .defaultValue(Items.AIR)
            .build()
    );

    private int tickCounter = 0;
    private boolean firstRun = true;

    public BetterAutoReplenish() {
        super(HWU_HWBuilder.HELPER_MODULES_CATEGORY, "better-auto-replenish",
                "Automatically refills specific items in each hotbar slot.");
    }

    @Override
    public void onActivate() {
        InvUtils.dropHand();
        tickCounter = 0;
        firstRun = true;
    }

    @Override
    public void onDeactivate() {
        InvUtils.dropHand();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (delay.get() > 0 && !firstRun) {
            if (tickCounter < delay.get()) {
                tickCounter++;
                return;
            }
            tickCounter = 0;
        } else {
            delay.get();
        }

        boolean flag = false;

        findAndMoveBestToolToFirstHotbarSlot();
        checkEChestShulkerSlot();

        Item[] itemsToCheck = new Item[]{
                slot2Item.get(), slot3Item.get(),
                slot4Item.get(), slot5Item.get(),
                slot6Item.get(), slot7Item.get(),
                slot8Item.get()
        };

        for (int i = 1; i <= 7; i++) {
            // TODO:Check for food instead of checking for air slots
            // TODO:Check for E-Chests, Obsidian, and warn the user about E-Gaps
            if (!itemsToCheck[i - 1].equals(Items.AIR) && !flag) flag = true;

            checkSlotWithDesignatedItem(i, itemsToCheck[i - 1]);
        }

        if (!flag) {
            // TODO:Tell the user to add a food slot
            error("Better Auto Replenish is not configured correctly, please configure the module and enable the \"Highway Builder\" module once again.");
            Module HWUHighwayBuilder = Modules.get().get("HWU-highway-builder");
            if (HWUHighwayBuilder.isActive()) HWUHighwayBuilder.toggle();
            toggle();
        }

        // After first run completed
        if (firstRun) {
            firstRun = false;
        }
    }

    private void checkEChestShulkerSlot() {
        assert mc.player != null;

        ItemStack currentStack = mc.player.getInventory().getStack(8); // 9th slot (0-indexed)

        if (!(currentStack.getItem() instanceof BlockItem &&
                ((BlockItem) currentStack.getItem()).getBlock() instanceof ShulkerBoxBlock) ||
                !hasEnderChestInShulker(currentStack)) {

            ItemStack shulkerWithEnderChest = findBestEnderChestShulker();

            if (shulkerWithEnderChest != null) {
                int sourceSlot = findItemStackSlot(shulkerWithEnderChest);
                if (sourceSlot != -1) {
                    InvUtils.move().from(sourceSlot).to(8); // 9th slot (0-indexed)
                }
            }
        }
    }

    private static boolean hasEnderChestInShulker(ItemStack shulkerBox) {
        ItemStack[] containerItems = new ItemStack[27];
        Utils.getItemsInContainerItem(shulkerBox, containerItems);

        for (ItemStack stack : containerItems) {
            if (!stack.isEmpty() && stack.getItem() == Items.ENDER_CHEST) {
                return true;
            }
        }
        return false;
    }

    public static ItemStack findBestEnderChestShulker() {
        for (int i = 0; i < Objects.requireNonNull(mc.player).getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);

            if (stack.getItem() instanceof BlockItem &&
                    ((BlockItem) stack.getItem()).getBlock() instanceof ShulkerBoxBlock) {

                if (hasEnderChestInShulker(stack)) {
                    return stack;
                }
            }
        }
        return null;
    }

    private int findItemStackSlot(ItemStack stack) {
        for (int i = 0; i < Objects.requireNonNull(mc.player).getInventory().size(); i++) {
            if (mc.player.getInventory().getStack(i).equals(stack)) {
                return i;
            }
        }
        return -1;
    }

    private void findAndMoveBestToolToFirstHotbarSlot() {
        Inventory inventory = mc.player.getInventory();

        int firstHotbarSlot = 0;
        ItemStack firstHotbarStack = inventory.getStack(firstHotbarSlot);

        if (firstHotbarStack.getItem() instanceof PickaxeItem
                && firstHotbarStack.getMaxDamage() - firstHotbarStack.getDamage() > 50
                && !hasEnchantments(firstHotbarStack, Enchantments.SILK_TOUCH)) { // TODO:Don't check for non-silkTouch picks if the player is not paving the highway
            return; // The first slot already has a valid pickaxe
        }

        int bestSlot = -1;

        for (int i = 0; i < inventory.size(); i++) {
            if (i == firstHotbarSlot) continue; // Skip the first slot

            ItemStack stack = inventory.getStack(i);
            if (stack.getItem() instanceof PickaxeItem
                    && stack.getMaxDamage() - stack.getDamage() > 50
                    && !hasEnchantments(stack, Enchantments.SILK_TOUCH)) { // TODO:Don't check for non-silkTouch picks if the player is not paving the highway
                bestSlot = i;
                break;
            }
        }

        if (bestSlot != -1) {
            InvUtils.move().from(bestSlot).toHotbar(firstHotbarSlot);
        }
    }

    private void checkSlotWithDesignatedItem(int slot, Item desiredItem) {
        assert mc.player != null;
        ItemStack currentStack = mc.player.getInventory().getStack(slot);

        if (desiredItem == Items.AIR) return;

        if (currentStack.isEmpty() || currentStack.getItem() != desiredItem) {
            int foundSlot = findSpecificItem(desiredItem, slot, threshold.get());
            if (foundSlot != -1) {
                addSlots(slot, foundSlot);
            }
        }
        else if (currentStack.isStackable() && currentStack.getCount() <= threshold.get()) {
            int foundSlot = findSpecificItem(desiredItem, slot, threshold.get() - currentStack.getCount() + 1);
            if (foundSlot != -1) {
                addSlots(slot, foundSlot);
            }
        }
    }

    private int findSpecificItem(Item item, int excludedSlot, int goodEnoughCount) {
        int slot = -1;
        int count = 0;

        assert mc.player != null;
        for (int i = mc.player.getInventory().size() - 2; i >= 0; i--) {
            ItemStack stack = mc.player.getInventory().getStack(i);

            if (i != excludedSlot && stack.getItem() == item) {
                if (stack.getCount() > count) {
                    slot = i;
                    count = stack.getCount();

                    if (count >= goodEnoughCount) break;
                }
            }
        }

        return slot;
    }

    private void addSlots(int to, int from) {
        InvUtils.move().from(from).to(to);
    }
}
