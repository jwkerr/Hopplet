package au.lupine.hopplet.filter;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public interface FilterMethod {

    @NotNull Set<FilterMethod> METHODS = new HashSet<>();

    @NotNull Plugin plugin();

    @NotNull String name();

    default @NotNull Set<String> aliases() {
        return Set.of();
    }

    default @NotNull Component description() {
        return Component.translatable("hopplet.message.this_filter_method_has_not_set_a_description");
    }

    boolean accepts(@NotNull ItemStack item, @Nullable String argument);

    default boolean accepts(@NotNull Item item, @Nullable String argument) {
        return accepts(item.getItemStack(), argument);
    }

    default @NotNull String namespaced() {
        String namespace = plugin().getName();
        return namespace + ":" + name();
    }

    static boolean register(@NotNull FilterMethod method) {
        for (FilterMethod other : METHODS) {
            if (other.namespaced().equals(method.namespaced())) return false;
        }
        return METHODS.add(method);
    }

    static boolean unregister(@NotNull FilterMethod method) {
        return METHODS.remove(method);
    }

    static <MethodType extends FilterMethod> @Nullable FilterMethod ofType(@NotNull Class<MethodType> type) {
        for (FilterMethod method : METHODS) {
            if (type.isInstance(method)) return type.cast(method);
        }
        return null;
    }
}
