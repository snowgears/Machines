package com.snowgears.machines;

import com.snowgears.machines.listeners.PlayerListener;
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

    private boolean usePerms = false;

    private PlayerListener playerListener = new PlayerListener(this);
    private MachineData machineData = new MachineData(this);
    private MachineHandler machineHandler;



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


        usePerms = config.getBoolean("usePermissions");
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
                    player.getInventory().addItem(machineData.getItem(MachineType.ANTIGRAV));
                    player.getInventory().addItem(machineData.getItem(MachineType.DRILL));
                    player.getInventory().addItem(machineData.getItem(MachineType.PUMP));
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
    public MachineData getMachineData(){
        return machineData;
    }
    public MachineHandler getMachineHandler(){
        return machineHandler;
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
