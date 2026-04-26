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
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EnchantmentFunction implements Function<Set<EnchantmentFunction.Spec>> {

    @Override
    public @NonNull String name() {
        return "enchantment";
    }

    @Override
    public @NonNull Set<String> aliases() {
        return Set.of(
            "ench",
            "enchant"
        );
    }

    @Override
    public @NonNull Component description() {
        return Component.translatable("hopplet.filter.function.enchantment.description");
    }

    @Override
    public @NonNull Plugin plugin() {
        return Hopplet.instance();
    }

    @Override
    public @NonNull Set<Spec> compile(@NonNull List<String> arguments) throws FilterCompileException {
        argsRequired(arguments);

        Set<Spec> enchantments = new HashSet<>();
        nextArgument: for (String argument : arguments) {
            String normalised = argument.toLowerCase().replaceAll("\\s", "");

            for (Comparator comparator : Comparator.values()) {
                String symbol = comparator.symbol();
                int index = normalised.indexOf(symbol);
                if (index < 0) continue;

                Enchantment enchantment = parseEnchantment(normalised.substring(0, index), argument);
                int level = parseLevel(normalised.substring(index + symbol.length()), argument);

                if (level < 1) {
                    throw new FilterCompileException(
                        Component.translatable(
                            "hopplet.filter.function.enchantment.compilation.exception.level_must_be_positive",
                            Argument.string("input", argument)
                        )
                    );
                }

                enchantments.add(new Spec(enchantment, comparator, level));
                continue nextArgument;
            }

            Enchantment enchantment = parseEnchantment(normalised, argument);
            enchantments.add(new Spec(enchantment, null, null));
        }

        return enchantments;
    }

    private static @NonNull Enchantment parseEnchantment(@NonNull String name, @NonNull String argument) {
        if (name.isEmpty()) {
            throw new FilterCompileException(
                Component.translatable(
                    "hopplet.filter.function.enchantment.compilation.exception.no_enchantment_specified",
                    Argument.string("input", argument)
                )
            );
        }

        Registry<Enchantment> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);

        NamespacedKey key = NamespacedKey.fromString(name);
        Enchantment enchantment = key == null ? null : registry.get(key);
        if (enchantment == null) {
            throw new FilterCompileException(
                Component.translatable(
                    "hopplet.filter.function.enchantment.compilation.exception.unknown_enchantment",
                    Argument.string("input", argument)
                )
            );
        }

        return enchantment;
    }

    private static int parseLevel(@NonNull String text, @NonNull String argument) {
        if (text.isEmpty()) {
            throw new FilterCompileException(
                Component.translatable(
                    "hopplet.filter.function.enchantment.compilation.exception.no_level_specified",
                    Argument.string("input", argument)
                )
            );
        }

        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new FilterCompileException(
                Component.translatable(
                    "hopplet.filter.function.enchantment.compilation.exception.invalid_level",
                    Argument.string("input", argument)
                )
            );
        }
    }

    @Override
    public boolean test(Filter.@NonNull Context context, @NonNull Set<Spec> arguments) {
        ItemStack item = context.stack();

        Map<Enchantment, Integer> enchantments = item.getItemMeta() instanceof EnchantmentStorageMeta meta
            ? meta.getStoredEnchants()
            : item.getEnchantments();

        if (enchantments.isEmpty()) return false;

        for (Spec argument : arguments) {
            Integer enchantLevel = enchantments.get(argument.enchantment);
            if (enchantLevel == null) continue;

            if (argument.comparator == null || argument.level == null) return true;

            return argument.comparator.compare(enchantLevel, argument.level);
        }

        return false;
    }

    public record Spec(@NonNull Enchantment enchantment, @Nullable Comparator comparator, @Nullable Integer level) {}
}
