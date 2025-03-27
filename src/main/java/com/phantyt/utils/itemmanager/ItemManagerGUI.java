package com.phantyt.utils.itemmanager;

import com.phantyt.utils.modules.ItemManagerModule;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class ItemManagerGUI {
    private final ItemManagerModule module;
    private final ItemManagerSave itemSaver;
    private static final int ITEMS_PER_PAGE = 45;
    private static final String INVENTORY_TITLE = ChatColor.DARK_PURPLE + "ItemManager";

    public ItemManagerGUI(ItemManagerModule module) {
        this.module = module;
        this.itemSaver = module.getItemSaver();
    }

    public void openGUI(Player player, int page) {
        List<String> itemIds = itemSaver.getItemIds();
        int totalItems = itemIds.size();
        int totalPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);

        if (page < 0 || page >= totalPages) {
            page = 0;
        }

        Inventory gui = Bukkit.createInventory(null, 54, INVENTORY_TITLE + " - Страница " + (page + 1));
        fillBorders(gui);

        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalItems);
        for (int i = startIndex; i < endIndex; i++) {
            String id = itemIds.get(i);
            ItemStack item = itemSaver.loadItemById(id);
            if (item != null) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                    lore.add(ChatColor.GRAY + "ЛКМ - взять предмет");
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
                gui.setItem(i - startIndex + 9, item);
            }
        }

        if (page > 0) {
            gui.setItem(45, createNavigationItem(Material.PLAYER_HEAD, ChatColor.GREEN + "Предыдущая страница", "MHF_ArrowLeft", page - 1));
        }
        if (page < totalPages - 1) {
            gui.setItem(53, createNavigationItem(Material.PLAYER_HEAD, ChatColor.GREEN + "Следующая страница", "MHF_ArrowRight", page + 1));
        }
        gui.setItem(49, createCloseItem());

        player.openInventory(gui);
    }

    private void fillBorders(Inventory gui) {
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.setDisplayName(ChatColor.GRAY + " ");
        border.setItemMeta(borderMeta);

        for (int i = 0; i < 9; i++) {
            gui.setItem(i, border.clone());
            gui.setItem(45 + i, border.clone());
        }

        gui.setItem(0, createHeadItem("MHF_Question", ChatColor.LIGHT_PURPLE + "ItemManager"));
        gui.setItem(8, createHeadItem("MHF_Exclamation", ChatColor.LIGHT_PURPLE + "Страница"));
    }

    private ItemStack createHeadItem(String owner, String displayName) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwner(owner);
        meta.setDisplayName(displayName);
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack createNavigationItem(Material material, String name, String owner, int page) {
        ItemStack item = new ItemStack(material);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwner(owner);
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Перейти на страницу " + (page + 1));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCloseItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Закрыть");
        item.setItemMeta(meta);
        return item;
    }

    public void handleClick(Player player, InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        event.setCancelled(true);

        int slot = event.getSlot();
        Inventory gui = event.getClickedInventory();
        String title = event.getView().getTitle();

        if (slot >= 9 && slot < 54 - 9) {
            List<String> itemIds = itemSaver.getItemIds();
            int page = getPageFromTitle(title);
            int index = page * ITEMS_PER_PAGE + (slot - 9);
            if (index < itemIds.size()) {
                String id = itemIds.get(index);
                new ItemManagerGive(module).giveItem(player, id);
            }
        } else if (slot == 45 && clickedItem.getType() == Material.PLAYER_HEAD) {
            int page = getPageFromTitle(title);
            openGUI(player, page - 1);
        } else if (slot == 53 && clickedItem.getType() == Material.PLAYER_HEAD) {
            int page = getPageFromTitle(title);
            openGUI(player, page + 1);
        } else if (slot == 49 && clickedItem.getType() == Material.BARRIER) {
            player.closeInventory();
        }
    }

    private int getPageFromTitle(String title) {
        String[] parts = title.split(" - Страница ");
        if (parts.length > 1) {
            try {
                return Integer.parseInt(parts[1]) - 1;
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    public String getInventoryTitle() {
        return INVENTORY_TITLE;
    }
}