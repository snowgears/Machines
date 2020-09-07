package com.snowgears.machines.conveyer;

import com.snowgears.machines.Machine;
import com.snowgears.machines.MachineType;
import com.snowgears.machines.Machines;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Lever;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

public class Conveyer extends Machine {

    private HashMap<Entity, Boolean> entitiesOnBelt = new HashMap<>();
    private Vector directionVector; //this will be the direction and velocity in which to push entities
    private long timeOfLastFuel;
    private int scanTaskID;
    private int beltTaskID;

    public Conveyer(UUID owner, Location baseLocation, BlockFace leverFace){
        this.type = MachineType.CONVEYER;
        this.owner = owner;
        this.baseLocation = baseLocation;
        this.topLocation = baseLocation.clone().add(0,1,0);
        this.facing = BlockFace.NORTH;

        calculateLeverLocation(baseLocation, leverFace);

        inventory = Machines.getPlugin().getConveyerConfig().createInventory(this.getOwner().getPlayer());
        entitiesOnBelt = new HashMap<>();
        rotationCycle = new BlockFace[] {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
    }

    public Conveyer(UUID owner, Location base, Location top, Location lever, BlockFace facing, ItemStack[] inventoryContents){
        this.type = MachineType.CONVEYER;
        this.owner = owner;
        this.baseLocation = base;
        this.topLocation = top;
        this.leverLocation = lever;
        this.facing = facing;
        this.fuelPower = 0;

        inventory = Machines.getPlugin().getConveyerConfig().createInventory(this.getOwner().getPlayer());
        inventory.setContents(inventoryContents);
        entitiesOnBelt = new HashMap<>();
        rotationCycle = new BlockFace[] {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
    }


    @Override
    public boolean activate() {
        if(!Machines.getPlugin().getConveyerConfig().isEnabled())
            return false;

        int power = fuelCheck(true);
        if(power == 0){
            deactivate();
            if(this.getOwner().getPlayer() != null)
                this.getOwner().getPlayer().sendMessage(Machines.getPlugin().getConveyerConfig().getFuelMessage());
            return false;
        }

        timeOfLastFuel = System.currentTimeMillis();

        this.setLever(true);

        createDirectionVector();

        //TODO populate beltLocations HashMap by recursively scanning in direction of machine for belts
        //break when one is not found or when maxDistance is hit (beltLocations should never be more than maxDistance)

        //start the scanning task
        scanTaskID = Machines.getPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(Machines.getPlugin(), new Runnable() {
            public void run() {
                int fuelCheck = fuelCheck(true);
                if(fuelCheck > 0) {
                    long secondsSinceLastFuel = 1000*(System.currentTimeMillis() - timeOfLastFuel);
                    //the turret has hit its active time limit and another fuel needs to be consumed
                    if(secondsSinceLastFuel > fuelCheck){
                        fuelCheck = fuelCheck(false);
                        if(fuelCheck <= 0) {
                            deactivate();
                            return;
                        }
                    }
                }
                else {
                    deactivate();
                    return;
                }

                //play sound to let player know the conveyer is working
                playWorkSound();

                scanForEntities();
            }
        }, 0L, 40L); //scan every 2 seconds
//
        //start the belt pushing task
        beltTaskID = Machines.getPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(Machines.getPlugin(), new Runnable() {
            public void run() {
                pushBelt();
            }
        }, 0L, 30L);

        baseLocation.getWorld().playSound(baseLocation, Machines.getPlugin().getConveyerConfig().getSoundActionOn(), 1.0F, 1.0F);
        isActive = true;
        return false;
    }

    @Override
    public boolean deactivate() {
        Bukkit.getScheduler().cancelTask(scanTaskID);
        Bukkit.getScheduler().cancelTask(beltTaskID);
        this.setLever(false);

        entitiesOnBelt.clear();

        workSoundVariant = false;
        baseLocation.getWorld().playSound(baseLocation, Machines.getPlugin().getConveyerConfig().getSoundActionOff(), 0.5F, 1.0F);
        isActive = false;
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean create() {
        if(leverLocation == null)
            return false;

        //before building top block, check that the location is clear
        if(Machines.getPlugin().getMachineData().isIgnoredMaterial(topLocation.getBlock().getType())) {
            this.topLocation.getBlock().setType(Material.QUARTZ_STAIRS);
        }
        else
            return false;

        this.baseLocation.getBlock().setType(Material.OBSIDIAN);

        Block leverBlock = leverLocation.getBlock();
        leverBlock.setType(Material.LEVER);
        setDirectionOfLever(leverBlock, baseLocation.getBlock().getFace(leverBlock));
        setFacing(((Lever)leverBlock.getState().getData()).getAttachedFace());

        return true;
    }

    private void pushBelt(){
        for(Entity e : entitiesOnBelt.keySet()) {
            //TODO check that each entity is on the belt, if not remove it
            //TODO check that each entity is not more than maxDistance away from machine
            if (e.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == Material.PACKED_ICE) {
                if(e instanceof Item)
                    e.setVelocity(directionVector);
                else
                    e.setVelocity(directionVector.clone().multiply(3));
            }
        }
    }

    private void scanForEntities(){
        int maxDistance = Machines.getPlugin().getConveyerConfig().getMaxDistance();
        Collection<Entity> entities = new ArrayList<>();
        switch(facing){
            case NORTH:
                entities = topLocation.getWorld().getNearbyEntities(topLocation, 2, 2, -maxDistance);
                break;
            case EAST:
                entities = topLocation.getWorld().getNearbyEntities(topLocation, maxDistance, 2, 2);
                break;
            case SOUTH:
                entities = topLocation.getWorld().getNearbyEntities(topLocation, 2, 2, maxDistance);
                break;
            case WEST:
                entities = topLocation.getWorld().getNearbyEntities(topLocation, -maxDistance, 2, 2);
                break;
            default:
                return;
        }
        entitiesOnBelt.clear();
        for(Entity e : entities){
            entitiesOnBelt.put(e, true);
        }
    }

    private void createDirectionVector(){
        switch(facing){
            case NORTH:
                directionVector = new Vector(0, 0, -0.1);
                break;
            case EAST:
                directionVector = new Vector(0.1, 0, 0);
                break;
            case SOUTH:
                directionVector = new Vector(0, 0, 0.1);
                break;
            case WEST:
                directionVector = new Vector(-0.1, 0, 0);
                break;
            default:
                return;
        }
    }

    //TODO may need to have setDirection method be overridden because of stairs

    @Override
    public void rotate(){
        super.rotate();
        baseLocation.getWorld().playSound(baseLocation, Machines.getPlugin().getConveyerConfig().getSoundActionRotate(), 1.0F, 1.0F);
    }
}