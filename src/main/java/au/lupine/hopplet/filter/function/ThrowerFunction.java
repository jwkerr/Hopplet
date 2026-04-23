package au.lupine.hopplet.filter.function;

import au.lupine.hopplet.Hopplet;
import au.lupine.hopplet.filter.Filter;
import au.lupine.hopplet.filter.Function;
import au.lupine.hopplet.filter.exception.FilterCompileException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Item;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ThrowerFunction implements Function<Set<String>> {

    @Override
    public @NonNull String name() {
        return "thrower";
    }

    @Override
    public @NonNull Set<String> aliases() {
        return Set.of("thrown_by");
    }

    @Override
    public @NonNull Component description() {
        return Component.translatable("hopplet.filter.function.thrower.description");
    }

    @Override
    public @NonNull Plugin plugin() {
        return Hopplet.instance();
    }

    @Override
    public @NonNull Set<String> compile(@NonNull List<String> arguments) throws FilterCompileException {
        if (arguments.isEmpty()) {
            throw new FilterCompileException(
                Component.translatable(
                    "hopplet.filter.function.default.compilation.exception.no_arguments_provided",
                    Argument.string("name", name())
                )
            );
        }

        return new HashSet<>(arguments);
    }

    @Override
    public boolean test(Filter.@NonNull Context context, @NonNull Set<String> arguments) {
        Item item = context.item();
        if (item == null) return false;

        UUID thrower = item.getThrower();
        if (thrower == null) return false;

        for (String argument : arguments) {
            try {
                UUID uuid = UUID.fromString(argument);
                if (thrower.equals(uuid)) return true;
            } catch (IllegalArgumentException e) {
                OfflinePlayer player = Bukkit.getOfflinePlayerIfCached(argument);
                if (player == null) continue;

                return thrower.equals(player.getUniqueId());
            }
        }

        return false;
    }
}
