package com.snowgears.machines.conveyer;

import com.snowgears.machines.Machine;
import com.snowgears.machines.MachineType;
import com.snowgears.machines.Machines;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Lever;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.UUID;

public class Conveyer extends Machine {

    private HashMap<Entity, Boolean> entitiesOnBelt = new HashMap<>();
    private Vector directionVector; //this will be the direction and velocity in which to push entities
    private long timeOfLastFuel;

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

        inventory = Machines.getPlugin().getTurretConfig().createInventory(this.getOwner().getPlayer());
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

        //TODO set directionVector
//
//        //start the scanning task
//        scanTaskID = Machines.getPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(Machines.getPlugin(), new Runnable() {
//            public void run() {
//                int fuelCheck = fuelCheck(true);
//                if(fuelCheck > 0) {
//                    long secondsSinceLastFuel = 1000*(System.currentTimeMillis() - timeOfLastFuel);
//                    //the turret has hit its active time limit and another fuel needs to be consumed
//                    if(secondsSinceLastFuel > fuelCheck){
//                        //TODO if nothing left to shoot in inventory (inventory is empty), don't go into next fuel check and stop the machine
//                        fuelCheck = fuelCheck(false);
//                        if(fuelCheck <= 0) {
//                            deactivate();
//                            return;
//                        }
//                    }
//                }
//                else {
//                    deactivate();
//                    return;
//                }
//
//                scanForTarget();
//            }
//        }, 0L, 20L); //scan every 20 ticks (1 second)
//
//        //start the shooting task
//        fireTaskID = Machines.getPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(Machines.getPlugin(), new Runnable() {
//            public void run() {
//                fireProjectile();
//            }
//        }, 0L, Machines.getPlugin().getTurretConfig().getSpeed());

        isActive = true;
        return false;
    }

    @Override
    public boolean deactivate() {
     //   Bukkit.getScheduler().cancelTask(scanTaskID);
     //   Bukkit.getScheduler().cancelTask(fireTaskID);
        this.setLever(false);

        entitiesOnBelt.clear();
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
        int beltTask = Machines.getPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(Machines.getPlugin(), new Runnable() {
            public void run() {
                for(Entity e : entitiesOnBelt.keySet()){
                    //TODO check that each entity is on the belt, if not remove it
                    Vector v = new Vector(0, 0, 0.1);
                    e.setVelocity(v);
                }
            }
        }, 0L, 30L); //1.5 seconds
    }

    private Location calculateFrontMarkerLocation(){
        int scanDistance = Machines.getPlugin().getTurretConfig().getScanDistance();
        switch (facing){
            case NORTH:
                return topLocation.clone().add(0,0,-scanDistance);
            case EAST:
                return topLocation.clone().add(scanDistance,0,0);
            case SOUTH:
                return topLocation.clone().add(0,0,scanDistance);
            case WEST:
                return topLocation.clone().add(-scanDistance,0,0);
            default:
                break;
        }
        return null;
    }

    @Override
    protected boolean setFacing(BlockFace direction){
        //this is necessary because stairs have different data values for directions than other blocks
        switch (direction) {
            case NORTH:
                topLocation.getBlock().setData((byte) 3);
                break;
            case SOUTH:
                topLocation.getBlock().setData((byte) 2);
                break;
            case WEST:
                topLocation.getBlock().setData((byte) 1);
                break;
            case EAST:
                topLocation.getBlock().setData((byte) 0);
                break;
            default:
                return false;
        }
        facing = direction;
        return true;
    }
}