package com.snowgears.machines;

import com.snowgears.machines.antigrav.AntiGrav;
import com.snowgears.machines.drill.Drill;
import com.snowgears.machines.paver.Paver;
import com.snowgears.machines.pump.Pump;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MachineData {

    private Machines plugin;

    private HashMap<MachineType,MaterialData> machineBaseMaterials = new HashMap<MachineType, MaterialData>();
    private HashMap<MachineType,MaterialData> machineTopMaterials = new HashMap<MachineType, MaterialData>();

    //all of the materials that can be overwritten by machines
    private HashMap<Material, Boolean> materialsIgnored = new HashMap<Material, Boolean>();

    private HashMap<MachineType, ItemStack> machineItems = new HashMap<MachineType, ItemStack>();

    public MachineData(Machines instance){
        plugin = instance;
        initMachineMaterials();
        initMaterialsIgnored();
        initMachineItems();
        initMachineRecipes();
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

    public MaterialData getInitialBaseMaterial(MachineType type){
        return machineBaseMaterials.get(type);
    }

    public MaterialData getInitialTopMaterial(MachineType type){
        return machineTopMaterials.get(type);
    }

    public boolean isIgnoredMaterial(Material material){
        if(materialsIgnored.get(material) != null)
            return true;
        return false;
    }

    public ItemStack getItem(MachineType type){
        return machineItems.get(type);
    }

    public ItemStack getItem(Machine machine){
        if(machine instanceof Drill)
            return machineItems.get(MachineType.DRILL);
        else if(machine instanceof Paver)
            return machineItems.get(MachineType.PAVER);
        else if(machine instanceof AntiGrav)
            return machineItems.get(MachineType.ANTIGRAV);
        else if(machine instanceof Pump)
            return machineItems.get(MachineType.PUMP);
        return null;
    }

    public MachineType getMachineType(ItemStack item){
        for(Map.Entry<MachineType, ItemStack> entry : machineItems.entrySet()) {
            if (item.isSimilar(entry.getValue()))
                return entry.getKey();
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private void initMachineMaterials(){
        machineBaseMaterials.put(MachineType.ANTIGRAV, new MaterialData(Material.ENDER_STONE));
        machineTopMaterials.put(MachineType.ANTIGRAV, new MaterialData(Material.BEACON));

        machineBaseMaterials.put(MachineType.DRILL, new MaterialData(Material.OBSIDIAN));
        machineTopMaterials.put(MachineType.DRILL, new MaterialData(Material.PISTON_BASE, (byte)1)); //piston:BlockFace.UP

        machineBaseMaterials.put(MachineType.PAVER, new MaterialData(Material.OBSIDIAN));
        machineTopMaterials.put(MachineType.PAVER, new MaterialData(Material.DISPENSER));

        machineBaseMaterials.put(MachineType.PUMP, new MaterialData(Material.SPONGE, (byte)1)); //WET_SPONGE
        machineTopMaterials.put(MachineType.PUMP, new MaterialData(Material.SEA_LANTERN));
    }

    private void initMaterialsIgnored(){
        materialsIgnored.put(Material.AIR, true);
        materialsIgnored.put(Material.SAPLING, true);
        materialsIgnored.put(Material.WATER, true);
        materialsIgnored.put(Material.STATIONARY_WATER, true);
        materialsIgnored.put(Material.LAVA, true);
        materialsIgnored.put(Material.STATIONARY_LAVA, true);
        materialsIgnored.put(Material.LONG_GRASS, true);
        materialsIgnored.put(Material.DEAD_BUSH, true);
        materialsIgnored.put(Material.YELLOW_FLOWER, true);
        materialsIgnored.put(Material.RED_ROSE, true);
        materialsIgnored.put(Material.BROWN_MUSHROOM, true);
        materialsIgnored.put(Material.RED_MUSHROOM, true);
        materialsIgnored.put(Material.FIRE, true);
        materialsIgnored.put(Material.SNOW, true);
        materialsIgnored.put(Material.VINE, true);
        materialsIgnored.put(Material.DOUBLE_PLANT, true);
    }

    private void initMachineItems(){

        if(plugin.getDrillConfig().isEnabled()){
            machineItems.put(MachineType.DRILL, plugin.getDrillConfig().getItem());
        }

        if(plugin.getPaverConfig().isEnabled()){
            machineItems.put(MachineType.PAVER, plugin.getPaverConfig().getItem());
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
                .setIngredient('E', Material.ENDER_STONE);
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
