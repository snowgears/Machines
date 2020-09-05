package com.snowgears.machines;

import com.snowgears.machines.conveyer.Conveyer;
import com.snowgears.machines.conveyer.ConveyerConfig;
import com.snowgears.machines.drill.Drill;
import com.snowgears.machines.drill.DrillConfig;
import com.snowgears.machines.listeners.PlayerListener;
import com.snowgears.machines.paver.Paver;
import com.snowgears.machines.paver.PaverConfig;
import com.snowgears.machines.turret.Turret;
import com.snowgears.machines.turret.TurretConfig;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Logger;

public class Machines extends JavaPlugin {

    private static final Logger log = Logger.getLogger("Minecraft");
    private static Machines plugin;
    private YamlConfiguration config;

    private boolean usePerms;
    private boolean useProtection;

    private List<String> messageAvailableCommands;
    private String messageOwnedMachines;

    private PlayerListener playerListener = new PlayerListener(this);
    private MachineData machineData;
    private MachineHandler machineHandler;
    private DrillConfig drillConfig;
    private PaverConfig paverConfig;
    private TurretConfig turretConfig;
    private ConveyerConfig conveyerConfig;

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
        drillConfig = new DrillConfig(drillConfigFile);

        //generate paver config file
        File paverConfigFile = new File(getDataFolder(), "paverConfig.yml");
        if (!paverConfigFile.exists()) {
            paverConfigFile.getParentFile().mkdirs();
            copy(getResource("com/snowgears/machines/paver/paverConfig.yml"), paverConfigFile);
        }
        paverConfig = new PaverConfig(paverConfigFile);

        //generate turret config file
        File turretConfigFile = new File(getDataFolder(), "turretConfig.yml");
        if (!turretConfigFile.exists()) {
            turretConfigFile.getParentFile().mkdirs();
            copy(getResource("com/snowgears/machines/turret/turretConfig.yml"), turretConfigFile);
        }
        turretConfig = new TurretConfig(turretConfigFile);

        //generate conveyer config file
        File conveyerConfigFile = new File(getDataFolder(), "conveyerConfig.yml");
        if (!conveyerConfigFile.exists()) {
            conveyerConfigFile.getParentFile().mkdirs();
            copy(getResource("com/snowgears/machines/conveyer/conveyerConfig.yml"), conveyerConfigFile);
        }
        conveyerConfig = new ConveyerConfig(conveyerConfigFile);

        machineData = new MachineData(this);

        //TODO generate other machine config files

        usePerms = config.getBoolean("usePermissions");
        useProtection = config.getBoolean("protection");

        messageAvailableCommands = config.getStringList("messages.availableCommands");
        messageOwnedMachines = config.getString("messages.machinesOwned");
    }

    @Override
    public void onDisable() {
        machineHandler.saveMachines();
        machineHandler.deactivateMachines();
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
                    if(sender instanceof Player){
                        Player player = (Player) sender;
                        //if using permissions, check that the player is allowed
                        if(player.isOp() || (plugin.usePerms() && (player.hasPermission("machines.operator")))){
                            if(drillConfig.isEnabled())
                                player.getInventory().addItem(machineData.getItem(MachineType.DRILL));
                            if(paverConfig.isEnabled())
                                player.getInventory().addItem(machineData.getItem(MachineType.PAVER));
                            if(turretConfig.isEnabled())
                                player.getInventory().addItem(machineData.getItem(MachineType.TURRET));
                            if(conveyerConfig.isEnabled())
                                player.getInventory().addItem(machineData.getItem(MachineType.CONVEYER));
                        }
                    }
                }
                else if(args[0].equalsIgnoreCase("list")) {
                    if(sender instanceof Player) {
                        Player player = (Player) sender;

                        messageOwnedMachines = messageOwnedMachines.replace("[amount owned]", "" + machineHandler.getMachines(player).size());
                        messageOwnedMachines = messageOwnedMachines.replace("[amount total]", "" + machineHandler.getNumberOfMachines());
                        messageOwnedMachines = ChatColor.translateAlternateColorCodes('&', messageOwnedMachines);

                        player.sendMessage(messageOwnedMachines);
                    }
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
    public MachineConfig getMachineConfig(Machine machine){
        if(machine != null){
            if(machine instanceof Drill)
                return drillConfig;
            else if(machine instanceof Paver)
                return paverConfig;
            else if(machine instanceof Turret)
                return turretConfig;
            else if(machine instanceof Conveyer)
                return conveyerConfig;
        }
        return null;
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
    public ConveyerConfig getConveyerConfig(){
        return conveyerConfig;
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
