package com.snowgears.machines.turret;

import com.snowgears.machines.Machine;
import com.snowgears.machines.Machines;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Furnace;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.util.Vector;

import java.util.UUID;

public class Turret extends Machine {

    private ArmorStand armorStand;
    private Vector centerVector;
    private Entity target;
    private int scanTaskID;
    private int fireTaskID;

    public Turret(UUID owner, Location baseLocation){
        this.owner = owner;
        this.baseLocation = baseLocation;
        this.topLocation = baseLocation.clone().add(0,1,0);
        this.facing = BlockFace.NORTH;

        calculateLeverLocation(baseLocation);
        inventory = Machines.getPlugin().getTurretConfig().createInventory(this.getOwner().getPlayer());
    }


    @Override
    public boolean activate() {
        if(!Machines.getPlugin().getTurretConfig().isEnabled())
            return false;

        int power = fuelCheck(true);
        if(power == 0){
            deactivate();
            if(this.getOwner().getPlayer() != null)
                this.getOwner().getPlayer().sendMessage(ChatColor.GRAY+"The machine needs fuel in order to start.");
            return false;
        }

        this.setLever(true);
        spawnArmorStand();
        ((Furnace) topLocation.getBlock().getState()).setBurnTime(Short.MAX_VALUE);
        topLocation.getBlock().setType(Material.BURNING_FURNACE);
        setFacing(facing);

        //start the scanning task
        scanTaskID = Machines.getPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(Machines.getPlugin(), new Runnable() {
            public void run() {
                //TODO check for fuel and consume
                scanForTarget();
            }
        }, 0L, 20L); //scan every 20 ticks (1 second)

        //start the shooting task
        fireTaskID = Machines.getPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(Machines.getPlugin(), new Runnable() {
            public void run() {
                fireProjectile();
            }
        }, 0L, Machines.getPlugin().getTurretConfig().getSpeed());

        isActive = true;
        return false;
    }

    @Override
    public boolean deactivate() {
        Bukkit.getScheduler().cancelTask(scanTaskID);
        Bukkit.getScheduler().cancelTask(fireTaskID);
        this.setLever(false);
        if(armorStand != null && !armorStand.isDead())
            armorStand.remove();
        topLocation.getBlock().setType(Material.FURNACE);
        setFacing(facing);

        target = null;
        isActive = false;
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean create() {
        if(leverLocation == null)
            return false;
        this.baseLocation.getBlock().setType(Material.RED_SANDSTONE);
        this.baseLocation.getBlock().setData((byte)1); //chiseled red sandstone

        //before building top block, check that the location is clear
        if(Machines.getPlugin().getMachineData().isIgnoredMaterial(topLocation.getBlock().getType())) {
            this.topLocation.getBlock().setType(Material.FURNACE);
        }
        else
            return false;

        Block leverBlock = leverLocation.getBlock();
        leverBlock.setType(Material.LEVER);
        setDirectionOfLever(leverBlock, baseLocation.getBlock().getFace(leverBlock));

        return true;
    }

    @Override
    public void rotate(){
        BlockFace[] faceCycle = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
        BlockFace nextDirection = null;
        if(this.facing == BlockFace.WEST)
            nextDirection = BlockFace.NORTH;
        else{
            for(int i=0; i<faceCycle.length; i++){
                if(this.facing == faceCycle[i]){
                    nextDirection = faceCycle[i+1];
                    break;
                }
            }
        }

        boolean rotated = setFacing(nextDirection);
        if(rotated)
            deactivate();
    }

    private void fireProjectile(){
        if(target == null || target.isDead())
            return;

        //TODO remove an item from the inventory of the machine and check that it is a projectile (or just shoot anything?)

        //TODO deactivate machine if inventory is empty



        ItemStack tempItem = inventory.getItem(0);
        if(tempItem == null || tempItem.getType() == Material.AIR)
            return;

        //fire that item
        if(target != null && armorStand != null){
            Vector targetVector = target.getLocation().clone().add(0,0.8,0).toVector().subtract(armorStand.getLocation().toVector());
            //this way works very well for closer targets but not for long targets
            //need a way to hit long targets without shooting arrow so hard it kills them instantly
            targetVector.normalize();
            targetVector.multiply(3);
            Projectile projectile = spawnProjectileFromItemstack(tempItem, armorStand.getLocation());
            if(projectile != null) {
                projectile.setVelocity(targetVector);
                if (this.getOwner().getPlayer() != null)
                    projectile.setShooter(this.getOwner().getPlayer());
            }
        }
    }

    private void scanForTarget(){
        int scanDistance = Machines.getPlugin().getTurretConfig().getScanDistance();
        if(armorStand == null)
            spawnArmorStand();

        //keep furnace lit when machine is active
        ((Furnace) topLocation.getBlock().getState()).setBurnTime(Short.MAX_VALUE);

        if(isViableTarget(target))
            return;
        else
            target = null;

        for (Entity entity : armorStand.getNearbyEntities(scanDistance, scanDistance, scanDistance)) {
            if (isViableTarget(entity)) {
                target = entity;
                return;
            }
        }
    }

    private boolean isViableTarget(Entity entity){
        if(entity == null || entity.isDead())
            return false;
        int scanDistance = Machines.getPlugin().getTurretConfig().getScanDistance();

        if(entity instanceof LivingEntity){

            if(entity.getType() == EntityType.ARMOR_STAND)
                return false;

            if(entity instanceof Player) //TODO remove this later and just change so it doesnt shoot owner of machine
                return false;

            //TODO may want to check for other things like hostility of mob, etc, (maybe make option for turrets to not shoot certain mobs)

            //if the entity is within the distance the machine is able to scan
            if (topLocation.distanceSquared(entity.getLocation()) < (scanDistance * scanDistance)) {
             //   System.out.println("Distance to " + entity.getType().toString() + ": " + topLocation.distanceSquared(entity.getLocation()));
             //   System.out.println("Distance max: " + (scanDistance * scanDistance));
                //if the armorstand (i.e. machine) can see the target
                if (armorStand.hasLineOfSight(entity)) {
                    Vector entityVector = entity.getLocation().toVector().subtract(topLocation.toVector());
                    //if the angle between entityVector and centerVector is less than 45 degrees (0.7855 radians), it is within the field of vision
                    if (entityVector.angle(centerVector) < 0.7855) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    //this spawns a new armor stand in front of the machine and calculates a new center vector
    private void spawnArmorStand(){
        if(armorStand != null)
            armorStand.remove();
        Location standLocation = topLocation.getBlock().getRelative(facing).getLocation().clone().add(0.5, 0, 0.5);
        armorStand = (ArmorStand)topLocation.getWorld().spawnEntity(standLocation, EntityType.ARMOR_STAND);
        armorStand.setMarker(true);
        armorStand.setSmall(true);
        armorStand.setGravity(false);
        armorStand.setVisible(false);

        //this creates a vector spanning from the top location of the machine to the location <scanDistance> blocks in the direction it is facing
        Location frontMarkerLocation = calculateFrontMarkerLocation();
        centerVector = frontMarkerLocation.toVector().subtract(topLocation.toVector()).clone();
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

    private Projectile spawnProjectileFromItemstack(ItemStack itemStack, Location location){
        Projectile projectile = null;
        switch (itemStack.getType()){
            case ARROW:
                projectile = (Projectile)location.getWorld().spawnEntity(location, EntityType.ARROW);
                break;
            case DRAGONS_BREATH:
                projectile = (Projectile)location.getWorld().spawnEntity(location, EntityType.DRAGON_FIREBALL);
                break;
            case EGG:
                projectile = (Projectile)location.getWorld().spawnEntity(location, EntityType.EGG);
                break;
            case ENDER_PEARL:
                projectile = (Projectile)location.getWorld().spawnEntity(location, EntityType.ENDER_PEARL); //TODO should be fixed on 1.9.3
                break;
            case FIREBALL:
                projectile = (Projectile)location.getWorld().spawnEntity(location, EntityType.FIREBALL);
                break;
            case LINGERING_POTION:
                projectile = (Projectile)location.getWorld().spawnEntity(location, EntityType.LINGERING_POTION);
                ((LingeringPotion)projectile).setItem(itemStack);
                break;
            case SNOW_BALL:
                projectile = (Projectile)location.getWorld().spawnEntity(location, EntityType.SNOWBALL);
                break;
            case SPECTRAL_ARROW:
                projectile = (Projectile)location.getWorld().spawnEntity(location, EntityType.SPECTRAL_ARROW);
                break;
            case SPLASH_POTION:
                projectile = (Projectile)location.getWorld().spawnEntity(location, EntityType.SPLASH_POTION);
                ((SplashPotion)projectile).setItem(itemStack);
                break;
            case EXP_BOTTLE:
                projectile = (Projectile)location.getWorld().spawnEntity(location, EntityType.THROWN_EXP_BOTTLE);
                break;
            case TIPPED_ARROW:
                projectile = (Projectile)location.getWorld().spawnEntity(location, EntityType.TIPPED_ARROW);
                ((TippedArrow)projectile).setBasePotionData(((PotionMeta)itemStack.getItemMeta()).getBasePotionData());
                break;
        }
        return projectile;
    }
}
