package com.snowgears.machines.paver;

import com.snowgears.machines.Machine;
import com.snowgears.machines.Machines;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;
import java.util.UUID;

public class Paver extends Machine {

    private Block taskBlock;
    private int taskID;

    public Paver(UUID owner, Location baseLocation) {
        this.owner = owner;
        this.topLocation = baseLocation;
        this.baseLocation = baseLocation.clone().add(0, 1, 0);
        this.facing = BlockFace.DOWN;
        this.fuelPower = 0;

        calculateLeverLocation(this.baseLocation);
        inventory = Machines.getPlugin().getPaverConfig().createInventory(this.getOwner().getPlayer());
    }


    @Override
    public boolean activate() {
        if (!Machines.getPlugin().getPaverConfig().isEnabled())
            return false;
        Block dispenser = this.getTopLocation().getBlock();
        //set the starting location to be in front of dispenser
        taskBlock = dispenser.getRelative(this.getFacing());

        int power = fuelCheck(true);
        if (power == 0) {
            deactivate();
            if (this.getOwner().getPlayer() != null)
                this.getOwner().getPlayer().sendMessage(ChatColor.GRAY + "The machine needs fuel in order to start.");
            return false;
        }

        //start the paving task
        taskID = Machines.getPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(Machines.getPlugin(), new Runnable() {
            public void run() {
                int fuelCheck = fuelCheck(false);
                if (fuelCheck > 0) {
                    paveMaterial();
                    toggleLever();
                    taskBlock.getWorld().playEffect(topLocation, Effect.SMOKE, 4);
                    taskBlock = taskBlock.getRelative(getFacing());
                } else
                    deactivate();
            }
        }, 0L, Machines.getPlugin().getDrillConfig().getSpeed());

        isActive = true;
        return true;
    }

    private void paveMaterial() {

        //make sure pavers do not pave over other machines
        Machine m = Machines.getPlugin().getMachineHandler().getMachineByBase(taskBlock.getLocation());
        if (m != null) {
            this.deactivate();
            return;
        }

        //TODO not sure if this iterator is going to make actual changes to the inventory
        //may be better just to write an inventory utility for the machines that adds and removes items better? But maybe not.
        ItemStack is = null;
        //take one item from first available slot and check if it is a placable block
        Iterator<ItemStack> iterator = this.getInventory().iterator();
        while(iterator.hasNext()){
            is = iterator.next();
            if(is.getType().isBlock()){
                is.setAmount(is.getAmount()-1);
                if(is.getAmount() == 0)
                    is.setType(Material.AIR);
            }
        }

        //make sure that player has permission to break the current task block
        Player player = this.getOwner().getPlayer();
        if (player != null) {
            if (Machines.getPlugin().getPaverConfig().canPave(taskBlock.getType())) {
                BlockBreakEvent event = new BlockBreakEvent(taskBlock, this.getOwner().getPlayer());
                Bukkit.getServer().getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    this.deactivate();
                    return;
                }
                else{
                    //TODO check to see if player can place a block there
                    BlockState oldState = taskBlock.getState();
                    Material oldMaterial = taskBlock.getType();
                    byte oldData = taskBlock.getData();
                    //TODO replace this material with the next itemstack removed from inventory
                    taskBlock.setType(Material.OBSIDIAN);
                    //TODO if there is a problem in the future, go back and implement this so that other plugins can go back and cancel the block place event
                    //Block placedAgainst = taskBlock.getRelative(this.getFacing().getOppositeFace());
                    //BlockPlaceEvent e = new BlockPlaceEvent(taskBlock, oldState, placedAgainst, new ItemStack(Material.OBSIDIAN), player, true);
                    //Bukkit.getServer().getPluginManager().callEvent(e);
                    //if(e.isCancelled()){
                    //    //set block back to old data
                    //    taskBlock.setTypeIdAndData(oldMaterial.getId(), oldData, true);
                    //}
                }
            } else {
                this.deactivate();
                return;
            }
        } else {
            this.deactivate();
            return;
        }

        //TODO remove drops from inventory, or if empty, deactivate
//        for (ItemStack is : taskBlock.getDrops()) {
//            HashMap<Integer, ItemStack> overflow = this.inventory.addItem(is);
//            if (!overflow.isEmpty()) {
//                for (ItemStack drop : overflow.values()) {
//                    taskBlock.getWorld().dropItemNaturally(taskBlock.getLocation(), drop);
//                }
//            }
//        }

        //simulate an added block with particle effect (after adding)
        taskBlock.getWorld().playEffect(taskBlock.getLocation(), Effect.STEP_SOUND, taskBlock.getType());
    }


    @Override
    public boolean deactivate() {
        Bukkit.getScheduler().cancelTask(taskID);
        this.setLever(false);

        isActive = false;
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean create() {
        if (leverLocation == null)
            return false;
        Material previousMaterial = this.topLocation.getBlock().getType();
        this.topLocation.getBlock().setType(Material.DISPENSER);
        this.topLocation.getBlock().setData((byte)0); //facing down

        //before building top block, check that the location is clear
        if (Machines.getPlugin().getMachineData().isIgnoredMaterial(baseLocation.getBlock().getType())) {
            this.baseLocation.getBlock().setType(Material.OBSIDIAN);
        } else {
            this.getTopLocation().getBlock().setType(previousMaterial);
            return false;
        }

        Block leverBlock = leverLocation.getBlock();
        leverBlock.setType(Material.LEVER);
        setDirectionOfLever(leverBlock, baseLocation.getBlock().getFace(leverBlock));
        return true;
    }
}
