package com.phantyt.utils.modules;

import com.phantyt.PhantCore;
import com.phantyt.utils.Module;
import com.phantyt.utils.commands.ItemManagerCommands;
import com.phantyt.utils.itemmanager.ItemManagerGUI;
import com.phantyt.utils.itemmanager.ItemManagerSave;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

public class ItemManagerModule implements Module, Listener {
    private final PhantCore plugin;
    private FileConfiguration config;
    private FileConfiguration languageConfig;
    private final Map<UUID, ItemCreationState> creationStates = new HashMap<>();
    private ItemManagerSave itemSaver;
    private ItemManagerCommands commandExecutor;
    private ItemManagerGUI gui;

    private static final Map<String, String> ENCHANTMENT_MAPPING = new HashMap<>();
    static {
        ENCHANTMENT_MAPPING.put("PROTECTION", "PROTECTION_ENVIRONMENTAL");
        ENCHANTMENT_MAPPING.put("FIRE_PROTECTION", "PROTECTION_FIRE");
        ENCHANTMENT_MAPPING.put("FEATHER_FALLING", "PROTECTION_FALL");
        ENCHANTMENT_MAPPING.put("BLAST_PROTECTION", "PROTECTION_EXPLOSIONS");
        ENCHANTMENT_MAPPING.put("PROJECTILE_PROTECTION", "PROTECTION_PROJECTILE");
        ENCHANTMENT_MAPPING.put("RESPIRATION", "OXYGEN");
        ENCHANTMENT_MAPPING.put("AQUA_AFFINITY", "WATER_WORKER");
        ENCHANTMENT_MAPPING.put("THORNS", "THORNS");
        ENCHANTMENT_MAPPING.put("DEPTH_STRIDER", "DEPTH_STRIDER");
        ENCHANTMENT_MAPPING.put("FROST_WALKER", "FROST_WALKER");
        ENCHANTMENT_MAPPING.put("BINDING_CURSE", "BINDING_CURSE");
        ENCHANTMENT_MAPPING.put("SHARPNESS", "DAMAGE_ALL");
        ENCHANTMENT_MAPPING.put("SMITE", "DAMAGE_UNDEAD");
        ENCHANTMENT_MAPPING.put("BANE_OF_ARTHROPODS", "DAMAGE_ARTHROPODS");
        ENCHANTMENT_MAPPING.put("KNOCKBACK", "KNOCKBACK");
        ENCHANTMENT_MAPPING.put("FIRE_ASPECT", "FIRE_ASPECT");
        ENCHANTMENT_MAPPING.put("LOOTING", "LOOT_BONUS_MOBS");
        ENCHANTMENT_MAPPING.put("SWEEPING_EDGE", "SWEEPING_EDGE");
        ENCHANTMENT_MAPPING.put("EFFICIENCY", "DIG_SPEED");
        ENCHANTMENT_MAPPING.put("SILK_TOUCH", "SILK_TOUCH");
        ENCHANTMENT_MAPPING.put("UNBREAKING", "DURABILITY");
        ENCHANTMENT_MAPPING.put("FORTUNE", "LOOT_BONUS_BLOCKS");
        ENCHANTMENT_MAPPING.put("POWER", "ARROW_DAMAGE");
        ENCHANTMENT_MAPPING.put("PUNCH", "ARROW_KNOCKBACK");
        ENCHANTMENT_MAPPING.put("FLAME", "ARROW_FIRE");
        ENCHANTMENT_MAPPING.put("INFINITY", "ARROW_INFINITE");
        ENCHANTMENT_MAPPING.put("LUCK_OF_THE_SEA", "LUCK");
        ENCHANTMENT_MAPPING.put("LURE", "LURE");
        ENCHANTMENT_MAPPING.put("LOYALTY", "LOYALTY");
        ENCHANTMENT_MAPPING.put("IMPALING", "IMPALING");
        ENCHANTMENT_MAPPING.put("RIPTIDE", "RIPTIDE");
        ENCHANTMENT_MAPPING.put("CHANNELING", "CHANNELING");
        ENCHANTMENT_MAPPING.put("MULTISHOT", "MULTISHOT");
        ENCHANTMENT_MAPPING.put("QUICK_CHARGE", "QUICK_CHARGE");
        ENCHANTMENT_MAPPING.put("PIERCING", "PIERCING");
        ENCHANTMENT_MAPPING.put("MENDING", "MENDING");
        ENCHANTMENT_MAPPING.put("VANISHING_CURSE", "VANISHING_CURSE");
    }

    public ItemManagerModule(PhantCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "ItemManager";
    }

    @Override
    public void enable(PhantCore plugin) {
        File configFile = new File(plugin.getDataFolder(), "itemmanager/config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("itemmanager/config.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);

        File languageFile = new File(plugin.getDataFolder(), "itemmanager/language.yml");
        if (!languageFile.exists()) {
            plugin.saveResource("itemmanager/language.yml", false);
        }
        this.languageConfig = YamlConfiguration.loadConfiguration(languageFile);

        this.itemSaver = new ItemManagerSave(plugin.getDataFolder());
        this.commandExecutor = new ItemManagerCommands(this);
        this.gui = new ItemManagerGUI(this);

        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getCommand("itemmanager").setExecutor(commandExecutor);
        plugin.getCommand("itemmanager").setTabCompleter(commandExecutor);
    }

    @Override
    public void disable(PhantCore plugin) {
        HandlerList.unregisterAll(this);
        plugin.getCommand("itemmanager").setExecutor(null);
        plugin.getCommand("itemmanager").setTabCompleter(null);
        creationStates.clear();
    }

    public void startItemCreation(Player player, String id) {
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand == null || itemInHand.getType() == Material.AIR) {
            player.sendMessage(getMessage("no-item-in-hand"));
            return;
        }

        ItemStack item = itemInHand.clone();
        ItemMeta meta = item.getItemMeta();
        meta.setUnbreakable(true);
        for (ItemFlag flag : ItemFlag.values()) {
            meta.addItemFlags(flag);
        }
        item.setItemMeta(meta);

        UUID playerId = player.getUniqueId();
        creationStates.put(playerId, new ItemCreationState(item, id));
        player.sendMessage(getMessage("enter-name"));
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        ItemCreationState state = creationStates.get(playerId);

        if (state == null) {
            return;
        }

        event.setCancelled(true);
        String message = event.getMessage();

        switch (state.getStage()) {
            case ID:
                state.setName(ChatColor.translateAlternateColorCodes('&', message));
                player.sendMessage(getMessage("enter-enchantment"));
                state.setStage(ItemCreationStage.ENCHANTMENT);
                break;
            case ENCHANTMENT:
                if (message.equalsIgnoreCase("2")) {
                    finishItemCreation(player, state);
                } else {
                    String[] parts = message.split(" ");
                    if (parts.length != 2) {
                        player.sendMessage(getMessage("invalid-enchantment-format"));
                        return;
                    }
                    String enchantName = parts[0].toUpperCase();
                    if (!config.contains("enchantments." + enchantName)) {
                        player.sendMessage(getMessage("enchantment-not-found"));
                        return;
                    }
                    try {
                        int level = Integer.parseInt(parts[1]);
                        state.addEnchantment(enchantName, level);
                        player.sendMessage(getMessage("enter-next-enchantment"));
                    } catch (NumberFormatException e) {
                        player.sendMessage(getMessage("invalid-enchantment-level"));
                    }
                }
                break;
        }
    }

    private void finishItemCreation(Player player, ItemCreationState state) {
        ItemStack item = state.getItem();
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', state.getName()));

        List<String> lore = new ArrayList<>();
        String separator = config.getString("lore.separator");
        lore.add(ChatColor.translateAlternateColorCodes('&', separator));
        for (Map.Entry<String, Integer> enchantment : state.getEnchantments().entrySet()) {
            String enchantFormat = config.getString("enchantments." + enchantment.getKey());
            lore.add(ChatColor.translateAlternateColorCodes('&', enchantFormat
                    .replace("{0}", String.valueOf(enchantment.getValue()))));
        }
        lore.add(ChatColor.translateAlternateColorCodes('&', config.getString("lore.unbreakable")));
        lore.add(ChatColor.translateAlternateColorCodes('&', separator));

        meta.setLore(lore);
        item.setItemMeta(meta);

        for (Map.Entry<String, Integer> enchantment : state.getEnchantments().entrySet()) {
            String mappedEnchantName = ENCHANTMENT_MAPPING.getOrDefault(enchantment.getKey(), enchantment.getKey());
            org.bukkit.enchantments.Enchantment ench = org.bukkit.enchantments.Enchantment.getByName(mappedEnchantName);
            if (ench != null) {
                item.addUnsafeEnchantment(ench, enchantment.getValue());
                player.sendMessage(ChatColor.YELLOW + "Applied enchantment: " + mappedEnchantName + " " + enchantment.getValue());
            } else {
                player.sendMessage(ChatColor.RED + "Failed to apply enchantment: " + mappedEnchantName);
            }
        }

        meta = item.getItemMeta();
        item.setItemMeta(meta);

        itemSaver.saveItem(item, state.getId());
        player.sendMessage(ChatColor.YELLOW + "Item enchantments: " + item.getEnchantments().toString());
        player.sendMessage(getMessage("item-created", state.getName()));
        creationStates.remove(player.getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        if (title.startsWith(gui.getInventoryTitle())) {
            gui.handleClick(player, event);
        }
    }

    public void openGUI(Player player) {
        gui.openGUI(player, 0);
    }

    public String getMessage(String path, String... replacements) {
        List<String> messageList = languageConfig.getStringList(path);
        if (messageList.isEmpty()) {
            return "Ошибка: сообщение '" + path + "' не найдено в itemmanager/language.yml";
        }
        StringBuilder message = new StringBuilder();
        for (String line : messageList) {
            String formattedLine = line;
            for (int i = 0; i < replacements.length; i++) {
                formattedLine = formattedLine.replace("{" + i + "}", replacements[i]);
            }
            message.append(formattedLine).append("\n");
        }
        return ChatColor.translateAlternateColorCodes('&', message.toString().trim());
    }

    public ItemManagerSave getItemSaver() {
        return itemSaver;
    }

    private enum ItemCreationStage {
        ID, ENCHANTMENT
    }

    private static class ItemCreationState {
        private final ItemStack item;
        private final String id;
        private String name;
        private final Map<String, Integer> enchantments = new LinkedHashMap<>();
        private ItemCreationStage stage = ItemCreationStage.ID;

        public ItemCreationState(ItemStack item, String id) {
            this.item = item;
            this.id = id;
        }

        public ItemStack getItem() {
            return item;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Map<String, Integer> getEnchantments() {
            return enchantments;
        }

        public void addEnchantment(String name, int level) {
            enchantments.put(name, level);
        }

        public ItemCreationStage getStage() {
            return stage;
        }

        public void setStage(ItemCreationStage stage) {
            this.stage = stage;
        }
    }
}