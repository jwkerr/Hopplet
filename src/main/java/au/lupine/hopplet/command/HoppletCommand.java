package au.lupine.hopplet.command;

import au.lupine.hopplet.Hopplet;
import au.lupine.hopplet.filter.Function;
import au.lupine.hopplet.util.edit.EditDialog;
import au.lupine.hopplet.util.edit.HopperEditTarget;
import au.lupine.hopplet.util.edit.HopperMinecartEditTarget;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.event.block.BlockBreakEvent;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.stream.Collectors;

public final class HoppletCommand {

    public static @NonNull LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("hopplet")
            .requires(source -> source.getSender().hasPermission("hopplet.command.hopplet"))
            .executes(context -> {
                context.getSource().getSender().sendMessage(
                    Component.translatable(
                        "hopplet.command.hopplet.feedback",
                        Argument.string("version", Hopplet.instance().getPluginMeta().getVersion())
                    )
                );
                return Command.SINGLE_SUCCESS;
            })
            .then(Commands.literal("edit")
                .requires(source -> source.getSender() instanceof Player && source.getSender().hasPermission("hopplet.command.hopplet.edit"))
                .executes(context -> {
                    Player player = (Player) context.getSource().getSender();

                    Entity entity = player.getTargetEntity(8, false);
                    if (entity instanceof HopperMinecart hopper) {
                        BlockBreakEvent bbe = new BlockBreakEvent(hopper.getLocation().getBlock(), player);
                        if (!bbe.callEvent()) {
                            player.sendMessage(Component.translatable("hopplet.command.hopplet.edit.feedback.no_permission"));
                            return 0;
                        }

                        EditDialog.open(player, new HopperMinecartEditTarget(hopper));
                        return Command.SINGLE_SUCCESS;
                    }

                    Block block = player.getTargetBlockExact(8);
                    if (block != null && block.getState() instanceof Hopper hopper) {
                        BlockBreakEvent bbe = new BlockBreakEvent(hopper.getBlock(), player);
                        if (!bbe.callEvent()) {
                            player.sendMessage(Component.translatable("hopplet.command.hopplet.edit.feedback.no_permission"));
                            return 0;
                        }

                        EditDialog.open(player, new HopperEditTarget(hopper));
                        return Command.SINGLE_SUCCESS;
                    }

                    player.sendMessage(Component.translatable("hopplet.command.hopplet.edit.feedback.no_target"));
                    return 0;
                })
            )
            .then(Commands.literal("function")
                .requires(source -> source.getSender().hasPermission("hopplet.command.hopplet.function"))
                .then(Commands.argument("function", StringArgumentType.string())
                    .suggests(((context, builder) -> {
                        Map<String, List<Function<?>>> names = new HashMap<>();
                        for (Function<?> function : Function.FUNCTIONS) {
                            names.computeIfAbsent(function.name(), k -> new ArrayList<>()).add(function);
                        }

                        String remaining = builder.getRemaining().toLowerCase();

                        for (Map.Entry<String, List<Function<?>>> entry : names.entrySet()) {
                            List<Function<?>> functions = entry.getValue();

                            if (functions.size() == 1) {
                                String name = entry.getKey();
                                if (name.toLowerCase().startsWith(remaining)) builder.suggest(name);
                            } else {
                                for (Function<?> function : functions) {
                                    String key = function.key().toString();
                                    if (key.toLowerCase().startsWith(remaining)) builder.suggest(key);
                                }
                            }
                        }

                        return builder.buildFuture();
                    }))
                    .executes(context -> {
                        String argument = context.getArgument("function", String.class);
                        Function<?> function = Function.of(argument);

                        CommandSender sender = context.getSource().getSender();
                        if (function == null) {
                            sender.sendMessage(Component.translatable(
                                "hopplet.command.hopplet.function.feedback.unknown_function",
                                Argument.string("name", argument)
                            ));
                            return 0;
                        }

                        String capitalised = Arrays.stream(function.name().split("_"))
                                .map(word -> word.isEmpty() ? word
                                    : Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase())
                                    .collect(Collectors.joining(" "));

                        sender.sendMessage(Component.translatable(
                            "hopplet.command.hopplet.function.feedback.function_info",
                            Argument.component("name", Component.text(
                                capitalised,
                                NamedTextColor.GOLD,
                                TextDecoration.BOLD
                            )),
                            Argument.component("description", function.description())
                        ));

                        return Command.SINGLE_SUCCESS;
                    })
                )
            )
            .then(Commands.literal("reload")
                .requires(source -> source.getSender().hasPermission("hopplet.command.hopplet.reload"))
                .executes(context -> {
                    Hopplet.instance().reload();

                    context.getSource().getSender().sendMessage(Component.translatable("hopplet.command.hopplet.reload.feedback.success"));
                    return Command.SINGLE_SUCCESS;
                })
            )
            .build();
    }
}
