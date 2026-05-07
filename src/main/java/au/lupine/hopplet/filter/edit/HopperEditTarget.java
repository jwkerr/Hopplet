package au.lupine.hopplet.filter.edit;

import au.lupine.hopplet.Hopplet;
import au.lupine.hopplet.filter.Filter;
import au.lupine.hopplet.filter.cache.FilterCache;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class HopperEditTarget implements EditTarget {

    private final @Nullable Component component;
    private final @NonNull Location location;

    public HopperEditTarget(@NonNull Hopper hopper) {
        this.component = hopper.customName();
        this.location = hopper.getLocation();
    }

    @Override
    public @Nullable Component component() {
        return component;
    }

    @Override
    public @NonNull Location location() {
        return location;
    }

    @Override
    public @NonNull Material icon() {
        return Material.HOPPER;
    }

    @Override
    public void edit(@NonNull String input, @Nullable Filter filter) {
        Hopplet instance = Hopplet.instance();

        instance.getServer().getRegionScheduler().run(instance, location, task -> {
            Block target = location.getBlock();
            if (!(target.getState(false) instanceof Hopper hopper)) return;

            FilterCache.invalidate(hopper);

            if (input.isBlank()) {
                hopper.customName(null);
            } else {
                hopper.customName(Component.text(input, Filter.style));
                if (filter != null) FilterCache.cache(hopper, filter);
            }

            hopper.setTransferCooldown(20);
            hopper.update();
        });
    }
}
