package au.lupine.hopplet.filter.cache;

import au.lupine.hopplet.filter.Filter;
import au.lupine.hopplet.filter.compiler.FilterCompiler;
import au.lupine.hopplet.filter.exception.FilterCompileException;
import it.unimi.dsi.fastutil.ints.AbstractInt2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.entity.minecart.HopperMinecart;
import org.jetbrains.annotations.Range;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FilterCache {

    // Using multiple nested maps allows invalidating a full world/chunk at once, doubt there's any noticeable difference in lookup speed
    // an important assumption is that only the region thread responsible for the chunk will access the final int to object map
    public static final Map<UUID, Map<Long, AbstractInt2ObjectMap<Filter>>> BLOCK_CACHE = new ConcurrentHashMap<>();
    public static final Map<UUID, Filter> ENTITY_CACHE = new ConcurrentHashMap<>();

    private static final java.util.function.Function<UUID, Map<Long, AbstractInt2ObjectMap<Filter>>> NEW_WORLD_CHUNK_MAP = ignored -> new ConcurrentHashMap<>();
    private static final java.util.function.Function<Long, AbstractInt2ObjectMap<Filter>> NEW_CHUNK_MAP = ignored -> new Int2ObjectOpenHashMap<>();

    // Generic methods

    public static void invalidate() {
        BLOCK_CACHE.clear();
        ENTITY_CACHE.clear();
    }

    public static void cache(final UUID worldUUID, final int x, final int y, final int z, final Filter filter) {
        BLOCK_CACHE
            .computeIfAbsent(worldUUID, NEW_WORLD_CHUNK_MAP)
            .computeIfAbsent(Chunk.getChunkKey(x >> 4, z >> 4), NEW_CHUNK_MAP)
            .put(packChunkRelativeCoords(x, y, z), filter);
    }

    private static @Nullable AbstractInt2ObjectMap<Filter> getChunkFilterMap(final UUID worldUUID, final int x, final int z) {
        final Map<Long, AbstractInt2ObjectMap<Filter>> chunkMap = BLOCK_CACHE.get(worldUUID);
        if (chunkMap == null) return null;

        return chunkMap.get(Chunk.getChunkKey(x >> 4, z >> 4));
    }

    public static @Nullable Filter get(final UUID worldUUID, final int x, final int y, final int z) {
        final AbstractInt2ObjectMap<Filter> filterMap = getChunkFilterMap(worldUUID, x, z);
        if (filterMap == null) return null;

        return filterMap.get(packChunkRelativeCoords(x, y, z));
    }

    public static void invalidate(final UUID worldUUID, final int x, final int y, final int z){
        final AbstractInt2ObjectMap<Filter> filterMap = getChunkFilterMap(worldUUID, x, z);
        if (filterMap != null) filterMap.remove(packChunkRelativeCoords(x, y, z));
    }

    // Location methods

    public static void cache(@NonNull Location location, @NonNull Filter filter) {
        cache(location.getWorld().getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), filter);
    }

    public static @Nullable Filter get(@NonNull Location location) {
        return get(location.getWorld().getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static void invalidate(@NonNull Location location) {
        invalidate(location.getWorld().getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    // Block methods

    public static void cache(@NonNull Block block, @NonNull Filter filter) {
        cache(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ(), filter);
    }

    public static @Nullable Filter get(@NonNull Block block) {
        return get(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }

    public static void invalidate(@NonNull Block block) {
        invalidate(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }

    public static void cache(@NonNull Hopper hopper, @NonNull Filter filter) {
        cache(hopper.getWorld().getUID(), hopper.getX(), hopper.getY(), hopper.getZ(), filter);
    }

    public static @Nullable Filter get(@NonNull Hopper hopper) {
        return get(hopper.getWorld().getUID(), hopper.getX(), hopper.getY(), hopper.getZ());
    }

    public static @Nullable Filter getOrCompile(@NonNull Hopper hopper) throws FilterCompileException {
        final AbstractInt2ObjectMap<Filter> map = BLOCK_CACHE
            .computeIfAbsent(hopper.getWorld().getUID(), NEW_WORLD_CHUNK_MAP)
            .computeIfAbsent(Chunk.getChunkKey(hopper.getX() >> 4, hopper.getZ() >> 4), NEW_CHUNK_MAP);

        final int packed = packChunkRelativeCoords(hopper.getX(), hopper.getY(), hopper.getZ());

        Filter filter = map.get(packed);
        if (filter != null) return filter;

        Filter compiled = FilterCompiler.compile(hopper);
        if (compiled != null) map.put(packed, compiled);

        return compiled;
    }

    public static void invalidate(@NonNull Hopper hopper) {
        invalidate(hopper.getWorld().getUID(), hopper.getX(), hopper.getY(), hopper.getZ());
    }

    // Hopper minecart methods

    public static void cache(@NonNull UUID uuid, @NonNull Filter filter) {
        ENTITY_CACHE.put(uuid, filter);
    }

    public static @Nullable Filter get(@NonNull UUID uuid) {
        return ENTITY_CACHE.get(uuid);
    }

    public static void invalidate(@NonNull UUID uuid) {
        ENTITY_CACHE.remove(uuid);
    }

    public static void cache(@NonNull HopperMinecart hopper, @NonNull Filter filter) {
        cache(hopper.getUniqueId(), filter);
    }

    public static @Nullable Filter get(@NonNull HopperMinecart hopper) {
        return get(hopper.getUniqueId());
    }

    public static @Nullable Filter getOrCompile(@NonNull HopperMinecart hopper) throws FilterCompileException {
        Filter filter = get(hopper);
        if (filter != null) return filter;

        Filter compiled = FilterCompiler.compile(hopper);
        if (compiled != null) cache(hopper, compiled);

        return compiled;
    }

    public static void invalidate(@NonNull HopperMinecart hopper) {
        invalidate(hopper.getUniqueId());
    }

    /**
     * Packs chunk relative x and z coords and the y coordinate into a single integer.
     *
     * @param x The world or chunk relative x coordinate.
     * @param y The world relative y coordinate, the maximum supported range is from 2^23 - 1 to -2^23
     * @param z The world or chunk relative z coordinate.
     * @return A packed integer that uniquely identifies these coords in a chunk.
     */
    public static int packChunkRelativeCoords(final int x, @Range(from = -8_388_608, to = 8_388_607) final int y, final int z) {
        return x & 0xF // mask x and z with 0xF (15) to ensure they are within range
            | (z & 0xF) << 4 // z is put into the next 4 bits
            | (y & 0xFFFFFF) << 8; // and y is put into the remaining 24 bits after x and z
    }
}
