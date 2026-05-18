package au.lupine.hopplet.listener;

import au.lupine.hopplet.Hopplet;
import au.lupine.hopplet.filter.Filter;
import au.lupine.hopplet.filter.cache.Cache;
import au.lupine.hopplet.filter.context.Context;
import au.lupine.hopplet.filter.context.HopperInventoryTransferContext;
import au.lupine.hopplet.filter.context.HopperPickupItemContext;
import au.lupine.hopplet.filter.exception.FilterCompileException;
import au.lupine.hopplet.util.HopperRouting;
import org.bukkit.block.Hopper;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

public final class HopperInventoryListener implements Listener {

    @EventHandler
    public void on(@NonNull InventoryMoveItemEvent event) {
        Inventory destination = event.getDestination();
        if (destination.getType() != InventoryType.HOPPER) return;

        InventoryHolder holder = destination.getHolder(false);
        if (holder == null) return;

        Filter filter;
        try {
            filter = switch (holder) {
                case Hopper hopper -> Cache.getOrCompile(hopper);
                case HopperMinecart hopper -> Cache.getOrCompile(hopper);
                default -> null;
            };
        } catch (FilterCompileException e) {
            if (Hopplet.instance().config().root().node("filter", "disable_hopper_on_compilation_error").getBoolean(false)) event.setCancelled(true);
            return;
        }

        ItemStack item = event.getItem();
        Inventory source = event.getSource();

        if (filter != null) {
            Context context = new HopperInventoryTransferContext(item, source, destination);

            if (!filter.test(context)) event.setCancelled(true);
            return;
        }

        if (!(holder instanceof Hopper destinationHopper)) return;

        Hopper alternative = HopperRouting.alternative(source, destinationHopper);
        if (alternative == null) return;

        if (!HopperRouting.enabled(alternative)) return;

        if (!HopperRouting.fits(alternative.getInventory(), item)) return;

        Filter alternativeFilter;
        try {
            alternativeFilter = Cache.getOrCompile(alternative);
        } catch (FilterCompileException e) {
            return;
        }

        if (alternativeFilter == null) return;

        if (alternativeFilter.test(new HopperInventoryTransferContext(item, source, alternative.getInventory()))) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void on(@NonNull InventoryPickupItemEvent event) {
        Inventory inventory = event.getInventory();
        if (!inventory.getType().equals(InventoryType.HOPPER)) return;

        InventoryHolder holder = inventory.getHolder(false);
        if (holder == null) return;

        Filter filter;
        try {
            filter = switch (holder) {
                case Hopper hopper -> Cache.getOrCompile(hopper);
                case HopperMinecart hopper -> Cache.getOrCompile(hopper);
                default -> null;
            };
        } catch (FilterCompileException e) {
            if (Hopplet.instance().config().root().node("filter", "disable_hopper_on_compilation_error").getBoolean(false)) event.setCancelled(true);
            return;
        }

        if (filter == null) return;

        Context context = new HopperPickupItemContext(event.getItem(), inventory);

        if (!filter.test(context)) event.setCancelled(true);
    }
}
