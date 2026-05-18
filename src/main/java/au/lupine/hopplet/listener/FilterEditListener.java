package au.lupine.hopplet.listener;

import au.lupine.hopplet.filter.edit.EditDialog;
import au.lupine.hopplet.filter.edit.HopperEditTarget;
import au.lupine.hopplet.filter.edit.HopperMinecartEditTarget;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jspecify.annotations.NonNull;

public final class FilterEditListener implements Listener {

    @EventHandler
    public void on(@NonNull PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("hopplet.edit.by_interacting")) return;
        if (!player.hasPermission("hopplet.edit.hopper")) return;

        if (!event.getAction().isRightClick()) return;

        if (event.getItem() != null && event.useItemInHand() != Event.Result.DENY) return;

        if (!player.isSneaking()) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        if (!(block.getState(false) instanceof Hopper hopper)) return;

        BlockBreakEvent bbe = new BlockBreakEvent(block, player);
        if (!bbe.callEvent()) return; // Player does not have permission to edit this hopper.

        event.setCancelled(true);
        EditDialog.open(player, new HopperEditTarget(hopper));
    }

    @EventHandler
    public void on(@NonNull PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("hopplet.edit.by_interacting")) return;
        if (!player.hasPermission("hopplet.edit.hopper_minecart")) return;

        if (!(event.getRightClicked() instanceof HopperMinecart hopper)) return;

        if (!player.isSneaking()) return;

        BlockBreakEvent bbe = new BlockBreakEvent(hopper.getLocation().getBlock(), player);
        if (!bbe.callEvent()) return;

        event.setCancelled(true);
        EditDialog.open(player, new HopperMinecartEditTarget(hopper));
    }
}
