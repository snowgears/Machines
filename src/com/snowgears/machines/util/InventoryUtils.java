package com.snowgears.machines.util;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;

public class InventoryUtils {

    //removes itemstack from inventory
    //returns the amount of items it could not remove
    public static int removeItem(Inventory inventory, ItemStack itemStack) {
        if(inventory == null)
            return itemStack.getAmount();
        if (itemStack.getAmount() <= 0)
            return 0;
        ItemStack[] contents = inventory.getContents();
        int amount = itemStack.getAmount();
        for (int i = 0; i < contents.length; i++) {
            ItemStack is = contents[i];
            if (is != null) {
                if (is.isSimilar(itemStack)) {
                    if (is.getAmount() > amount) {
                        contents[i].setAmount(is.getAmount() - amount);
                        inventory.setContents(contents);
                        return 0;
                    } else if (is.getAmount() == amount) {
                        contents[i].setType(Material.AIR);
                        inventory.setContents(contents);
                        return 0;
                    } else {
                        amount -= is.getAmount();
                        contents[i].setType(Material.AIR);
                    }
                }
            }
        }
        inventory.setContents(contents);
        return amount;
    }

    //takes an ItemStack and splits it up into multiple ItemStacks with correct stack sizes
    //then adds those items to the given inventory
    public static int addItem(Inventory inventory, ItemStack itemStack) {
        if(inventory == null)
            return itemStack.getAmount();
        if (itemStack.getAmount() <= 0)
            return 0;
        ArrayList<ItemStack> itemStacksAdding = new ArrayList<ItemStack>();

        //break up the itemstack into multiple ItemStacks with correct stack size
        int fullStacks = itemStack.getAmount() / itemStack.getMaxStackSize();
        int partialStack = itemStack.getAmount() % itemStack.getMaxStackSize();
        for (int i = 0; i < fullStacks; i++) {
            ItemStack is = itemStack.clone();
            is.setAmount(is.getMaxStackSize());
            itemStacksAdding.add(is);
        }
        ItemStack is = itemStack.clone();
        is.setAmount(partialStack);
        if (partialStack > 0)
            itemStacksAdding.add(is);

        //try adding all items from itemStacksAdding and return number of ones you couldnt add
        int amount = 0;
        for (ItemStack addItem : itemStacksAdding) {
            HashMap<Integer, ItemStack> noAdd = inventory.addItem(addItem);
            amount += noAdd.size();
        }
        return amount;
    }

    //gets the amount of items in inventory
    public static int getAmount(Inventory inventory, ItemStack itemStack){
        if(inventory == null)
            return 0;
        ItemStack[] contents = inventory.getContents();
        int amount = 0;
        for (int i = 0; i < contents.length; i++) {
            ItemStack is = contents[i];
            if (is != null) {
                if (is.isSimilar(itemStack)) {
                    amount += is.getAmount();
                }
            }
        }
        return (amount / itemStack.getAmount());
    }
}
