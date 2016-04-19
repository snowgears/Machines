package com.snowgears.machines.listeners;


import com.snowgears.machines.Machine;
import com.snowgears.machines.MachineType;
import com.snowgears.machines.Machines;
import com.snowgears.machines.antigrav.AntiGrav;
import com.snowgears.machines.drill.Drill;
import com.snowgears.machines.pump.Pump;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class PlayerListener implements Listener{

    public Machines plugin = Machines.getPlugin();
    private long interactEventTick = 0;

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
                final Machine machine;
                switch(machineType){
                    case ANTIGRAV:
                        machine = new AntiGrav(player.getUniqueId(), event.getBlock().getLocation());
                        break;
                    case DRILL:
                        machine = new Drill(player.getUniqueId(), event.getBlock().getLocation());
                        break;
                    case PUMP:
                        machine = new Pump(player.getUniqueId(), event.getBlock().getLocation());
                        break;
                    default:
                        return;
                }

                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run() {
                        if(machine.create()){
                            //if machine creation was successful, save the machine in handler
                            plugin.getMachineHandler().addMachine(machine);
                        }
                    }
                }, 2L);
            }
            else{
                player.sendMessage(ChatColor.DARK_RED+"You do not have permission to use this machine.");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event){
        if(event.isCancelled())
            return;
        Player player = event.getPlayer();

        boolean brokeBase = false;

        Machine machine = plugin.getMachineHandler().getMachineByBase(event.getBlock().getLocation());
        if(machine == null){
            machine = plugin.getMachineHandler().getMachine(event.getBlock().getLocation());
        }
        else
            brokeBase = true;

        if(machine == null)
            return;

        if(brokeBase) {
            //player is removing their own machine
            if(machine.getOwner().getUniqueId().equals(player.getUniqueId())){
                machine.remove(true);
            }
            //someone is trying to break another player's machine
            else{
                //the player has operator permissions
                if (player.isOp() || (plugin.usePerms() && player.hasPermission("machines.operator"))) {
                    machine.remove(true);
                }
                else{
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.DARK_RED+"You do not have permission to destroy this machine.");
                }
            }
        }
        //the player tried to break a part of the machine that wasn't the base
        else{
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED+"You must break the base of the machine to remove it.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockClick(PlayerInteractEvent event) {
        //must check the time between this and last interact event since it is thrown twice in MC 1.9
        long tickCheck = System.currentTimeMillis();
        if(tickCheck - interactEventTick < 10) {
            event.setCancelled(true);
        }
        interactEventTick = tickCheck;

        if (event.isCancelled())
            return;
        Player player = event.getPlayer();

        if(event.getAction() == Action.RIGHT_CLICK_BLOCK){
            Location clicked = event.getClickedBlock().getLocation();

            //check to see if machine base was clicked
            Machine machine = plugin.getMachineHandler().getMachineByBase(clicked);
            if(machine != null){
                player.openInventory(machine.getInventory());
                event.setCancelled(true);
                return;
            }

            //check to see if machine lever was clicked
            machine = plugin.getMachineHandler().getMachineByLever(clicked);
            if(machine != null) {
                event.setCancelled(true);

               if (machine.isActive())
                   machine.deactivate();
               else
                   machine.activate();


                return;
            }

            //check to see if machine "top" was clicked (ROTATE)
            machine = plugin.getMachineHandler().getMachine(clicked);
            if(machine != null) {
                machine.rotate();
                event.setCancelled(true);
                return;
            }
        }
    }
}
