package com.snowgears.machines.drill;

import com.snowgears.machines.Machines;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Set;

public class DrillConfig {

    private HashMap<Material, Boolean> materialBlacklist;
    private HashMap<Material, Integer> fuelMap;
    private int speed;
    private boolean fillBuckets;

    public DrillConfig(){
        materialBlacklist = new HashMap<>();
        fuelMap = new HashMap<>();
        loadConfig();
    }

    public boolean canDrill(Material type){
        if(materialBlacklist.containsKey(type))
            return false;
        return true;
    }

    public int getFuelPower(Material type){
        if(fuelMap.containsKey(type)){
            return fuelMap.get(type);
        }
        return 0;
    }

    public int getSpeed(){
        return speed;
    }

    public boolean fillBuckets(){
        return fillBuckets;
    }

    private void loadConfig(){
        File configFile = new File(Machines.getPlugin().getDataFolder(), "drillConfig.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        //populate material blacklist
        Set<String> blacklist = config.getConfigurationSection("drill.blockBlacklist").getKeys(true);
        for(String s : blacklist){
            Material m = Material.valueOf(s);
            boolean isTrue = config.getBoolean("drill.blockBlacklist."+s);
            if(isTrue)
                materialBlacklist.put(m, true);
        }

        //populate fuel types
        Set<String> fuelTypeStrings = config.getConfigurationSection("drill.fuelTypes").getKeys(true);
        for(String s : fuelTypeStrings){
            Material m = Material.valueOf(s);
            int power = config.getInt("drill.fuelTypes."+s);
            fuelMap.put(m, power);
        }

        speed = config.getInt("drill.speedInTicks");
        fillBuckets = config.getBoolean("drill.fillBuckets");
    }
}
