package com.snowgears.machines.drill;

import com.snowgears.machines.MachineConfig;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.List;

public class DrillConfig extends MachineConfig{

    private HashMap<Material, Boolean> materialBlacklist;
    private boolean fillBuckets;

    public DrillConfig(File configFile){
        super(configFile);

        materialBlacklist = new HashMap<>();
        loadConfig(configFile);
    }

    public boolean canDrill(Material type){
        if(materialBlacklist.containsKey(type))
            return false;
        return true;
    }

    public boolean fillBuckets(){
        return fillBuckets;
    }

    private void loadConfig(File configFile){
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        if(enabled) {
            fillBuckets = config.getBoolean("machine.fillBuckets");

            //populate material blacklist
            List<String> blacklist = config.getStringList("machine.blockBlacklist");
            for (String s : blacklist) {
                Material m = Material.valueOf(s);
                materialBlacklist.put(m, true);
            }
        }
    }
}
