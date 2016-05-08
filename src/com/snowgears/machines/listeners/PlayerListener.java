package com.snowgears.machines.listeners;


import com.snowgears.machines.Machine;
import com.snowgears.machines.MachineType;
import com.snowgears.machines.Machines;
import com.snowgears.machines.antigrav.AntiGrav;
import com.snowgears.machines.drill.Drill;
import com.snowgears.machines.paver.Paver;
import com.snowgears.machines.pump.Pump;
import com.snowgears.machines.turret.Turret;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class PlayerListener implements Listener{

    public Machines plugin = Machines.getPlugin();
    private HashMap<String, Long> interactEventTick = new HashMap<>();

    public PlayerListener(Machines instance) {
        plugin = instance;
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(final BlockPlaceEvent event) {
        if(event.isCancelled())
            return;

        Player player = event.getPlayer();
        MachineType machineType = plugin.getMachineData().getMachineType(player.getItemInHand());
        if(machineType != null){
            event.setCancelled(true);

            if(event.getBlock().getLocation().getBlockY() >= event.getBlock().getWorld().getMaxHeight()-1)
                return;

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
                    case PAVER:
                        machine = new Paver(player.getUniqueId(), event.getBlock().getLocation());
                        break;
                    case PUMP:
                        machine = new Pump(player.getUniqueId(), event.getBlock().getLocation());
                        break;
                    case TURRET:
                        machine = new Turret(player.getUniqueId(), event.getBlock().getLocation());
                        break;
                    default:
                        return;
                }

                if(player.getGameMode() == GameMode.SURVIVAL)
                    event.getItemInHand().setAmount(event.getItemInHand().getAmount()-1);

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
                if(player.getGameMode() == GameMode.CREATIVE)
                    machine.remove(false);
                else
                    machine.remove(true);
            }
            //someone is trying to break another player's machine
            else{
                //the player has operator permissions
                if (player.isOp() || (plugin.usePerms() && player.hasPermission("machines.operator"))) {
                    if(player.getGameMode() == GameMode.CREATIVE)
                        machine.remove(false);
                    else
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
        Player player = event.getPlayer();

        //must check the time between this and last interact event since it is thrown twice in MC 1.9
        long tickCheck = System.currentTimeMillis();
        if(interactEventTick.containsKey(player.getName())) {
            if (tickCheck - interactEventTick.get(player.getName()) < 10) {
                event.setCancelled(true);
            }
        }
        interactEventTick.put(player.getName(), tickCheck);

        if (event.isCancelled())
            return;

        if(event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK){
            Location clicked = event.getClickedBlock().getLocation();

            //make sure machine can not be clicked while on cooldown
            Machine machine = plugin.getMachineHandler().getMachine(clicked);
            if(machine != null){
                if(machine.onCooldown()) {
                    event.setCancelled(true);
                    return;
                }
                //player is trying to use another players machine
                if(plugin.useProtection()) {
                    if (!player.getUniqueId().equals(machine.getOwner().getUniqueId())) {
                        if (player.isOp() || (plugin.usePerms() && player.hasPermission("machines.operator"))) {
                            //do nothing. They are allowed to use the machine
                        }
                        else{
                            player.sendMessage(ChatColor.RED+"You do not have permission to use this machine.");
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
            }
        }

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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFuelBarrierClick(InventoryClickEvent event){
        if(event.getSlotType() == InventoryType.SlotType.CONTAINER){
            ItemStack is = event.getCurrentItem();
            if(is.getType() == Material.BARRIER){
                if(is.getItemMeta().getDisplayName() != null && is.getItemMeta().getDisplayName().equals(" "))
                    event.setCancelled(true);
            }
        }
    }


    //TODO this is from color portals. Implement same protection system here
    @EventHandler
    public void onExplosion(EntityExplodeEvent event) {
        final ArrayList<Block> blocksToDestroy = new ArrayList<Block>(50);

        //save all potential machine blocks (for sake of time during explosion)
        Iterator<Block> blockIterator = event.blockList().iterator();
        while (blockIterator.hasNext()) {

            Block block = blockIterator.next();
            Machine machine = null;
            if(block.getType() == Material.LEVER){
                machine = plugin.getMachineHandler().getMachineByLever(block.getLocation());
            }
            else{
                machine = plugin.getMachineHandler().getMachine(block.getLocation());
            }

            if (machine != null) {
                if (plugin.useProtection()) {
                    blockIterator.remove();
                } else {
                    machine.remove(true);
                }
            }
        }
    }

    //Prevent Drills from moving each other
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPistonExtend(BlockPistonExtendEvent event){
        if(event.isCancelled())
            return;
        Machine m = plugin.getMachineHandler().getMachine(event.getBlock().getLocation());
        if(m != null){
            if(!m.isActive())
                event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent event){
        for(Machine machine : plugin.getMachineHandler().getMachines(event.getPlayer())){
            machine.deactivate();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCraft(CraftItemEvent event){
        if(!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player)event.getWhoClicked();
        MachineType type = Machines.getPlugin().getMachineData().getMachineType(event.getCurrentItem());
        if(type != null){
            //if using permissions, check that the player is allowed
            String permString = "machines."+type.toString().toLowerCase()+".craft";
            if(!plugin.usePerms() || (player.hasPermission("machines.operator") || player.hasPermission(permString))){
                //do nothing
            }
            else
                event.setCancelled(true);
        }
    }
}
