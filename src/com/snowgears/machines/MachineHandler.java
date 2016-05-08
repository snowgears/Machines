package com.snowgears.machines;

import com.snowgears.machines.drill.Drill;
import com.snowgears.machines.paver.Paver;
import com.snowgears.machines.turret.Turret;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Lever;
import org.bukkit.material.PistonExtensionMaterial;

import java.io.File;
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

        //make sure if getting by a piston extension piece, the base is checked instead
        if(block.getType() == Material.PISTON_EXTENSION){
            PistonExtensionMaterial pistonEnd = (PistonExtensionMaterial)block.getState().getData();
            loc = block.getRelative(pistonEnd.getAttachedFace()).getLocation();
        }

        Machine machine;
        if(block.getType() == Material.LEVER){
            machine = this.getMachineByLever(loc);
        }
        else {
            machine = this.getMachineByBase(loc);
            if(machine == null){
                machine = this.getMachineByBase(loc.clone().add(0,-1,0));
                if(machine != null && machine.getFacing() == BlockFace.DOWN)
                    machine = null;
            }
            if(machine == null){
                machine = this.getMachineByBase(loc.clone().add(0,1,0));
                if(machine != null && machine.getFacing() != BlockFace.DOWN)
                    machine = null;
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

    public ArrayList<Machine> getMachines(Player player) {
        ArrayList<Machine> machines = new ArrayList<>();
        for (Machine machine : allMachines.values()) {
            if (machine.getOwner().getUniqueId().equals(player.getUniqueId()))
                machines.add(machine);
        }
        return machines;
    }

    private ArrayList<Machine> orderedMachineList() {
        ArrayList<Machine> list = new ArrayList<Machine>(allMachines.values());
        Collections.sort(list, new Comparator<Machine>() {
            @Override
            public int compare(Machine o1, Machine o2) {
                int compare = o1.getOwner().getName().toLowerCase().compareTo(o2.getOwner().getName().toLowerCase());
                if(compare == 0)
                    compare =o1.getType().toString().compareTo(o2.getType().toString());
                return compare;
            }
        });
        return list;
    }

    public void deactivateMachines(){
        for(Machine machine : allMachines.values()){
            if(machine.isActive())
                machine.deactivate();
        }
    }

    //TODO in each file, order by machine type, then number like Shop does
    public void saveMachines() {
        try {
            File fileDirectory = new File(plugin.getDataFolder(), "Data");
            deleteDirectory(fileDirectory);
            if (!fileDirectory.exists())
                fileDirectory.mkdir();

            ArrayList<Machine> machineList = orderedMachineList();
            if (machineList.isEmpty())
                return;
            OfflinePlayer lastOwner = null;
            File currentFile = null;
            int machineNumber = 0;
            String owner;

            for (Machine machine : machineList) {

                //use current file
                if (machine.getOwner().equals(lastOwner)) {
                    if (!currentFile.exists()) // file doesn't exist
                        currentFile.createNewFile();
                    machineNumber++;
                }
                //change current file to next player
                else {
                    lastOwner = machine.getOwner();
                    currentFile = new File(fileDirectory + "/" + lastOwner.getName() + " (" + lastOwner.getUniqueId().toString() + ").yml");
                    if (!currentFile.exists()) // file doesn't exist
                        currentFile.createNewFile();
                    machineNumber = 1;
                }
                YamlConfiguration config = YamlConfiguration.loadConfiguration(currentFile);

                owner =  lastOwner.getName() + " (" + lastOwner.getUniqueId().toString() + ")";
                config.set(owner + "." + machine.getType().toString() + "." + machineNumber + ".location.base", locationToString(machine.getBaseLocation()));
                config.set(owner + "." + machine.getType().toString() + "." + machineNumber + ".location.top", locationToString(machine.getTopLocation()));
                config.set(owner + "." + machine.getType().toString() + "." + machineNumber + ".location.lever", locationToString(machine.getLeverLocation()));
                config.set(owner + "." + machine.getType().toString() + "." + machineNumber + ".facing", machine.getFacing().toString());
                config.set(owner + "." + machine.getType().toString() + "." + machineNumber + ".inventory", machine.getInventory().getContents());

                config.save(currentFile);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    //TODO load the information from each player's file into the HashMap
    //TODO rename file to 'new name'-'UUID' if only UUID matches
    public void loadMachines() {
        File fileDirectory = new File(plugin.getDataFolder(), "Data");
        if (!fileDirectory.exists())
            return;

        // load all the yml files from the data directory
        for (File file : fileDirectory.listFiles()) {
            if (file.isFile()) {
                loadMachinesFromFile(file);
            }
        }
    }

     // TODO load the information from each player's file into the HashMap
    //  TODO rename file to 'new name'-'UUID' if only UUID matches
    private void loadMachinesFromFile(File machineFile) {
        String fileName = machineFile.getName();
        if(!fileName.endsWith(".yml"))
            return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(machineFile);

        UUID ownerUUID = null;
        try{
            ownerUUID = UUID.fromString(fileName.substring(fileName.indexOf('(')+1, fileName.lastIndexOf(')')));
        } catch (Exception e){
            return;
        }

        Set<String> keys = config.getKeys(false);
        if(keys.isEmpty())
            return;
        String owner = keys.iterator().next();

        for(MachineType type : MachineType.values()){

            ConfigurationSection section = config.getConfigurationSection(owner + "." + type.toString());
            if(section != null) {
                for(String machineNumber : section.getKeys(false)) {
                    String sBaseLocation = section.getString(machineNumber + ".location.base");
                    String sTopLocation = section.getString(machineNumber + ".location.top");
                    String sLeverLocation = section.getString(machineNumber + ".location.lever");
                    BlockFace facing = BlockFace.valueOf(section.getString(machineNumber + ".facing"));
                    ItemStack[] contents = ((List<ItemStack>) section.get(machineNumber + ".inventory")).toArray(new ItemStack[0]);

                    Location base = locationFromString(sBaseLocation);
                    Location top = locationFromString(sTopLocation);
                    Location lever = locationFromString(sLeverLocation);

                    loadMachine(type, ownerUUID, base, top, lever, facing, contents);
                }
            }
        }
    }

    private void loadMachine(MachineType type, UUID owner, Location base, Location top, Location lever, BlockFace facing, ItemStack[] inventoryContents){
        Machine machine = null;
        switch (type) {
            case DRILL:
                machine = new Drill(owner, base, top, lever, facing, inventoryContents);
                break;
            case PAVER:
                machine = new Paver(owner, base, top, lever, facing, inventoryContents);
                break;
            case TURRET:
                machine = new Turret(owner, base, top, lever, facing, inventoryContents);
                break;
        }
        if(machine != null)
            this.addMachine(machine);
    }

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

    public static boolean deleteDirectory(File directory) {
        if(directory.exists()){
            File[] files = directory.listFiles();
            if(null!=files){
                for(int i=0; i<files.length; i++) {
                    if(files[i].isDirectory()) {
                        deleteDirectory(files[i]);
                    }
                    else {
                        files[i].delete();
                    }
                }
            }
        }
        return(directory.delete());
    }
}
