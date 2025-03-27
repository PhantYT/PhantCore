package com.phantyt.utils.modules;

import com.phantyt.PhantCore;
import com.phantyt.utils.Module;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import com.phantyt.utils.commands.PhysicsCommands;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class PhysicsModule implements Module, Listener {
    private final PhantCore plugin;
    private FileConfiguration config;
    private FileConfiguration languageConfig;
    private boolean blockFallingEnabled;
    private boolean plantGrowthEnabled;
    private boolean liquidFlowEnabled;
    private boolean blockSpreadEnabled;
    private boolean leafDecayEnabled;
    private boolean coralDryEnabled;
    private boolean snowMeltEnabled;
    private boolean blockBreakSupportEnabled;
    private boolean concreteHardeningEnabled;

    private static final String INVENTORY_TITLE = ChatColor.translateAlternateColorCodes('&', "&8&lУправление физикой");

    public PhysicsModule(PhantCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "Physics";
    }

    @Override
    public void enable(PhantCore plugin) {
        File configFile = new File(plugin.getDataFolder(), "physics/config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("physics/config.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);

        File languageFile = new File(plugin.getDataFolder(), "physics/language.yml");
        if (!languageFile.exists()) {
            plugin.saveResource("physics/language.yml", false);
        }
        this.languageConfig = YamlConfiguration.loadConfiguration(languageFile);

        blockFallingEnabled = config.getBoolean("settings.block-falling", true);
        plantGrowthEnabled = config.getBoolean("settings.plant-growth", true);
        liquidFlowEnabled = config.getBoolean("settings.liquid-flow", true);
        blockSpreadEnabled = config.getBoolean("settings.block-spread", true);
        leafDecayEnabled = config.getBoolean("settings.leaf-decay", true);
        coralDryEnabled = config.getBoolean("settings.coral-dry", true);
        snowMeltEnabled = config.getBoolean("settings.snow-melt", true);
        blockBreakSupportEnabled = config.getBoolean("settings.block-break-support", true);
        concreteHardeningEnabled = config.getBoolean("settings.concrete-hardening", true);

        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getCommand("physic").setExecutor(new PhysicsCommands(this));
    }

    @Override
    public void disable(PhantCore plugin) {
        saveConfig();
        plugin.getCommand("physic").setExecutor(null);
        org.bukkit.event.HandlerList.unregisterAll(this);
    }

    private void saveConfig() {
        config.set("settings.block-falling", blockFallingEnabled);
        config.set("settings.plant-growth", plantGrowthEnabled);
        config.set("settings.liquid-flow", liquidFlowEnabled);
        config.set("settings.block-spread", blockSpreadEnabled);
        config.set("settings.leaf-decay", leafDecayEnabled);
        config.set("settings.coral-dry", coralDryEnabled);
        config.set("settings.snow-melt", snowMeltEnabled);
        config.set("settings.block-break-support", blockBreakSupportEnabled);
        config.set("settings.concrete-hardening", concreteHardeningEnabled);
        try {
            config.save(new File(plugin.getDataFolder(), "physics/config.yml"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void openPhysicsGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 18, INVENTORY_TITLE);

        gui.setItem(1, createItem("Падение блоков", blockFallingEnabled, Material.SAND));
        gui.setItem(2, createItem("Рост растений", plantGrowthEnabled, Material.WHEAT_SEEDS));
        gui.setItem(3, createItem("Течение жидкостей", liquidFlowEnabled, Material.WATER_BUCKET));
        gui.setItem(4, createItem("Распространение блоков", blockSpreadEnabled, Material.GRASS_BLOCK));
        gui.setItem(5, createItem("Опадение листвы", leafDecayEnabled, Material.OAK_LEAVES));
        gui.setItem(6, createItem("Высыхание кораллов", coralDryEnabled, Material.DEAD_BRAIN_CORAL));
        gui.setItem(7, createItem("Таяние снега", snowMeltEnabled, Material.SNOWBALL));
        gui.setItem(8, createItem("Разрушение поддерживаемых блоков", blockBreakSupportEnabled, Material.LADDER));
        gui.setItem(9, createItem("Затвердевание цемента", concreteHardeningEnabled, Material.GRAY_CONCRETE_POWDER));

        for (int i = 0; i < 18; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, createDecorativePane());
            }
        }

        player.openInventory(gui);
    }

    private ItemStack createDecorativePane() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(ChatColor.RESET + "");
        pane.setItemMeta(meta);
        return pane;
    }

    private ItemStack createItem(String name, boolean enabled, Material material) {
        ItemStack item = new ItemStack(enabled ? material : Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName((enabled ? ChatColor.GREEN : ChatColor.RED) + name);
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Статус: " + (enabled ? "Включено" : "Выключено"),
                ChatColor.YELLOW + "Нажмите, чтобы " + (enabled ? "выключить" : "включить")
        ));
        if (enabled) meta.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(INVENTORY_TITLE)) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        if (!player.hasPermission("physics.control")) return;

        int slot = event.getSlot();
        switch (slot) {
            case 1:
                blockFallingEnabled = !blockFallingEnabled;
                player.sendMessage(getMessage("toggle", new HashMap<String, String>() {{
                    put("feature", "Падение блоков");
                    put("status", blockFallingEnabled ? "включено" : "выключено");
                }}));
                saveConfig();
                break;
            case 2:
                plantGrowthEnabled = !plantGrowthEnabled;
                player.sendMessage(getMessage("toggle", new HashMap<String, String>() {{
                    put("feature", "Рост растений");
                    put("status", plantGrowthEnabled ? "включено" : "выключено");
                }}));
                saveConfig();
                break;
            case 3:
                liquidFlowEnabled = !liquidFlowEnabled;
                player.sendMessage(getMessage("toggle", new HashMap<String, String>() {{
                    put("feature", "Течение жидкостей");
                    put("status", liquidFlowEnabled ? "включено" : "выключено");
                }}));
                saveConfig();
                break;
            case 4:
                blockSpreadEnabled = !blockSpreadEnabled;
                player.sendMessage(getMessage("toggle", new HashMap<String, String>() {{
                    put("feature", "Распространение блоков");
                    put("status", blockSpreadEnabled ? "включено" : "выключено");
                }}));
                saveConfig();
                break;
            case 5:
                leafDecayEnabled = !leafDecayEnabled;
                player.sendMessage(getMessage("toggle", new HashMap<String, String>() {{
                    put("feature", "Опадение листвы");
                    put("status", leafDecayEnabled ? "включено" : "выключено");
                }}));
                saveConfig();
                break;
            case 6:
                coralDryEnabled = !coralDryEnabled;
                player.sendMessage(getMessage("toggle", new HashMap<String, String>() {{
                    put("feature", "Высыхание кораллов");
                    put("status", coralDryEnabled ? "включено" : "выключено");
                }}));
                saveConfig();
                break;
            case 7:
                snowMeltEnabled = !snowMeltEnabled;
                player.sendMessage(getMessage("toggle", new HashMap<String, String>() {{
                    put("feature", "Таяние снега");
                    put("status", snowMeltEnabled ? "включено" : "выключено");
                }}));
                saveConfig();
                break;
            case 8:
                blockBreakSupportEnabled = !blockBreakSupportEnabled;
                player.sendMessage(getMessage("toggle", new HashMap<String, String>() {{
                    put("feature", "Разрушение поддерживаемых блоков");
                    put("status", blockBreakSupportEnabled ? "включено" : "выключено");
                }}));
                saveConfig();
                break;
            case 9:
                concreteHardeningEnabled = !concreteHardeningEnabled;
                player.sendMessage(getMessage("toggle", new HashMap<String, String>() {{
                    put("feature", "Затвердевание цемента");
                    put("status", concreteHardeningEnabled ? "включено" : "выключено");
                }}));
                saveConfig();
                break;
            default:
                return;
        }
        openPhysicsGUI(player);
    }

    @EventHandler
    public void onBlockFall(EntityChangeBlockEvent event) {
        if (!blockFallingEnabled && event.getTo().isBlock()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlantGrowth(BlockGrowEvent event) {
        if (!plantGrowthEnabled) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onLiquidFlow(BlockFromToEvent event) {
        if (!liquidFlowEnabled) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockSpread(BlockSpreadEvent event) {
        if (!blockSpreadEnabled) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onLeafDecay(LeavesDecayEvent event) {
        if (!leafDecayEnabled) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCoralDry(BlockFadeEvent event) {
        if (!coralDryEnabled && event.getBlock().getType().name().contains("CORAL")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSnowMelt(BlockFadeEvent event) {
        if (!snowMeltEnabled && (event.getBlock().getType() == Material.SNOW || event.getBlock().getType() == Material.SNOW_BLOCK)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onConcreteHarden(BlockPhysicsEvent event) {
        if (!concreteHardeningEnabled) {
            Block block = event.getBlock();
            if (block.getType().name().contains("CONCRETE_POWDER")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (blockBreakSupportEnabled) return;

        Block block = event.getBlock();
        if (isSupportDependentBlock(block.getType())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        if (blockBreakSupportEnabled) return;

        Block block = event.getBlock();
        checkAdjacentBlocks(block);
    }

    private void checkAdjacentBlocks(Block block) {
        for (BlockFace face : new BlockFace[]{
                BlockFace.NORTH,
                BlockFace.EAST,
                BlockFace.SOUTH,
                BlockFace.WEST,
                BlockFace.UP,
                BlockFace.DOWN
        }) {
            Block relative = block.getRelative(face);
            if (isSupportDependentBlock(relative.getType())) {
                relative.getState().update(true, false);
            }
        }
    }

    private boolean isSupportDependentBlock(Material material) {
        return material.name().contains("LADDER") ||
                material.name().contains("CARPET") ||
                material.name().contains("RAIL") ||
                material == Material.TORCH ||
                material == Material.REDSTONE_TORCH ||
                material.name().contains("SIGN") ||
                material.name().contains("BANNER") ||
                material.name().contains("BUTTON") ||
                material.name().contains("PRESSURE_PLATE") ||
                material == Material.VINE ||
                material.name().contains("TRAPDOOR") ||
                material.name().contains("DOOR") ||
                material.name().contains("FENCE_GATE");
    }

    public String getMessage(String key, Map<String, String> placeholders) {
        String message = languageConfig.getString("messages." + key, "Сообщение не найдено!");
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}