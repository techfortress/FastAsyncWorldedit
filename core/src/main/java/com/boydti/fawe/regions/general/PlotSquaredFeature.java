package com.boydti.fawe.regions.general;

import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.regions.FaweMask;
import com.boydti.fawe.regions.FaweMaskManager;
import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.object.Location;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotId;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.object.RegionWrapper;
import com.sk89q.worldedit.BlockVector;
import java.util.HashSet;

public class PlotSquaredFeature extends FaweMaskManager {
    public PlotSquaredFeature() {
        super("PlotSquared");
        PS.get().worldedit = null;
    }

    @Override
    public FaweMask getMask(FawePlayer fp) {
        final PlotPlayer pp = PlotPlayer.wrap(fp.parent);
        Plot plot = pp.getCurrentPlot();
        Location loc = pp.getLocation();
        final String world = loc.getWorld();
        if (plot == null) {
            int min = Integer.MAX_VALUE;
            for (final Plot p : pp.getPlots()) {
                if (p.getArea().worldname.equals(world)) {
                    Location bot = p.getBottomAbs();
                    Location top = p.getTopAbs();
                    Location center = new Location(bot.getWorld(), (bot.getX() + top.getX())/2, 0, (bot.getZ() + top.getZ()) / 2);
                    final double d = center.getEuclideanDistanceSquared(loc);
                    if (d < min) {
                        min = (int) d;
                        plot = p;
                    }
                }
            }
        }
        if (plot != null) {
            final PlotId id = plot.getId();
            if (plot.owner != null) {
                if (plot.isOwner(pp.getUUID()) || plot.getTrusted().contains(pp.getUUID()) || (plot.getMembers().contains(pp.getUUID()) && pp.hasPermission("fawe.plotsquared.member"))) {
                    RegionWrapper region = plot.getLargestRegion();
                    HashSet<RegionWrapper> regions = plot.getRegions();

                    final BlockVector pos1 = new BlockVector(region.minX, 0, region.minZ);
                    final BlockVector pos2 = new BlockVector(region.maxX, 256, region.maxZ);

                    final HashSet<com.boydti.fawe.object.RegionWrapper> faweRegions = new HashSet<com.boydti.fawe.object.RegionWrapper>();
                    for (final com.intellectualcrafters.plot.object.RegionWrapper current : regions) {
                        faweRegions.add(new com.boydti.fawe.object.RegionWrapper(current.minX, current.maxX, current.minZ, current.maxZ));
                    }
                    return new FaweMask(pos1, pos2) {
                        @Override
                        public String getName() {
                            return "PLOT^2:" + id;
                        }

                        @Override
                        public HashSet<com.boydti.fawe.object.RegionWrapper> getRegions() {
                            return faweRegions;
                        }
                    };
                }
            }
        }
        return null;
    }
}
