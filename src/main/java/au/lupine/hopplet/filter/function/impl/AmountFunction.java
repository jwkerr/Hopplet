package au.lupine.hopplet.filter.function.impl;

import au.lupine.hopplet.Hopplet;
import au.lupine.hopplet.filter.context.Context;
import au.lupine.hopplet.filter.exception.FilterCompileException;
import au.lupine.hopplet.filter.function.Matcher;
import au.lupine.hopplet.util.Comparator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;

import java.util.Set;

public final class AmountFunction implements Matcher<Comparator> {

    @Override
    public @NonNull String name() {
        return "amount";
    }

    @Override
    public @NonNull Set<String> aliases() {
        return Set.of("size");
    }

    @Override
    public @NonNull Component description() {
        return Component.translatable("hopplet.filter.function.amount.description");
    }

    @Override
    public @NonNull Plugin plugin() {
        return Hopplet.instance();
    }

    @Override
    public @NonNull Comparator parse(@NonNull String argument) throws FilterCompileException {
        Comparator comparator = Comparator.of(argument);

        comparator.wholeValueOrThrow(argument);

        if (comparator.value() < 1) {
            throw new FilterCompileException(
                Component.translatable(
                    "hopplet.filter.function.amount.compilation.exception.amount_must_be_positive",
                    Argument.string("input", argument)
                )
            );
        }

        return comparator;
    }

    @Override
    public boolean matches(@NonNull Context context, @NonNull Comparator comparator) {
        // It's important to note that an InventoryMoveItemEvent will always have a stack size of 1.
        // As such, this function is only really useful for item entities.
        return comparator.test(context.stack().getAmount());
    }
}
