package com.snowgears.machines.drill;

import com.snowgears.machines.Machines;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

public class DrillConfig {

    private boolean enabled;
    private String inventoryName;
    private int inventoryRows;
    private ItemStack item;
    private HashMap<Material, Boolean> materialBlacklist;
    private HashMap<Material, Integer> fuelMap;
    private int speed;
    private boolean fillBuckets;

    public DrillConfig(){
        materialBlacklist = new HashMap<>();
        fuelMap = new HashMap<>();
        loadConfig();
    }

    public boolean isEnabled(){
        return enabled;
    }

    public Inventory createInventory(Player player){
        Inventory inventory = Bukkit.createInventory(player, (9*inventoryRows), inventoryName);

        //create the barrier item with no name
        ItemStack fuelChamber = new ItemStack(Material.BARRIER);
        ItemMeta im = fuelChamber.getItemMeta();
        im.setDisplayName(" ");
        fuelChamber.setItemMeta(im);

        if(inventoryRows == 1){
            int cornerSlot = 7;
            inventory.setItem(cornerSlot, fuelChamber);
        }
        else{
            int cornerSlot = (7 * (inventoryRows-1)) + (2 * (inventoryRows-2));
            int rightSlot = cornerSlot + 1;
            int bottomSlot = cornerSlot + 9;
            inventory.setItem(cornerSlot, fuelChamber);
            inventory.setItem(rightSlot, fuelChamber);
            inventory.setItem(bottomSlot, fuelChamber);
        }
        return inventory;
    }

    public ItemStack getItem(){
        return item;
    }

    public boolean canDrill(Material type){
        if(materialBlacklist.containsKey(type))
            return false;
        return true;
    }

    public int getFuelPower(Material type){
        if(fuelMap.containsKey(type)){
            return fuelMap.get(type);
        }
        return 0;
    }

    public int getSpeed(){
        return speed;
    }

    public boolean fillBuckets(){
        return fillBuckets;
    }

    private void loadConfig(){
        File configFile = new File(Machines.getPlugin().getDataFolder(), "drillConfig.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        enabled = config.getBoolean("drill.enabled");
        inventoryName = config.getString("drill.inventory.name");
        inventoryRows = config.getInt("drill.inventory.rows");
        speed = config.getInt("drill.speedInTicks");
        fillBuckets = config.getBoolean("drill.fillBuckets");

        if(enabled) {
            //create the drill item from config file
            ItemStack is = new ItemStack(Material.valueOf(config.getString("drill.item.type")));
            ItemMeta itemMeta = is.getItemMeta();
            String name = config.getString("drill.item.name");
            name = ChatColor.translateAlternateColorCodes('&', name);
            itemMeta.setDisplayName(name);
            List<String> lore = new ArrayList<>();
            Map<String, Object> loreStrings = config.getConfigurationSection("drill.item.lore").getValues(false);
            if (loreStrings != null) {
                for (Object s : loreStrings.values()) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', (String)s));
                }
            }
            itemMeta.setLore(lore);
            is.setItemMeta(itemMeta);
            item = is;

            //create the drill recipe from config file
            List<String> shapeList = config.getStringList("drill.crafting.shape");
            String[] shape = new String[shapeList.size()];
            shape = shapeList.toArray(shape);
            ShapedRecipe drillRecipe = new ShapedRecipe(this.getItem())
                    .shape(shape);

            Map<String, Object> shapeValues = config.getConfigurationSection("drill.crafting.mapping").getValues(false);
            Iterator<Map.Entry<String, Object>> it = shapeValues.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Object> entry = it.next();
                Material m = Material.valueOf((String) entry.getValue());
                char c = entry.getKey().charAt(0);
                drillRecipe.setIngredient(c, m);
            }
            Machines.getPlugin().getServer().addRecipe(drillRecipe);

            //populate material blacklist
            List<String> blacklist = config.getStringList("drill.blockBlacklist");
            for (String s : blacklist) {
                Material m = Material.valueOf(s);
                materialBlacklist.put(m, true);
            }

            //populate fuel types
            Set<String> fuelTypeStrings = config.getConfigurationSection("drill.fuelTypes").getKeys(true);
            for (String s : fuelTypeStrings) {
                Material m = Material.valueOf(s);
                int power = config.getInt("drill.fuelTypes." + s);
                fuelMap.put(m, power);
            }
        }
    }
}
