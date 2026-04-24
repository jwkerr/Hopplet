package au.lupine.hopplet.filter.function;

import au.lupine.hopplet.Hopplet;
import au.lupine.hopplet.filter.Filter;
import au.lupine.hopplet.filter.Function;
import au.lupine.hopplet.filter.exception.FilterCompileException;
import au.lupine.hopplet.util.Either;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Item;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ThrowerFunction implements Function<Set<Either<UUID, String>>> {

    @Override
    public @NonNull String name() {
        return "thrower";
    }

    @Override
    public @NonNull Set<String> aliases() {
        return Set.of("thrown_by");
    }

    @Override
    public @NonNull Component description() {
        return Component.translatable("hopplet.filter.function.thrower.description");
    }

    @Override
    public @NonNull Plugin plugin() {
        return Hopplet.instance();
    }

    @Override
    public @NonNull Set<Either<UUID, String>> compile(@NonNull List<String> arguments) throws FilterCompileException {
        if (arguments.isEmpty()) {
            throw new FilterCompileException(
                Component.translatable(
                    "hopplet.filter.function.default.compilation.exception.no_arguments_provided",
                    Argument.string("name", name())
                )
            );
        }

        final Set<Either<UUID, String>> eithers = new HashSet<>();

        for (final String argument : arguments) {
            if (argument.length() == 36) {
                try {
                    eithers.add(Either.left(UUID.fromString(argument)));
                    continue;
                } catch (IllegalArgumentException ignored) {}
            }

            if (isReasonablePlayerName(argument)) {
                eithers.add(Either.right(argument));
                continue;
            }

            throw new FilterCompileException(
                Component.translatable(
                    "hopplet.filter.function.thrower.compilation.exception.invalid_name",
                    Argument.string("input", argument)
                )
            );
        }

        return eithers;
    }

    @Override
    public boolean test(Filter.@NonNull Context context, @NonNull Set<Either<UUID, String>> arguments) {
        Item item = context.item();
        if (item == null) return false;

        UUID thrower = item.getThrower();
        if (thrower == null) return false;

        for (Either<UUID, String> argument : arguments) {
            if (argument.map(thrower::equals, name -> {
                final OfflinePlayer player = Bukkit.getOfflinePlayerIfCached(name);
                return player != null && thrower.equals(player.getUniqueId());
            })) {
                return true;
            }
        }

        return false;
    }

    // Taken from paper inside net/minecraft/util/StringUtil
    // does create a small problem for servers using floodgate, the default prefix '.' is already considered reasonable, but directly integrating with its api and retrieving the prefix from that is also possible if wanted.
    private static boolean isReasonablePlayerName(final String name) {
        if (name.isEmpty() || name.length() > 16) {
            return false;
        }

        for (int i = 0, len = name.length(); i < len; ++i) {
            final char c = name.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || (c == '_' || c == '.')) {
                continue;
            }

            return false;
        }

        return true;
    }
}
