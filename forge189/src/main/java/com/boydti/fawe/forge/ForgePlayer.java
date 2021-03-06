package com.boydti.fawe.forge;

import com.boydti.fawe.object.FaweLocation;
import com.boydti.fawe.object.FawePlayer;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.forge.ForgeWorldEdit;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

public class ForgePlayer extends FawePlayer<EntityPlayerMP> {
    public ForgePlayer(EntityPlayerMP parent) {
        super(parent);
    }

    @Override
    public String getName() {
        return parent.getName();
    }

    @Override
    public UUID getUUID() {
        return parent.getUniqueID();
    }

    @Override
    public boolean hasPermission(String perm) {
        Object meta = getMeta(perm);
        return meta instanceof Boolean ? (boolean) meta : ForgeWorldEdit.inst.getPermissionsProvider().hasPermission(parent, perm);
    }

    @Override
    public void setPermission(String perm, boolean flag) {
        setMeta(perm, flag);
    }

    @Override
    public void sendMessage(String msg) {
        for (String part : msg.split("\n")) {
            part = EnumChatFormatting.getTextWithoutFormattingCodes(msg);
            ChatComponentText component = new ChatComponentText(part);
            component.getChatStyle().setColor(EnumChatFormatting.LIGHT_PURPLE);
            this.parent.addChatMessage(component);
        }
    }

    @Override
    public void executeCommand(String substring) {
        throw new UnsupportedOperationException("NOT IMPLEMENTED");
    }

    @Override
    public FaweLocation getLocation() {
        World world = parent.worldObj;
        BlockPos pos = parent.getPosition();
        return new FaweLocation(world.provider.getDimensionName(), pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public Player getPlayer() {
        return ForgeWorldEdit.inst.wrap(this.parent);
    }
}
