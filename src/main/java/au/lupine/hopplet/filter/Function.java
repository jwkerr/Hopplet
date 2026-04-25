package au.lupine.hopplet.filter;

import au.lupine.hopplet.Hopplet;
import au.lupine.hopplet.filter.exception.FilterCompileException;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
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

    /// Functions will be {@link #key() keyed} as `plugin:function_name`.
    /// This namespace can be used if another function has claimed your {@link #name()}.
    /// @return The plugin that owns this function.
    @NonNull Plugin plugin();

    @NonNull ArgumentType compile(@NonNull List<String> arguments) throws FilterCompileException;

    boolean test(Filter.@NonNull Context context, @NonNull ArgumentType compiled);

    default boolean enabled() {
        List<String> disabled;
        try {
            disabled = Hopplet.instance().config().root().node("filter", "function", "disabled").getList(String.class, List.of());
        } catch (SerializationException e) {
            disabled = List.of();
        }
        return !disabled.contains(key().toString());
    }

    default @NonNull NamespacedKey key() {
        return new NamespacedKey(plugin(), name());
    }

    default @NonNull Set<NamespacedKey> keys() {
        Set<NamespacedKey> keys = new HashSet<>();
        keys.add(key());
        for (String alias : aliases()) {
            keys.add(new NamespacedKey(plugin(), alias));
        }
        return keys;
    }

    /// Register your {@link Function functions} to be usable in filters.
    /// This method is idempotent, repeated calls will not register a function again.
    static void register(@NonNull Function<?>... functions) {
        outer: for (Function<?> function : functions) {
            for (Function<?> other : FUNCTIONS) {
                if(other.key().equals(function.key())) continue outer;
            }

            FUNCTIONS.add(function);
        }
    }

    static void unregister(@NonNull Function<?>... functions) {
        for (Function<?> function : functions) {
            FUNCTIONS.remove(function);
        }
    }

    static <FunctionType extends Function<?>> @Nullable FunctionType of(@NonNull Class<FunctionType> type) {
        for (Function<?> function : FUNCTIONS) {
            if (type.isInstance(function)) return type.cast(function);
        }
        return null;
    }

    static @Nullable Function<?> of(@NonNull NamespacedKey key) {
        for (Function<?> function : FUNCTIONS) {
            if (function.keys().contains(key)) return function;
        }
        return null;
    }

    static @Nullable Function<?> of(@NonNull String identifier) {
        if (identifier.contains(":")) {
            NamespacedKey key = NamespacedKey.fromString(identifier);
            if (key == null) return null;

            return of(key);
        }

        for (Function<?> function : FUNCTIONS) {
            if (function.name().equals(identifier)) return function;
            if (function.aliases().contains(identifier)) return function;
        }

        return null;
    }

    final class NoArguments {
        private NoArguments() {}
    }
}