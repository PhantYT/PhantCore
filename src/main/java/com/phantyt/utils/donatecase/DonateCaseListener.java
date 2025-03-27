package com.phantyt.utils.donatecase;

import com.phantyt.utils.modules.DonateCaseModule;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class DonateCaseListener implements Listener {
    private final DonateCaseModule module;
    private Location lastClickedShulker;
    private final Set<UUID> activeAnimations = new HashSet<>();
    private final Set<UUID> displayingWinner = new HashSet<>();
    private final Map<String, UUID> activeCases = new HashMap<>(); // locKey -> player UUID

    public DonateCaseListener(DonateCaseModule module) {
        this.module = module;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.PURPLE_SHULKER_BOX) return;

        Location loc = block.getLocation();
        FileConfiguration config = module.getConfig();
        String locKey = loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
        if (!config.contains("case-locations." + locKey)) return;

        Player player = event.getPlayer();
        if (activeAnimations.contains(player.getUniqueId()) || displayingWinner.contains(player.getUniqueId())) {
            player.sendMessage(module.getMessage("error.animation-in-progress"));
            event.setCancelled(true);
            return;
        }
        // Убрана проверка activeCases здесь, чтобы можно было открыть меню

        event.setCancelled(true);
        lastClickedShulker = loc;
        openCaseMenu(player, config.getString("case-locations." + locKey));
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.PURPLE_SHULKER_BOX) return;

        Location loc = block.getLocation();
        FileConfiguration config = module.getConfig();
        String locKey = loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
        if (!config.contains("case-locations." + locKey)) return;

        Player player = event.getPlayer();
        player.sendMessage(ChatColor.RED + "Вы не можете сломать кейс!");
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        String menuTitle = ChatColor.translateAlternateColorCodes('&', module.getConfig().getString("menu-title", "&5&l✨ Донат Кейсы ✨"));
        if (!event.getView().getTitle().equals(menuTitle)) return;

        event.setCancelled(true);
        if (activeAnimations.contains(player.getUniqueId()) || displayingWinner.contains(player.getUniqueId())) {
            player.sendMessage(module.getMessage("error.animation-in-progress"));
            return;
        }

        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta() || item.getType() != Material.PURPLE_SHULKER_BOX) {
            return;
        }

        String displayName = item.getItemMeta().getDisplayName();
        FileConfiguration casesConfig = module.getCasesConfig();
        String caseName = null;

        for (String key : casesConfig.getConfigurationSection("cases").getKeys(false)) {
            String configDisplayName = ChatColor.translateAlternateColorCodes('&', casesConfig.getString("cases." + key + ".displayname", key));
            if (configDisplayName.equals(displayName)) {
                caseName = key;
                break;
            }
        }

        if (caseName == null) {
            player.sendMessage(module.getMessage("error.case-not-found").replace("%case%", ChatColor.stripColor(displayName)));
            return;
        }

        FileConfiguration playerConfig = module.getPlayerConfig();
        String casePath = "player-cases." + player.getUniqueId() + "." + caseName;
        int amount = playerConfig.getInt(casePath, 0);

        if (amount <= 0) {
            player.sendMessage(module.getMessage("error.no-cases").replace("%case%", caseName));
            return;
        }

        // Проверяем, занят ли кейс перед началом анимации
        String locKey = lastClickedShulker.getWorld().getName() + "_" + lastClickedShulker.getBlockX() + "_" +
                lastClickedShulker.getBlockY() + "_" + lastClickedShulker.getBlockZ();
        if (activeCases.containsKey(locKey)) {
            player.sendMessage(ChatColor.RED + "Этот кейс сейчас используется другим игроком!");
            return;
        }

        playerConfig.set(casePath, amount - 1);
        module.savePlayerConfig();

        player.closeInventory();

        ConfigurationSection rewards = casesConfig.getConfigurationSection("cases." + caseName + ".rewards");
        if (rewards == null || rewards.getKeys(false).isEmpty()) {
            player.sendMessage(module.getMessage("error.no-rewards").replace("%case%", caseName));
            return;
        }

        activeAnimations.add(player.getUniqueId());
        activeCases.put(locKey, player.getUniqueId()); // Занимаем кейс только при старте анимации
        CaseAnimation animation = new CaseAnimation(player, lastClickedShulker, rewards, this, caseName, module);
        animation.start();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (activeAnimations.contains(player.getUniqueId()) || displayingWinner.contains(player.getUniqueId())) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                event.setTo(new Location(from.getWorld(), from.getX(), from.getY(), from.getZ(), to.getYaw(), to.getPitch()));
            }
        }
    }

    public void onAnimationStart(Player player) {
        activeAnimations.add(player.getUniqueId());
    }

    public void onAnimationFinish(Player player) {
        activeAnimations.remove(player.getUniqueId());
        displayingWinner.add(player.getUniqueId());
    }

    public void onDisplayFinish(Player player) {
        displayingWinner.remove(player.getUniqueId());
        String locKey = lastClickedShulker.getWorld().getName() + "_" + lastClickedShulker.getBlockX() + "_" +
                lastClickedShulker.getBlockY() + "_" + lastClickedShulker.getBlockZ();
        activeCases.remove(locKey); // Освобождаем кейс после завершения
    }

    private void openCaseMenu(Player player, String caseName) {
        if (activeAnimations.contains(player.getUniqueId()) || displayingWinner.contains(player.getUniqueId())) {
            player.sendMessage(module.getMessage("error.animation-in-progress"));
            return;
        }

        String menuTitle = ChatColor.translateAlternateColorCodes('&', module.getConfig().getString("menu-title", "&5&l✨ Донат Кейсы ✨"));
        Inventory inv = Bukkit.createInventory(null, 54, menuTitle);
        FileConfiguration casesConfig = module.getCasesConfig();
        FileConfiguration config = module.getConfig();
        FileConfiguration playerConfig = module.getPlayerConfig();

        ItemStack border = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.setDisplayName(" ");
        border.setItemMeta(borderMeta);

        ItemStack glowBorder = new ItemStack(Material.MAGENTA_STAINED_GLASS_PANE);
        ItemMeta glowMeta = glowBorder.getItemMeta();
        glowMeta.setDisplayName(" ");
        glowBorder.setItemMeta(glowMeta);

        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || (i + 1) % 9 == 0) {
                inv.setItem(i, (i / 9) % 2 == 0 ? border : glowBorder);
            }
        }

        ConfigurationSection cases = casesConfig.getConfigurationSection("cases");
        if (cases != null && cases.contains(caseName)) {
            int slot = config.getInt("case-slots." + caseName, 22);
            ItemStack caseItem = new ItemStack(Material.PURPLE_SHULKER_BOX);
            ItemMeta meta = caseItem.getItemMeta();
            String displayName = casesConfig.getString("cases." + caseName + ".displayname", caseName);
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));

            int amount = playerConfig.getInt("player-cases." + player.getUniqueId() + "." + caseName, 0);
            List<String> lore = casesConfig.getStringList("cases." + caseName + ".lore");
            if (lore.isEmpty()) {
                lore = new ArrayList<>();
                lore.add(ChatColor.YELLOW + "У вас: " + amount + " кейсов");
                lore.add(ChatColor.GRAY + "Кликните, чтобы открыть!");
            } else {
                lore.replaceAll(line -> ChatColor.translateAlternateColorCodes('&', line.replace("%amount%", String.valueOf(amount))));
            }
            meta.setLore(lore);
            caseItem.setItemMeta(meta);

            inv.setItem(slot, caseItem);
        } else {
            player.sendMessage(module.getMessage("error.case-not-found").replace("%case%", caseName));
            return;
        }

        player.openInventory(inv);
    }
}