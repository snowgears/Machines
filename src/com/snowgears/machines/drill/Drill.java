package com.snowgears.machines.drill;


import com.snowgears.machines.Machine;
import com.snowgears.machines.Machines;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.material.PistonBaseMaterial;
import org.bukkit.material.PistonExtensionMaterial;

import java.util.UUID;

public class Drill extends Machine {

    private boolean leverOn = false;
    private int taskID;

    public Drill(UUID owner, Location baseLocation){
        this.owner = owner;
        this.topLocation = baseLocation;
        this.baseLocation = baseLocation.clone().add(0,1,0);
        this.facing = BlockFace.DOWN;

        calculateLeverLocation(this.baseLocation);
        inventory = Bukkit.createInventory(Bukkit.getPlayer(owner), 9, "Drill");
    }


    @Override
    public boolean activate() {
        //start the piston task
        taskID = Machines.getPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(Machines.getPlugin(), new Runnable() {
            public void run() {
                //TODO move to own methods
                //toggleLever()
                //gatherMaterials()
                if(leverOn) {
                    setLever(false);
                    leverOn = false;
                }
                else{
                    setLever(true);
                    leverOn = true;
                }
            }
        }, 0L, 10L);

        isActive = true;
        return false;
    }

    @Override
    public boolean deactivate() {
        Bukkit.getScheduler().cancelTask(taskID);
        this.setLever(false);

        //cancel all tasks
        isActive = false;
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean create() {
        if(leverLocation == null)
            return false;
        this.baseLocation.getBlock().setType(Material.OBSIDIAN);

        //before building top block, check that the location is clear
        if(Machines.getPlugin().getMachineData().isIgnoredMaterial(topLocation.getBlock().getType())) {
            this.topLocation.getBlock().setType(Material.PISTON_BASE);
            this.topLocation.getBlock().setData((byte)0); //piston:BlockFace.DOWN
        }
        else
            return false;

        Block leverBlock = leverLocation.getBlock();
        leverBlock.setType(Material.LEVER);
        setDirectionOfLever(leverBlock, baseLocation.getBlock().getFace(leverBlock));

        return true;
    }
}
