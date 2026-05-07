package au.lupine.hopplet.filter.edit;

import au.lupine.hopplet.filter.Filter;
import au.lupine.hopplet.filter.compiler.FilterCompiler;
import au.lupine.hopplet.filter.exception.FilterCompileException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public interface EditTarget {

    @Nullable Component component();

    default @NonNull String name() {
        Component component = component();
        return component == null ? "" : PlainTextComponentSerializer.plainText().serialize(component);
    }

    @NonNull Location location();

    @NonNull Material icon();

    void edit(@NonNull String input, @Nullable Filter filter);

    default void edit(@NonNull Filter filter) {
        edit(filter.raw(), filter);
    }

    default void edit(@NonNull String input, @NonNull Player player) {
        Filter filter = null;

        try {
            filter = FilterCompiler.compile(input);
        } catch (FilterCompileException e) {
            player.sendMessage(e);
        }

        edit(input, filter);
    }
}
