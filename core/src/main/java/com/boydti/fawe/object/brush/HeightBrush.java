package com.boydti.fawe.object.brush;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.IntegerPair;
import com.boydti.fawe.object.brush.heightmap.ArrayHeightMap;
import com.boydti.fawe.object.brush.heightmap.HeightMap;
import com.boydti.fawe.object.exception.FaweException;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.pattern.Pattern;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import javax.imageio.ImageIO;

public class HeightBrush implements Brush {

    public final HeightMap heightMap;
    private final int rotation;
    double yscale = 1;
    private final BrushTool tool;

    public HeightBrush(File file, int rotation, double yscale, BrushTool tool, EditSession session, Clipboard clipboard) {
        this.tool = tool;
        this.rotation = (rotation / 90) % 4;
        this.yscale = yscale;
        if (file == null || !file.exists()) {
            // Since I can't be bothered using separate args, we'll get it from the filename
            if (file.getName().equalsIgnoreCase("#clipboard.png") && clipboard != null) {
                Vector dim = clipboard.getDimensions();
                byte[][] heightArray = new byte[dim.getBlockX()][dim.getBlockZ()];
                int minX = clipboard.getMinimumPoint().getBlockX();
                int minZ = clipboard.getMinimumPoint().getBlockZ();
                int minY = clipboard.getMinimumPoint().getBlockY();
                int maxY = clipboard.getMaximumPoint().getBlockY();
                int clipHeight = maxY - minY + 1;
                HashSet<IntegerPair> visited = new HashSet<>();
                for (Vector pos : clipboard.getRegion()) {
                    IntegerPair pair = new IntegerPair((int) pos.x, (int) pos.z);
                    if (visited.contains(pair)) {
                        continue;
                    }
                    visited.add(pair);
                    int xx = pos.getBlockX();
                    int zz = pos.getBlockZ();
                    int highestY = 0;
                    for (int y = minY; y <= maxY; y++) {
                        pos.y = y;
                        BaseBlock block = clipboard.getBlock(pos);
                        if (block != EditSession.nullBlock) {
                            highestY = y + 1;
                        }
                    }
                    int pointHeight = Math.min(255, (256 * (highestY - minY)) / clipHeight);
                    int x = xx - minX;
                    int z = zz - minZ;
                    heightArray[x][z] = (byte) pointHeight;
                }
                heightMap = new ArrayHeightMap(heightArray);
            } else {
                heightMap = new HeightMap();
            }
        } else {
            try {
                BufferedImage heightFile = ImageIO.read(file);
                int width = heightFile.getWidth();
                int length = heightFile.getHeight();
                Raster data = heightFile.getData();
                byte[][] array = new byte[width][length];
                for (int x = 0; x < width; x++) {
                    for (int z = 0; z < length; z++) {
                        int pixel = heightFile.getRGB(x, z);
                        int red = (pixel >> 16) & 0xFF;
                        int green = (pixel >> 8) & 0xFF;
                        int blue = (pixel >> 0) & 0xFF;
                        int intensity = (red + green + blue) / 3;
                        array[x][z] = (byte) intensity;
                    }
                }
                heightMap = new ArrayHeightMap(array);
            } catch (IOException e) {
                throw new FaweException(BBC.BRUSH_HEIGHT_INVALID);
            }
        }
    }

    @Override
    public void build(EditSession editSession, Vector position, Pattern pattern, double sizeDouble) throws MaxChangedBlocksException {
        Mask mask = tool.getMask();
        if (mask == Masks.alwaysTrue() || mask == Masks.alwaysTrue2D()) {
            mask = null;
        }
        int size = (int) sizeDouble;
        heightMap.setSize(size);
        int size2 = size * size;
        int startY = position.getBlockY() + size;
        int endY = position.getBlockY() - size;
        int cx = position.getBlockX();
        int cz = position.getBlockZ();
        Vector mutablePos = new Vector(0, 0, 0);
        for (int x = -size; x <= size; x++) {
            int xx = cx + x;
            mutablePos.x = xx;
            for (int z = -size; z <= size; z++) {
                int zz = cz + z;
                int raise;
                switch (rotation) {
                    default:
                        raise = heightMap.getHeight(x, z);
                        break;
                    case 1:
                        raise = heightMap.getHeight(z, x);
                        break;
                    case 2:
                        raise = heightMap.getHeight(-x, -z);
                        break;
                    case 3:
                        raise = heightMap.getHeight(-z, -x);
                        break;
                }
                raise = (int) (yscale * raise);
                if (raise == 0) {
                    continue;
                }
                mutablePos.z = zz;
                int foundHeight = Integer.MAX_VALUE;
                BaseBlock block = null;
                for (int y = startY; y >= endY; y--) {
                    block = editSession.getLazyBlock(xx, y, zz);
                    if (block != EditSession.nullBlock) {
                        if (mask != null) {
                            mutablePos.y = y;
                            if (!mask.test(mutablePos)) {
                                continue;
                            }
                        }
                        foundHeight = y;
                        break;
                    }
                }
                if (foundHeight  == Integer.MAX_VALUE) {
                    continue;
                }
                if (raise > 0) {
                    for (int y = foundHeight + 1; y <= foundHeight + raise; y++) {
                        mutablePos.y = y;
                        editSession.setBlock(mutablePos, block);
                    }
                } else {
                    for (int y = foundHeight; y > foundHeight + raise; y--) {
                        mutablePos.y = y;
                        editSession.setBlock(mutablePos, EditSession.nullBlock);
                    }
                    mutablePos.y = foundHeight + raise;
                    editSession.setBlock(mutablePos, block);
                }
            }
        }
    }
}
