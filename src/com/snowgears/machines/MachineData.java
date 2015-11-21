package com.snowgears.machines;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MachineData {

    private HashMap<MachineType,MaterialData> machineBaseMaterials = new HashMap<MachineType, MaterialData>();
    private HashMap<MachineType,MaterialData> machineTopMaterials = new HashMap<MachineType, MaterialData>();

    //all of the materials that can be overwritten by machines
    private HashMap<Material, Boolean> materialsIgnored = new HashMap<Material, Boolean>();

    private HashMap<MachineType, ItemStack> machineItems = new HashMap<MachineType, ItemStack>();

    public MachineData(){
        initMachineMaterials();
        initMaterialsIgnored();
        initMachineItems();
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

    public MachineType getMachineType(ItemStack item){
        for(Map.Entry<MachineType, ItemStack> entry : machineItems.entrySet()) {
            if (item.isSimilar(entry.getValue()))
                return entry.getKey();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void initMachineMaterials(){
        machineBaseMaterials.put(MachineType.GRAVITY, new MaterialData(Material.ENDER_STONE));
        machineTopMaterials.put(MachineType.GRAVITY, new MaterialData(Material.BEACON));

        machineBaseMaterials.put(MachineType.MINER, new MaterialData(Material.OBSIDIAN));
        machineTopMaterials.put(MachineType.MINER, new MaterialData(Material.PISTON_BASE, (byte)1)); //piston:BlockFace.UP

        machineBaseMaterials.put(MachineType.PUMP, new MaterialData(Material.SPONGE, (byte)1)); //WET_SPONGE
        machineTopMaterials.put(MachineType.PUMP, new MaterialData(Material.DISPENSER, (byte)1)); //dispenser:BlockFace.UP
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
        //gravity machine
        ItemStack gravityMachine = new ItemStack(Material.BEACON);
        ItemMeta gravityMeta = gravityMachine.getItemMeta();
        gravityMeta.setDisplayName(ChatColor.GOLD+"Anti-Grav Machine");
        ArrayList<String> gravityLore = new ArrayList<String>();
        gravityLore.add(ChatColor.WHITE+""+ChatColor.ITALIC+"'It glows ominously and parts spin inside'");
        gravityLore.add(ChatColor.GRAY+"Requires fuel");
        gravityMeta.setLore(gravityLore);
        gravityMachine.setItemMeta(gravityMeta);
        machineItems.put(MachineType.GRAVITY, gravityMachine);

        //mining machine
        ItemStack miningMachine = new ItemStack(Material.PISTON_BASE);
        ItemMeta miningMeta = miningMachine.getItemMeta();
        miningMeta.setDisplayName(ChatColor.GOLD+"Mining Machine");
        ArrayList<String> miningLore = new ArrayList<String>();
        miningLore.add(ChatColor.WHITE+""+ChatColor.ITALIC+"'It looks very old and heavy'");
        miningLore.add(ChatColor.GRAY+"Requires fuel");
        miningMeta.setLore(miningLore);
        miningMachine.setItemMeta(miningMeta);
        machineItems.put(MachineType.MINER, miningMachine);

        //pump
        ItemStack pump = new ItemStack(Material.DISPENSER);
        ItemMeta pumpMeta = pump.getItemMeta();
        pumpMeta.setDisplayName(ChatColor.GOLD+"Pump");
        ArrayList<String> pumpLore = new ArrayList<String>();
        pumpLore.add(ChatColor.WHITE+""+ChatColor.ITALIC+"'There are gears and tubes inside'");
        pumpLore.add(ChatColor.GRAY+"Requires fuel");
        pumpMeta.setLore(pumpLore);
        pump.setItemMeta(pumpMeta);
        machineItems.put(MachineType.PUMP, pump);
    }
}
