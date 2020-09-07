package com.snowgears.machines;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.*;
import org.bukkit.block.data.type.Switch;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

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
    protected boolean workSoundVariant;

    //TODO make 1 or 2 constructors which you can call super() on from other machines

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

    protected boolean setFacing(BlockFace direction){
        switch (direction) {
            case DOWN:
                switchTopAndBottom(BlockFace.DOWN); //switch top and bottom with top facing down
                break;
            case UP:
                if (facing == BlockFace.DOWN)
                    switchTopAndBottom(BlockFace.UP); //switch top and bottom with top facing up
                else
                    this.setBlockDirection(topLocation.getBlock(), direction);
                break;
            case NORTH:
            case SOUTH:
            case WEST:
            case EAST:
                this.setBlockDirection(topLocation.getBlock(), direction);
                break;
            default:
                return false;

        }
        facing = direction;
        return true;
    }

    protected void setBlockDirection(Block block, BlockFace direction){
        BlockData blockData = block.getBlockData();
        if (blockData instanceof Directional) {
            ((Directional) blockData).setFacing(direction);
            block.setBlockData(blockData);
        }
        else if (blockData instanceof Orientable) {
            ((Orientable) blockData).setAxis(convertBlockFaceToAxis(direction));
            block.setBlockData(blockData);
        }
        else if (blockData instanceof Rotatable) {
            ((Rotatable) blockData).setRotation(direction);
            block.setBlockData(blockData);
        }
    }

    private Axis convertBlockFaceToAxis(BlockFace face) {
        switch (face) {
            case NORTH:
            case SOUTH:
                return Axis.Z;
            case EAST:
            case WEST:
                return Axis.X;
            case UP:
            case DOWN:
                return Axis.Y;
            default:
                return Axis.X;
        }
    }

    protected boolean switchTopAndBottom(BlockFace facing){
        //make sure machine has room for new lever location first
        Location originalLever = leverLocation.clone();
        Block leverBlock = leverLocation.getBlock();
        if(leverBlock.getBlockData() instanceof Switch){
            BlockFace leverFace = ((Switch)leverLocation.getBlock().getBlockData()).getFacing().getOppositeFace();
            boolean hasRoom = this.calculateLeverLocation(topLocation, leverFace);
            if(!hasRoom){
                leverLocation = originalLever;
                return false;
            }
        }
        else
            return false;


        //switch top and bottom blocks of machine
        originalLever.getBlock().setType(Material.AIR);
        Material topMat = topLocation.getBlock().getType();
        topLocation.getBlock().setType(baseLocation.getBlock().getType());
        //baseLocation.getBlock().getData(), true); //TODO, used to also set the top location data. is this still necessary???
        baseLocation.getBlock().setType(topMat); //, data, true); //(byte)0 = Facing DOWN, (byte)1 = Facing UP
        this.setBlockDirection(baseLocation.getBlock(), facing);
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

    protected void setDirectionOfLever(Block lever, BlockFace face){
        if(lever.getBlockData() instanceof Switch){
            Switch leverSwitch = (Switch)lever.getBlockData();
            leverSwitch.setAttachedFace(FaceAttachable.AttachedFace.WALL);
            leverSwitch.setFacing(face);
            lever.setBlockData(leverSwitch);
        }
    }

    protected void setLever(boolean on) {
        Block leverBlock = leverLocation.getBlock();
        if(!(leverBlock.getBlockData() instanceof Switch))
            return;

        Switch leverSwitch = (Switch)leverBlock.getBlockData();
        leverSwitch.setPowered(on);
        leverOn = on;
        leverBlock.setBlockData(leverSwitch);
    }

    protected void toggleLever() {
        setLever(!leverOn);
    }

    protected void playWorkSound(){
        Sound sound;
        if(workSoundVariant)
            sound = Machines.getPlugin().getMachineConfig(this).getSoundActionWork1();
        else
            sound = Machines.getPlugin().getMachineConfig(this).getSoundActionWork2();
        topLocation.getWorld().playSound(topLocation, sound, 1.0F, 1.0F);
        workSoundVariant = !workSoundVariant;
    }

}
