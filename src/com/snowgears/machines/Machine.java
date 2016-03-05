package com.snowgears.machines;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public abstract class Machine {

    protected UUID owner;
    protected Location baseLocation;
    protected Location topLocation;
    protected Location leverLocation;
    protected Inventory inventory;

    //    public Machine(){}

    //    public Machine(UUID owner, Location baseLocation);
    //        this.owner = owner;
    //        this.baseLocation = baseLocation;
    //        this.topLocation = baseLocation.clone().add(0,1,0);
    //        calculateLeverLocation(baseLocation);
    //
    //        inventory = Bukkit.createInventory(Bukkit.getPlayer(owner), 9, type.name());
    //    }

    public abstract boolean activate();

    public abstract boolean deactivate();

    public abstract boolean isActive();

    public abstract OfflinePlayer getOwner();

    public abstract Inventory getInventory();

    public abstract Location getBaseLocation();

    public abstract Location getTopLocation();

    public abstract Location getLeverLocation();

    public abstract boolean create();

    public abstract boolean remove(boolean dropInventory);

    //    public boolean activate(){
    //        if(this instanceof AntiGrav){
    //            ((AntiGrav)this).activate();
    //        }
    //        //TODO etc...
    //
    //        return false;
    //    }

    //    public boolean deactivate(){
    //        return false;
    //    }

    //    public boolean isActive(){
    //        return false;
    //    }

    //    public Player getOwner(){
    //        return Bukkit.getPlayer(owner);
    //    }
    //
    //    public Inventory getInventory(){
    //        return inventory;
    //    }
    //
    //    public Location getBaseLocation(){
    //        return baseLocation;
    //    }
    //
    //    public Location getTopLocation(){
    //        return topLocation;
    //    }
    //
    //    public Location getLeverLocation(){
    //        return leverLocation;
    //    }

    //    @SuppressWarnings("deprecation")
    //    public boolean create(){
    //        if(leverLocation == null)
    //            return false;
    //        MaterialData baseData = Machines.getPlugin().getMachineData().getInitialBaseMaterial(type);
    //        this.baseLocation.getBlock().setTypeIdAndData(baseData.getItemTypeId(), baseData.getData(), true);
    //
    //        if(Machines.getPlugin().getMachineData().isIgnoredMaterial(topLocation.getBlock().getType())) {
    //            MaterialData topData = Machines.getPlugin().getMachineData().getInitialTopMaterial(type);
    //            this.topLocation.getBlock().setTypeIdAndData(topData.getItemTypeId(), topData.getData(), true);
    //        }
    //        else
    //            return false;
    //
    //        Block leverBlock = leverLocation.getBlock();
    //        leverBlock.setType(Material.LEVER);
    //        setDirectionOfLever(leverBlock, baseLocation.getBlock().getFace(leverBlock));
    //
    //        return true;
    //    }
    //
    //    public boolean remove(boolean dropInventory){
    //        if(dropInventory) {
    //            //TODO you will want a method in machine to get relavent inventory items to drop on the ground
    //        }
    //        leverLocation.getBlock().setType(Material.AIR);
    //        topLocation.getBlock().setType(Material.AIR);
    //        baseLocation.getBlock().setType(Material.AIR);
    //        Machines.getPlugin().getMachineHandler().removeMachine(this);
    //        return true;
    //    }

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
