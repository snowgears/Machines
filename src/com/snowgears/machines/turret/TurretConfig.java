package com.snowgears.machines.turret;

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

public class TurretConfig {

    private boolean enabled;
    private String inventoryName;
    private int inventoryRows;
    private ItemStack item;
    private HashMap<Material, Integer> fuelMap;
    private int speed;
    private int scanDistance;

    public TurretConfig(){
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
        System.out.println("[Machines] Getting item from turret config: "+item.getType().toString());
        return item;
    }

    public int getFuelPower(Material type){
        if (fuelMap.containsKey(type)) {
            return fuelMap.get(type);
        }
        return 0;
    }

    public int getShootSpeed(){
        return speed;
    }

    public int getScanDistance(){
        return scanDistance;
    }

    private void loadConfig(){
        File configFile = new File(Machines.getPlugin().getDataFolder(), "turretConfig.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        enabled = config.getBoolean("turret.enabled");
        inventoryName = config.getString("turret.inventory.name");
        inventoryRows = config.getInt("turret.inventory.rows");
        speed = config.getInt("turret.speedInTicks");
        scanDistance = config.getInt("turret.scanDistance");

        if(enabled) {
            //create the turret item from config file
            ItemStack is = new ItemStack(Material.valueOf(config.getString("turret.item.type")));
            ItemMeta itemMeta = is.getItemMeta();
            String name = config.getString("turret.item.name");
            name = ChatColor.translateAlternateColorCodes('&', name);
            itemMeta.setDisplayName(name);
            List<String> lore = new ArrayList<>();
            Map<String, Object> loreStrings = config.getConfigurationSection("turret.item.lore").getValues(false);
            if (loreStrings != null) {
                for (Object s : loreStrings.values()) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', (String)s));
                }
            }
            itemMeta.setLore(lore);
            is.setItemMeta(itemMeta);
            item = is;

            //create the turret recipe from config file
            List<String> shapeList = config.getStringList("turret.crafting.shape");
            String[] shape = new String[shapeList.size()];
            shape = shapeList.toArray(shape);
            ShapedRecipe drillRecipe = new ShapedRecipe(this.getItem())
                    .shape(shape);

            Map<String, Object> shapeValues = config.getConfigurationSection("turret.crafting.mapping").getValues(false);
            Iterator<Map.Entry<String, Object>> it = shapeValues.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Object> entry = it.next();
                Material m = Material.valueOf((String) entry.getValue());
                char c = entry.getKey().charAt(0);
                drillRecipe.setIngredient(c, m);
            }
            Machines.getPlugin().getServer().addRecipe(drillRecipe);

            //populate fuel types
            Set<String> fuelTypeStrings = config.getConfigurationSection("turret.fuelTypes").getKeys(true);
            for (String s : fuelTypeStrings) {
                Material m = Material.valueOf(s);
                int power = config.getInt("turret.fuelTypes." + s);
                fuelMap.put(m, power);
            }
        }
    }
}
