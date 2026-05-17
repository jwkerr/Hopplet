package au.lupine.hopplet.filter.function.impl;

import au.lupine.hopplet.Hopplet;
import au.lupine.hopplet.filter.context.Context;
import au.lupine.hopplet.filter.exception.FilterCompileException;
import au.lupine.hopplet.filter.function.Function;
import au.lupine.hopplet.filter.function.Matcher;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;

public final class LoreEndsWithFunction implements Matcher<String> {

    @Override
    public @NonNull String name() {
        return "lore_ends_with";
    }

    @Override
    public @NonNull Component description() {
        return Component.translatable("hopplet.filter.function.lore_ends_with.description");
    }

    @Override
    public @NonNull Plugin plugin() {
        return Hopplet.instance();
    }

    @Override
    public @NonNull String parse(@NonNull String argument) throws FilterCompileException {
        return argument;
    }

    @Override
    public boolean matches(@NonNull Context context, @NonNull String argument) {
        String lore = Function.lore(context.stack());
        if (lore == null) return false;

        return lore.endsWith(argument);
    }
}
