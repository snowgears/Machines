package com.snowgears.machines.turret;

import com.snowgears.machines.MachineConfig;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.HashMap;
import java.util.List;

public class TurretConfig extends MachineConfig {

    private int scanDistance;
    private HashMap<EntityType, Boolean> entityBlacklist;
    private HashMap<Material, Boolean> projectileMaterials;

    public TurretConfig(File configFile){
        super(configFile);

        loadConfig(configFile);
    }

    public int getScanDistance(){
        return scanDistance;
    }

    public boolean canTarget(Entity entity){
        if(entity == null || entity.isDead())
            return false;
        if(entityBlacklist.containsKey(entity.getType()))
            return false;
        return true;
    }

    public boolean isProjectile(ItemStack itemStack){
        if(itemStack == null)
            return false;
        return projectileMaterials.containsKey(itemStack.getType());
    }

    private void loadConfig(File configFile){
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        if(enabled) {
            scanDistance = config.getInt("machine.scanDistance");

            //populate entity blacklist
            entityBlacklist = new HashMap<>();
            List<String> blacklist = config.getStringList("machine.entityBlacklist");
            for (String s : blacklist) {
                entityBlacklist.put(EntityType.valueOf(s), true);
            }

            //TODO in the future, maybe allow turrets to shoot items and get all of these from config
            projectileMaterials = new HashMap<>();
            projectileMaterials.put(Material.ARROW, true);
            projectileMaterials.put(Material.DRAGONS_BREATH, true);
            projectileMaterials.put(Material.EGG, true);
            projectileMaterials.put(Material.ENDER_PEARL, true);
            projectileMaterials.put(Material.FIREBALL, true);
            projectileMaterials.put(Material.LINGERING_POTION, true);
            projectileMaterials.put(Material.SNOW_BALL, true);
            projectileMaterials.put(Material.SPECTRAL_ARROW, true);
            projectileMaterials.put(Material.SPLASH_POTION, true);
            projectileMaterials.put(Material.EXP_BOTTLE, true);
            projectileMaterials.put(Material.TIPPED_ARROW, true);
        }
    }
}
