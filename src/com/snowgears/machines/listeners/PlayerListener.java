package com.snowgears.machines.listeners;


import com.snowgears.machines.Machine;
import com.snowgears.machines.MachineType;
import com.snowgears.machines.Machines;
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

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if(event.isCancelled())
            return;
        Player player = event.getPlayer();
        MachineType machineType = plugin.getMachineData().getMachineType(player.getItemInHand());
        if(machineType != null){
            event.setCancelled(true);

            final Machine machine = new Machine(machineType, player.getUniqueId(), event.getBlock().getLocation());

            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                public void run() {
                    machine.create();
                }
            }, 2L);
        }
    }
}
