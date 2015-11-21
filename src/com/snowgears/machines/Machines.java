package com.snowgears.machines;

import com.snowgears.machines.listeners.PlayerListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.logging.Logger;

public class Machines extends JavaPlugin {

    private static final Logger log = Logger.getLogger("Minecraft");
    private static Machines plugin;
    private YamlConfiguration config;

    private PlayerListener playerListener = new PlayerListener(this);
    private MachineData machineData = new MachineData();



    public static Machines getPlugin() {
        return plugin;
    }

    @Override
    public void onEnable() {
        plugin = this;

        getServer().getPluginManager().registerEvents(playerListener, this);

        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            copy(getResource("config.yml"), configFile);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    @Override
    public void onDisable() {

    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("machines")) {
            if (args.length == 0) {
                sender.sendMessage("[Machines] Available Commands:");
                sender.sendMessage("   /machine list");
            } else if (args.length == 1) {
                if(args[0].equalsIgnoreCase("give")) {
                    Player player = (Player)sender;
                    player.getInventory().addItem(machineData.getItem(MachineType.GRAVITY));
                    player.getInventory().addItem(machineData.getItem(MachineType.MINER));
                    player.getInventory().addItem(machineData.getItem(MachineType.PUMP));
                }
            }
        }

        return true;
    }

    public MachineData getMachineData(){
        return machineData;
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
