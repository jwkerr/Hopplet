package au.lupine.hopplet.filter.function.impl;

import au.lupine.hopplet.Hopplet;
import au.lupine.hopplet.filter.context.Context;
import au.lupine.hopplet.filter.exception.FilterCompileException;
import au.lupine.hopplet.filter.function.Matcher;
import au.lupine.hopplet.util.Comparator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class PotionDurationFunction implements Matcher<Comparator> {

    @Override
    public @NonNull String name() {
        return "potion_duration";
    }

    @Override
    public @NonNull Set<String> aliases() {
        return Set.of(
            "duration",
            "pot_dur"
        );
    }

    @Override
    public @NonNull Component description() {
        return Component.translatable("hopplet.filter.function.potion_duration.description");
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

        if (normalised.equals("infinite") || normalised.equals("unlimited")) return Comparator.of(PotionEffect.INFINITE_DURATION);

        Comparator comparator = Comparator.of(normalised);

        comparator.wholeValueOrThrow(argument);

        if (comparator.value() < 0) {
            throw new FilterCompileException(
                Component.translatable(
                    "hopplet.filter.function.potion_duration.compilation.exception.negative_duration",
                    Argument.string("input", argument)
                )
            );
        }

        return comparator;
    }

    @Override
    public boolean matches(@NonNull Context context, @NonNull Comparator comparator) {
        ItemStack stack = context.stack();

        if (!(stack.getItemMeta() instanceof PotionMeta meta)) return false;

        List<PotionEffect> effects = new ArrayList<>();

        PotionType base = meta.getBasePotionType();
        if (base != null) effects.addAll(base.getPotionEffects());

        effects.addAll(meta.getCustomEffects());
        if (effects.isEmpty()) return false;

        boolean infiniteComparator = comparator.value() == PotionEffect.INFINITE_DURATION;

        for (PotionEffect effect : effects) {
            boolean infiniteEffect = effect.isInfinite();

            if (infiniteEffect && infiniteComparator) return true;

            Comparator.Type type = comparator.type();

            if (infiniteEffect) {
                if (type == Comparator.Type.GREATER_THAN_OR_EQUAL_TO ||
                    type == Comparator.Type.GREATER_THAN ||
                    type == Comparator.Type.NOT_EQUAL
                ) return true;

                continue;
            }

            if (infiniteComparator) {
                if (type == Comparator.Type.LESS_THAN_OR_EQUAL_TO ||
                    type == Comparator.Type.LESS_THAN ||
                    type == Comparator.Type.NOT_EQUAL
                ) return true;

                continue;
            }

            if (comparator.test(effect.getDuration())) return true;
        }

        return false;
    }
}
