package com.phantyt.utils.itemmanager;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemManagerSave {
    private final File dataFile;
    private FileConfiguration dataConfig;

    public ItemManagerSave(File dataFolder) {
        this.dataFile = new File(dataFolder, "itemmanager/data.yml");
        if (!dataFile.getParentFile().exists()) {
            dataFile.getParentFile().mkdirs();
        }
        loadDataConfig();
    }

    private void loadDataConfig() {
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        if (!dataConfig.contains("items")) {
            dataConfig.createSection("items");
        }
    }

    public void saveItem(ItemStack item, String id) {
        ConfigurationSection itemsSection = dataConfig.getConfigurationSection("items");
        if (itemsSection == null) {
            itemsSection = dataConfig.createSection("items");
        }

        ConfigurationSection itemSection = itemsSection.createSection(id);
        itemSection.set("type", item.getType().name());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                itemSection.set("meta.display-name", meta.getDisplayName());
            }
            if (meta.hasLore()) {
                itemSection.set("meta.lore", meta.getLore());
            }
            itemSection.set("meta.unbreakable", meta.isUnbreakable());
            if (!meta.getItemFlags().isEmpty()) {
                List<String> flags = new ArrayList<>();
                for (ItemFlag flag : meta.getItemFlags()) {
                    flags.add(flag.name());
                }
                itemSection.set("meta.item-flags", flags);
            }
            if (!item.getEnchantments().isEmpty()) {
                List<String> enchants = new ArrayList<>();
                for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
                    enchants.add(entry.getKey().getName() + ":" + entry.getValue());
                }
                itemSection.set("meta.enchantments", enchants);
            }
            // Сохранение данных о голове (SkullMeta)
            if (meta instanceof SkullMeta) {
                SkullMeta skullMeta = (SkullMeta) meta;
                if (skullMeta.hasOwner()) {
                    itemSection.set("meta.skull-owner", skullMeta.getOwner());
                }
            }
            if (itemSection.contains("attributes")) {
                itemSection.set("attributes", itemSection.get("attributes"));
            }
        }

        saveDataConfig();
    }

    public void addAttribute(String id, String slot, double amount, String type) {
        ConfigurationSection itemSection = dataConfig.getConfigurationSection("items." + id);
        if (itemSection == null) return;

        ConfigurationSection attributesSection = itemSection.getConfigurationSection("attributes");
        if (attributesSection == null) {
            attributesSection = itemSection.createSection("attributes");
        }

        ConfigurationSection attributeSection = attributesSection.createSection(slot.toUpperCase());
        attributeSection.set("amount", amount);
        attributeSection.set("type", type.toUpperCase());

        saveDataConfig();
    }

    private void saveDataConfig() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ItemStack loadItemById(String id) {
        ConfigurationSection itemSection = dataConfig.getConfigurationSection("items." + id);
        if (itemSection == null) return null;

        Material material = Material.getMaterial(itemSection.getString("type", "STONE"));
        if (material == null) material = Material.STONE; // Защита от null
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) { // Проверяем, что meta не null
            if (itemSection.contains("meta.display-name")) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', itemSection.getString("meta.display-name")));
            }
            if (itemSection.contains("meta.lore")) {
                meta.setLore(itemSection.getStringList("meta.lore"));
            }
            meta.setUnbreakable(itemSection.getBoolean("meta.unbreakable", false));
            if (itemSection.contains("meta.item-flags")) {
                List<String> flags = itemSection.getStringList("meta.item-flags");
                for (String flag : flags) {
                    try {
                        meta.addItemFlags(ItemFlag.valueOf(flag));
                    } catch (IllegalArgumentException e) {
                        System.err.println("Invalid item flag: " + flag);
                    }
                }
            }
            // Загрузка данных о голове (SkullMeta)
            if (material == Material.PLAYER_HEAD && itemSection.contains("meta.skull-owner")) {
                SkullMeta skullMeta = (SkullMeta) meta;
                skullMeta.setOwner(itemSection.getString("meta.skull-owner"));
                item.setItemMeta(skullMeta);
            } else {
                item.setItemMeta(meta); // Устанавливаем meta перед добавлением зачарований
            }
        }

        // Применяем зачарования после установки базового meta
        if (itemSection.contains("meta.enchantments")) {
            List<String> enchants = itemSection.getStringList("meta.enchantments");
            for (String enchant : enchants) {
                String[] parts = enchant.split(":");
                if (parts.length == 2) {
                    org.bukkit.enchantments.Enchantment enchantment = org.bukkit.enchantments.Enchantment.getByName(parts[0]);
                    try {
                        int level = Integer.parseInt(parts[1]);
                        if (enchantment != null) {
                            item.addUnsafeEnchantment(enchantment, level);
                        } else {
                            System.err.println("Invalid enchantment: " + parts[0]);
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid enchantment level: " + parts[1]);
                    }
                }
            }
        }

        return item;
    }

    public List<String> getItemIds() {
        ConfigurationSection itemsSection = dataConfig.getConfigurationSection("items");
        if (itemsSection == null) return new ArrayList<>();
        return new ArrayList<>(itemsSection.getKeys(false));
    }
}