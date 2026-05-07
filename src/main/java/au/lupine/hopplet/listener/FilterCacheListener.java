package au.lupine.hopplet.listener;

import au.lupine.hopplet.filter.Filter;
import au.lupine.hopplet.filter.cache.FilterCache;
import au.lupine.hopplet.filter.compiler.FilterCompiler;
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
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.Map;

public final class FilterCacheListener implements Listener {

    private void invalidate(@NonNull World world,@NonNull Collection<Block> blocks) {
        Map<Long, AbstractInt2ObjectMap<Filter>> worldChunkCache = FilterCache.BLOCK_CACHE.get(world.getUID());
        if (worldChunkCache == null) return;

        for (Block block : blocks) {
            if (block.getType() != Material.HOPPER) continue;

            AbstractInt2ObjectMap<Filter> chunkCache = worldChunkCache.get(Chunk.getChunkKey(block.getX() >> 4, block.getZ() >> 4));
            if (chunkCache == null) continue;

            chunkCache.remove(FilterCache.packChunkRelativeCoords(block.getX(), block.getY(), block.getZ()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(@NonNull BlockPlaceEvent event) {
        if (!(event.getBlock().getState(false) instanceof Hopper hopper)) return;

        Filter filter;
        try {
            filter = FilterCompiler.compile(hopper);
        } catch (FilterCompileException e) {
            return;
        }

        if (filter == null) return;

        FilterCache.cache(hopper, filter);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(@NonNull BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.HOPPER) FilterCache.invalidate(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(@NonNull EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof HopperMinecart hopper)) return;

        Filter filter;
        try {
            filter = FilterCompiler.compile(hopper);
        } catch (FilterCompileException e) {
            return;
        }

        if (filter == null) return;

        FilterCache.cache(hopper, filter);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(@NonNull EntityRemoveFromWorldEvent event) {
        if (event.getEntity() instanceof HopperMinecart hopper) FilterCache.invalidate(hopper);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(@NonNull BlockExplodeEvent event) {
        invalidate(event.getBlock().getWorld(), event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(@NonNull EntityExplodeEvent event) {
        invalidate(event.getEntity().getWorld(), event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(@NonNull WorldUnloadEvent event) {
        FilterCache.BLOCK_CACHE.remove(event.getWorld().getUID());
    }

    @EventHandler
    public void cleanupBlockFilterCache(@NonNull ChunkUnloadEvent event) {
        Map<Long, ?> worldChunkCache = FilterCache.BLOCK_CACHE.get(event.getWorld().getUID());

        if (worldChunkCache == null) return;

        Chunk chunk = event.getChunk();
        worldChunkCache.remove(Chunk.getChunkKey(chunk.getX(), chunk.getZ()));
    }
}
