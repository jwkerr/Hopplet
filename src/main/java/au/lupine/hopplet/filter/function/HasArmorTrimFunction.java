package au.lupine.hopplet.filter.function;

import au.lupine.hopplet.Hopplet;
import au.lupine.hopplet.filter.Filter;
import au.lupine.hopplet.filter.Function;
import au.lupine.hopplet.filter.exception.FilterCompileException;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Set;

public final class HasArmorTrimFunction implements Function<Function.NoArguments> {

    @Override
    public @NonNull String name() {
        return "has_armor_trim";
    }

    @Override
    public @NonNull Set<String> aliases() {
        return Set.of(
            "is_trimmed",
            "trimmed"
        );
    }

    @Override
    public @NonNull Component description() {
        return Component.translatable("hopplet.filter.function.has_armor_trim.description");
    }

    @Override
    public @NonNull Plugin plugin() {
        return Hopplet.instance();
    }

    @Override
    public @NonNull NoArguments compile(@NonNull List<String> arguments) throws FilterCompileException {
        argsNotRequired(arguments);

        return NO_ARGUMENTS;
    }

    @Override
    public boolean test(Filter.@NonNull Context context, Function.@NonNull NoArguments arguments) {
        return context.stack().getItemMeta() instanceof ArmorMeta meta && meta.hasTrim();
    }
}
