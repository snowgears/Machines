package com.snowgears.machines.turret;

import com.snowgears.machines.Machine;
import com.snowgears.machines.MachineType;
import com.snowgears.machines.Machines;
import org.bukkit.Bukkit;
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
    private long timeOfLastFuel;

    public Turret(UUID owner, Location baseLocation){
        this.type = MachineType.TURRET;
        this.owner = owner;
        this.baseLocation = baseLocation;
        this.topLocation = baseLocation.clone().add(0,1,0);
        this.facing = BlockFace.NORTH;

        calculateLeverLocation(baseLocation);
        inventory = Machines.getPlugin().getTurretConfig().createInventory(this.getOwner().getPlayer());
    }

    public Turret(UUID owner, Location base, Location top, Location lever, BlockFace facing, ItemStack[] inventoryContents){
        this.type = MachineType.TURRET;
        this.owner = owner;
        this.baseLocation = base;
        this.topLocation = top;
        this.leverLocation = lever;
        this.facing = facing;
        this.fuelPower = 0;

        inventory = Machines.getPlugin().getTurretConfig().createInventory(this.getOwner().getPlayer());
        inventory.setContents(inventoryContents);
    }


    @Override
    public boolean activate() {
        if(!Machines.getPlugin().getTurretConfig().isEnabled())
            return false;

        int power = fuelCheck(true);
        if(power == 0){
            deactivate();
            if(this.getOwner().getPlayer() != null)
                this.getOwner().getPlayer().sendMessage(Machines.getPlugin().getTurretConfig().getFuelMessage());
            return false;
        }

        timeOfLastFuel = System.currentTimeMillis();

        this.setLever(true);
        spawnArmorStand();
        ((Furnace) topLocation.getBlock().getState()).setBurnTime(Short.MAX_VALUE);
        topLocation.getBlock().setType(Material.BURNING_FURNACE);
        setFacing(facing);

        //start the scanning task
        scanTaskID = Machines.getPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(Machines.getPlugin(), new Runnable() {
            public void run() {
                int fuelCheck = fuelCheck(true);
                if(fuelCheck > 0) {
                    long secondsSinceLastFuel = 1000*(System.currentTimeMillis() - timeOfLastFuel);
                    //the turret has hit its active time limit and another fuel needs to be consumed
                    if(secondsSinceLastFuel > fuelCheck){
                        //TODO if nothing left to shoot in inventory (inventory is empty), don't go into next fuel check and stop the machine
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

        //before building top block, check that the location is clear
        if(Machines.getPlugin().getMachineData().isIgnoredMaterial(topLocation.getBlock().getType())) {
            this.topLocation.getBlock().setType(Material.FURNACE);
        }
        else
            return false;

        this.baseLocation.getBlock().setType(Material.OBSIDIAN);

        Block leverBlock = leverLocation.getBlock();
        leverBlock.setType(Material.LEVER);
        setDirectionOfLever(leverBlock, baseLocation.getBlock().getFace(leverBlock));

        return true;
    }

    private void fireProjectile(){
        if(target == null || target.isDead())
            return;
        if(armorStand == null || armorStand.isDead())
            spawnArmorStand();

        //fire that item
        if(target != null && armorStand != null){
            Vector targetVector = target.getLocation().clone().add(0,0.8,0).toVector().subtract(armorStand.getLocation().toVector());
            //this way works very well for closer targets but not for long targets
            //need a way to hit long targets without shooting arrow so hard it kills them instantly
            targetVector.normalize();
            targetVector.multiply(3);

            for(int i=0; i<inventory.getSize(); i++){
                ItemStack is = inventory.getItem(i);
                if(Machines.getPlugin().getTurretConfig().isProjectile(is)){
                    Projectile projectile = spawnProjectileFromItemstack(is, armorStand.getLocation());
                    if(projectile != null) {
                        projectile.setVelocity(targetVector);
                        if (this.getOwner().getPlayer() != null)
                            projectile.setShooter(this.getOwner().getPlayer());
                    }
                    is.setAmount(is.getAmount()-1);
                    if(is.getAmount() == 0)
                        is = new ItemStack(Material.AIR);
                    inventory.setItem(i, is);

                    return;
                }
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
        if(!Machines.getPlugin().getTurretConfig().canTarget(entity))
            return false;
        int scanDistance = Machines.getPlugin().getTurretConfig().getScanDistance();

        if(entity instanceof LivingEntity){

            if(entity instanceof Player){
                if(entity.getUniqueId().equals(this.owner))
                    return false;
            }

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
