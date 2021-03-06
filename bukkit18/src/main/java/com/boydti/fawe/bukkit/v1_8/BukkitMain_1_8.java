package com.boydti.fawe.bukkit.v1_8;

import com.boydti.fawe.bukkit.BukkitMain;
import com.boydti.fawe.bukkit.v0.BukkitQueue_0;
import com.boydti.fawe.object.EditSessionWrapper;
import com.sk89q.worldedit.EditSession;

public class BukkitMain_1_8 extends BukkitMain {
    @Override
    public BukkitQueue_0 getQueue(String world) {
        return new BukkitQueue18R3(world);
    }

    @Override
    public EditSessionWrapper getEditSessionWrapper(EditSession session) {
        return new EditSessionWrapper(session);
    }
}
