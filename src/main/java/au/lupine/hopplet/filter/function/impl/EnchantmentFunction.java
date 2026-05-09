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
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;

import java.util.Map;
import java.util.Set;

public final class EnchantmentFunction implements Matcher<Pair<Enchantment, Comparator>> {

    private static final Registry<Enchantment> ENCHANTMENT_REGISTRY = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);

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
    public @NonNull MatchStrategy<Pair<Enchantment, Comparator>> strategy() {
        return MatchStrategy.all();
    }

    @Override
    public @NonNull Pair<Enchantment, Comparator> parse(@NonNull String argument) throws FilterCompileException {
        Pair<String, Comparator> pair = Comparator.split(argument, Comparator.of(Comparator.Type.GREATER_THAN_OR_EQUAL_TO, 1));

        Enchantment enchantment = enchantment(pair.left(), argument);
        Comparator comparator = pair.right();

        if (comparator.value() < 1) {
            throw new FilterCompileException(
                Component.translatable(
                    "hopplet.filter.function.enchantment.compilation.exception.level_must_be_positive",
                    Argument.string("input", argument)
                )
            );
        }

        return Pair.of(enchantment, comparator);
    }

    @Override
    public boolean matches(@NonNull Context context, @NonNull Pair<Enchantment, Comparator> pair) {
        ItemStack stack = context.stack();

        Map<Enchantment, Integer> enchantments = stack.getItemMeta() instanceof EnchantmentStorageMeta meta
            ? meta.getStoredEnchants()
            : stack.getEnchantments();

        if (enchantments.isEmpty()) return false;

        Integer level = enchantments.get(pair.left()); // TODO: unbreaking>=1 doesn't work, unbreaking=3 does
        if (level == null) return false;

        return pair.right().test(level);
    }

    private static @NonNull Enchantment enchantment(@NonNull String name, @NonNull String argument) throws FilterCompileException {
        if (name.isEmpty()) {
            throw new FilterCompileException(
                Component.translatable(
                    "hopplet.filter.function.enchantment.compilation.exception.no_enchantment_specified",
                    Argument.string("input", argument)
                )
            );
        }

        NamespacedKey key = NamespacedKey.fromString(name);
        Enchantment enchantment = key == null ? null : ENCHANTMENT_REGISTRY.get(key);
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
}
