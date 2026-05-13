package au.lupine.hopplet.filter.function.impl;

import au.lupine.bottlet.api.Bottle;
import au.lupine.hopplet.Hopplet;
import au.lupine.hopplet.filter.context.Context;
import au.lupine.hopplet.filter.exception.FilterCompileException;
import au.lupine.hopplet.filter.function.Matcher;
import au.lupine.hopplet.util.Comparator;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;

import java.util.Set;

public final class StoredExperienceFunction implements Matcher<Comparator> {

    @Override
    public @NonNull String name() {
        return "stored_experience";
    }

    @Override
    public @NonNull Set<String> aliases() {
        return Set.of(
            "stored_exp",
            "stored_xp",
            "experience",
            "exp",
            "xp"
        );
    }

    @Override
    public @NonNull Component description() {
        return Component.translatable("hopplet.filter.function.stored_experience.description");
    }

    @Override
    public @NonNull Plugin plugin() {
        return Hopplet.instance();
    }

    @Override
    public boolean disabled() {
        return !Hopplet.instance().getServer()
            .getPluginManager()
            .isPluginEnabled("Bottlet");
    }

    @Override
    public @NonNull MatchStrategy<Comparator> strategy() {
        return MatchStrategy.all();
    }

    @Override
    public @NonNull Comparator parse(@NonNull String argument) throws FilterCompileException {
        return Comparator.of(argument);
    }

    @Override
    public boolean matches(@NonNull Context context, @NonNull Comparator comparator) {
        ItemStack stack = context.stack();
        if (stack.getType() != Material.EXPERIENCE_BOTTLE) return false;

        return comparator.test(Bottle.stored(stack));
    }
}
