package com.snowgears.machines;

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

public class MachineConfig {

    protected boolean enabled;
    protected String inventoryName;
    protected int inventoryRows;
    protected ItemStack item;
    protected HashMap<Material, Integer> fuelMap;
    protected int speed;

    public MachineConfig(File configFile){
        fuelMap = new HashMap<>();
        loadConfig(configFile);
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

    public int getFuelPower(Material type){
        if (fuelMap.containsKey(type)) {
            return fuelMap.get(type);
        }
        return 0;
    }

    public int getSpeed(){
        return speed;
    }

    private void loadConfig(File configFile){
      //  File configFile = new File(Machines.getPlugin().getDataFolder(), "drillConfig.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        enabled = config.getBoolean("machine.enabled");
        inventoryName = config.getString("machine.inventory.name");
        inventoryRows = config.getInt("machine.inventory.rows");
        speed = config.getInt("machine.speedInTicks");

        if(enabled) {
            //create the drill item from config file
            ItemStack is = new ItemStack(Material.valueOf(config.getString("machine.item.type")));
            ItemMeta itemMeta = is.getItemMeta();
            String name = config.getString("machine.item.name");
            name = ChatColor.translateAlternateColorCodes('&', name);
            itemMeta.setDisplayName(name);
            List<String> lore = new ArrayList<>();
            Map<String, Object> loreStrings = config.getConfigurationSection("machine.item.lore").getValues(false);
            if (loreStrings != null) {
                for (Object s : loreStrings.values()) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', (String)s));
                }
            }
            itemMeta.setLore(lore);
            is.setItemMeta(itemMeta);
            item = is;

            //create the drill recipe from config file
            List<String> shapeList = config.getStringList("machine.crafting.shape");
            String[] shape = new String[shapeList.size()];
            shape = shapeList.toArray(shape);
            ShapedRecipe drillRecipe = new ShapedRecipe(this.getItem())
                    .shape(shape);

            Map<String, Object> shapeValues = config.getConfigurationSection("machine.crafting.mapping").getValues(false);
            Iterator<Map.Entry<String, Object>> it = shapeValues.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Object> entry = it.next();
                Material m = Material.valueOf((String) entry.getValue());
                char c = entry.getKey().charAt(0);
                drillRecipe.setIngredient(c, m);
            }
            Machines.getPlugin().getServer().addRecipe(drillRecipe);

            //populate fuel types
            Set<String> fuelTypeStrings = config.getConfigurationSection("machine.fuelTypes").getKeys(true);
            for (String s : fuelTypeStrings) {
                Material m = Material.valueOf(s);
                int power = config.getInt("machine.fuelTypes." + s);
                fuelMap.put(m, power);
            }
        }
    }

}
