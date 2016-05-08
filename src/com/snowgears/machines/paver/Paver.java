package com.snowgears.machines.paver;

import com.snowgears.machines.Machine;
import com.snowgears.machines.MachineType;
import com.snowgears.machines.Machines;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class Paver extends Machine {

    private Block taskBlock;
    private int taskID;
    private int currentSlot;

    public Paver(UUID owner, Location baseLocation) {
        this.type = MachineType.PAVER;
        this.owner = owner;
        this.topLocation = baseLocation;
        this.baseLocation = baseLocation.clone().add(0, 1, 0);
        this.facing = BlockFace.DOWN;
        this.fuelPower = 0;
        this.currentSlot = 0;

        calculateLeverLocation(this.baseLocation);
        inventory = Machines.getPlugin().getPaverConfig().createInventory(this.getOwner().getPlayer());
    }

    public Paver(UUID owner, Location base, Location top, Location lever, BlockFace facing, ItemStack[] inventoryContents){
        this.type = MachineType.PAVER;
        this.owner = owner;
        this.baseLocation = base;
        this.topLocation = top;
        this.leverLocation = lever;
        this.facing = facing;
        this.fuelPower = 0;

        inventory = Machines.getPlugin().getPaverConfig().createInventory(this.getOwner().getPlayer());
        inventory.setContents(inventoryContents);
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
                    boolean paved = paveMaterial();
                    if(!paved){
                        deactivate();
                        return;
                    }
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

    private boolean paveMaterial() {

        if(taskBlock.getLocation().getBlockY() >= taskBlock.getWorld().getMaxHeight()-1)
            return false;

        //make sure pavers do not pave over other machines
        Machine m = Machines.getPlugin().getMachineHandler().getMachineByBase(taskBlock.getLocation());
        if (m != null) {
            return false;
        }

        ItemStack paveItem = null;
        for(int i = 0; i < inventory.getSize(); i++){
            if(currentSlot > inventory.getSize()-2) //current slot can't be last slot (reserved for fuel)
                currentSlot = 0;
            ItemStack itemStack = inventory.getItem(currentSlot);
            currentSlot++;

            if(itemStack != null && itemStack.getType() != Material.AIR && itemStack.getType().isBlock()){
                if(!(itemStack.getType() == Material.BARRIER && (" ").equals(itemStack.getItemMeta().getDisplayName()))) {
                    paveItem = itemStack.clone();
                    paveItem.setAmount(paveItem.getAmount()-1);
                    if(paveItem.getAmount() == 0)
                        paveItem.setType(Material.AIR);
                    break;
                }
            }
        }
        if(paveItem == null) {
            return false;
        }

        //make sure that player has permission to break the current task block
        Player player = this.getOwner().getPlayer();
        if (player != null) {
            if (Machines.getPlugin().getPaverConfig().canPave(taskBlock.getType())) {
                BlockBreakEvent event = new BlockBreakEvent(taskBlock, this.getOwner().getPlayer());
                Bukkit.getServer().getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    return false;
                } else {
                    //TODO check to see if player can place a block there
//                    BlockState oldState = taskBlock.getState();
//                    Material oldMaterial = taskBlock.getType();
//                    byte oldData = taskBlock.getData();
                    //TODO replace this material with the next itemstack removed from inventory
//                    taskBlock.setType(Material.OBSIDIAN);
                    //TODO if there is a problem in the future, go back and implement this so that other plugins can go back and cancel the block place event
                    //Block placedAgainst = taskBlock.getRelative(this.getFacing().getOppositeFace());
                    //BlockPlaceEvent e = new BlockPlaceEvent(taskBlock, oldState, placedAgainst, new ItemStack(Material.OBSIDIAN), player, true);
                    //Bukkit.getServer().getPluginManager().callEvent(e);
                    //if(e.isCancelled()){
                    //    //set block back to old data
                    //    taskBlock.setTypeIdAndData(oldMaterial.getId(), oldData, true);
                    //}

                    int takeSlot = currentSlot - 1;
                    if(takeSlot < 0)
                        takeSlot = inventory.getSize()-3;
                    inventory.setItem(takeSlot, paveItem);
                    taskBlock.setTypeIdAndData(paveItem.getType().getId(), paveItem.getData().getData(), true);
                    //simulate an added block with particle effect (after adding)
                    taskBlock.getWorld().playEffect(taskBlock.getLocation(), Effect.STEP_SOUND, taskBlock.getType());
                    return true;
                }
            } else {
                return false;
            }
        }
        return false;
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
