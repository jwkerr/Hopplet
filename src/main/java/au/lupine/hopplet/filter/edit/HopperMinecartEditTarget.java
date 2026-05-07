package au.lupine.hopplet.filter.edit;

import au.lupine.hopplet.Hopplet;
import au.lupine.hopplet.filter.Filter;
import au.lupine.hopplet.filter.cache.FilterCache;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.minecart.HopperMinecart;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class HopperMinecartEditTarget implements EditTarget {

    private final @NonNull HopperMinecart hopper;
    private final @Nullable Component component;

    public HopperMinecartEditTarget(@NonNull HopperMinecart hopper) {
        this.hopper = hopper;
        this.component = hopper.customName();
    }

    @Override
    public @Nullable Component component() {
        return component;
    }

    @Override
    public @NonNull Location location() {
        return hopper.getLocation();
    }

    @Override
    public @NonNull Material icon() {
        return Material.HOPPER_MINECART;
    }

    @Override
    public void edit(@NonNull String input, @Nullable Filter filter) {
        Hopplet instance = Hopplet.instance();

        instance.getServer().getRegionScheduler().run(instance, hopper.getLocation(), task -> {
            if (!hopper.isValid()) return;

            FilterCache.invalidate(hopper);

            if (input.isBlank()) {
                hopper.customName(null);
            } else {
                hopper.customName(Component.text(input, Filter.style));
                if (filter != null) FilterCache.cache(hopper, filter);
            }
        });
    }
}
