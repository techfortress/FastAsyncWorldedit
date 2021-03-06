package com.boydti.fawe.bukkit.v1_9;

import com.boydti.fawe.bukkit.BukkitMain;
import com.boydti.fawe.bukkit.v0.BukkitQueue_0;
import com.boydti.fawe.object.EditSessionWrapper;
import com.sk89q.worldedit.EditSession;

public class BukkitMain_1_9 extends BukkitMain {
    @Override
    public BukkitQueue_0 getQueue(String world) {
        return new BukkitQueue_1_9_R1(world);
    }

    @Override
    public EditSessionWrapper getEditSessionWrapper(EditSession session) {
        return new EditSessionWrapper(session);
    }
}
