package com.snowgears.machines.conveyer;

import com.snowgears.machines.MachineConfig;
import com.snowgears.machines.Machines;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

public class ConveyerConfig extends MachineConfig {

    private int maxDistance;
    private HashMap<EntityType, Boolean> entityBlacklist;
    private ItemStack beltItem;

    public ConveyerConfig(File configFile){
        super(configFile);

        loadConfig(configFile);
    }

    public int getMaxDistance(){
        return maxDistance;
    }

    public boolean canTarget(Entity entity){
        if(entity == null || entity.isDead())
            return false;
        if(entityBlacklist.containsKey(entity.getType()))
            return false;
        return true;
    }

    public ItemStack getBeltItem(){
        return beltItem;
    }

    private void loadConfig(File configFile){
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        if(enabled) {
            maxDistance = config.getInt("machine.maxDistance");

            //populate entity blacklist
            entityBlacklist = new HashMap<>();
            List<String> blacklist = config.getStringList("machine.entityBlacklist");
            for (String s : blacklist) {
                entityBlacklist.put(EntityType.valueOf(s), true);
            }

            //create the belt item from config file
            ItemStack is = new ItemStack(Material.PACKED_ICE);
            ItemMeta itemMeta = is.getItemMeta();
            String name = config.getString("machine.beltItem.name");
            name = ChatColor.translateAlternateColorCodes('&', name);
            itemMeta.setDisplayName(name);
            List<String> lore = new ArrayList<>();
            List<String> loreStrings = config.getStringList("machine.beltItem.lore");
            if (loreStrings != null) {
                for (String s : loreStrings) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', s));
                }
            }
            itemMeta.setLore(lore);
            is.setItemMeta(itemMeta);
            is.setAmount(config.getInt("machine.beltCrafting.yield"));
            beltItem = is;

            //create the belt item recipe from config file
            List<String> shapeList = config.getStringList("machine.beltCrafting.shape");
            String[] shape = new String[shapeList.size()];
            shape = shapeList.toArray(shape);
            ShapedRecipe beltRecipe = new ShapedRecipe(this.getBeltItem())
                    .shape(shape);

            Map<String, Object> shapeValues = config.getConfigurationSection("machine.beltCrafting.mapping").getValues(false);
            Iterator<Map.Entry<String, Object>> it = shapeValues.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Object> entry = it.next();
                Material m = Material.valueOf((String) entry.getValue());
                char c = entry.getKey().charAt(0);
                beltRecipe.setIngredient(c, m);
            }
            Machines.getPlugin().getServer().addRecipe(beltRecipe);
        }
    }
}
