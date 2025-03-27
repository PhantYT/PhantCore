package com.phantyt.utils.modules;

import com.phantyt.PhantCore;
import com.phantyt.utils.Module;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShulkerOpenModule implements Module, Listener {
    private final PhantCore plugin;
    private FileConfiguration config;
    private final Map<UUID, Inventory> openShulkerInventories = new HashMap<>();
    private final Map<UUID, Inventory> originalInventories = new HashMap<>();
    private final Map<UUID, Integer> shulkerSlots = new HashMap<>();
    private int taskId = -1;

    public ShulkerOpenModule(PhantCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "ShulkerOpen";
    }

    @Override
    public void enable(PhantCore plugin) {
        File configFile = new File(plugin.getDataFolder(), "shulkeropen/config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("shulkeropen/config.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);

        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Запускаем задачу для проверки открытых шалкеров
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::checkAllOpenShulkers, 0L, 0);
    }

    @Override
    public void disable(PhantCore plugin) {
        HandlerList.unregisterAll(this);
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!config.getBoolean("modes.open-in-hand", true)) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (openShulkerInventories.containsKey(playerId)) {
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && isContainerBlock(event.getClickedBlock())) {
            return;
        }

        if (item != null && isShulkerBox(item.getType())) {
            openShulkerInventory(player, item, player.getInventory(), -1);
            if (!config.getBoolean("allow-place", false)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();

        boolean openInInventory = config.getBoolean("modes.open-in-inventory", true);
        boolean openInChest = config.getBoolean("modes.open-in-chest", true);

        if ((openInInventory || openInChest) && event.getClick() == ClickType.RIGHT) {
            if (event.getClickedInventory() == null || event.getCurrentItem() == null) {
                return;
            }

            ItemStack item = event.getCurrentItem();
            if (!isShulkerBox(item.getType())) {
                return;
            }

            if (openShulkerInventories.containsKey(playerId)) {
                return;
            }

            Inventory clickedInventory = event.getClickedInventory();
            boolean isPlayerInventory = clickedInventory.equals(player.getInventory());
            boolean isContainer = !isPlayerInventory;

            if ((openInInventory && isPlayerInventory) || (openInChest && isContainer)) {
                openShulkerInventory(player, item, clickedInventory, event.getSlot());
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        UUID playerId = player.getUniqueId();
        Inventory closedInventory = event.getInventory();

        if (openShulkerInventories.containsKey(playerId) && openShulkerInventories.get(playerId).equals(closedInventory)) {
            Inventory originalInventory = originalInventories.get(playerId);
            int shulkerSlot = shulkerSlots.getOrDefault(playerId, -1);
            ItemStack shulkerBox = null;

            // Определяем исходный шалкер-бокс
            if (shulkerSlot == -1) {
                shulkerBox = player.getInventory().getItemInMainHand();
                if (shulkerBox == null || !isShulkerBox(shulkerBox.getType())) {
                    for (ItemStack item : player.getInventory().getContents()) {
                        if (item != null && isShulkerBox(item.getType())) {
                            shulkerBox = item;
                            break;
                        }
                    }
                }
            } else if (originalInventory != null) {
                shulkerBox = originalInventory.getItem(shulkerSlot);
            }

            if (shulkerBox != null && shulkerBox.getItemMeta() instanceof BlockStateMeta) {
                BlockStateMeta meta = (BlockStateMeta) shulkerBox.getItemMeta();
                if (meta.getBlockState() instanceof ShulkerBox) {
                    ShulkerBox shulker = (ShulkerBox) meta.getBlockState();

                    // Проверяем содержимое на наличие шалкеров и перемещаем их в инвентарь
                    Inventory shulkerInventory = shulker.getInventory();
                    for (int i = 0; i < closedInventory.getSize(); i++) {
                        ItemStack item = closedInventory.getItem(i);
                        if (item != null && isShulkerBox(item.getType())) {
                            int freeSlot = player.getInventory().firstEmpty();
                            if (freeSlot != -1) {
                                player.getInventory().setItem(freeSlot, item.clone());
                            } else {
                                player.getWorld().dropItemNaturally(player.getLocation(), item.clone());
                            }
                            closedInventory.setItem(i, null); // Удаляем шалкер из содержимого
                        }
                    }

                    // Обновляем содержимое шалкер-бокса
                    shulkerInventory.setContents(closedInventory.getContents());
                    meta.setBlockState(shulker);
                    shulkerBox.setItemMeta(meta);

                    // Обновляем исходный инвентарь
                    if (shulkerSlot != -1 && originalInventory != null) {
                        originalInventory.setItem(shulkerSlot, shulkerBox);
                    }
                }
            }

            // Возвращаем игрока в исходный инвентарь, если он был открыт
            if (originalInventory != null && !originalInventory.equals(player.getInventory())) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> player.openInventory(originalInventory), 1L);
            }

            // Очищаем данные игрока
            openShulkerInventories.remove(playerId);
            originalInventories.remove(playerId);
            shulkerSlots.remove(playerId);
        }
    }

    private void openShulkerInventory(Player player, ItemStack shulkerBox, Inventory originalInventory, int slot) {
        if (!(shulkerBox.getItemMeta() instanceof BlockStateMeta)) {
            return;
        }

        BlockStateMeta meta = (BlockStateMeta) shulkerBox.getItemMeta();
        if (!(meta.getBlockState() instanceof ShulkerBox)) {
            return;
        }

        ShulkerBox shulker = (ShulkerBox) meta.getBlockState();
        Inventory shulkerInventory = Bukkit.createInventory(null, 27, "Шалкер-бокс");
        shulkerInventory.setContents(shulker.getInventory().getContents());

        UUID playerId = player.getUniqueId();
        player.openInventory(shulkerInventory);
        player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, 1.0f, 1.0f);
        openShulkerInventories.put(playerId, shulkerInventory);
        originalInventories.put(playerId, originalInventory);
        if (slot >= 0) {
            shulkerSlots.put(playerId, slot);
        }
    }

    private void checkAllOpenShulkers() {
        for (Map.Entry<UUID, Inventory> entry : openShulkerInventories.entrySet()) {
            UUID playerId = entry.getKey();
            Inventory shulkerInventory = entry.getValue();
            Player player = Bukkit.getPlayer(playerId);

            if (player != null && player.isOnline()) {
                checkAndReturnShulkers(player, shulkerInventory);
            }
        }
    }

    private void checkAndReturnShulkers(Player player, Inventory shulkerInventory) {
        Inventory playerInventory = player.getInventory();
        for (int i = 0; i < shulkerInventory.getSize(); i++) {
            ItemStack item = shulkerInventory.getItem(i);
            if (item != null && isShulkerBox(item.getType())) {
                int freeSlot = playerInventory.firstEmpty();
                if (freeSlot != -1) {
                    playerInventory.setItem(freeSlot, item.clone());
                    shulkerInventory.setItem(i, null);
                } else {
                    player.getWorld().dropItemNaturally(player.getLocation(), item.clone());
                    shulkerInventory.setItem(i, null);
                }
            }
        }
    }

    private boolean isShulkerBox(Material material) {
        return material == Material.SHULKER_BOX ||
                material == Material.WHITE_SHULKER_BOX ||
                material == Material.ORANGE_SHULKER_BOX ||
                material == Material.MAGENTA_SHULKER_BOX ||
                material == Material.LIGHT_BLUE_SHULKER_BOX ||
                material == Material.YELLOW_SHULKER_BOX ||
                material == Material.LIME_SHULKER_BOX ||
                material == Material.PINK_SHULKER_BOX ||
                material == Material.GRAY_SHULKER_BOX ||
                material == Material.LIGHT_GRAY_SHULKER_BOX ||
                material == Material.CYAN_SHULKER_BOX ||
                material == Material.PURPLE_SHULKER_BOX ||
                material == Material.BLUE_SHULKER_BOX ||
                material == Material.BROWN_SHULKER_BOX ||
                material == Material.GREEN_SHULKER_BOX ||
                material == Material.RED_SHULKER_BOX ||
                material == Material.BLACK_SHULKER_BOX;
    }

    private boolean isContainerBlock(Block block) {
        if (block == null) return false;
        Material type = block.getType();
        return type == Material.CHEST ||
                type == Material.TRAPPED_CHEST ||
                type == Material.ENDER_CHEST ||
                type == Material.BARREL ||
                type == Material.DISPENSER ||
                type == Material.DROPPER ||
                type == Material.HOPPER ||
                type.name().endsWith("_SHULKER_BOX");
    }
}