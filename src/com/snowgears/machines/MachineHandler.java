package com.snowgears.machines;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Lever;
import org.bukkit.material.Sign;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class MachineHandler {

    private Machines plugin;

    private HashMap<Location, Machine> allMachines = new HashMap<Location, Machine>();

    public MachineHandler(Machines instance) {
        plugin = instance;
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                loadMachines();
            }
        }, 1L);
    }

    public Machine getMachineByBase(Location loc) {
        return allMachines.get(loc);
    }

    public Machine getMachine(Location loc){
        Block block = loc.getBlock();

        Machine machine;
        if(block.getType() == Material.LEVER){
            machine = plugin.getMachineHandler().getMachineByLever(loc);
        }
        else {
            machine = plugin.getMachineHandler().getMachine(loc);
            if(machine == null){
                machine = plugin.getMachineHandler().getMachine(loc.clone().add(0,-1,0));
            }
        }
        return machine;
    }

    public Machine getMachineByLever(Location loc) {
        Block leverBlock = loc.getBlock();
        if(leverBlock.getType() == Material.LEVER){
            Lever lever = (Lever)leverBlock.getState().getData();
            return allMachines.get(leverBlock.getRelative(lever.getAttachedFace()).getLocation());
        }
        return null;
    }

    public void addMachine(Machine machine) {
        allMachines.put(machine.getBaseLocation(), machine);
    }

    //This method should only be used by Machine class to delete
    public boolean removeMachine(Machine machine) {
        if (allMachines.containsKey(machine.getBaseLocation())) {
            allMachines.remove(machine.getBaseLocation());
            return true;
        }
        return false;
    }

    public int getNumberOfMachines() {
        return allMachines.size();
    }

    public int getNumberOfMachines(Player player) {
        int size = 0;
        for (Machine machine : allMachines.values()) {
            if (machine.getOwner().getUniqueId().equals(player.getUniqueId()))
                size++;
        }
        return size;
    }

    private ArrayList<Machine> orderedMachineList() {
        ArrayList<Machine> list = new ArrayList<Machine>(allMachines.values());
        Collections.sort(list, new Comparator<Machine>() {
            @Override
            public int compare(Machine o1, Machine o2) {
                return o1.getOwner().getName().toLowerCase().compareTo(o2.getOwner().getName().toLowerCase());
            }
        });
        return list;
    }

    //TODO make each player have their own save/data file
    //TODO in each file, order by machine type, then number like Shop does
//    public void saveMachines() {
//        File fileDirectory = new File(plugin.getDataFolder(), "Data");
//        if (!fileDirectory.exists())
//            fileDirectory.mkdir();
//        File machineFile = new File(fileDirectory + "/machines.yml");
//        if (!machineFile.exists()) { // file doesn't exist
//            try {
//                machineFile.createNewFile();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        } else { //does exist, clear it for future saving
//            PrintWriter writer = null;
//            try {
//                writer = new PrintWriter(machineFile);
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            }
//            writer.print("");
//            writer.close();
//        }
//
//        YamlConfiguration config = YamlConfiguration.loadConfiguration(machineFile);
//        ArrayList<Machine> machineList = orderedMachineList();
//
//        String owner;
//
//        int machineNumber = 1;
//        for (int i = 0; i < shopList.size(); i++) {
//            Machine s = shopList.get(i);
//            //don't save shops that are not initialized with items
//            if (s.isInitialized()) {
//                owner = s.getOwnerName() + " (" + s.getOwnerUUID().toString() + ")";
//                config.set("shops." + owner + "." + machineNumber + ".location", locationToString(s.getSignLocation()));
//                config.set("shops." + owner + "." + machineNumber + ".price", s.getPrice());
//                config.set("shops." + owner + "." + machineNumber + ".amount", s.getAmount());
//                String type = "";
//                if (s.isAdminShop())
//                    type = "admin ";
//                type = type + s.getType().toString();
//                config.set("shops." + owner + "." + machineNumber + ".type", type);
//
//                ItemStack itemStack = s.getItemStack();
//                itemStack.setAmount(1);
//                config.set("shops." + owner + "." + machineNumber + ".item", itemStack);
//
//                if (s.getType() == ShopType.BARTER) {
//                    ItemStack barterItemStack = s.getBarterItemStack();
//                    barterItemStack.setAmount(1);
//                    config.set("shops." + owner + "." + machineNumber + ".itemBarter", barterItemStack);
//                }
//
//                machineNumber++;
//                //reset shop number if next shop has a different owner
//                if (i < shopList.size() - 1) {
//                    if (!(s.getOwnerUUID().equals(shopList.get(i + 1).getOwnerUUID())))
//                        machineNumber = 1;
//                }
//            }
//        }
//
//        try {
//            config.save(machineFile);
//        } catch (IOException ioe) {
//            ioe.printStackTrace();
//        }
//
//        plugin.getEnderChestHandler().saveEnderChests();
//    }

    //TODO load the information from each player's file into the HashMap
    public void loadMachines() {
//        File fileDirectory = new File(plugin.getDataFolder(), "Data");
//        if (!fileDirectory.exists())
//            return;
//        File shopFile = new File(fileDirectory + "/shops.yml");
//        if (!shopFile.exists())
//            return;
//
//        YamlConfiguration config = YamlConfiguration.loadConfiguration(shopFile);
//        loadShopsFromConfig(config);
    }

     // TODO load the information from each player's file into the HashMap
//    private void loadMachinesFromConfig(YamlConfiguration config) {
//
//        if (config.getConfigurationSection("shops") == null)
//            return;
//        Set<String> allShopOwners = config.getConfigurationSection("shops").getKeys(false);
//
//        for (String shopOwner : allShopOwners) {
//            Set<String> allShopNumbers = config.getConfigurationSection("shops." + shopOwner).getKeys(false);
//            for (String shopNumber : allShopNumbers) {
//                Location signLoc = locationFromString(config.getString("shops." + shopOwner + "." + shopNumber + ".location"));
//                Block b = signLoc.getBlock();
//                if (b.getType() == Material.WALL_SIGN) {
//                    org.bukkit.material.Sign sign = (org.bukkit.material.Sign) b.getState().getData();
//                    //Location loc = b.getRelative(sign.getAttachedFace()).getLocation();
//                    UUID owner = uidFromString(shopOwner);
//                    double price = Double.parseDouble(config.getString("shops." + shopOwner + "." + shopNumber + ".price"));
//                    int amount = Integer.parseInt(config.getString("shops." + shopOwner + "." + shopNumber + ".amount"));
//                    String type = config.getString("shops." + shopOwner + "." + shopNumber + ".type");
//                    boolean isAdmin = false;
//                    if (type.contains("admin"))
//                        isAdmin = true;
//                    ShopType shopType = typeFromString(type);
//
//                    ItemStack itemStack = config.getItemStack("shops." + shopOwner + "." + shopNumber + ".item");
//                    Machine shop = new Machine(signLoc, owner, price, amount, isAdmin, shopType);
//                    shop.setItemStack(itemStack);
//                    if (shop.getType() == ShopType.BARTER) {
//                        ItemStack barterItemStack = config.getItemStack("shops." + shopOwner + "." + shopNumber + ".itemBarter");
//                        shop.setBarterItemStack(barterItemStack);
//                    }
//                    shop.updateSign();
//                    this.addShop(shop);
//                }
//            }
//        }
//    }

    private String locationToString(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private Location locationFromString(String locString) {
        String[] parts = locString.split(",");
        return new Location(plugin.getServer().getWorld(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
    }

    private UUID uidFromString(String ownerString) {
        int index = ownerString.indexOf("(");
        String uidString = ownerString.substring(index + 1, ownerString.length() - 1);
        return UUID.fromString(uidString);
    }
}
