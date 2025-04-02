package HWU.group.addon.commands;

import HWU.group.addon.helpers.Utils;
import HWU.group.addon.config.ProxyConfig;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;

import java.util.List;

import static HWU.group.addon.modules.ProxySwitcher.handleAccountSwitch;

public class ProxyCommand extends Command {
    private final ProxyConfig proxyConfig = new ProxyConfig();

    public ProxyCommand() {
        super("proxy", "Manages your proxy settings.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("add")
                .then(argument("name", StringArgumentType.word())
                        .then(argument("ip", StringArgumentType.word())
                                .then(argument("port", StringArgumentType.word())
                                        .executes(context -> {
                                            String name = StringArgumentType.getString(context, "name");
                                            String ip = StringArgumentType.getString(context, "ip");
                                            String port = StringArgumentType.getString(context, "port");

                                            // Add proxy with IP and port
                                            proxyConfig.addProxy(name, ip + ":" + port, true);
                                            return SINGLE_SUCCESS;
                                        })
                                )
                                .executes(context -> {
                                    String name = StringArgumentType.getString(context, "name");
                                    String ip = StringArgumentType.getString(context, "ip");

                                    // Add proxy without port
                                    proxyConfig.addProxy(name, ip, true);
                                    return SINGLE_SUCCESS;
                                })
                        )
                )
        );

        builder.then(literal("remove")
                .then(argument("name", StringArgumentType.word())
                        .suggests((context2, builder2) -> Utils.suggestUsernames(builder2))
                        .executes(context -> {
                            String name = StringArgumentType.getString(context, "name");
                            proxyConfig.removeProxy(name, true);
                            return SINGLE_SUCCESS;
                        })
                )
        );

        builder.then(literal("list")
                .executes(context -> {
                    List<String> proxies = proxyConfig.listProxies();
                    if (!proxies.isEmpty())
                        info("Proxy list:");

                    proxies.forEach(this::info);
                    return SINGLE_SUCCESS;
                })
        );

        builder.then(literal("clear")
                .executes(context -> {
                    proxyConfig.clearProxies();
                    return SINGLE_SUCCESS;
                })
        );

        builder.then(literal("switch")
                .then(argument("username", StringArgumentType.word())
                        .suggests((context1, builder1) -> Utils.suggestUsernames(builder1))
                        .executes(context -> {
                            String username = StringArgumentType.getString(context, "username");
                            handleAccountSwitch(username);
                            return SINGLE_SUCCESS;
                        })
                )
        );

        builder.then(literal("edit-username")
                .then(argument("oldName", StringArgumentType.word())
                        .then(argument("newName", StringArgumentType.word())
                                .executes(context -> {
                                    String oldName = StringArgumentType.getString(context, "oldName");
                                    String newName = StringArgumentType.getString(context, "newName");

                                    proxyConfig.editUsername(oldName, newName);
                                    return SINGLE_SUCCESS;
                                })
                        )
                )
        );

        builder.then(literal("edit-server-ip")
                .then(argument("name", StringArgumentType.word())
                        .then(argument("newIp", StringArgumentType.word())
                                .then(argument("newPort", StringArgumentType.word())
                                        .executes(context -> {
                                            String name = StringArgumentType.getString(context, "name");
                                            String newIp = StringArgumentType.getString(context, "newIp");
                                            String newPort = StringArgumentType.getString(context, "newPort");

                                            // Edit proxy with new IP and port
                                            proxyConfig.editServerIp(name, newIp + ":" + newPort);
                                            return SINGLE_SUCCESS;
                                        })
                                )
                                .executes(context -> {
                                    String name = StringArgumentType.getString(context, "name");
                                    String newIp = StringArgumentType.getString(context, "newIp");

                                    // Edit proxy with new IP only
                                    proxyConfig.editServerIp(name, newIp);
                                    return SINGLE_SUCCESS;
                                })
                        )
                )
        );
    }
}
