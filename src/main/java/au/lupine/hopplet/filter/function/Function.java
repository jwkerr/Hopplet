package au.lupine.hopplet.filter.function;

import au.lupine.hopplet.Hopplet;
import au.lupine.hopplet.event.FunctionRegisteredEvent;
import au.lupine.hopplet.event.PreFunctionRegisterEvent;
import au.lupine.hopplet.filter.context.Context;
import au.lupine.hopplet.filter.exception.FilterCompileException;
import au.lupine.hopplet.util.Either;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public interface Function<ArgumentType> {

    @NonNull Set<Function<?>> FUNCTIONS = new CopyOnWriteArraySet<>();

    @NonNull Map<NamespacedKey, Function<?>> FUNCTIONS_BY_KEY = new ConcurrentHashMap<>();
    @NonNull Map<String, Function<?>> FUNCTIONS_BY_NAME = new ConcurrentHashMap<>();

    /// @return The name of this function in `snake_case`.
    @NonNull String name();

    /// @return The aliases to use for this function, must contain the same elements throughout the lifetime of the program.
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

    default @NonNull String namespace() {
        return plugin().namespace();
    }

    @NonNull ArgumentType compile(@NonNull List<String> arguments) throws FilterCompileException;

    boolean test(@NonNull Context context, @NonNull ArgumentType compiled);

    /// Override this if you want to conditionally disable your function, for example, when a dependency isn't present.
    /// @return Whether this function is disabled.
    default boolean disabled() {
        return false;
    }

    /// This should not be overridden, use {@link #disabled()} if you need to disable the function.
    /// @return Whether this function is enabled, taking into consideration what the output of {@link #disabled()} is.
    default boolean enabled() {
        if (disabled()) return false;

        List<String> disabled;
        try {
            disabled = Hopplet.instance().config().root().node("filter", "function", "disable").getList(String.class, List.of());
        } catch (SerializationException e) {
            disabled = List.of();
        }
        return !disabled.contains(key().toString());
    }

    default @NonNull NamespacedKey key() {
        return new NamespacedKey(namespace(), name());
    }

    default @NonNull Set<NamespacedKey> keys() {
        Set<NamespacedKey> keys = new HashSet<>();
        keys.add(key());
        for (String alias : aliases()) {
            keys.add(new NamespacedKey(namespace(), alias));
        }
        return keys;
    }

    /// Register your {@link Function functions} to be usable in filters.
    /// This method is idempotent, repeated calls will not register a function again.
    static void register(@NonNull Function<?>... functions) {
        List<String> registered = new ArrayList<>();

        for (Function<?> function : functions) {
            NamespacedKey functionKey = function.key();
            if (FUNCTIONS_BY_KEY.containsKey(functionKey)) continue;

            PreFunctionRegisterEvent event = new PreFunctionRegisterEvent(function);
            if (!event.callEvent()) continue;

            FUNCTIONS.add(function);

            FUNCTIONS_BY_KEY.put(functionKey, function);
            for (NamespacedKey key : function.keys()) FUNCTIONS_BY_KEY.putIfAbsent(key, function);

            FUNCTIONS_BY_NAME.putIfAbsent(function.name(), function);
            for (String alias : function.aliases()) FUNCTIONS_BY_NAME.putIfAbsent(alias, function);

            new FunctionRegisteredEvent(function).callEvent();

            registered.add(function.key().asString());
        }

        if (registered.isEmpty()) return;
        Hopplet.instance().getLogger().info("Registering functions: " + String.join(", ", registered));
    }

    static void unregister(@NonNull Function<?>... functions) {
        List<String> unregistered = new ArrayList<>();

        for (Function<?> function : functions) {
            NamespacedKey functionKey = function.key();

            FUNCTIONS.remove(function);

            FUNCTIONS_BY_KEY.remove(functionKey, function);
            for (NamespacedKey key : function.keys()) FUNCTIONS_BY_KEY.remove(key, function);

            FUNCTIONS_BY_NAME.remove(function.name(), function);
            for (String alias : function.aliases()) FUNCTIONS_BY_NAME.remove(alias, function);

            unregistered.add(function.key().asString());
        }

        if (unregistered.isEmpty()) return;
        Hopplet.instance().getLogger().info("Unregistering functions: " + String.join(", ", unregistered));
    }

    static <FunctionType extends Function<?>> @Nullable FunctionType of(@NonNull Class<FunctionType> type) {
        for (Function<?> function : FUNCTIONS) {
            if (type.isInstance(function)) return type.cast(function);
        }
        return null;
    }

    static @Nullable Function<?> of(@NonNull NamespacedKey key) {
        return FUNCTIONS_BY_KEY.get(key);
    }

    static @Nullable Function<?> of(@NonNull String identifier) {
        if (identifier.contains(":")) {
            NamespacedKey key = NamespacedKey.fromString(identifier);
            if (key != null) return of(key);
        }

        return FUNCTIONS_BY_NAME.get(identifier);
    }

    // Function utilities that should probably have a better spot (inheritance?)

    static void checkKey(@Nullable NamespacedKey key, @NonNull String argument) {
        if (key == null) {
            throw new FilterCompileException(
                Component.translatable(
                    "hopplet.filter.function.default.compilation.exception.invalid_key",
                    Argument.string("input", argument)
                )
            );
        }
    }

    static @NonNull Either<UUID, String> playerEither(@NonNull String argument) {
        if (argument.length() == 36) {
            try {
                return Either.left(UUID.fromString(argument));
            } catch (IllegalArgumentException ignored) {}
        }

        if (isReasonablePlayerName(argument)) return Either.right(argument);

        throw new FilterCompileException(
            Component.translatable(
                "hopplet.filter.function.default.compilation.exception.invalid_uuid_or_name",
                Argument.string("input", argument)
            )
        );
    }

    // Taken from paper inside net/minecraft/util/StringUtil
    // does create a small problem for servers using floodgate, the default prefix '.' is already considered reasonable, but directly integrating with its api and retrieving the prefix from that is also possible if wanted.
    private static boolean isReasonablePlayerName(@NonNull String name) {
        if (name.isEmpty() || name.length() > 16) return false;

        for (int i = 0, len = name.length(); i < len; ++i) {
            char c = name.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || (c == '_' || c == '.')) continue;

            return false;
        }

        return true;
    }

    static @Nullable String lore(@NonNull ItemStack stack) {
        List<Component> components = stack.lore();
        if (components == null) return null;

        return PlainTextComponentSerializer.plainText()
            .serialize(
                Component.join(
                    JoinConfiguration.spaces(),
                    components
                )
            );
    }
}