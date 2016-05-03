package com.snowgears.machines.turret;

import com.snowgears.machines.MachineConfig;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class TurretConfig extends MachineConfig {

    private int scanDistance;

    public TurretConfig(File configFile){
        super(configFile);

        loadConfig(configFile);
    }

    public int getScanDistance(){
        return scanDistance;
    }

    private void loadConfig(File configFile){
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        if(enabled) {
            scanDistance = config.getInt("machine.scanDistance");
        }
    }
}
