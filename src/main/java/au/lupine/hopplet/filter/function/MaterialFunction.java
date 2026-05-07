package au.lupine.hopplet.filter.function;

import au.lupine.hopplet.Hopplet;
import au.lupine.hopplet.filter.Function;
import au.lupine.hopplet.filter.context.FilterContext;
import au.lupine.hopplet.filter.exception.FilterCompileException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MaterialFunction implements Function<Set<Material>> {

    @Override
    public @NonNull String name() {
        return "material";
    }

    @Override
    public @NonNull Set<String> aliases() {
        return Set.of("type");
    }

    @Override
    public @NonNull Component description() {
        return Component.translatable("hopplet.filter.function.material.description");
    }

    @Override
    public @NonNull Plugin plugin() {
        return Hopplet.instance();
    }

    @Override
    public @NonNull Set<Material> compile(@NonNull List<String> arguments) throws FilterCompileException {
        argsRequired(arguments);

        Set<Material> materials = new HashSet<>();
        for (String argument : arguments) {
            NamespacedKey key = NamespacedKey.fromString(argument.toLowerCase());
            Material material = key == null ? null : Registry.MATERIAL.get(key);

            if (material == null) {
                throw new FilterCompileException(
                    Component.translatable(
                        "hopplet.filter.function.material.compilation.exception.unknown_material",
                        Argument.string("input", argument)
                    )
                );
            }

            materials.add(material);
        }

        return materials;
    }

    @Override
    public boolean test(@NonNull FilterContext context, @NonNull Set<Material> materials) {
        Material type = context.stack().getType();

        return materials.contains(type);
    }
}
