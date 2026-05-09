package au.lupine.hopplet.filter.function.impl;

import au.lupine.hopplet.Hopplet;
import au.lupine.hopplet.filter.context.Context;
import au.lupine.hopplet.filter.exception.FilterCompileException;
import au.lupine.hopplet.filter.function.Matcher;
import au.lupine.hopplet.util.Comparator;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import it.unimi.dsi.fastutil.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class PotionEffectFunction implements Matcher<Pair<PotionEffectType, Comparator>> {

    private static final Registry<PotionEffectType> POTION_EFFECT_TYPE_REGISTRY = RegistryAccess.registryAccess().getRegistry(RegistryKey.MOB_EFFECT);

    @Override
    public @NonNull String name() {
        return "potion_effect";
    }

    @Override
    public @NonNull Set<String> aliases() {
        return Set.of(
            "effect",
            "pot_eff",
            "eff"
        );
    }

    @Override
    public @NonNull Component description() {
        return Component.translatable("hopplet.filter.function.potion_effect.description");
    }

    @Override
    public @NonNull Plugin plugin() {
        return Hopplet.instance();
    }

    @Override
    public @NonNull MatchStrategy<Pair<PotionEffectType, Comparator>> strategy() {
        return MatchStrategy.all();
    }

    @Override
    public @NonNull Pair<PotionEffectType, Comparator> parse(@NonNull String argument) throws FilterCompileException {
        Pair<String, Comparator> pair = Comparator.split(argument, Comparator.of(Comparator.Type.GREATER_THAN_OR_EQUAL_TO, 1));

        PotionEffectType effect = effect(pair.left(), argument);
        Comparator comparator = pair.right();

        comparator.wholeValueOrThrow(argument);

        if (comparator.value() < 1) {
            throw new FilterCompileException(
                Component.translatable(
                    "hopplet.filter.function.potion_effect.compilation.exception.amplifier_must_be_positive",
                    Argument.string("input", argument)
                )
            );
        }

        return Pair.of(effect, comparator);
    }

    @Override
    public boolean matches(@NonNull Context context, @NonNull Pair<PotionEffectType, Comparator> pair) {
        ItemStack stack = context.stack();

        if (!(stack.getItemMeta() instanceof PotionMeta meta)) return false;

        List<PotionEffect> effects = new ArrayList<>();

        PotionType base = meta.getBasePotionType();
        if (base != null) effects.addAll(base.getPotionEffects());

        effects.addAll(meta.getCustomEffects());
        if (effects.isEmpty()) return false;

        PotionEffectType specified = pair.left();
        Comparator comparator = pair.right();

        for (PotionEffect effect : effects) {
            if (effect.getType() != specified) continue;

            if (comparator.test(effect.getAmplifier() + 1)) return true;
        }

        return false;
    }

    private static @NonNull PotionEffectType effect(@NonNull String name, @NonNull String argument) throws FilterCompileException {
        if (name.isEmpty()) {
            throw new FilterCompileException(
                Component.translatable(
                    "hopplet.filter.function.potion_effect.compilation.exception.no_potion_effect_specified",
                    Argument.string("input", argument)
                )
            );
        }

        NamespacedKey key = NamespacedKey.fromString(name);
        PotionEffectType effect = key == null ? null : POTION_EFFECT_TYPE_REGISTRY.get(key);
        if (effect == null) {
            throw new FilterCompileException(
                Component.translatable(
                    "hopplet.filter.function.potion_effect.compilation.exception.unknown_potion_effect",
                    Argument.string("input", argument)
                )
            );
        }

        return effect;
    }
}
