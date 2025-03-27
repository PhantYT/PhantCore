package com.phantyt.utils.modules;

import com.phantyt.PhantCore;
import com.phantyt.utils.Module;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.File;
import java.util.List;

public class HotSwapModule implements Module, Listener {
    private final PhantCore plugin;
    private FileConfiguration config;
    private FileConfiguration languageConfig;

    public HotSwapModule(PhantCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "HotSwap";
    }

    @Override
    public void enable(PhantCore plugin) {
        File configFile = new File(plugin.getDataFolder(), "hotswap/config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("hotswap/config.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);

        File languageFile = new File(plugin.getDataFolder(), "language.yml");
        if (!languageFile.exists()) {
            plugin.saveResource("language.yml", false);
        }
        this.languageConfig = YamlConfiguration.loadConfiguration(languageFile);

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void disable(PhantCore plugin) {
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!config.getBoolean("swap-from-hand", true)) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand == null || itemInHand.getType() == Material.AIR) {
            return;
        }

        // Проверяем, подходит ли предмет для свапа
        if (swapItemWithArmor(player, itemInHand, player.getInventory().getHeldItemSlot())) {
            event.setCancelled(true); // Отменяем только если произошёл свап
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!config.getBoolean("swap-from-inventory", true)) {
            return;
        }

        if (event.getClick() != ClickType.RIGHT) {
            return;
        }

        if (event.getClickedInventory() == null || event.getCurrentItem() == null) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        if (!player.getInventory().equals(event.getClickedInventory())) {
            return; // Срабатывает только в инвентаре игрока
        }

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        // Проверяем, произошёл ли свап
        if (swapItemWithArmor(player, item, event.getSlot())) {
            event.setCancelled(true); // Отменяем только если произошёл свап
        }
    }

    private boolean swapItemWithArmor(Player player, ItemStack item, int sourceSlot) {
        PlayerInventory inventory = player.getInventory();
        Material itemType = item.getType();

        if (isHelmet(itemType) && config.getBoolean("swap-helmets", true)) {
            swapWithSlot(player, inventory, item, EquipmentSlot.HEAD, sourceSlot);
            return true;
        } else if (isChestplate(itemType) && config.getBoolean("swap-chestplates", true)) {
            swapWithSlot(player, inventory, item, EquipmentSlot.CHEST, sourceSlot);
            return true;
        } else if (isLeggings(itemType) && config.getBoolean("swap-leggings", true)) {
            swapWithSlot(player, inventory, item, EquipmentSlot.LEGS, sourceSlot);
            return true;
        } else if (isBoots(itemType) && config.getBoolean("swap-boots", true)) {
            swapWithSlot(player, inventory, item, EquipmentSlot.FEET, sourceSlot);
            return true;
        } else if (isElytra(itemType) && config.getBoolean("swap-elytra", true)) {
            swapWithSlot(player, inventory, item, EquipmentSlot.CHEST, sourceSlot);
            return true;
        }
        return false; // Возвращаем false, если свап не произошёл
    }

    private void swapWithSlot(Player player, PlayerInventory inventory, ItemStack item, EquipmentSlot slot, int sourceSlot) {
        ItemStack currentItem = null;

        switch (slot) {
            case HEAD:
                currentItem = inventory.getHelmet();
                inventory.setHelmet(item.clone());
                break;
            case CHEST:
                currentItem = inventory.getChestplate();
                inventory.setChestplate(item.clone());
                break;
            case LEGS:
                currentItem = inventory.getLeggings();
                inventory.setLeggings(item.clone());
                break;
            case FEET:
                currentItem = inventory.getBoots();
                inventory.setBoots(item.clone());
                break;
            default:
                return;
        }

        // Меняем предметы местами: текущий предмет брони возвращается в исходный слот
        if (config.getBoolean("swap-from-hand", true) && sourceSlot == inventory.getHeldItemSlot()) {
            if (currentItem != null && currentItem.getType() != Material.AIR) {
                inventory.setItemInMainHand(currentItem.clone());
            } else {
                inventory.setItemInMainHand(null);
            }
        } else if (config.getBoolean("swap-from-inventory", true)) {
            if (currentItem != null && currentItem.getType() != Material.AIR) {
                inventory.setItem(sourceSlot, currentItem.clone());
            } else {
                inventory.setItem(sourceSlot, null);
            }
        }

        // Воспроизводим звук при свапе
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        player.updateInventory(); // Обновляем инвентарь игрока
    }

    private boolean isHelmet(Material material) {
        return material == Material.LEATHER_HELMET ||
                material == Material.CHAINMAIL_HELMET ||
                material == Material.IRON_HELMET ||
                material == Material.GOLDEN_HELMET ||
                material == Material.DIAMOND_HELMET ||
                material == Material.NETHERITE_HELMET ||
                material == Material.TURTLE_HELMET;
    }

    private boolean isChestplate(Material material) {
        return material == Material.LEATHER_CHESTPLATE ||
                material == Material.CHAINMAIL_CHESTPLATE ||
                material == Material.IRON_CHESTPLATE ||
                material == Material.GOLDEN_CHESTPLATE ||
                material == Material.DIAMOND_CHESTPLATE ||
                material == Material.NETHERITE_CHESTPLATE;
    }

    private boolean isLeggings(Material material) {
        return material == Material.LEATHER_LEGGINGS ||
                material == Material.CHAINMAIL_LEGGINGS ||
                material == Material.IRON_LEGGINGS ||
                material == Material.GOLDEN_LEGGINGS ||
                material == Material.DIAMOND_LEGGINGS ||
                material == Material.NETHERITE_LEGGINGS;
    }

    private boolean isBoots(Material material) {
        return material == Material.LEATHER_BOOTS ||
                material == Material.CHAINMAIL_BOOTS ||
                material == Material.IRON_BOOTS ||
                material == Material.GOLDEN_BOOTS ||
                material == Material.DIAMOND_BOOTS ||
                material == Material.NETHERITE_BOOTS;
    }

    private boolean isElytra(Material material) {
        return material == Material.ELYTRA;
    }

    private String getMessage(String path, String... replacements) {
        List<String> messageList = languageConfig.getStringList(path);
        if (messageList.isEmpty()) {
            return "Ошибка: сообщение '" + path + "' не найдено в language.yml";
        }
        StringBuilder message = new StringBuilder();
        for (String line : messageList) {
            String formattedLine = line;
            for (int i = 0; i < replacements.length; i++) {
                formattedLine = formattedLine.replace("{" + i + "}", replacements[i]);
            }
            message.append(formattedLine).append("\n");
        }
        return message.toString().trim();
    }
}