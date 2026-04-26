package au.lupine.hopplet.filter.function;

import au.lupine.hopplet.Hopplet;
import au.lupine.hopplet.filter.Filter;
import au.lupine.hopplet.filter.Function;
import au.lupine.hopplet.filter.exception.FilterCompileException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SourceTypeFunction implements Function<Set<InventoryType>> {

    @Override
    public @NonNull String name() {
        return "source_type";
    }

    @Override
    public @NonNull Set<String> aliases() {
        return Set.of("source");
    }

    @Override
    public @NonNull Component description() {
        return Component.translatable("hopplet.filter.function.source_type.description");
    }

    @Override
    public @NonNull Plugin plugin() {
        return Hopplet.instance();
    }

    @Override
    public @NonNull Set<InventoryType> compile(@NonNull List<String> arguments) throws FilterCompileException {
        argsRequired(arguments);

        Set<InventoryType> types = new HashSet<>();
        for (String argument : arguments) {
            try {
                types.add(InventoryType.valueOf(argument.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new FilterCompileException(
                    Component.translatable(
                        "hopplet.filter.function.source_type.compilation.exception.unknown_inventory_type",
                        Argument.string("input", argument)
                    )
                );
            }
        }

        return types;
    }

    @Override
    public boolean test(Filter.@NonNull Context context, @NonNull Set<InventoryType> types) {
        Inventory source = context.source();
        if (source == null) return false;

        return types.contains(source.getType());
    }
}
