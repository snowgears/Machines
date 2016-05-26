package com.snowgears.machines;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Lever;

import java.util.Iterator;
import java.util.UUID;

public abstract class Machine {

    protected MachineType type;
    protected UUID owner;
    protected Location baseLocation;
    protected Location topLocation;
    protected Location leverLocation;
    protected Inventory inventory;
    protected BlockFace facing;
    protected BlockFace[] rotationCycle;
    protected boolean isActive;
    protected boolean leverOn;
    protected boolean onCooldown;
    protected int fuelPower;

    public abstract boolean activate();

    public abstract boolean deactivate();

    public abstract boolean create();

    public boolean remove(boolean dropItem){
        deactivate();
        leverLocation.getBlock().setType(Material.AIR);
        topLocation.getBlock().setType(Material.AIR);
        baseLocation.getBlock().setType(Material.AIR);
        this.dropInventory();
        if(dropItem) {
            ItemStack machineItem = Machines.getPlugin().getMachineData().getItem(type);
            if(machineItem != null)
                baseLocation.getWorld().dropItemNaturally(baseLocation, machineItem);
        }
        Machines.getPlugin().getMachineHandler().removeMachine(this);
        return true;
    }

    protected void dropInventory(){
        Iterator<ItemStack> it = inventory.iterator();
        while(it.hasNext()){
            ItemStack is = it.next();
            if(is != null){
                if(!(is.getType() == Material.BARRIER && (" ").equals(is.getItemMeta().getDisplayName())))
                    baseLocation.getWorld().dropItemNaturally(baseLocation, is);
            }
        }
    }

    protected int fuelCheck(boolean startCheck){
        if(!startCheck){
            if(fuelPower != 0)
                fuelPower--;
        }
        if(fuelPower == 0) {
            int lastSlot = this.getInventory().getSize() - 1;
            ItemStack fuel = this.getInventory().getItem(lastSlot);
            int power = 0;
            if(fuel != null) {
                    power = Machines.getPlugin().getMachineConfig(this).getFuelPower(fuel.getType());
            }
            if(!startCheck) {
                if (power == 0) {
                    fuelPower = 0;
                    deactivate();
                    return 0;
                }
                else
                    consumeFuel();
                fuelPower = power;
            }
            return power;
        }
        return fuelPower;
    }

    protected boolean consumeFuel() {
        int lastSlot = this.getInventory().getSize() - 1;
        ItemStack fuel = this.getInventory().getItem(lastSlot);
        if (fuel != null) {
            if (fuel.getType() == Material.LAVA_BUCKET || fuel.getType() == Material.WATER_BUCKET)
                fuel.setType(Material.BUCKET);
            else
                fuel.setAmount(fuel.getAmount() - 1);

            if (fuel.getAmount() == 0)
                this.getInventory().setItem(lastSlot, new ItemStack(Material.AIR));
            else
                this.getInventory().setItem(lastSlot, fuel);
            return true;
        }
        return false;
    }

    public void rotate(){
        BlockFace nextDirection = null;
        if(this.facing == rotationCycle[rotationCycle.length-1])
            nextDirection = rotationCycle[0];
        else{
            for(int i=0; i<rotationCycle.length; i++){
                if(this.facing == rotationCycle[i]){
                    nextDirection = rotationCycle[i+1];
                    break;
                }
            }
        }

        //need make sure the machine has time to deactivate before rotating
        if(isActive) {
            this.onCooldown = true;
            this.deactivate();
            final BlockFace nextDirectionFinal = nextDirection;
            Machines.getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(Machines.getPlugin(), new Runnable() {
                public void run() {
                    setFacing(nextDirectionFinal);
                    onCooldown = false;
                }
            }, 10L);
        }
        else
            setFacing(nextDirection);
    }

    public MachineType getType(){
        return type;
    }

    public BlockFace getFacing(){
        return facing;
    }

    @SuppressWarnings("deprecation")
    protected boolean setFacing(BlockFace direction){
        switch (direction) {
            case DOWN:
                switchTopAndBottom((byte) 0); //switch top and bottom with top facing down ((byte)0 = Facing DOWN)
                break;
            case UP:
                if (facing == BlockFace.DOWN)
                    switchTopAndBottom((byte) 1); //switch top and bottom with top facing up ((byte)1 = Facing UP)
                else
                    topLocation.getBlock().setData((byte) 1);
                break;
            case NORTH:
                topLocation.getBlock().setData((byte) 2);
                break;
            case SOUTH:
                topLocation.getBlock().setData((byte) 3);
                break;
            case WEST:
                topLocation.getBlock().setData((byte) 4);
                break;
            case EAST:
                topLocation.getBlock().setData((byte) 5);
                break;
            default:
                return false;

        }
        facing = direction;
        return true;
    }

    @SuppressWarnings("deprecation")
    protected boolean switchTopAndBottom(byte data){
        //make sure machine has room for new lever location first
        Location originalLever = leverLocation.clone();
        BlockFace leverFace = ((Lever)leverLocation.getBlock().getState().getData()).getAttachedFace();
        boolean hasRoom = this.calculateLeverLocation(topLocation, leverFace);
        if(!hasRoom){
            leverLocation = originalLever;
            return false;
        }

        //switch top and bottom blocks of machine
        originalLever.getBlock().setType(Material.AIR);
        Material topMat = topLocation.getBlock().getType();
        topLocation.getBlock().setTypeIdAndData(baseLocation.getBlock().getTypeId(), baseLocation.getBlock().getData(), true);
        baseLocation.getBlock().setTypeIdAndData(topMat.getId(), data, true); //(byte)0 = Facing DOWN, (byte)1 = Facing UP
        leverLocation.getBlock().setType(Material.LEVER);

        //remove machine, switch stored top and bottom locations, put machine back
        Machines.getPlugin().getMachineHandler().removeMachine(this);
        Location tempTopLocation = topLocation.clone();
        topLocation = baseLocation;
        baseLocation = tempTopLocation;
        setDirectionOfLever(leverLocation.getBlock(), baseLocation.getBlock().getFace(leverLocation.getBlock()));

        Machines.getPlugin().getMachineHandler().addMachine(this);
        return true;
    }


    public OfflinePlayer getOwner(){
        return Bukkit.getOfflinePlayer(owner);
    }

    public Inventory getInventory(){
        return inventory;
    }

    public Location getBaseLocation(){
        return baseLocation;
    }

    public Location getTopLocation(){
        return topLocation;
    }

    public Location getLeverLocation(){
        return leverLocation;
    }

    public boolean isActive(){
        return isActive;
    }

    public boolean onCooldown(){
        return onCooldown;
    }

    protected boolean calculateLeverLocation(Location baseLocation, BlockFace leverFace){
        leverLocation = null;
        Block base = baseLocation.getBlock();
        BlockFace[] faces = {leverFace, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
        for(BlockFace face : faces){
            if(Machines.getPlugin().getMachineData().isIgnoredMaterial(base.getRelative(face).getType())){
                leverLocation = base.getRelative(face).getLocation();
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    protected void setDirectionOfLever(Block lever, BlockFace bf){
        if(bf == BlockFace.NORTH)
            lever.setData((byte)4);
        else if(bf == BlockFace.EAST)
            lever.setData((byte)1);
        else if(bf == BlockFace.SOUTH)
            lever.setData((byte)3);
        else if(bf == BlockFace.WEST)
            lever.setData((byte)2);
    }

    @SuppressWarnings("deprecation")
    protected void setLever(boolean on) {
        Block leverBlock = leverLocation.getBlock();
        Lever lever = null;
        if(leverBlock.getType() == Material.LEVER)
            lever = (Lever) leverBlock.getState().getData();
        else
            return;

        //this flips the lever
        lever.setPowered(on);
        leverBlock.setData(lever.getData(), true);
        leverOn = on;

        //hacky fix for state not updating bug
        Block supportBlock = leverBlock.getRelative(lever.getAttachedFace());
        BlockState initialSupportState = supportBlock.getState();
        BlockState supportState = supportBlock.getState();
        supportState.setType(Material.AIR);
        supportState.update(true, false);
        initialSupportState.update(true);
    }

    protected void toggleLever() {
        setLever(!leverOn);
    }

}
