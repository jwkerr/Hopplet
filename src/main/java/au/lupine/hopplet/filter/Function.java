package au.lupine.hopplet.filter;

import au.lupine.hopplet.Hopplet;
import au.lupine.hopplet.filter.exception.FilterCompileException;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public interface Function<ArgumentType> {

    @NonNull Set<Function<?>> FUNCTIONS = new CopyOnWriteArraySet<>();

    @NonNull NoArguments NO_ARGUMENTS = new NoArguments();

    /// @return The name of this function in `snake_case`.
    @NonNull String name();

    default @NonNull Set<String> aliases() {
        return Set.of();
    }

    default @NonNull Component description() {
        return Component.translatable("hopplet.filter.function.default.description");
    }

    /// Functions will be {@link #namespaced()} as `plugin:function_name`.
    /// This namespace can be used if another function has claimed your {@link #name()}.
    /// @return The plugin that owns this function.
    @NonNull Plugin plugin();

    @NonNull ArgumentType compile(@NonNull List<String> arguments) throws FilterCompileException;

    boolean test(Filter.@NonNull Context context, @NonNull ArgumentType compiled);

    default @NonNull String namespaced(@NonNull String name) {
        return plugin().namespace() + ":" + name;
    }

    /// @return The namespaced name of this function. i.e. `plugin:function_name`.
    default @NonNull String namespaced() {
        return namespaced(name());
    }

    default @NonNull Set<String> namespacedAliases() {
        Set<String> aliases = new HashSet<>();
        for (String alias : aliases()) {
            aliases.add(namespaced(alias));
        }
        return aliases;
    }

    static void register(@NonNull Function<?>... functions) {
        List<String> disabled;
        try {
            disabled = Hopplet.instance().config().root().node("functions", "disabled").getList(String.class, List.of());
        } catch (SerializationException e) {
            disabled = List.of();
        }

        for (Function<?> function : functions) {
            if (disabled.contains(function.namespaced())) return;

            for (Function<?> other : FUNCTIONS) {
                if(other.namespaced().equals(function.namespaced())) return;
            }
            FUNCTIONS.add(function);
        }
    }

    static void unregister(@NonNull Function<?>... functions) {
        for (Function<?> function : functions) {
            FUNCTIONS.remove(function);
        }
    }

    static <FunctionType extends Function<?>> @Nullable Function<?> ofType(@NonNull Class<FunctionType> type) {
        for (Function<?> function : FUNCTIONS) {
            if (type.isInstance(function)) return type.cast(function);
        }
        return null;
    }

    static @Nullable Function<?> ofName(@NonNull String name) {
        for (Function<?> function : FUNCTIONS) {
            if (function.name().equals(name)) return function;

            for (String alias : function.aliases()) {
                if (alias.equals(name)) return function;
            }
        }
        return null;
    }

    static @Nullable Function<?> ofNamespacedOrName(@NonNull String name) {
        for (Function<?> function : FUNCTIONS) {
            if (function.namespaced().equals(name)) return function;

            for (String alias : function.namespacedAliases()) {
                if (alias.equals(name)) return function;
            }
        }
        return ofName(name);
    }

    final class NoArguments {
        private NoArguments() {}
    }
}