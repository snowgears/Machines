package com.snowgears.machines;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MachineData {

    private Machines plugin;

    //all of the materials that can be overwritten by machines
    private HashMap<Material, Boolean> materialsIgnored = new HashMap<Material, Boolean>();

    private HashMap<MachineType, ItemStack> machineItems = new HashMap<MachineType, ItemStack>();

    public MachineData(Machines instance){
        plugin = instance;
        initMaterialsIgnored();
        initMachineItems();
       // initMachineRecipes(); //NOT NEEDED ANYMORE SINCE ALL RECIPES ARE IN MACHINE CONFIGS
    }

    public Inventory getInfoGUI(Player player){
        Inventory invGUI = Bukkit.createInventory(player, 9, "Machines Info");
        if(plugin.getDrillConfig().isEnabled()){
            if (!player.isOp() && plugin.usePerms() && !(player.hasPermission("machines.drill") || player.hasPermission("machines.operator"))) {
                //do nothing. Don't add drill item to GUI
            }
            else{
                invGUI.addItem(this.getItem(MachineType.DRILL));
                //TODO implement all of this menu stuff with the menu class
                //TODO also make it so when you click on this, it opens a new menu with items that you can click to:
                // - show Crafting Recipe
                // -
            }
        }
        return invGUI;
    }

    public boolean isIgnoredMaterial(Material material){
        if(materialsIgnored.get(material) != null)
            return true;
        return false;
    }

    public ItemStack getItem(MachineType type){
        return machineItems.get(type);
    }

    public MachineType getMachineType(ItemStack item){
        for(Map.Entry<MachineType, ItemStack> entry : machineItems.entrySet()) {
            if (item.isSimilar(entry.getValue()))
                return entry.getKey();
        }
        return null;
    }

    private void initMaterialsIgnored(){
        materialsIgnored.put(Material.AIR, true);
        materialsIgnored.put(Material.ACACIA_SAPLING, true);
        materialsIgnored.put(Material.BAMBOO_SAPLING, true);
        materialsIgnored.put(Material.BIRCH_SAPLING, true);
        materialsIgnored.put(Material.DARK_OAK_SAPLING, true);
        materialsIgnored.put(Material.JUNGLE_SAPLING, true);
        materialsIgnored.put(Material.OAK_SAPLING, true);
        materialsIgnored.put(Material.WATER, true);
        materialsIgnored.put(Material.LAVA, true);
        materialsIgnored.put(Material.TALL_GRASS, true);
        materialsIgnored.put(Material.DEAD_BUSH, true);
        materialsIgnored.put(Material.DANDELION, true);
        materialsIgnored.put(Material.POPPY, true);
        materialsIgnored.put(Material.BROWN_MUSHROOM, true);
        materialsIgnored.put(Material.RED_MUSHROOM, true);
        materialsIgnored.put(Material.FIRE, true);
        materialsIgnored.put(Material.SNOW, true);
        materialsIgnored.put(Material.VINE, true);
    }

    private void initMachineItems(){

        if(plugin.getDrillConfig().isEnabled()){
            machineItems.put(MachineType.DRILL, plugin.getDrillConfig().getItem());
        }

        if(plugin.getPaverConfig().isEnabled()){
            machineItems.put(MachineType.PAVER, plugin.getPaverConfig().getItem());
        }

        if(plugin.getTurretConfig().isEnabled()){
            machineItems.put(MachineType.TURRET, plugin.getTurretConfig().getItem());
        }

        if(plugin.getConveyerConfig().isEnabled()){
            machineItems.put(MachineType.CONVEYER, plugin.getConveyerConfig().getItem());
        }

        //TODO replace these once as individual machine config files are done

        //antigrav machine
        ItemStack gravityMachine = new ItemStack(Material.BEACON);
        ItemMeta gravityMeta = gravityMachine.getItemMeta();
        gravityMeta.setDisplayName(ChatColor.GOLD+"Anti-Grav Machine");
        ArrayList<String> gravityLore = new ArrayList<String>();
        gravityLore.add(ChatColor.WHITE+""+ChatColor.ITALIC+"'It glows ominously and parts spin inside'");
        gravityLore.add(ChatColor.GRAY+"Requires fuel");
        gravityMeta.setLore(gravityLore);
        gravityMachine.setItemMeta(gravityMeta);
        machineItems.put(MachineType.ANTIGRAV, gravityMachine);

        //pump
        ItemStack pump = new ItemStack(Material.SEA_LANTERN);
        ItemMeta pumpMeta = pump.getItemMeta();
        pumpMeta.setDisplayName(ChatColor.GOLD+"Pump");
        ArrayList<String> pumpLore = new ArrayList<String>();
        pumpLore.add(ChatColor.WHITE+""+ChatColor.ITALIC+"'There are gears and tubes inside'");
        pumpLore.add(ChatColor.GRAY+"Requires fuel");
        pumpMeta.setLore(pumpLore);
        pump.setItemMeta(pumpMeta);
        machineItems.put(MachineType.PUMP, pump);
    }

    private void initMachineRecipes(){

        //TODO remove these completely once all individual machine config files are done
        //antigrav machine
        ShapedRecipe gravityRecipe = new ShapedRecipe(machineItems.get(MachineType.ANTIGRAV))
                .shape("RBR", "RFR", "PEP")
                .setIngredient('R', Material.BLAZE_ROD)
                .setIngredient('B', Material.BEACON)
                .setIngredient('F', Material.FURNACE)
                .setIngredient('P', Material.ENDER_PEARL)
                .setIngredient('E', Material.END_STONE);
        plugin.getServer().addRecipe(gravityRecipe);

        //pump
        ShapedRecipe pumpRecipe = new ShapedRecipe(machineItems.get(MachineType.PUMP))
                .shape("RDR", "RFR", "BSB")
                .setIngredient('R', Material.BLAZE_ROD)
                .setIngredient('D', Material.DISPENSER)
                .setIngredient('F', Material.FURNACE)
                .setIngredient('B', Material.BUCKET)
                .setIngredient('S', Material.SPONGE);
        plugin.getServer().addRecipe(pumpRecipe);
    }
}
