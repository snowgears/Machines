package com.snowgears.machines.drill;


import com.snowgears.machines.Machine;
import com.snowgears.machines.MachineType;
import com.snowgears.machines.Machines;
import com.snowgears.machines.util.InventoryUtils;
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

public class Drill extends Machine {

    private Block taskBlock;
    private int taskID;

    public Drill(UUID owner, Location baseLocation, BlockFace leverFace){
        this.type = MachineType.DRILL;
        this.owner = owner;
        this.topLocation = baseLocation;
        this.baseLocation = baseLocation.clone().add(0,1,0);
        this.facing = BlockFace.DOWN;
        this.fuelPower = 0;

        calculateLeverLocation(this.baseLocation, leverFace);
        inventory = Machines.getPlugin().getDrillConfig().createInventory(this.getOwner().getPlayer());
        rotationCycle = new BlockFace[] {BlockFace.UP, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.DOWN};
    }

    public Drill(UUID owner, Location base, Location top, Location lever, BlockFace facing, ItemStack[] inventoryContents){
        this.type = MachineType.DRILL;
        this.owner = owner;
        this.baseLocation = base;
        this.topLocation = top;
        this.leverLocation = lever;
        this.facing = facing;
        this.fuelPower = 0;

        inventory = Machines.getPlugin().getDrillConfig().createInventory(this.getOwner().getPlayer());
        inventory.setContents(inventoryContents);
        rotationCycle = new BlockFace[] {BlockFace.UP, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.DOWN};
    }


    @Override
    public boolean activate() {
        if(!Machines.getPlugin().getDrillConfig().isEnabled())
            return false;
        Block piston = this.getTopLocation().getBlock();
        //set the starting location to be in front of piston
        taskBlock = piston.getRelative(this.getFacing());

        int power = fuelCheck(true);
        if(power == 0){
            deactivate();
            if(this.getOwner().getPlayer() != null)
                this.getOwner().getPlayer().sendMessage(Machines.getPlugin().getDrillConfig().getFuelMessage());
            return false;
        }

        //start the piston task
        taskID = Machines.getPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(Machines.getPlugin(), new Runnable() {
            public void run() {
                int fuelCheck = fuelCheck(false);
                if(fuelCheck > 0) {
                    gatherMaterial();
                    toggleLever();
                    taskBlock.getWorld().playEffect(topLocation, Effect.SMOKE, 4);
                    taskBlock = taskBlock.getRelative(getFacing());
                }
                else
                    deactivate();
            }
        }, 0L, Machines.getPlugin().getDrillConfig().getSpeed());

        isActive = true;
        return true;
    }

    private void gatherMaterial(){

        if(taskBlock.getLocation().getBlockY() >= taskBlock.getWorld().getMaxHeight()-1) {
            this.deactivate();
            return;
        }
        //make sure drills do not drill through other machines
        Machine m = Machines.getPlugin().getMachineHandler().getMachineByBase(taskBlock.getLocation());
        if(m != null){
            this.deactivate();
            return;
        }

        //make sure that player has permission to break the current task block
        Player player = this.getOwner().getPlayer();
        if(player != null) {
            if(Machines.getPlugin().getDrillConfig().canDrill(taskBlock.getType())) {
                BlockBreakEvent event = new BlockBreakEvent(taskBlock, this.getOwner().getPlayer());
                Bukkit.getServer().getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    this.deactivate();
                    return;
                }
            }
            else {
                this.deactivate();
                return;
            }
        }
        else{
            this.deactivate();
            return;
        }

        //add drops to inventory, or drop the items on ground if inventory is full
        for(ItemStack is : taskBlock.getDrops()) {
            HashMap<Integer, ItemStack> overflow = this.inventory.addItem(is);
            if(!overflow.isEmpty()){
                for(ItemStack drop : overflow.values()){
                    taskBlock.getWorld().dropItemNaturally(taskBlock.getLocation(), drop);
                }
            }
        }

        //fill empty buckets with water/lava if turned on in settings
        if(Machines.getPlugin().getDrillConfig().fillBuckets()){
            if(taskBlock.getType() == Material.WATER || taskBlock.getType() == Material.STATIONARY_WATER) {
                int underflow = InventoryUtils.removeItem(this.getInventory(), new ItemStack(Material.BUCKET));
                //a bucket was able to be removed
                if (underflow == 0) {
                    int overflow = InventoryUtils.addItem(this.getInventory(), new ItemStack(Material.WATER_BUCKET));
                    if(overflow > 0){
                        //not enough room for water bucket; replace original bucket
                        InventoryUtils.addItem(this.getInventory(), new ItemStack(Material.BUCKET));
                    }
                }
            }
            else if(taskBlock.getType() == Material.LAVA || taskBlock.getType() == Material.STATIONARY_LAVA) {
                int underflow = InventoryUtils.removeItem(this.getInventory(), new ItemStack(Material.BUCKET));
                //a bucket was able to be removed
                if (underflow == 0) {
                    int overflow = InventoryUtils.addItem(this.getInventory(), new ItemStack(Material.LAVA_BUCKET));
                    if(overflow > 0){
                        //not enough room for water bucket; replace original bucket
                        InventoryUtils.addItem(this.getInventory(), new ItemStack(Material.BUCKET));
                    }
                }
            }
        }

        //simulate a broken block with particle effect and removal
        taskBlock.getWorld().playEffect(taskBlock.getLocation(), Effect.STEP_SOUND, taskBlock.getType());
        taskBlock.setType(Material.AIR);
    }


    @Override
    public boolean deactivate() {
        Bukkit.getScheduler().cancelTask(taskID);
        this.setLever(false);

        //cancel all tasks
        isActive = false;
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean create() {
        if(leverLocation == null)
            return false;
        Material previousMaterial = this.topLocation.getBlock().getType();
        this.topLocation.getBlock().setType(Material.PISTON_BASE);
        this.topLocation.getBlock().setData((byte)0); //piston:BlockFace.DOWN

        //before building top block, check that the location is clear
        if(Machines.getPlugin().getMachineData().isIgnoredMaterial(baseLocation.getBlock().getType())) {
            this.baseLocation.getBlock().setType(Material.OBSIDIAN);
        }
        else {
            this.getTopLocation().getBlock().setType(previousMaterial);
            return false;
        }

        Block leverBlock = leverLocation.getBlock();
        leverBlock.setType(Material.LEVER);
        setDirectionOfLever(leverBlock, baseLocation.getBlock().getFace(leverBlock));
        return true;
    }
}
