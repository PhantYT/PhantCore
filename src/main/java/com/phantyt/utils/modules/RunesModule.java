package com.phantyt.utils.modules;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.phantyt.PhantCore;
import com.phantyt.utils.Module;
import com.phantyt.utils.commands.RunesCommands;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

public class RunesModule implements Module, Listener {
    private final PhantCore plugin;
    private FileConfiguration config;
    private FileConfiguration languageConfig;
    private final Map<Player, BukkitRunnable> effectTasks = new HashMap<>();
    private final Map<Player, String> lastActiveRune = new HashMap<>();
    private final Map<Player, Set<PotionEffectType>> runeEffects = new HashMap<>();
    private final Set<Player> playersNotified = new HashSet<>();
    private final NamespacedKey runeKey;
    private BukkitRunnable checkTask; // Для хранения задачи проверки

    public RunesModule(PhantCore plugin) {
        this.plugin = plugin;
        this.runeKey = new NamespacedKey(plugin, "rune_id");
    }

    @Override
    public String getName() {
        return "Runes";
    }

    @Override
    public void enable(PhantCore plugin) {
        File configFile = new File(plugin.getDataFolder(), "runes/config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("runes/config.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);

        File languageFile = new File(plugin.getDataFolder(), "runes/language.yml");
        if (!languageFile.exists()) {
            plugin.saveResource("runes/language.yml", false);
        }
        this.languageConfig = new YamlConfiguration();
        try {
            this.languageConfig.load(languageFile);
        } catch (IOException | org.bukkit.configuration.InvalidConfigurationException e) {
            e.printStackTrace();
        }

        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getCommand("runes").setExecutor(new RunesCommands(this));
        plugin.getCommand("runes").setTabCompleter(new RunesCommands(this));

        // Запускаем задачу проверки и сохраняем её
        checkTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    checkPlayerItem(player);
                }
            }
        };
        checkTask.runTaskTimer(plugin, 0L, 5L);

    }

    @Override
    public void disable(PhantCore plugin) {
        // Отменяем задачу проверки, если она существует
        if (checkTask != null && !checkTask.isCancelled()) {
            checkTask.cancel();
            checkTask = null;
        }

        // Удаляем эффекты у всех онлайн-игроков
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeEffects(player);
        }

        // Очищаем внутренние коллекции
        effectTasks.clear();
        lastActiveRune.clear();
        runeEffects.clear();
        playersNotified.clear();

        // Снимаем регистрацию команд
        plugin.getCommand("runes").setExecutor(null);
        plugin.getCommand("runes").setTabCompleter(null);

        // Отменяем все слушатели событий
        org.bukkit.event.HandlerList.unregisterAll(this);

    }

    public String getMessage(String key, Map<String, String> placeholders) {
        List<String> messages = languageConfig.getStringList("messages." + key);
        StringBuilder messageBuilder = new StringBuilder();
        for (String message : messages) {
            messageBuilder.append(message).append("\n");
        }
        if (messageBuilder.length() > 0) {
            messageBuilder.setLength(messageBuilder.length() - 1);
        }

        String message = messageBuilder.toString();
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }
        return message.isEmpty() ? "Сообщение не найдено, обратитесь к администратору @pogostik!" : message;
    }

    public ItemStack createItem(String itemName) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
        String displayName = config.getString("items." + itemName + ".display-name");
        List<String> lore = config.getStringList("items." + itemName + ".lore");

        if (displayName != null) {
            skullMeta.setDisplayName(displayName);
        }
        if (lore != null && !lore.isEmpty()) {
            skullMeta.setLore(lore);
        }

        skullMeta.getPersistentDataContainer().set(runeKey, PersistentDataType.STRING, itemName);

        String texture = config.getString("items." + itemName + ".data");
        if (texture != null && !texture.isEmpty()) {
            GameProfile profile = new GameProfile(UUID.randomUUID(), null);
            profile.getProperties().put("textures", new Property("textures", texture));
            try {
                Field profileField = skullMeta.getClass().getDeclaredField("profile");
                profileField.setAccessible(true);
                profileField.set(skullMeta, profile);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                e.printStackTrace();
            }
        }

        item.setItemMeta(skullMeta);
        return item;
    }

    private void checkPlayerItem(Player player) {
        ItemStack offHandItem = player.getInventory().getItemInOffHand();
        ItemStack[] mainInventory = player.getInventory().getContents();
        boolean effectsApplied = false;
        boolean handEffectActive = false;
        int maxRunes = config.getInt("max-runes", 1);
        int runeCount = countRunesInInventory(player);
        if (runeCount > maxRunes) {
            dropExcessRunes(player, runeCount - maxRunes);
        }

        if (offHandItem != null && offHandItem.getType() == Material.PLAYER_HEAD) {
            String handItemName = getItemName(offHandItem);
            if (handItemName != null && config.getString("items." + handItemName + ".type").equalsIgnoreCase("hand")) {
                removeEffects(player);
                applyEffects(player, handItemName);
                handEffectActive = true;
            }
        }

        for (ItemStack item : mainInventory) {
            if (item != null && item.getType() == Material.PLAYER_HEAD) {
                String itemName = getItemName(item);
                if (itemName != null) {
                    String itemType = config.getString("items." + itemName + ".type");
                    if (itemType.equalsIgnoreCase("inventory") && !handEffectActive) {
                        removeEffects(player);
                        applyEffects(player, itemName);
                        effectsApplied = true;
                    }
                }
            }
        }

        if (!handEffectActive && !effectsApplied) {
            removeEffects(player);
        }
    }

    private void dropExcessRunes(Player player, int excessRunes) {
        ItemStack[] inventoryContents = player.getInventory().getContents();
        int runesDropped = 0;

        for (int i = 0; i < inventoryContents.length && runesDropped < excessRunes; i++) {
            ItemStack item = inventoryContents[i];
            if (item != null && item.getType() == Material.PLAYER_HEAD && isRune(item)) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
                player.getInventory().setItem(i, null);
                runesDropped++;
            }
        }

        player.sendMessage(getMessage("max-runes-limit", null));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        if ((currentItem != null && currentItem.getType() == Material.PLAYER_HEAD && isRune(currentItem) ||
                cursorItem != null && cursorItem.getType() == Material.PLAYER_HEAD && isRune(cursorItem)) &&
                currentItem != null && cursorItem != null && currentItem.isSimilar(cursorItem)) {
            event.setCancelled(true);
            event.getWhoClicked().sendMessage(getMessage("stack-runes", null));
        }
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        final Player player = event.getPlayer();
        ItemStack item = event.getItem().getItemStack();
        if (item.getType() == Material.PLAYER_HEAD && isRune(item)) {
            int maxRunes = config.getInt("max-runes", 1);
            int runeCount = countRunesInInventory(player);
            if (runeCount >= maxRunes) {
                event.setCancelled(true);
                if (!playersNotified.contains(player)) {
                    player.sendMessage(getMessage("pickup-runes", null));
                    playersNotified.add(player);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            playersNotified.remove(player);
                        }
                    }.runTaskLater(plugin, 100L);
                }
            }
        }
    }

    private String getItemName(ItemStack item) {
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta != null && itemMeta.getPersistentDataContainer().has(runeKey, PersistentDataType.STRING)) {
            String runeId = itemMeta.getPersistentDataContainer().get(runeKey, PersistentDataType.STRING);
            if (config.contains("items." + runeId)) {
                return runeId;
            }
        }
        return null;
    }

    private boolean isRune(ItemStack item) {
        ItemMeta itemMeta = item.getItemMeta();
        return itemMeta != null && itemMeta.getPersistentDataContainer().has(runeKey, PersistentDataType.STRING) &&
                config.contains("items." + itemMeta.getPersistentDataContainer().get(runeKey, PersistentDataType.STRING));
    }

    private int countRunesInInventory(Player player) {
        int runeCount = 0;
        ItemStack[] inventoryContents = player.getInventory().getContents();
        for (ItemStack item : inventoryContents) {
            if (item != null && item.getType() == Material.PLAYER_HEAD && isRune(item)) {
                runeCount++;
            }
        }
        return runeCount;
    }

    private void applyEffects(Player player, String itemName) {
        if (config.contains("items." + itemName + ".effects")) {
            List<?> effects = config.getList("items." + itemName + ".effects");
            Set<PotionEffectType> appliedEffects = new HashSet<>();
            for (Object effect : effects) {
                String type = ((Map<?, ?>) effect).get("type").toString();
                int duration = ((Number) ((Map<?, ?>) effect).get("duration")).intValue();
                int amplifier = ((Number) ((Map<?, ?>) effect).get("amplifier")).intValue();
                PotionEffectType potionEffectType = PotionEffectType.getByName(type);
                if (potionEffectType != null) {
                    player.addPotionEffect(new PotionEffect(potionEffectType, duration * 20, amplifier, true, false));
                    appliedEffects.add(potionEffectType);
                }
            }
            runeEffects.put(player, appliedEffects);
        }
    }

    private void removeEffects(Player player) {
        if (runeEffects.containsKey(player)) {
            for (PotionEffectType effectType : runeEffects.get(player)) {
                if (player.hasPotionEffect(effectType)) {
                    player.removePotionEffect(effectType);
                }
            }
            runeEffects.remove(player);
        }
    }

    // Геттеры для RunesCommands
    public FileConfiguration getConfig() {
        return config;
    }

    public ItemStack createRune(String itemName) {
        return createItem(itemName);
    }

    public PhantCore getPlugin() {
        return plugin;
    }
}