package com.snowgears.machines;

import com.snowgears.machines.drill.DrillConfig;
import com.snowgears.machines.listeners.PlayerListener;
import com.snowgears.machines.paver.PaverConfig;
import com.snowgears.machines.turret.TurretConfig;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

public class Machines extends JavaPlugin {

    private static final Logger log = Logger.getLogger("Minecraft");
    private static Machines plugin;
    private YamlConfiguration config;

    private boolean usePerms;
    private boolean useProtection;

    private PlayerListener playerListener = new PlayerListener(this);
    private MachineData machineData;
    private MachineHandler machineHandler;
    private DrillConfig drillConfig;
    private PaverConfig paverConfig;
    private TurretConfig turretConfig;

    public static Machines getPlugin() {
        return plugin;
    }

    @Override
    public void onEnable() {
        plugin = this;

        machineHandler = new MachineHandler(this);
        getServer().getPluginManager().registerEvents(playerListener, this);

        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            copy(getResource("config.yml"), configFile);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        //generate drill config file
        File drillConfigFile = new File(getDataFolder(), "drillConfig.yml");
        if (!drillConfigFile.exists()) {
            drillConfigFile.getParentFile().mkdirs();
            copy(getResource("com/snowgears/machines/drill/drillConfig.yml"), drillConfigFile);
        }
        drillConfig = new DrillConfig();

        //generate paver config file
        File paverConfigFile = new File(getDataFolder(), "paverConfig.yml");
        if (!paverConfigFile.exists()) {
            paverConfigFile.getParentFile().mkdirs();
            copy(getResource("com/snowgears/machines/paver/paverConfig.yml"), paverConfigFile);
        }
        paverConfig = new PaverConfig();

        //generate turret config file
        File turretConfigFile = new File(getDataFolder(), "turretConfig.yml");
        if (!turretConfigFile.exists()) {
            turretConfigFile.getParentFile().mkdirs();
            copy(getResource("com/snowgears/machines/turret/turretConfig.yml"), turretConfigFile);
        }
        turretConfig = new TurretConfig();

        machineData = new MachineData(this);

        //TODO generate other machine config files

        usePerms = config.getBoolean("usePermissions");
        useProtection = config.getBoolean("protection");
    }

    @Override
    public void onDisable() {

    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("machines")) {
            if (args.length == 0) {
                sender.sendMessage("[Machines] Available Commands:");
                sender.sendMessage("   /machines list");
                sender.sendMessage("   /machines give");
            } else if (args.length == 1) {
                if(args[0].equalsIgnoreCase("give")) {
                    Player player = (Player)sender;
                    if(drillConfig.isEnabled())
                        player.getInventory().addItem(machineData.getItem(MachineType.DRILL));
                    if(paverConfig.isEnabled())
                        player.getInventory().addItem(machineData.getItem(MachineType.PAVER));
                    if(turretConfig.isEnabled())
                        player.getInventory().addItem(machineData.getItem(MachineType.TURRET));
                    //player.getInventory().addItem(machineData.getItem(MachineType.ANTIGRAV));
                    //player.getInventory().addItem(machineData.getItem(MachineType.PUMP));
                }
                else if(args[0].equalsIgnoreCase("list")) {
                    Player player = (Player)sender;
                    player.sendMessage("You own "+machineHandler.getNumberOfMachines(player)+" of the "+machineHandler.getNumberOfMachines()+" machines registered on this server.");
                }
            }
        }

        return true;
    }

    public boolean usePerms() {
        return usePerms;
    }
    public boolean useProtection() {
        return useProtection;
    }
    public MachineData getMachineData(){
        return machineData;
    }
    public MachineHandler getMachineHandler(){
        return machineHandler;
    }
    public DrillConfig getDrillConfig(){
        return drillConfig;
    }
    public PaverConfig getPaverConfig(){
        return paverConfig;
    }
    public TurretConfig getTurretConfig(){
        return turretConfig;
    }

    private void copy(InputStream in, File file) {
        try {
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.close();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
