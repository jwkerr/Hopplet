package au.lupine.hopplet.filter.function;

import au.lupine.hopplet.Hopplet;
import au.lupine.hopplet.filter.Filter;
import au.lupine.hopplet.filter.Function;
import au.lupine.hopplet.filter.exception.FilterCompileException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.SmokingRecipe;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class FurnaceTypeFunction implements Function<EnumSet<FurnaceTypeFunction.FurnaceType>> {

    private static volatile @Nullable Map<Material, EnumSet<FurnaceType>> recipeCache;

    public static void warmCache() {
        recipeCache = buildCache();
    }

    public static void invalidateCache() {
        recipeCache = null;
    }

    private static @NonNull Map<Material, EnumSet<FurnaceType>> getCache() {
        final Map<Material, EnumSet<FurnaceType>> cache = recipeCache;
        return cache != null ? cache : Map.of();
    }

    private static @NonNull Map<Material, EnumSet<FurnaceType>> buildCache() {
        final Map<Material, EnumSet<FurnaceType>> cache = new HashMap<>();
        final Iterator<Recipe> it = Bukkit.recipeIterator();
        while (it.hasNext()) {
            final Recipe recipe = it.next();
            final FurnaceType type;
            switch (recipe) {
                case FurnaceRecipe ignored -> type = FurnaceType.FURNACE;
                case SmokingRecipe ignored -> type = FurnaceType.SMOKER;
                case BlastingRecipe ignored -> type = FurnaceType.BLAST_FURNACE;
                case null, default -> {
                    continue;
                }
            }
            final RecipeChoice input = ((CookingRecipe<?>) recipe).getInputChoice();
            if (input instanceof RecipeChoice.MaterialChoice mc) {
                for (final Material m : mc.getChoices()) {
                    cache.computeIfAbsent(m, k -> EnumSet.noneOf(FurnaceType.class)).add(type);
                }
            } else if (input instanceof RecipeChoice.ExactChoice ec) {
                for (final ItemStack is : ec.getChoices()) {
                    cache.computeIfAbsent(is.getType(), k -> EnumSet.noneOf(FurnaceType.class)).add(type);
                }
            }
        }
        return cache;
    }

    @Override
    public @NonNull String name() {
        return "furnace_type";
    }

    @Override
    public @NonNull Component description() {
        return Component.translatable("hopplet.filter.function.furnace_type.description");
    }

    @Override
    public @NonNull Plugin plugin() {
        return Hopplet.instance();
    }

    @Override
    public @NonNull EnumSet<FurnaceType> compile(@NonNull List<String> arguments) throws FilterCompileException {
        if (arguments.isEmpty()) return EnumSet.allOf(FurnaceType.class);

        final EnumSet<FurnaceType> types = EnumSet.noneOf(FurnaceType.class);
        for (String argument : arguments) {
            try {
                types.add(FurnaceType.valueOf(argument.toUpperCase()));
            } catch (final IllegalArgumentException e) {
                throw new FilterCompileException(
                        Component.translatable(
                                "hopplet.filter.function.furnace_type.compilation.exception.unknown_furnace_type",
                                Argument.string("input", argument)
                        )
                );
            }
        }
        return types;
    }

    @Override
    public boolean test(Filter.@NonNull Context context, @NonNull EnumSet<FurnaceType> types) {
        final EnumSet<FurnaceType> supported = getCache().get(context.stack().getType());
        if (supported == null) return false;
        for (final FurnaceType type : types) {
            if (supported.contains(type)) return true;
        }
        return false;
    }

    public enum FurnaceType {
        FURNACE, SMOKER, BLAST_FURNACE
    }
}
