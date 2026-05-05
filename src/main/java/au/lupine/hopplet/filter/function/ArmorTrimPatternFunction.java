package au.lupine.hopplet.filter.function;

import au.lupine.hopplet.Hopplet;
import au.lupine.hopplet.filter.Filter;
import au.lupine.hopplet.filter.Function;
import au.lupine.hopplet.filter.exception.FilterCompileException;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ArmorTrimPatternFunction implements Function<Set<TrimPattern>> {

    @Override
    public @NonNull String name() {
        return "armor_trim_pattern";
    }

    @Override
    public @NonNull Set<String> aliases() {
        return Set.of("trim_pattern");
    }

    @Override
    public @NonNull Component description() {
        return Component.translatable("hopplet.filter.function.armor_trim_pattern.description");
    }

    @Override
    public @NonNull Plugin plugin() {
        return Hopplet.instance();
    }

    @Override
    public @NonNull Set<TrimPattern> compile(@NonNull List<String> arguments) throws FilterCompileException {
        argsRequired(arguments);

        Registry<TrimPattern> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_PATTERN);
        Set<TrimPattern> patterns = new HashSet<>();

        for (String argument : arguments) {
            NamespacedKey key = NamespacedKey.fromString(argument.toLowerCase());

            if (key == null) {
                throw new FilterCompileException(
                    Component.translatable(
                        "hopplet.filter.function.default.compilation.exception.invalid_key",
                        Argument.string("input", argument)
                    )
                );
            }

            TrimPattern pattern = registry.get(key);
            if (pattern == null) {
                throw new FilterCompileException(
                    Component.translatable(
                        "hopplet.filter.function.armor_trim_pattern.compilation.exception.unknown_trim_pattern",
                        Argument.string("input", argument)
                    )
                );
            }

            patterns.add(pattern);
        }

        return patterns;
    }

    @Override
    public boolean test(Filter.@NonNull Context context, @NonNull Set<TrimPattern> patterns) {
        if (!(context.stack().getItemMeta() instanceof ArmorMeta meta)) return false;

        ArmorTrim trim = meta.getTrim();
        if (trim == null) return false;

        return patterns.contains(trim.getPattern());
    }
}
