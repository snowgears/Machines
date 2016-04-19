package com.snowgears.machines.paver;

import com.snowgears.machines.Machine;
import com.snowgears.machines.Machines;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.UUID;

//TODO this machine will work as the drill does, only it will take blocks out of its inventory (in order) and place them. Machine will stop once hitting a material that cannot be overwritten
public class Paver extends Machine {

    private Block taskBlock;
    private boolean leverOn = false;
    private int taskID;

    public Paver(UUID owner, Location baseLocation) {
        this.owner = owner;
        this.topLocation = baseLocation;
        this.baseLocation = baseLocation.clone().add(0, 1, 0);
        this.facing = BlockFace.DOWN;

        calculateLeverLocation(this.baseLocation);
        inventory = Bukkit.createInventory(Bukkit.getPlayer(owner), 9, "Paver");
    }


    @Override
    public boolean activate() {
        Block piston = this.getTopLocation().getBlock();
        //set the starting location to be in front of piston
        taskBlock = piston.getRelative(this.getFacing());
        //gather the material of the taskBlock before starting machine task (to avoid piston head)
        gatherMaterial();
        taskBlock = piston.getRelative(this.getFacing());
        //start the piston task
        taskID = Machines.getPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(Machines.getPlugin(), new Runnable() {
            public void run() {
                //TODO check for fuel and consume it
                toggleLever();
                gatherMaterial();
                taskBlock.getWorld().playEffect(topLocation, Effect.SMOKE, 4);
                taskBlock = taskBlock.getRelative(getFacing());
            }
        }, 0L, 10L);

        isActive = true;
        return false;
    }

    private void toggleLever() {
        if (leverOn) {
            setLever(false);
            leverOn = false;
        } else {
            setLever(true);
            leverOn = true;
        }
    }

    private void gatherMaterial() {
        //TODO also switch order of the way these happen. Make sure if cancelled it is not added to inventory
        //TODO add filling of water/lava in buckets
        //TODO also make sure it halts when hitting another machine
        for (ItemStack is : taskBlock.getDrops()) {
            HashMap<Integer, ItemStack> overflow = this.inventory.addItem(is);
            if (!overflow.isEmpty()) {
                for (ItemStack drop : overflow.values()) {
                    taskBlock.getWorld().dropItem(taskBlock.getLocation(), drop);
                }
            }
        }

        Player player = this.getOwner().getPlayer();
        if (player != null) {
            if (Machines.getPlugin().getDrillConfig().canDrill(taskBlock.getType())) {
                BlockBreakEvent event = new BlockBreakEvent(taskBlock, this.getOwner().getPlayer());
                Bukkit.getServer().getPluginManager().callEvent(event);
                if (!event.isCancelled()) {
                    taskBlock.getWorld().playEffect(taskBlock.getLocation(), Effect.STEP_SOUND, taskBlock.getType());
                    taskBlock.setType(Material.AIR);
                } else
                    this.deactivate();
            } else
                deactivate();
        } else {
            this.deactivate();
        }
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
        if (leverLocation == null)
            return false;
        this.baseLocation.getBlock().setType(Material.OBSIDIAN);

        //before building top block, check that the location is clear
        if (Machines.getPlugin().getMachineData().isIgnoredMaterial(topLocation.getBlock().getType())) {
            this.topLocation.getBlock().setType(Material.PISTON_BASE);
            this.topLocation.getBlock().setData((byte) 0); //piston:BlockFace.DOWN
        } else
            return false;

        Block leverBlock = leverLocation.getBlock();
        leverBlock.setType(Material.LEVER);
        setDirectionOfLever(leverBlock, baseLocation.getBlock().getFace(leverBlock));

        return true;
    }
}
