package com.boydti.fawe.bukkit.v1_9;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.bukkit.v0.BukkitQueue_0;
import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.IntegerPair;
import com.boydti.fawe.object.PseudoRandom;
import com.boydti.fawe.util.MemUtil;
import com.sk89q.worldedit.LocalSession;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.server.v1_9_R1.Block;
import net.minecraft.server.v1_9_R1.BlockPosition;
import net.minecraft.server.v1_9_R1.Blocks;
import net.minecraft.server.v1_9_R1.ChunkSection;
import net.minecraft.server.v1_9_R1.DataBits;
import net.minecraft.server.v1_9_R1.DataPalette;
import net.minecraft.server.v1_9_R1.DataPaletteBlock;
import net.minecraft.server.v1_9_R1.IBlockData;
import net.minecraft.server.v1_9_R1.TileEntity;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.v1_9_R1.CraftChunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;

public class BukkitQueue_1_9_R1 extends BukkitQueue_0<Chunk, ChunkSection[], DataPaletteBlock> {

    private IBlockData air;

    public BukkitQueue_1_9_R1(final String world) {
        super(world);
        try {
            Field fieldAir = DataPaletteBlock.class.getDeclaredField("a");
            fieldAir.setAccessible(true);
            air = (IBlockData) fieldAir.get(null);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public ChunkSection[] getCachedChunk(World world, int cx, int cz) {
        CraftChunk chunk = (CraftChunk) world.getChunkAt(cx, cz);
        return chunk.getHandle().getSections();
    }

    @Override
    public DataPaletteBlock getCachedSection(ChunkSection[] chunkSections, int cy) {
        ChunkSection nibble = chunkSections[cy];
        return nibble != null ? nibble.getBlocks() : null;
    }

    @Override
    public int getCombinedId4Data(DataPaletteBlock lastSection, int x, int y, int z) {
        IBlockData ibd = lastSection.a(x & 15, y & 15, z & 15);
        Block block = ibd.getBlock();
        int id = Block.getId(block);
        if (FaweCache.hasData(id)) {
            return (id << 4) + block.toLegacyData(ibd);
        } else {
            return id << 4;
        }
    }

    @Override
    public void refreshChunk(World world, Chunk fc) {
        world.refreshChunk(fc.getX(), fc.getZ());
    }

    @Override
    public boolean fixLighting(final FaweChunk pc, final boolean fixAll) {
        try {
            final CharFaweChunk bc = (CharFaweChunk) pc;
            final Chunk chunk = (Chunk) bc.getChunk();
            if (!chunk.isLoaded()) {
                if (Fawe.get().getMainThread() != Thread.currentThread()) {
                    return false;
                }
                chunk.load(false);
            }
            // Initialize lighting
            net.minecraft.server.v1_9_R1.Chunk c = ((CraftChunk) chunk).getHandle();
            c.initLighting();

            if (((bc.getTotalRelight() == 0) && !fixAll)) {
                return true;
            }

            ChunkSection[] sections = c.getSections();
            net.minecraft.server.v1_9_R1.World w = c.world;

            final int X = chunk.getX() << 4;
            final int Z = chunk.getZ() << 4;

            BlockPosition.MutableBlockPosition pos = new BlockPosition.MutableBlockPosition(0, 0, 0);
            for (int j = 0; j < sections.length; j++) {
                final Object section = sections[j];
                if (section == null) {
                    continue;
                }
                if (((bc.getRelight(j) == 0) && !fixAll) || (bc.getCount(j) == 0) || ((bc.getCount(j) >= 4096) && (bc.getAir(j) == 0))) {
                    continue;
                }
                final char[] array = bc.getIdArray(j);
                if (array == null) {
                    continue;
                }
                int l = PseudoRandom.random.random(2);
                for (int k = 0; k < array.length; k++) {
                    final int i = array[k];
                    if (i < 16) {
                        continue;
                    }
                    final short id = (short) (i >> 4);
                    switch (id) { // Lighting
                        case 0:
                            continue;
                        default:
                            if (!fixAll) {
                                continue;
                            }
                            if ((k & 1) == l) {
                                l = 1 - l;
                                continue;
                            }
                        case 10:
                        case 11:
                        case 39:
                        case 40:
                        case 50:
                        case 51:
                        case 62:
                        case 74:
                        case 76:
                        case 89:
                        case 122:
                        case 124:
                        case 130:
                        case 138:
                        case 169:
                            final int x = FaweCache.CACHE_X[j][k];
                            final int y = FaweCache.CACHE_Y[j][k];
                            final int z = FaweCache.CACHE_Z[j][k];
                            if (this.isSurrounded(bc.getIdArrays(), x, y, z)) {
                                continue;
                            }
                            pos.c(X + x, y, Z + z);
                            w.w(pos);
                    }
                }
            }
            return true;
        } catch (final Throwable e) {
            if (Thread.currentThread() == Fawe.get().getMainThread()) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public boolean isSurrounded(final char[][] sections, final int x, final int y, final int z) {
        return this.isSolid(this.getId(sections, x, y + 1, z))
                && this.isSolid(this.getId(sections, x + 1, y - 1, z))
                && this.isSolid(this.getId(sections, x - 1, y, z))
                && this.isSolid(this.getId(sections, x, y, z + 1))
                && this.isSolid(this.getId(sections, x, y, z - 1));
    }

    public boolean isSolid(final int i) {
        if (i != 0) {
            final Material material = Material.getMaterial(i);
            return (material != null) && Material.getMaterial(i).isOccluding();
        }
        return false;
    }

    public int getId(final char[][] sections, final int x, final int y, final int z) {
        if ((x < 0) || (x > 15) || (z < 0) || (z > 15)) {
            return 1;
        }
        if ((y < 0) || (y > 255)) {
            return 1;
        }
        final int i = FaweCache.CACHE_I[y][x][z];
        final char[] section = sections[i];
        if (section == null) {
            return 0;
        }
        final int j = FaweCache.CACHE_J[y][x][z];
        return section[j] >> 4;
    }

    public void setCount(int tickingBlockCount, int nonEmptyBlockCount, ChunkSection section) throws NoSuchFieldException, IllegalAccessException {
        Class<? extends ChunkSection> clazz = section.getClass();
        Field fieldTickingBlockCount = clazz.getDeclaredField("tickingBlockCount");
        Field fieldNonEmptyBlockCount = clazz.getDeclaredField("nonEmptyBlockCount");
        fieldTickingBlockCount.setAccessible(true);
        fieldNonEmptyBlockCount.setAccessible(true);
        fieldTickingBlockCount.set(section, tickingBlockCount);
        fieldNonEmptyBlockCount.set(section, nonEmptyBlockCount);
    }

    public void setPalette(ChunkSection section, DataPaletteBlock palette) throws NoSuchFieldException, IllegalAccessException {
        Field fieldSection = ChunkSection.class.getDeclaredField("blockIds");
        fieldSection.setAccessible(true);
        fieldSection.set(section, palette);
    }

    public ChunkSection newChunkSection(int y2, boolean flag, char[] array) {
        try {
            if (array == null) {
                return new ChunkSection(y2, flag);
            } else {
                return new ChunkSection(y2, flag, array);
            }
        } catch (Throwable e) {
            try {
                if (array == null) {
                    Constructor<ChunkSection> constructor = ChunkSection.class.getDeclaredConstructor(int.class, boolean.class, IBlockData[].class);
                    return constructor.newInstance(y2, flag, (IBlockData[]) null);
                } else {
                    Constructor<ChunkSection> constructor = ChunkSection.class.getDeclaredConstructor(int.class, boolean.class, char[].class, IBlockData[].class);
                    return constructor.newInstance(y2, flag, array, (IBlockData[]) null);
                }
            } catch (Throwable e2) {
                throw new RuntimeException(e2);
            }
        }
    }

    @Override
    public boolean setComponents(final FaweChunk pc) {
        final BukkitChunk_1_9 fs = (BukkitChunk_1_9) pc;
        final Chunk chunk = (Chunk) fs.getChunk();
        final World world = chunk.getWorld();
        chunk.load(true);
        try {
            final boolean flag = world.getEnvironment() == Environment.NORMAL;

            // Sections
            net.minecraft.server.v1_9_R1.Chunk c = ((CraftChunk) chunk).getHandle();
            net.minecraft.server.v1_9_R1.World w = c.world;
            ChunkSection[] sections = c.getSections();

            Class<? extends net.minecraft.server.v1_9_R1.Chunk> clazzChunk = c.getClass();
            final Field ef = clazzChunk.getDeclaredField("entitySlices");
            final Collection<?>[] entities = (Collection<?>[]) ef.get(c);

            // Trim tiles
            boolean removed = false;
            Map<BlockPosition, TileEntity> tiles = c.getTileEntities();
            if (fs.getTotalCount() >= 65536) {
                tiles.clear();
                removed = true;
            } else {
                Iterator<Entry<BlockPosition, TileEntity>> iter = tiles.entrySet().iterator();
                while (iter.hasNext()) {
                    Entry<BlockPosition, TileEntity> tile = iter.next();
                    BlockPosition pos = tile.getKey();
                    final int lx = pos.getX() & 15;
                    final int ly = pos.getY();
                    final int lz = pos.getZ() & 15;
                    final int j = FaweCache.CACHE_I[ly][lx][lz];
                    final int k = FaweCache.CACHE_J[ly][lx][lz];
                    final char[] array = fs.getIdArray(j);
                    if (array == null) {
                        continue;
                    }
                    if (array[k] != 0) {
                        removed = true;
                        iter.remove();
                    }
                }
            }

            // Trim entities
            for (int i = 0; i < 16; i++) {
                if ((entities[i] != null) && (fs.getCount(i) >= 4096)) {
                    entities[i].clear();
                }
            }
            // Efficiently merge sections
            for (int j = 0; j < sections.length; j++) {
                int count = fs.getCount(j);
                if (count == 0) {
                    continue;
                }
                final char[] array = fs.getIdArray(j);
                if (array == null) {
                    continue;
                }
                ChunkSection section = sections[j];
                if (section == null) {
                    if (fs.sectionPalettes != null && fs.sectionPalettes[j] != null) {
                        section = sections[j] = newChunkSection(j << 4, flag, null);
                        setPalette(section, fs.sectionPalettes[j]);
                        setCount(0, count - fs.getAir(j), section);
                        continue;
                    } else {
                        sections[j] = newChunkSection(j << 4, flag, array);
                    }
                    continue;
                } else if (count >= 4096) {
                    if (fs.sectionPalettes != null && fs.sectionPalettes[j] != null) {
                        setPalette(section, fs.sectionPalettes[j]);
                        setCount(0, count - fs.getAir(j), section);
                        continue;
                    } else {
                        sections[j] = newChunkSection(j << 4, flag, array);
                    }
                    continue;
                }
                DataPaletteBlock nibble = section.getBlocks();
                Field fieldBits = nibble.getClass().getDeclaredField("b");
                fieldBits.setAccessible(true);
                DataBits bits = (DataBits) fieldBits.get(nibble);

                Field fieldPalette = nibble.getClass().getDeclaredField("c");
                fieldPalette.setAccessible(true);
                DataPalette palette = (DataPalette) fieldPalette.get(nibble);
                int nonEmptyBlockCount = 0;
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            char combinedId = array[FaweCache.CACHE_J[y][x][z]];
                            switch (combinedId) {
                                case 0:
                                    IBlockData existing = nibble.a(x, y, z);
                                    if (existing != air) {
                                        nonEmptyBlockCount++;
                                    }
                                    continue;
                                case 1:
                                    nibble.setBlock(x, y, z, Blocks.AIR.getBlockData());
                                    continue;
                                default:
                                    nonEmptyBlockCount++;
                                    nibble.setBlock(x, y, z, Block.getById(combinedId >> 4).fromLegacyData(combinedId & 0xF));
                            }
                        }
                    }
                }
                setCount(0, nonEmptyBlockCount, section);
            }
            // Clear
        } catch (Throwable e) {
            e.printStackTrace();
        }
        final int[][] biomes = fs.getBiomeArray();
        final Biome[] values = Biome.values();
        if (biomes != null) {
            for (int x = 0; x < 16; x++) {
                final int[] array = biomes[x];
                if (array == null) {
                    continue;
                }
                for (int z = 0; z < 16; z++) {
                    final int biome = array[z];
                    if (biome == 0) {
                        continue;
                    }
                    chunk.getBlock(x, 0, z).setBiome(values[biome]);
                }
            }
        }
        sendChunk(fs);
        return true;
    }

    /**
     * This method is called when the server is < 1% available memory (i.e. likely to crash)<br>
     *  - You can disable this in the conifg<br>
     *  - Will try to free up some memory<br>
     *  - Clears the queue<br>
     *  - Clears worldedit history<br>
     *  - Clears entities<br>
     *  - Unloads chunks in vacant worlds<br>
     *  - Unloads non visible chunks<br>
     */
    @Override
    public void saveMemory() {
        super.saveMemory();
        // Clear the queue
        super.clear();
        ArrayDeque<Chunk> toUnload = new ArrayDeque<>();
        final int distance = Bukkit.getViewDistance() + 2;
        HashMap<String, HashMap<IntegerPair, Integer>> players = new HashMap<>();
        for (final Player player : Bukkit.getOnlinePlayers()) {
            // Clear history
            final FawePlayer<Object> fp = FawePlayer.wrap(player);
            final LocalSession s = fp.getSession();
            if (s != null) {
                s.clearHistory();
                s.setClipboard(null);
            }
            final Location loc = player.getLocation();
            final World worldObj = loc.getWorld();
            final String world = worldObj.getName();
            HashMap<IntegerPair, Integer> map = players.get(world);
            if (map == null) {
                map = new HashMap<>();
                players.put(world, map);
            }
            final IntegerPair origin = new IntegerPair(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
            Integer val = map.get(origin);
            int check;
            if (val != null) {
                if (val == distance) {
                    continue;
                }
                check = distance - val;
            } else {
                check = distance;
                map.put(origin, distance);
            }
            for (int x = -distance; x <= distance; x++) {
                if ((x >= check) || (-x >= check)) {
                    continue;
                }
                for (int z = -distance; z <= distance; z++) {
                    if ((z >= check) || (-z >= check)) {
                        continue;
                    }
                    final int weight = distance - Math.max(Math.abs(x), Math.abs(z));
                    final IntegerPair chunk = new IntegerPair(x + origin.x, z + origin.z);
                    val = map.get(chunk);
                    if ((val == null) || (val < weight)) {
                        map.put(chunk, weight);
                    }
                }
            }
        }
        Fawe.get().getWorldEdit().clearSessions();
        for (final World world : Bukkit.getWorlds()) {
            final String name = world.getName();
            final HashMap<IntegerPair, Integer> map = players.get(name);
            if ((map == null) || (map.size() == 0)) {
                final boolean save = world.isAutoSave();
                world.setAutoSave(false);
                for (final Chunk chunk : world.getLoadedChunks()) {
                    this.unloadChunk(name, chunk);
                }
                world.setAutoSave(save);
                continue;
            }
            final Chunk[] chunks = world.getLoadedChunks();
            for (final Chunk chunk : chunks) {
                final int x = chunk.getX();
                final int z = chunk.getZ();
                if (!map.containsKey(new IntegerPair(x, z))) {
                    toUnload.add(chunk);
                } else if (chunk.getEntities().length > 4096) {
                    for (final Entity ent : chunk.getEntities()) {
                        ent.remove();
                    }
                }
            }
        }
        // GC again
        System.gc();
        System.gc();
        // If still critical memory
        int free = MemUtil.calculateMemory();
        if (free <= 1) {
            for (final Chunk chunk : toUnload) {
                this.unloadChunk(chunk.getWorld().getName(), chunk);
            }
        } else if (free == Integer.MAX_VALUE) {
            for (final Chunk chunk : toUnload) {
                chunk.unload(true, false);
            }
        } else {
            return;
        }
        toUnload = null;
        players = null;
    }

    public boolean unloadChunk(final String world, final Chunk chunk) {
        net.minecraft.server.v1_9_R1.Chunk c = ((CraftChunk) chunk).getHandle();
        c.mustSave = false;
        if (chunk.isLoaded()) {
            chunk.unload(false, false);
        }
        return true;
    }

    public ChunkGenerator setGenerator(final World world, final ChunkGenerator newGen) {
        try {
            final ChunkGenerator gen = world.getGenerator();
            final Class<? extends World> clazz = world.getClass();
            final Field generator = clazz.getDeclaredField("generator");
            generator.setAccessible(true);
            generator.set(world, newGen);

            final Field wf = clazz.getDeclaredField("world");
            wf.setAccessible(true);
            final Object w = wf.get(world);
            final Class<?> clazz2 = w.getClass().getSuperclass();
            final Field generator2 = clazz2.getDeclaredField("generator");
            generator2.set(w, newGen);

            return gen;
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<BlockPopulator> setPopulator(final World world, final List<BlockPopulator> newPop) {
        try {
            final List<BlockPopulator> pop = world.getPopulators();
            final Field populators = world.getClass().getDeclaredField("populators");
            populators.setAccessible(true);
            populators.set(world, newPop);
            return pop;
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setEntitiesAndTiles(final Chunk chunk, final List<?>[] entities, final Map<?, ?> tiles) {
        try {
            final Class<? extends Chunk> clazz = chunk.getClass();
            final Method handle = clazz.getMethod("getHandle");
            final Object c = handle.invoke(chunk);
            final Class<? extends Object> clazz2 = c.getClass();

            if (tiles.size() > 0) {
                final Field tef = clazz2.getDeclaredField("tileEntities");
                final Map<?, ?> te = (Map<?, ?>) tef.get(c);
                final Method put = te.getClass().getMethod("putAll", Map.class);
                put.invoke(te, tiles);
            }

            final Field esf = clazz2.getDeclaredField("entitySlices");
            esf.setAccessible(true);
            esf.set(c, entities);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public Object getProvider(final World world) {
        try {
            // Provider 1
            final Class<? extends World> clazz = world.getClass();
            final Field wf = clazz.getDeclaredField("world");
            wf.setAccessible(true);
            final Object w = wf.get(world);
            final Field provider = w.getClass().getSuperclass().getDeclaredField("chunkProvider");
            provider.setAccessible(true);
            // ChunkProviderServer
            final Class<? extends Object> clazz2 = w.getClass();
            final Field wpsf = clazz2.getDeclaredField("chunkProviderServer");
            // Store old provider server
            final Object worldProviderServer = wpsf.get(w);
            // Store the old provider
            final Field cp = worldProviderServer.getClass().getDeclaredField("chunkProvider");
            return cp.get(worldProviderServer);
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Object setProvider(final World world, Object newProvider) {
        try {
            // Provider 1
            final Class<? extends World> clazz = world.getClass();
            final Field wf = clazz.getDeclaredField("world");
            wf.setAccessible(true);
            final Object w = wf.get(world);
            // ChunkProviderServer
            final Class<? extends Object> clazz2 = w.getClass();
            final Field wpsf = clazz2.getDeclaredField("chunkProviderServer");
            // Store old provider server
            final Object worldProviderServer = wpsf.get(w);
            // Store the old provider
            final Field cp = worldProviderServer.getClass().getDeclaredField("chunkProvider");
            final Object oldProvider = cp.get(worldProviderServer);
            // Provider 2
            final Class<? extends Object> clazz3 = worldProviderServer.getClass();
            final Field provider2 = clazz3.getDeclaredField("chunkProvider");
            // If the provider needs to be calculated
            if (newProvider == null) {
                Method k;
                try {
                    k = clazz2.getDeclaredMethod("k");
                } catch (final Throwable e) {
                    try {
                        k = clazz2.getDeclaredMethod("j");
                    } catch (final Throwable e2) {
                        e2.printStackTrace();
                        return null;
                    }
                }
                k.setAccessible(true);
                final Object tempProviderServer = k.invoke(w);
                newProvider = cp.get(tempProviderServer);
                // Restore old provider
                wpsf.set(w, worldProviderServer);
            }
            // Set provider for provider server
            provider2.set(worldProviderServer, newProvider);
            // Return the previous provider
            return oldProvider;
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public FaweChunk getChunk(int x, int z) {
        return new BukkitChunk_1_9(this, x, z);
    }
}
