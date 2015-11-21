package com.snowgears.machines.listeners;


import com.snowgears.machines.Machine;
import com.snowgears.machines.MachineType;
import com.snowgears.machines.Machines;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class PlayerListener implements Listener{

    public Machines plugin = Machines.getPlugin();

    public PlayerListener(Machines instance) {
        plugin = instance;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if(event.isCancelled())
            return;
        Player player = event.getPlayer();
        MachineType machineType = plugin.getMachineData().getMachineType(player.getItemInHand());
        if(machineType != null){
            event.setCancelled(true);

            //if using permissions, check that the player is allowed
            String permString = "machines."+machineType.toString().toLowerCase()+".use";
            if(!plugin.usePerms() || (player.hasPermission("machines.operator") || player.hasPermission(permString))) {
                //setup the machine
                final Machine machine = new Machine(machineType, player.getUniqueId(), event.getBlock().getLocation());
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run() {
                        machine.create();
                    }
                }, 2L);
            }
            else{
                player.sendMessage(ChatColor.DARK_RED+"You do not have permission to use this machine.");
            }
        }
    }
}
