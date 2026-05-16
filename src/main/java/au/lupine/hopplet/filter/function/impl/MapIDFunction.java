package au.lupine.hopplet.filter.function.impl;

import au.lupine.hopplet.Hopplet;
import au.lupine.hopplet.filter.context.Context;
import au.lupine.hopplet.filter.exception.FilterCompileException;
import au.lupine.hopplet.filter.function.Matcher;
import au.lupine.hopplet.util.Comparator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;

import java.util.Set;

public final class MapIDFunction implements Matcher<Comparator> {

    @Override
    public @NonNull String name() {
        return "map_id";
    }

    @Override
    public @NonNull Set<String> aliases() {
        return Set.of("map");
    }

    @Override
    public @NonNull Component description() {
        return Component.translatable("hopplet.filter.function.map_id.description");
    }

    @Override
    public @NonNull Plugin plugin() {
        return Hopplet.instance();
    }

    @Override
    public @NonNull MatchStrategy<Comparator> strategy() {
        return MatchStrategy.all();
    }

    @Override
    public @NonNull Comparator parse(@NonNull String argument) throws FilterCompileException {
        Comparator comparator = Comparator.of(argument);

        comparator.wholeValueOrThrow(argument);

        if (comparator.value() < 1) {
            throw new FilterCompileException(
                Component.translatable(
                    "hopplet.filter.function.map_id.compilation.exception.map_id_must_be_positive",
                    Argument.string("input", argument)
                )
            );
        }

        return comparator;
    }

    @Override
    public boolean matches(@NonNull Context context, @NonNull Comparator comparator) {
        ItemStack stack = context.stack();

        if (!(stack.getItemMeta() instanceof MapMeta map)) return false;

        if (!map.hasMapId()) return false;

        return comparator.test(map.getMapId());
    }
}
