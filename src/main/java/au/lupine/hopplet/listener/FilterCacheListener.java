package au.lupine.hopplet.listener;

import au.lupine.hopplet.filter.Filter;
import au.lupine.hopplet.filter.exception.FilterCompileException;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import it.unimi.dsi.fastutil.ints.AbstractInt2ObjectMap;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.jspecify.annotations.NullMarked;

import java.util.Collection;
import java.util.Map;

@NullMarked
public final class FilterCacheListener implements Listener {

    private void invalidate(final World world, Collection<Block> blocks) {
        final Map<Long, AbstractInt2ObjectMap<Filter>> worldChunkCache = Filter.Cache.BLOCK_CACHE.get(world.getUID());
        if (worldChunkCache == null) return;

        for (final Block block : blocks) {
            if (block.getType() != Material.HOPPER) continue;

            final AbstractInt2ObjectMap<Filter> chunkCache = worldChunkCache.get(Chunk.getChunkKey(block.getX() >> 4, block.getZ() >> 4));
            if (chunkCache == null) continue;

            chunkCache.remove(Filter.Cache.packChunkRelativeCoords(block.getX(), block.getY(), block.getZ()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(BlockPlaceEvent event) {
        if (!(event.getBlock().getState(false) instanceof Hopper hopper)) return;

        Filter filter;
        try {
            filter = Filter.Compiler.compile(hopper);
        } catch (FilterCompileException e) {
            return;
        }

        if (filter == null) return;

        Filter.Cache.cache(hopper, filter);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.HOPPER) Filter.Cache.invalidate(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof HopperMinecart hopper)) return;

        Filter filter;
        try {
            filter = Filter.Compiler.compile(hopper);
        } catch (FilterCompileException e) {
            return;
        }

        if (filter == null) return;

        Filter.Cache.cache(hopper, filter);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(EntityRemoveFromWorldEvent event) {
        if (event.getEntity() instanceof HopperMinecart hopper) Filter.Cache.invalidate(hopper);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(BlockExplodeEvent event) {
        invalidate(event.getBlock().getWorld(), event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(EntityExplodeEvent event) {
        invalidate(event.getEntity().getWorld(), event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(WorldUnloadEvent event) {
        Filter.Cache.BLOCK_CACHE.remove(event.getWorld().getUID());
    }

    @EventHandler
    public void cleanupBlockFilterCache(final ChunkUnloadEvent event) {
        final Map<Long, ?> worldChunkCache = Filter.Cache.BLOCK_CACHE.get(event.getWorld().getUID());

        if (worldChunkCache == null) return;

        final Chunk chunk = event.getChunk();
        worldChunkCache.remove(Chunk.getChunkKey(chunk.getX(), chunk.getZ()));
    }
}
