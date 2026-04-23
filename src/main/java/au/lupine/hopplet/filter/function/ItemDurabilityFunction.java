package au.lupine.hopplet.filter.function;

import au.lupine.hopplet.Hopplet;
import au.lupine.hopplet.filter.Filter;
import au.lupine.hopplet.filter.Function;
import au.lupine.hopplet.filter.exception.FilterCompileException;
import au.lupine.hopplet.util.Comparator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Range;
import org.jspecify.annotations.NonNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ItemDurabilityFunction implements Function<Set<ItemDurabilityFunction.Spec>> {

    @Override
    public @NonNull String name() {
        return "item_durability";
    }

    @Override
    public @NonNull Set<String> aliases() {
        return Set.of("durability");
    }

    @Override
    public @NonNull Component description() {
        return Component.translatable("hopplet.filter.function.item_durability.description");
    }

    @Override
    public @NonNull Plugin plugin() {
        return Hopplet.instance();
    }

    @Override
    public @NonNull Set<Spec> compile(@NonNull List<String> arguments) throws FilterCompileException {
        if (arguments.isEmpty()) {
            throw new FilterCompileException(
                Component.translatable(
                    "hopplet.filter.function.default.compilation.exception.no_arguments_provided",
                    Argument.string("name", name())
                )
            );
        }

        Set<Spec> specs = new HashSet<>();
        for (String argument : arguments) {
            String normalised = argument.toLowerCase().replaceAll("\\s", "");

            if (normalised.equals("max") || normalised.equals("undamaged")) {
                specs.add(new Spec(Comparator.EQUAL_TO, 100));
                continue;
            }

            Comparator comparator = Comparator.EQUAL_TO;
            String value = normalised;

            for (Comparator c : Comparator.values()) {
                String symbol = c.symbol();

                if (normalised.startsWith(symbol)) {
                    comparator = c;
                    value = normalised.substring(symbol.length());
                    break;
                }
            }

            int durability = parseDurability(value, argument);
            specs.add(new Spec(comparator, durability));
        }

        return specs;
    }

    @Override
    public boolean test(Filter.@NonNull Context context, @NonNull Set<Spec> arguments) {
        ItemStack item = context.stack();
        if (!(item.getItemMeta() instanceof Damageable meta)) return false;

        int max = item.getType().getMaxDurability();
        if (max <= 0) return false;

        int remaining = max - meta.getDamage();
        int percentage = (int) ((remaining * 100L) / max);

        for (Spec spec : arguments) {
            if (spec.comparator.compare(percentage, spec.durability)) return true;
        }

        return false;
    }

    private static int parseDurability(@NonNull String text, @NonNull String argument) {
        if (text.isEmpty()) {
            throw new FilterCompileException(
                Component.translatable(
                    "hopplet.filter.function.item_durability.compilation.exception.no_durability_specified",
                    Argument.string("input", argument)
                )
            );
        }

        int durability;
        try {
            durability = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new FilterCompileException(
                Component.translatable(
                    "hopplet.filter.function.item_durability.compilation.exception.invalid_durability",
                    Argument.string("input", argument)
                )
            );
        }

        if (durability < 0) {
            throw new FilterCompileException(
                Component.translatable(
                    "hopplet.filter.function.item_durability.compilation.exception.less_than_zero",
                    Argument.string("input", argument)
                )
            );
        }

        if (durability > 100) {
            throw new FilterCompileException(
                Component.translatable(
                    "hopplet.filter.function.item_durability.compilation.exception.greater_than_one_hundred",
                    Argument.string("input", argument)
                )
            );
        }

        return durability;
    }

    public record Spec(@NonNull Comparator comparator, @Range(from = 0, to = 100) int durability) {}
}
