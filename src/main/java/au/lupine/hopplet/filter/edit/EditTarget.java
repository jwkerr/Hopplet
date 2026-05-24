package au.lupine.hopplet.filter.edit;

import au.lupine.hopplet.event.FilterEditedByPlayerEvent;
import au.lupine.hopplet.event.PreFilterEditByPlayerEvent;
import au.lupine.hopplet.filter.Filter;
import au.lupine.hopplet.filter.compiler.Compiler;
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
            filter = Compiler.compile(input);

            // TODO: move this into EditDialog, implementation shouldn't throw events (for api users)
            PreFilterEditByPlayerEvent event = new PreFilterEditByPlayerEvent(player, this, input, filter);
            if (!event.callEvent()) throw new FilterCompileException(event.message());
        } catch (FilterCompileException e) {
            player.sendMessage(e);
        }

        edit(input, filter);

        new FilterEditedByPlayerEvent(player, this, input, filter).callEvent();
    }
}
