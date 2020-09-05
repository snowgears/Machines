package com.snowgears.machines.paver;

import com.snowgears.machines.MachineConfig;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.List;

public class PaverConfig extends MachineConfig {

    private HashMap<Material, Boolean> paverWhitelist;

    public PaverConfig(File configFile){
        super(configFile);

        paverWhitelist = new HashMap<>();
        loadConfig(configFile);
    }

    public boolean canPave(Material type){
        if(paverWhitelist.containsKey(type))
            return true;
        return false;
    }

    private void loadConfig(File configFile){
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        if(enabled) {
            //populate material whitelist
            List<String> blacklist = config.getStringList("machine.blockWhitelist");
            for (String s : blacklist) {
                Material m = Material.valueOf(s);
                paverWhitelist.put(m, true);
            }
        }
    }
}
