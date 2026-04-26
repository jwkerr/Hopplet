package au.lupine.hopplet.filter.function;

import au.lupine.hopplet.Hopplet;
import au.lupine.hopplet.filter.Filter;
import au.lupine.hopplet.filter.Function;
import au.lupine.hopplet.filter.exception.FilterCompileException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MaterialStartsWithFunction implements Function<Set<String>> {

    @Override
    public @NonNull String name() {
        return "material_starts_with";
    }

    @Override
    public @NonNull Set<String> aliases() {
        return Set.of("type_starts_with");
    }

    @Override
    public @NonNull Component description() {
        return Component.translatable("hopplet.filter.function.material_starts_with.description");
    }

    @Override
    public @NonNull Plugin plugin() {
        return Hopplet.instance();
    }

    @Override
    public @NonNull Set<String> compile(@NonNull List<String> arguments) throws FilterCompileException {
        argsRequired(arguments);

        return new HashSet<>(arguments);
    }

    @Override
    public boolean test(Filter.@NonNull Context context, @NonNull Set<String> arguments) {
        String name = context.stack().getType().getKey().getKey();

        for (String argument : arguments) {
            if (name.startsWith(argument)) return true;
        }

        return false;
    }
}