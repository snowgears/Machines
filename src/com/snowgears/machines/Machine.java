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
    protected BlockFace facing;

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

    public void rotate(){
        BlockFace[] faceCycle = {BlockFace.UP, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.DOWN};
        BlockFace nextDirection = null;
        if(this.facing == BlockFace.DOWN)
            nextDirection = BlockFace.UP;
        else{
            for(int i=0; i<faceCycle.length; i++){
                if(this.facing == faceCycle[i]){
                    nextDirection = faceCycle[i+1];
                    break;
                }
            }
        }
        setFacing(nextDirection);
    }

    public BlockFace getFacing(){
        return facing;
    }

    protected boolean setFacing(BlockFace direction){

        switch (direction){
            case DOWN:
                switchTopAndBottom((byte)0); //switch top and bottom with top facing down ((byte)0 = Facing DOWN)
                break;
            case UP:
                if(facing == BlockFace.DOWN)
                    switchTopAndBottom((byte)1); //switch top and bottom with top facing up ((byte)1 = Facing UP)
                else
                    topLocation.getBlock().setData((byte)1);
                break;
            case NORTH:
                topLocation.getBlock().setData((byte)2);
                break;
            case SOUTH:
                topLocation.getBlock().setData((byte)3);
                break;
            case WEST:
                topLocation.getBlock().setData((byte)4);
                break;
            case EAST:
                topLocation.getBlock().setData((byte)5);
                break;
            default:
                return false;

        }
        facing = direction;
        return true;
    }

    protected boolean switchTopAndBottom(byte data){
        //make sure machine has room for new lever location first
        Location originalLever = leverLocation.clone();
        boolean hasRoom = this.calculateLeverLocation(topLocation);
        if(!hasRoom){
            leverLocation = originalLever;
            return false;
        }

        //switch top and bottom blocks of machine
        originalLever.getBlock().setType(Material.AIR);
        Material topMat = topLocation.getBlock().getType();
        topLocation.getBlock().setTypeIdAndData(baseLocation.getBlock().getTypeId(), baseLocation.getBlock().getData(), true);
        baseLocation.getBlock().setTypeIdAndData(topMat.getId(), data, true); //(byte)0 = Facing DOWN, (byte)1 = Facing UP
        leverLocation.getBlock().setType(Material.LEVER);
        baseLocation.getBlock();

        //remove machine, switch stored top and bottom locations, put machine back
        Machines.getPlugin().getMachineHandler().removeMachine(this);
        Location tempTopLocation = topLocation.clone();
        topLocation = baseLocation;
        baseLocation = tempTopLocation;

        Machines.getPlugin().getMachineHandler().addMachine(this);
        return true;
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

    protected boolean calculateLeverLocation(Location baseLocation){
        leverLocation = null;
        Block base = baseLocation.getBlock();
        BlockFace[] faces = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
        for(BlockFace face : faces){
            if(Machines.getPlugin().getMachineData().isIgnoredMaterial(base.getRelative(face).getType())){
                leverLocation = base.getRelative(face).getLocation();
                return true;
            }
        }
        return false;
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
