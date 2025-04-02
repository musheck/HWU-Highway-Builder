/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 *
 * Edited by HWU Group (2b2t).
 */

package HWU.group.addon.modules;

import HWU.group.addon.HWU_HWBuilder;
import net.minecraft.item.SpawnEggItem;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.BlockItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;

import static HWU.group.addon.helpers.Utils.airPlace;

public class GrimAirPlace extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRange = settings.createGroup("Range");

    // General
    private final Setting<Integer> placeCooldown = sgRange.add(new IntSetting.Builder()
            .name("place-cooldown")
            .description("Cooldown between block placements (in ticks).")
            .defaultValue(3)
            .min(0)
            .sliderMax(20)
            .build()
    );

    private final Setting<Boolean> render = sgGeneral.add(new BoolSetting.Builder()
            .name("render")
            .description("Renders a block overlay where the obsidian will be placed.")
            .defaultValue(true)
            .build()
    );

    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build()
    );

    private final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The color of the sides of the blocks being rendered.")
            .defaultValue(new SettingColor(204, 0, 0, 10))
            .build()
    );

    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The color of the lines of the blocks being rendered.")
            .defaultValue(new SettingColor(204, 0, 0, 255))
            .build()
    );

    // Range
    private final Setting<Boolean> customRange = sgRange.add(new BoolSetting.Builder()
            .name("custom-range")
            .description("Use custom range for air place.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Double> range = sgRange.add(new DoubleSetting.Builder()
            .name("range")
            .description("Custom range to place at.")
            .visible(customRange::get)
            .defaultValue(5)
            .min(0)
            .sliderMax(6)
            .build()
    );

    private HitResult hitResult;

    public GrimAirPlace() {
        super(HWU_HWBuilder.CATEGORY, "grim-air-place", "Places a block where your crosshair is pointing at.");
    }

    private int tickCooldown = 0;

    @EventHandler
    private void onTick(TickEvent.Post event) {
        double r = customRange.get() ? range.get() : mc.player.getBlockInteractionRange();
        hitResult = mc.getCameraEntity().raycast(r, 0, false);

        if (!(hitResult instanceof BlockHitResult blockHitResult) || !(mc.player.getMainHandStack().getItem() instanceof BlockItem) && !(mc.player.getMainHandStack().getItem() instanceof SpawnEggItem)) return;

        if (tickCooldown > 0) tickCooldown--;

        if (mc.options.useKey.isPressed()) {
            if (tickCooldown == 0) {
                airPlace(blockHitResult.getBlockPos(), Direction.DOWN);
                tickCooldown = placeCooldown.get();
            }
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!(hitResult instanceof BlockHitResult blockHitResult)) return;

        if (!mc.world.getBlockState(blockHitResult.getBlockPos()).isReplaceable()
                || !(mc.player.getMainHandStack().getItem() instanceof BlockItem)
                && !(mc.player.getMainHandStack().getItem() instanceof SpawnEggItem)) return;

        if (render.get()) {
            event.renderer.box(
                    blockHitResult.getBlockPos(),
                    new SettingColor(sideColor.get().r, sideColor.get().g, sideColor.get().b, sideColor.get().a / 2),
                    new SettingColor(lineColor.get().r, lineColor.get().g, lineColor.get().b, 100),
                    ShapeMode.Sides,
                    1
            );

            event.renderer.box(
                    blockHitResult.getBlockPos(),
                    sideColor.get(),
                    lineColor.get(),
                    shapeMode.get(),
                    0
            );
        }
    }
}