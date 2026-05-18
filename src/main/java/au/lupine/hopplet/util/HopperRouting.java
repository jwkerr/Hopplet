package au.lupine.hopplet.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Hopper;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class HopperRouting {

    private HopperRouting() {}

    public static @Nullable Hopper alternative(@NonNull Inventory sourceInventory, @NonNull Hopper destination) {
        if (!(sourceInventory.getHolder(false) instanceof Hopper source)) return null;

        org.bukkit.block.data.type.Hopper data = (org.bukkit.block.data.type.Hopper) source.getBlockData();

        BlockFace face = data.getFacing();
        if (face == BlockFace.DOWN) return null;

        Block facing = source.getBlock().getRelative(face);
        if (facing.getType() != Material.HOPPER) return null;

        Hopper facingHopper = (Hopper) facing.getState(false);
        if (facingHopper.equals(destination)) {
            Block below = source.getBlock().getRelative(BlockFace.DOWN);
            if (below.getType() != Material.HOPPER) return null;

            return (Hopper) below.getState(false);
        }

        return facingHopper;
    }

    public static boolean enabled(@NonNull Hopper hopper) {
        org.bukkit.block.data.type.Hopper data = (org.bukkit.block.data.type.Hopper) hopper.getBlockData();
        return data.isEnabled();
    }

    public static boolean fits(@NonNull Inventory inventory, @NonNull ItemStack item) {
        Inventory clone = Bukkit.createInventory(null, InventoryType.HOPPER);
        clone.setContents(inventory.getContents());
        return clone.addItem(item).isEmpty();
    }
}
