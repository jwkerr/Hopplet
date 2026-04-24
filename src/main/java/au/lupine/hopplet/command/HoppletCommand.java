package au.lupine.hopplet.command;

import au.lupine.hopplet.Hopplet;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NonNull;

public final class HoppletCommand {

    public static @NonNull LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("hopplet")
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
