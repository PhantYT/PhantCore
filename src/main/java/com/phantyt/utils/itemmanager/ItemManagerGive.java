package com.phantyt.utils.itemmanager;

import com.phantyt.utils.modules.ItemManagerModule;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ItemManagerGive {
    private final ItemManagerModule module;
    private final ItemManagerSave itemSaver;

    public ItemManagerGive(ItemManagerModule module) {
        this.module = module;
        this.itemSaver = module.getItemSaver();
    }

    public void giveItem(Player player, String id) {
        ItemStack item = itemSaver.loadItemById(id);
        if (item == null) {
            player.sendMessage(module.getMessage("item-not-found", id));
            return;
        }

        ItemStack clonedItem = item.clone(); // Клонируем предмет
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItem(player.getLocation(), clonedItem);
        } else {
            player.getInventory().addItem(clonedItem);
        }
        player.sendMessage(module.getMessage("item-given", clonedItem.hasItemMeta() && clonedItem.getItemMeta().hasDisplayName()
                ? clonedItem.getItemMeta().getDisplayName()
                : clonedItem.getType().name()));
    }
}