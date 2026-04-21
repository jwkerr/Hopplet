package au.lupine.hopplet.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.Hopper;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.HopperInventorySearchEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class HopperListener implements Listener {

    @EventHandler
    public void on(@NotNull InventoryMoveItemEvent event) {
        Inventory destination = event.getDestination();
        if (!destination.getType().equals(InventoryType.HOPPER)) return;

        InventoryHolder holder = destination.getHolder(false);
        if (holder == null) return;

        String name = hopperName(holder);
        if (name == null) return;

        ItemStack item = event.getItem();
    }

    @EventHandler
    public void on(@NotNull HopperInventorySearchEvent event) {

    }

    private @Nullable String hopperName(@NotNull InventoryHolder holder) {
        if (holder instanceof Hopper hopper) return serialise(hopper.customName());
        if (holder instanceof HopperMinecart hopper) return serialise(hopper.customName());
        return null;
    }

    private @Nullable String serialise(@Nullable Component component) {
        return component == null ? null : PlainTextComponentSerializer.plainText().serialize(component);
    }
}
