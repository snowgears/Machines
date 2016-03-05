package com.snowgears.machines;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;
import java.util.UUID;

public abstract class Machine {

    protected UUID owner;
    protected Location baseLocation;
    protected Location topLocation;
    protected Location leverLocation;
    protected Inventory inventory;

    public abstract boolean activate();

    public abstract boolean deactivate();

    public abstract boolean isActive();

    public abstract boolean create();

    public boolean remove(boolean dropInventory){
        leverLocation.getBlock().setType(Material.AIR);
        topLocation.getBlock().setType(Material.AIR);
        baseLocation.getBlock().setType(Material.AIR);
        if(dropInventory) {
            this.dropInventory();
        }
        Machines.getPlugin().getMachineHandler().removeMachine(this);
        return true;
    }

    protected void dropInventory(){
        Iterator<ItemStack> it = inventory.iterator();
        while(it.hasNext()){
            ItemStack is = it.next();
            if(is != null && is.getType() != Material.PORTAL){
                baseLocation.getWorld().dropItemNaturally(baseLocation, is);
            }
        }
    }

    //TODO write default public method to rotate machine
    //TODO also remove machine from handler and put back with baselocation switched
    //TODO will also have to rewrite getMachine() method in handler to reflect this change
    public void rotate(){

    }


    public OfflinePlayer getOwner(){
        return Bukkit.getOfflinePlayer(owner);
    }

    public Inventory getInventory(){
        return inventory;
    }

    public Location getBaseLocation(){
        return baseLocation;
    }

    public Location getTopLocation(){
        return topLocation;
    }

    public Location getLeverLocation(){
        return leverLocation;
    }

    protected void calculateLeverLocation(Location baseLocation){
        leverLocation = null;
        Block base = baseLocation.getBlock();
        BlockFace[] faces = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
        for(BlockFace face : faces){
            if(Machines.getPlugin().getMachineData().isIgnoredMaterial(base.getRelative(face).getType())){
                leverLocation = base.getRelative(face).getLocation();
                break;
            }
        }
    }

    @SuppressWarnings("deprecation")
    protected void setDirectionOfLever(Block lever, BlockFace bf){
        if(bf == BlockFace.NORTH)
            lever.setData((byte)4);
        else if(bf == BlockFace.EAST)
            lever.setData((byte)1);
        else if(bf == BlockFace.SOUTH)
            lever.setData((byte)3);
        else if(bf == BlockFace.WEST)
            lever.setData((byte)2);
    }

}
