package com.snowgears.machines;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.material.MaterialData;

import java.util.UUID;

public class Machine {

    protected UUID owner;
    protected MachineType type;
    protected Location baseLocation;
    protected Location topLocation;
    protected Location leverLocation;
    protected Inventory inventory;

    public Machine(){}

    public Machine(MachineType type, UUID owner, Location baseLocation){
        this.type = type;
        this.owner = owner;
        this.baseLocation = baseLocation;
        this.topLocation = baseLocation.clone().add(0,1,0);
        calculateLeverLocation(baseLocation);

        inventory = Bukkit.createInventory(Bukkit.getPlayer(owner), 9, type.name());
    }

    public boolean activate(){
        return false;
    }

    public boolean deactivate(){
        return false;
    }

    public boolean isActive(){
        return false;
    }

    public Player getOwner(){
        return Bukkit.getPlayer(owner);
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

    @SuppressWarnings("deprecation")
    public boolean create(){
        if(leverLocation == null)
            return false;
        MaterialData baseData = Machines.getPlugin().getMachineData().getInitialBaseMaterial(type);
        this.baseLocation.getBlock().setTypeIdAndData(baseData.getItemTypeId(), baseData.getData(), true);

        if(Machines.getPlugin().getMachineData().isIgnoredMaterial(topLocation.getBlock().getType())) {
            MaterialData topData = Machines.getPlugin().getMachineData().getInitialTopMaterial(type);
            this.topLocation.getBlock().setTypeIdAndData(topData.getItemTypeId(), topData.getData(), true);
        }
        else
            return false;

        Block leverBlock = leverLocation.getBlock();
        leverBlock.setType(Material.LEVER);
        setDirectionOfLever(leverBlock, baseLocation.getBlock().getFace(leverBlock));

        return true;
    }

    public boolean remove(){
        leverLocation.getBlock().setType(Material.AIR);
        topLocation.getBlock().setType(Material.AIR);
        baseLocation.getBlock().setType(Material.AIR);
        return true;
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
