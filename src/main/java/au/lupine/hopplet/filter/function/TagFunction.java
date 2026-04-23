package au.lupine.hopplet.filter.function;

import au.lupine.hopplet.Hopplet;
import au.lupine.hopplet.filter.Filter;
import au.lupine.hopplet.filter.Function;
import au.lupine.hopplet.filter.exception.FilterCompileException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class TagFunction implements Function<Set<Material>> {

    @Override
    public @NonNull String name() {
        return "tag";
    }

    @Override
    public @NonNull Set<String> aliases() {
        return Set.of("tagged");
    }

    @Override
    public @NonNull Component description() {
        return Component.translatable("hopplet.filter.function.tag.description");
    }

    @Override
    public @NonNull Plugin plugin() {
        return Hopplet.instance();
    }

    @Override
    public @NonNull Set<Material> compile(@NonNull List<String> arguments) throws FilterCompileException {
        if (arguments.isEmpty()) {
            throw new FilterCompileException(
                Component.translatable(
                    "hopplet.filter.function.default.compilation.exception.no_arguments_provided",
                    Argument.string("name", name())
                )
            );
        }

        Set<Material> materials = EnumSet.noneOf(Material.class);
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

            Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_ITEMS, key, Material.class);
            if (tag == null) tag = Bukkit.getTag(Tag.REGISTRY_BLOCKS, key, Material.class);

            if (tag == null) {
                throw new FilterCompileException(
                    Component.translatable(
                        "hopplet.filter.function.tag.compilation.exception.unknown_tag",
                        Argument.string("input", argument)
                    )
                );
            }

            materials.addAll(tag.getValues());
        }

        return materials;
    }

    @Override
    public boolean test(Filter.@NonNull Context context, @NonNull Set<Material> materials) {
        return materials.contains(context.stack().getType());
    }
}
