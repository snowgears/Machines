package com.snowgears.machines.drill;


import com.snowgears.machines.Machine;
import com.snowgears.machines.Machines;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class Drill extends Machine {


    public Drill(UUID owner, Location baseLocation){
        this.owner = owner;
        this.baseLocation = baseLocation;
        this.topLocation = baseLocation.clone().add(0,1,0);

        calculateLeverLocation(baseLocation);
        inventory = Bukkit.createInventory(Bukkit.getPlayer(owner), 9, "Drill");
    }


    @Override
    public boolean activate() {
        return false;
    }

    @Override
    public boolean deactivate() {
        return false;
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public boolean create() {
        if(leverLocation == null)
            return false;
        this.baseLocation.getBlock().setType(Material.OBSIDIAN);

        //before building top block, check that the location is clear
        if(Machines.getPlugin().getMachineData().isIgnoredMaterial(topLocation.getBlock().getType())) {
            this.topLocation.getBlock().setType(Material.PISTON_BASE);
            this.topLocation.getBlock().setData((byte)1); //piston:BlockFace.UP
        }
        else
            return false;

        Block leverBlock = leverLocation.getBlock();
        leverBlock.setType(Material.LEVER);
        setDirectionOfLever(leverBlock, baseLocation.getBlock().getFace(leverBlock));

        return true;
    }
}
