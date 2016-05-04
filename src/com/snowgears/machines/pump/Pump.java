package com.snowgears.machines.pump;

import com.snowgears.machines.Machine;
import com.snowgears.machines.Machines;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.UUID;

public class Pump extends Machine {

    public Pump(UUID owner, Location baseLocation){
        this.owner = owner;
        this.baseLocation = baseLocation;
        this.topLocation = baseLocation.clone().add(0,1,0);
        this.facing = BlockFace.UP;

        calculateLeverLocation(baseLocation);
        inventory = Bukkit.createInventory(Bukkit.getPlayer(owner), 9, "Pump");
    }


    @Override
    public boolean activate() {
        this.setLever(true);

        isActive = true;
        return false;
    }

    @Override
    public boolean deactivate() {
        this.setLever(false);

        isActive = false;
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean create() {
        if(leverLocation == null)
            return false;
        this.baseLocation.getBlock().setType(Material.SPONGE);
        this.baseLocation.getBlock().setData((byte)1); //sponge:WET

        //before building top block, check that the location is clear
        if(Machines.getPlugin().getMachineData().isIgnoredMaterial(topLocation.getBlock().getType())) {
            this.topLocation.getBlock().setType(Material.SEA_LANTERN);
        }
        else
            return false;

        Block leverBlock = leverLocation.getBlock();
        leverBlock.setType(Material.LEVER);
        setDirectionOfLever(leverBlock, baseLocation.getBlock().getFace(leverBlock));

        return true;
    }
}
