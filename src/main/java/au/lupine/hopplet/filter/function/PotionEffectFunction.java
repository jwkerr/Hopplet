package au.lupine.hopplet.filter.function;

import au.lupine.hopplet.Hopplet;
import au.lupine.hopplet.filter.Filter;
import au.lupine.hopplet.filter.Function;
import au.lupine.hopplet.filter.exception.FilterCompileException;
import au.lupine.hopplet.util.Comparator;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
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
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PotionEffectFunction implements Function<Set<PotionEffectFunction.Spec>> {

    @Override
    public @NonNull String name() {
        return "potion_effect";
    }

    @Override
    public @NonNull Set<String> aliases() {
        return Set.of("effect");
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
    public @NonNull Set<Spec> compile(@NonNull List<String> arguments) throws FilterCompileException {
        argsRequired(arguments);

        Set<Spec> potions = new HashSet<>();
        nextArgument: for (String argument : arguments) {
            String normalised = argument.toLowerCase().replaceAll("\\s", "");

            for (Comparator comparator : Comparator.values()) {
                String symbol = comparator.symbol();
                int index = normalised.indexOf(symbol);
                if (index < 0) continue;

                PotionEffectType effect = parseEffect(normalised.substring(0, index), argument);
                int level = parseAmplifier(normalised.substring(index + symbol.length()), argument);

                if (level < 1) {
                    throw new FilterCompileException(
                        Component.translatable(
                            "hopplet.filter.function.potion_effect.compilation.exception.amplifier_must_be_positive",
                            Argument.string("input", argument)
                        )
                    );
                }

                potions.add(new Spec(effect, comparator, level));
                continue nextArgument;
            }

            PotionEffectType effect = parseEffect(normalised, argument);
            potions.add(new Spec(effect, null, null));
        }

        return potions;
    }

    @Override
    public boolean test(Filter.@NonNull Context context, @NonNull Set<Spec> arguments) {
        ItemStack item = context.stack();

        if (!(item.getItemMeta() instanceof PotionMeta meta)) return false;

        List<PotionEffect> effects = new ArrayList<>();

        PotionType base = meta.getBasePotionType();
        if (base != null) effects.addAll(base.getPotionEffects());

        effects.addAll(meta.getCustomEffects());
        if (effects.isEmpty()) return false;

        for (Spec argument : arguments) {
            for (PotionEffect effect : effects) {
                if (!effect.getType().equals(argument.effect)) continue;

                if (argument.comparator == null || argument.amplifier == null) return true;

                if (argument.comparator.compare(effect.getAmplifier() + 1, argument.amplifier)) return true;
            }
        }

        return false;
    }

    private static @NonNull PotionEffectType parseEffect(@NonNull String name, @NonNull String argument) {
        if (name.isEmpty()) {
            throw new FilterCompileException(
                Component.translatable(
                    "hopplet.filter.function.potion_effect.compilation.exception.no_potion_effect_specified",
                    Argument.string("input", argument)
                )
            );
        }

        Registry<PotionEffectType> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.MOB_EFFECT);

        NamespacedKey key = NamespacedKey.fromString(name);
        PotionEffectType effect = key == null ? null : registry.get(key);
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

    private static int parseAmplifier(@NonNull String text, @NonNull String argument) {
        if (text.isEmpty()) {
            throw new FilterCompileException(
                Component.translatable(
                    "hopplet.filter.function.potion_effect.compilation.exception.no_amplifier_specified",
                    Argument.string("input", argument)
                )
            );
        }

        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new FilterCompileException(
                Component.translatable(
                    "hopplet.filter.function.potion_effect.compilation.exception.invalid_amplifier",
                    Argument.string("input", argument)
                )
            );
        }
    }

    public record Spec(@NonNull PotionEffectType effect, @Nullable Comparator comparator, @Nullable Integer amplifier) {}
}
