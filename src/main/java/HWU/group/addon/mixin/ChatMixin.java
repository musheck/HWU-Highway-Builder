package HWU.group.addon.mixin;

import HWU.group.addon.modules.CoordsHider;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(ChatHud.class)
public class ChatMixin {

    @Unique
    private static final Pattern COORD_PATTERN = Pattern.compile("(?<!\\S)(-?\\d+(\\.\\d+)?)[\\s,/]+(-?\\d+(\\.\\d+)?)[\\s,/]+(-?\\d+(\\.\\d+)?)(?!\\S)");

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), cancellable = true)
    private void onAddMessage(Text message, CallbackInfo ci) {
        if (!CoordsHider.getIsEnabled()) return;

        String messageStr = message.getString();
        Matcher matcher = COORD_PATTERN.matcher(messageStr);

        if (matcher.find()) {
            String coords = matcher.group();
            String modifiedText = messageStr.replace("[Meteor] ", "").replaceAll(COORD_PATTERN.pattern(), "[coordinates]");

            MutableText mainText = Text.literal(modifiedText)
                    .setStyle(Style.EMPTY.withColor(TextColor.fromFormatting(Formatting.GREEN))
                            .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, coords)));

            MutableText clickableCoords = Text.literal(" (click to copy)")
                    .setStyle(Style.EMPTY
                            .withColor(TextColor.fromFormatting(Formatting.YELLOW))
                            .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, coords)));

            mainText.append(clickableCoords);

            ((ChatHud) (Object) this).addMessage(mainText);
            ci.cancel();
        }
    }
}
