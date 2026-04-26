package au.lupine.hopplet.filter.function;

import au.lupine.hopplet.Hopplet;
import au.lupine.hopplet.filter.Filter;
import au.lupine.hopplet.filter.Function;
import au.lupine.hopplet.filter.exception.FilterCompileException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class BookGenerationFunction implements Function<Set<BookMeta.Generation>> {

    @Override
    public @NonNull String name() {
        return "book_generation";
    }

    @Override
    public @NonNull Set<String> aliases() {
        return Set.of("generation");
    }

    @Override
    public @NonNull Component description() {
        return Component.translatable("hopplet.filter.function.book_generation.description");
    }

    @Override
    public @NonNull Plugin plugin() {
        return Hopplet.instance();
    }

    @Override
    public @NonNull Set<BookMeta.Generation> compile(@NonNull List<String> arguments) throws FilterCompileException {
        argsRequired(arguments);

        Set<BookMeta.Generation> generations = new HashSet<>();
        for (String argument : arguments) {
            try {
                BookMeta.Generation generation = BookMeta.Generation.valueOf(argument.toUpperCase());
                generations.add(generation);
            } catch (IllegalArgumentException e) {
                throw new FilterCompileException(
                    Component.translatable(
                        "hopplet.filter.function.book_generation.compilation.exception.unknown_book_generation",
                        Argument.string("input", argument)
                    )
                );
            }
        }

        return generations;
    }

    @Override
    public boolean test(Filter.@NonNull Context context, @NonNull Set<BookMeta.Generation> generations) {
        ItemStack item = context.stack();
        if (!(item.getItemMeta() instanceof BookMeta meta)) return false;

        BookMeta.Generation generation = meta.getGeneration();
        if (generation == null) generation = BookMeta.Generation.ORIGINAL;

        return generations.contains(generation);
    }
}
