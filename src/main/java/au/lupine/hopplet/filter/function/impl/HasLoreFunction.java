package au.lupine.hopplet.filter.function.impl;

import au.lupine.hopplet.Hopplet;
import au.lupine.hopplet.filter.context.Context;
import au.lupine.hopplet.filter.function.Predicate;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;

import java.util.Set;

public final class HasLoreFunction implements Predicate {

    @Override
    public @NonNull String name() {
        return "has_lore";
    }

    @Override
    public @NonNull Set<String> aliases() {
        return Set.of("lored");
    }

    @Override
    public @NonNull Component description() {
        return Component.translatable("hopplet.filter.function.has_lore.description");
    }

    @Override
    public @NonNull Plugin plugin() {
        return Hopplet.instance();
    }

    @Override
    public boolean test(@NonNull Context context, @NonNull Void compiled) {
        return context.stack().getItemMeta().hasLore();
    }
}
