package com.snowgears.machines.drill;


import com.snowgears.machines.Machine;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class Drill extends Machine {


    public Drill(UUID owner, Location baseLocation){

    }


    @Override
    public boolean activate() {
        return false;
    }

    @Override
    public boolean deactivate() {
        return false;
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public OfflinePlayer getOwner() {
        return null;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    @Override
    public Location getBaseLocation() {
        return null;
    }

    @Override
    public Location getTopLocation() {
        return null;
    }

    @Override
    public Location getLeverLocation() {
        return null;
    }

    @Override
    public boolean create() {
        Bukkit.broadcastMessage("This would create a drill machine.");
        return false;
    }

    @Override
    public boolean remove(boolean dropInventory) {
        return false;
    }
}
