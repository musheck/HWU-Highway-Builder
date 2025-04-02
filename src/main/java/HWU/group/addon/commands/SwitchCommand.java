package HWU.group.addon.commands;

import HWU.group.addon.helpers.Utils;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;
import static HWU.group.addon.modules.ProxySwitcher.handleAccountSwitch;

public class SwitchCommand extends Command {

    public SwitchCommand() {
        super("switch", "Switch between accounts.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("username", StringArgumentType.word())
            .suggests((context1, builder1) -> Utils.suggestUsernames(builder1))
            .executes(context -> {
                String username = StringArgumentType.getString(context, "username");
                handleAccountSwitch(username);
                return SINGLE_SUCCESS;
            })
        );
    }
}
