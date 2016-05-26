package com.snowgears.machines.pump;

import com.snowgears.machines.Machine;
import com.snowgears.machines.MachineType;
import com.snowgears.machines.Machines;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class Pump extends Machine {

    public Pump(UUID owner, Location baseLocation, BlockFace leverFace){
        this.type = MachineType.PUMP;
        this.owner = owner;
        this.baseLocation = baseLocation;
        this.topLocation = baseLocation.clone().add(0,1,0);
        this.facing = BlockFace.UP;

        calculateLeverLocation(baseLocation, leverFace);
        inventory = Bukkit.createInventory(Bukkit.getPlayer(owner), 9, "Pump");
    }

    public Pump(UUID owner, Location base, Location top, Location lever, BlockFace facing, ItemStack[] inventoryContents){
        this.type = MachineType.PUMP;
        this.owner = owner;
        this.baseLocation = base;
        this.topLocation = top;
        this.leverLocation = lever;
        this.facing = facing;
        this.fuelPower = 0;

        //inventory = Machines.getPlugin().getPumpConfig().createInventory(this.getOwner().getPlayer());
        //inventory.setContents(inventoryContents);
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
