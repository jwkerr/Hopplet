package au.lupine.hopplet.filter.function;

import au.lupine.hopplet.Hopplet;
import au.lupine.hopplet.filter.Filter;
import au.lupine.hopplet.filter.Function;
import au.lupine.hopplet.filter.exception.FilterCompileException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ItemRarityFunction implements Function<Set<ItemRarity>> {

    @Override
    public @NonNull String name() {
        return "item_rarity";
    }

    @Override
    public @NonNull Set<String> aliases() {
        return Set.of("rarity");
    }

    @Override
    public @NonNull Component description() {
        return Component.translatable("hopplet.filter.function.item_rarity.description");
    }

    @Override
    public @NonNull Plugin plugin() {
        return Hopplet.instance();
    }

    @Override
    public @NonNull Set<ItemRarity> compile(@NonNull List<String> arguments) throws FilterCompileException {
        argsRequired(arguments);

        Set<ItemRarity> rarities = new HashSet<>();
        for (String argument : arguments) {
            try {
                rarities.add(ItemRarity.valueOf(argument));
            } catch (IllegalArgumentException e) {
                throw new FilterCompileException(
                    Component.translatable(
                        "hopplet.filter.function.item_rarity.compilation.exception.unknown_item_rarity",
                        Argument.string("input", name())
                    )
                );
            }
        }

        return rarities;
    }

    @Override
    public boolean test(Filter.@NonNull Context context, @NonNull Set<ItemRarity> rarities) {
        ItemMeta meta = context.stack().getItemMeta();

        if (!meta.hasRarity()) return rarities.contains(ItemRarity.COMMON);
        return rarities.contains(context.stack().getItemMeta().getRarity());
    }
}
