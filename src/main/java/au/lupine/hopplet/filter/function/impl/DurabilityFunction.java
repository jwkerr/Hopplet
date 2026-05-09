package au.lupine.hopplet.filter.function.impl;

import au.lupine.hopplet.Hopplet;
import au.lupine.hopplet.filter.context.Context;
import au.lupine.hopplet.filter.exception.FilterCompileException;
import au.lupine.hopplet.filter.function.Matcher;
import au.lupine.hopplet.util.Comparator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;

import java.util.Set;

public final class DurabilityFunction implements Matcher<Comparator> {

    @Override
    public @NonNull String name() {
        return "durability";
    }

    @Override
    public @NonNull Set<String> aliases() {
        return Set.of("dur");
    }

    @Override
    public @NonNull Component description() {
        return Component.translatable("hopplet.filter.function.durability.description");
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
        String normalised = argument.toLowerCase().replaceAll("\\s", "");

        if (normalised.equals("max") || normalised.equals("undamaged")) return Comparator.of(100);

        Comparator comparator = Comparator.of(normalised);

        double value = comparator.value();
        if (value < 0) {
            throw new FilterCompileException(
                Component.translatable(
                    "hopplet.filter.function.durability.compilation.exception.less_than_zero",
                    Argument.string("input", argument)
                )
            );
        }

        if (value > 100) {
            throw new FilterCompileException(
                Component.translatable(
                    "hopplet.filter.function.durability.compilation.exception.greater_than_one_hundred",
                    Argument.string("input", argument)
                )
            );
        }

        return comparator;
    }

    @Override
    public boolean matches(@NonNull Context context, @NonNull Comparator comparator) {
        ItemStack item = context.stack();
        if (!(item.getItemMeta() instanceof Damageable meta)) return false;

        short max = item.getType().getMaxDurability();
        if (max <= 0) return false;

        int remaining = max - meta.getDamage();
        double percentage = (double) (remaining * 100L) / max;

        return comparator.test(percentage);
    }
}
