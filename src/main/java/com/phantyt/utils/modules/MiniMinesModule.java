package com.phantyt.utils.modules;

import com.phantyt.PhantCore;
import com.phantyt.utils.Module;
import com.phantyt.utils.commands.MiniMinesCommands;
import com.phantyt.utils.minimines.MineData;
import com.phantyt.utils.minimines.listeners.*;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class MiniMinesModule implements Module {
    private final PhantCore plugin;
    private FileConfiguration config;
    private FileConfiguration languageConfig;
    public FileConfiguration minesConfig;
    public final Map<String, MineData> mines = new HashMap<>();
    private final Map<UUID, Location> firstPoints = new HashMap<>();
    private final Map<UUID, Location> secondPoints = new HashMap<>();
    private final Map<UUID, String> pendingMines = new HashMap<>();
    public final Map<UUID, Integer> playerPage = new HashMap<>();
    private final List<String> hologramNames = new ArrayList<>();
    public static final int ITEMS_PER_PAGE = 45;
    private InventoryClickListener inventoryClickListener;
    private PlayerInteractListener playerInteractListener;
    private InventoryCloseListener inventoryCloseListener;
    private PlayerChatListener playerChatListener;
    private TaskScheduler taskScheduler;
    private HologramUpdateScheduler hologramUpdateScheduler;
    private MiniMinesCommands miniMinesCommands;

    public MiniMinesModule(PhantCore plugin) {
        this.plugin = plugin;
        initializeListeners();
    }

    private void initializeListeners() {
        this.inventoryClickListener = new InventoryClickListener(this);
        this.playerInteractListener = new PlayerInteractListener(this);
        this.inventoryCloseListener = new InventoryCloseListener(this);
        this.playerChatListener = new PlayerChatListener(this);
        this.taskScheduler = new TaskScheduler(this, plugin);
        this.hologramUpdateScheduler = new HologramUpdateScheduler(this, plugin);
        this.miniMinesCommands = new MiniMinesCommands(this);
    }

    @Override
    public String getName() {
        return "MiniMines";
    }

    @Override
    public void enable(PhantCore plugin) {
        if (inventoryClickListener == null) initializeListeners();

        File configFile = new File(plugin.getDataFolder(), "minimines/config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("minimines/config.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);

        File languageFile = new File(plugin.getDataFolder(), "minimines/language.yml");
        if (!languageFile.exists()) {
            plugin.saveResource("minimines/language.yml", false);
        }
        this.languageConfig = YamlConfiguration.loadConfiguration(languageFile);

        File minesFile = new File(plugin.getDataFolder(), "minimines/mines.yml");
        if (!minesFile.exists()) {
            plugin.saveResource("minimines/mines.yml", false);
        }
        this.minesConfig = YamlConfiguration.loadConfiguration(minesFile);

        for (String hologramName : hologramNames) {
            Hologram hologram = DHAPI.getHologram(hologramName);
            if (hologram != null) {
                hologram.delete();
                plugin.getLogger().warning("Found and removed leftover hologram: " + hologramName);
            }
        }
        hologramNames.clear();

        loadMines();
        plugin.getServer().getPluginManager().registerEvents(playerInteractListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(inventoryClickListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(inventoryCloseListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(playerChatListener, plugin);
        plugin.getCommand("minimines").setExecutor(miniMinesCommands);
        plugin.getCommand("minimines").setTabCompleter(miniMinesCommands);
    }

    @Override
    public void disable(PhantCore plugin) {
        clearAllHolograms();
        HandlerList.unregisterAll(plugin);
        plugin.getLogger().info("Unregistered all event listeners for MiniMines");

        if (plugin.getCommand("minimines") != null) {
            plugin.getCommand("minimines").setExecutor(null);
            plugin.getCommand("minimines").setTabCompleter(null);
            plugin.getLogger().info("Unregistered command executor and tab completer for /minimines");
        }

        if (taskScheduler != null) taskScheduler.cancelTask();
        if (hologramUpdateScheduler != null) hologramUpdateScheduler.cancelTask();
        plugin.getLogger().info("Cancelled TaskScheduler and HologramUpdateScheduler for MiniMines");

        saveMines();

        this.inventoryClickListener = null;
        this.playerInteractListener = null;
        this.inventoryCloseListener = null;
        this.playerChatListener = null;
        this.taskScheduler = null;
        this.hologramUpdateScheduler = null;
        this.miniMinesCommands = null;
        this.config = null;
        this.languageConfig = null;
        this.minesConfig = null;
        this.mines.clear();
        this.firstPoints.clear();
        this.secondPoints.clear();
        this.pendingMines.clear();
        this.hologramNames.clear();
        this.playerPage.clear();
        plugin.getLogger().info("Cleared all MiniMinesModule resources");
    }

    private void clearAllHolograms() {
        for (MineData mine : mines.values()) {
            mine.removeHologram();
            plugin.getLogger().info("Removed hologram for mine via MineData: " + mine.getName());
        }
        for (String hologramName : new ArrayList<>(hologramNames)) {
            Hologram hologram = DHAPI.getHologram(hologramName);
            if (hologram != null) {
                hologram.delete();
                plugin.getLogger().info("Removed hologram via DHAPI: " + hologramName);
            } else {
                plugin.getLogger().warning("Hologram " + hologramName + " was not found in DHAPI during cleanup.");
            }
        }
        hologramNames.clear();
        plugin.getLogger().info("Cleared all hologram names");
    }

    public void addHologramName(String name) {
        if (!hologramNames.contains(name)) {
            hologramNames.add(name);
        }
    }

    public InventoryClickListener getInventoryClickListener() {
        return inventoryClickListener;
    }

    private void loadMines() {
        if (minesConfig.getKeys(false).isEmpty()) {
            return;
        }
        for (String key : minesConfig.getKeys(false)) {
            MineData mine = new MineData(key, minesConfig.getLocation(key + ".point1"), minesConfig.getLocation(key + ".point2"));
            mine.keys = minesConfig.getStringList(key + ".blocks").stream().map(Material::valueOf).collect(Collectors.toList());
            if (minesConfig.getConfigurationSection(key + ".percentages") != null) {
                for (String block : minesConfig.getConfigurationSection(key + ".percentages").getKeys(false)) {
                    mine.percentages.put(Material.valueOf(block), minesConfig.getDouble(key + ".percentages." + block));
                }
            }
            mine.cooldown = minesConfig.getLong(key + ".cooldown");
            mine.teleportPoint = minesConfig.getLocation(key + ".teleport");
            mine.notify = minesConfig.getBoolean(key + ".notify");
            mine.notifyBefore = minesConfig.getLong(key + ".notifyBefore");
            mine.timeLeft = mine.cooldown;
            mine.running = true;
            mine.pvpEnabled = minesConfig.getBoolean(key + ".pvpEnabled", true);
            mine.hologramEnabled = minesConfig.getBoolean(key + ".hologramEnabled", true);
            mine.respawnSound = minesConfig.getString(key + ".respawnSound", "ENTITY_ENDER_DRAGON_GROWL");
            mine.respawnParticle = minesConfig.getString(key + ".respawnParticle", "EXPLOSION_LARGE");
            mine.particleCount = minesConfig.getInt(key + ".particleCount", 50); // Загрузка количества частиц
            mine.soundEnabled = minesConfig.getBoolean(key + ".soundEnabled", true);
            mine.particleEnabled = minesConfig.getBoolean(key + ".particleEnabled", true);
            mine.setHologramLines(minesConfig.getStringList(key + ".hologramLines").isEmpty() ?
                    Arrays.asList("&6Шахта: &e{name}", "&7Время до респавна: &e{TIME} сек", "&7Всего блоков: &e{BLOCKS_ALL}", "&7Осталось блоков: &e{BLOCKS_LOST}") :
                    minesConfig.getStringList(key + ".hologramLines"));
            mine.setHologramYOffset(minesConfig.getDouble(key + ".hologramYOffset", 2.0));
            mines.put(key, mine);

            String hologramName = "minimines_" + key.toLowerCase().replace(" ", "_");
            Hologram existingHologram = DHAPI.getHologram(hologramName);
            if (existingHologram != null) {
                existingHologram.delete();
                plugin.getLogger().warning("Deleted existing hologram during load: " + hologramName);
            }
            mine.createHologram(this);
        }
    }

    public void saveMines() {
        for (String key : mines.keySet()) {
            MineData mine = mines.get(key);
            minesConfig.set(key + ".point1", mine.point1);
            minesConfig.set(key + ".point2", mine.point2);
            minesConfig.set(key + ".blocks", mine.keys.stream().map(Material::name).collect(Collectors.toList()));
            for (Map.Entry<Material, Double> entry : mine.percentages.entrySet()) {
                minesConfig.set(key + ".percentages." + entry.getKey().name(), entry.getValue());
            }
            minesConfig.set(key + ".cooldown", mine.cooldown);
            minesConfig.set(key + ".teleport", mine.teleportPoint);
            minesConfig.set(key + ".notify", mine.notify);
            minesConfig.set(key + ".notifyBefore", mine.notifyBefore);
            minesConfig.set(key + ".pvpEnabled", mine.pvpEnabled);
            minesConfig.set(key + ".hologramEnabled", mine.hologramEnabled);
            minesConfig.set(key + ".respawnSound", mine.respawnSound);
            minesConfig.set(key + ".respawnParticle", mine.respawnParticle);
            minesConfig.set(key + ".particleCount", mine.particleCount); // Сохранение количества частиц
            minesConfig.set(key + ".soundEnabled", mine.soundEnabled);
            minesConfig.set(key + ".particleEnabled", mine.particleEnabled);
            minesConfig.set(key + ".hologramLines", mine.getHologramLines());
            minesConfig.set(key + ".hologramYOffset", mine.getHologramYOffset());
        }
        try {
            minesConfig.save(new File(plugin.getDataFolder(), "minimines/mines.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getMessage(String path, String... replacements) {
        String message = languageConfig.getString(path, "Message not found: " + path);
        for (int i = 0; i < replacements.length; i++) {
            message = message.replace("{" + i + "}", replacements[i]);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public Map<String, MineData> getMines() {
        return mines;
    }

    public PhantCore getPlugin() {
        return plugin;
    }

    public void openMainGUI(Player player) {
        UUID uuid = player.getUniqueId();
        int currentPage = playerPage.getOrDefault(uuid, 0);
        List<String> mineNames = new ArrayList<>(mines.keySet());
        int totalPages = (int) Math.ceil((double) mineNames.size() / ITEMS_PER_PAGE);

        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0) currentPage = 0;
        playerPage.put(uuid, currentPage);

        Inventory gui = Bukkit.createInventory(null, 54, "§6MiniMines - Шахты (стр. " + (currentPage + 1) + "/" + totalPages + ")");

        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, mineNames.size());
        for (int i = startIndex; i < endIndex; i++) {
            String mineName = mineNames.get(i);
            ItemStack item = new ItemStack(Material.DIAMOND_PICKAXE);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§e" + mineName);
            List<String> lore = new ArrayList<>();
            lore.add("§7Кликните для управления");
            meta.setLore(lore);
            item.setItemMeta(meta);
            gui.setItem(i - startIndex, item);
        }

        for (int i = mineNames.size() - startIndex; i < ITEMS_PER_PAGE; i++) {
            gui.setItem(i, createFillerItem());
        }

        for (int i = 45; i < 54; i++) {
            gui.setItem(i, createFillerItem());
        }
        if (currentPage > 0) {
            gui.setItem(45, createItem(Material.ARROW, "§eПредыдущая страница", "§7Перейти на страницу " + currentPage));
        }
        if (currentPage < totalPages - 1) {
            gui.setItem(53, createItem(Material.ARROW, "§eСледующая страница", "§7Перейти на страницу " + (currentPage + 2)));
        }

        player.openInventory(gui);
    }

    public void openMineGUI(Player player, MineData mine) {
        Inventory gui = Bukkit.createInventory(null, 54, "§6MiniMines - " + mine.getName());

        for (int i = 0; i < 18; i++) {
            gui.setItem(i, createFillerItem());
        }
        for (int i = 27; i < 54; i++) {
            if (i > 40) {
                gui.setItem(i, createFillerItem());
            }
        }

        for (int i = 0; i < 9; i++) {
            if (i < mine.keys.size()) {
                Material block = mine.keys.get(i);
                Double percent = mine.percentages.getOrDefault(block, 0.0);
                gui.setItem(18 + i, createItem(block, "§e" + block.name(), "§7Процент: " + percent + "%", "§7Клик - настройки"));
            } else {
                gui.setItem(18 + i, createItem(Material.BARRIER, "§eДобавить блок", "§7Клик - ввести ID блока"));
            }
        }

        gui.setItem(27, createItem(Material.BARRIER, "§cОтключить уведомления", "§7Отключает уведомления", "§7Статус: " + (mine.notify ? "вкл" : "выкл")));
        gui.setItem(28, createItem(Material.CLOCK, "§eУстановить кулдаун", "§7Текущий: " + mine.cooldown + " сек", "§7Клик - изменить"));
        gui.setItem(29, createItem(Material.REDSTONE, "§eРеспавн шахты", "§7Заrespawnить сейчас", "§7Клик - выполнить"));
        gui.setItem(30, createItem(Material.BARRIER, "§cУдалить шахту", "§7Удаляет шахту навсегда", "§7Клик - удалить"));
        gui.setItem(31, createItem(Material.ENDER_PEARL, "§eТелепортироваться", "§7К точке телепорта", "§7Клик - ТП"));
        gui.setItem(32, createItem(Material.COMPASS, "§eУстановить точку ТП", "§7Текущая позиция", "§7Клик - установить"));
        gui.setItem(33, createItem(Material.GREEN_DYE, "§aЗапустить таймер", "§7Начать отсчет", "§7Клик - запуск"));
        gui.setItem(34, createItem(Material.RED_DYE, "§cОстановить таймер", "§7Остановить отсчет", "§7Клик - стоп"));
        gui.setItem(35, createItem(Material.BELL, "§eНастройка уведомлений", "§7Текущий: " + (mine.notify ? mine.notifyBefore + " сек" : "выкл"), "§7Клик - изменить"));
        gui.setItem(36, createItem(Material.DIAMOND_SWORD, "§ePvP: " + (mine.pvpEnabled ? "Вкл" : "Выкл"), "§7Клик - переключить"));
        gui.setItem(37, createItem(Material.BEACON, "§eГолограмма: " + (mine.isHologramEnabled() ? "Вкл" : "Выкл"), "§7Клик - переключить"));
        gui.setItem(38, createItem(Material.NOTE_BLOCK, "§eЗвук респавна: " + mine.respawnSound, "§7Клик - изменить", "§7Статус: " + (mine.soundEnabled ? "Вкл" : "Выкл")));
        gui.setItem(39, createItem(Material.FIREWORK_ROCKET, "§eЧастицы: " + mine.respawnParticle, "§7Клик - изменить", "§7Статус: " + (mine.particleEnabled ? "Вкл" : "Выкл")));
        gui.setItem(40, createItem(Material.GLOWSTONE_DUST, "§eКол-во частиц: " + mine.particleCount, "§7Клик - изменить"));

        player.openInventory(gui);
    }

    public void openBlockGUI(Player player, MineData mine, Material block) {
        Inventory gui = Bukkit.createInventory(null, 9, "§6MiniMines - Блок " + mine.getName() + " в " + block.name());
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, createFillerItem());
        }
        Double percent = mine.percentages.getOrDefault(block, 0.0);
        gui.setItem(2, createItem(Material.PAPER, "§eУстановить процент", "§7Текущий: " + percent + "%", "§7Клик - изменить"));
        gui.setItem(6, createItem(Material.BARRIER, "§cУдалить блок", "§7Удалить из шахты", "§7Клик - удалить"));
        player.openInventory(gui);
    }

    private ItemStack createFillerItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§8 ");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) {
            meta.setLore(Arrays.asList(lore));
        }
        item.setItemMeta(meta);
        return item;
    }

    public void startMineCreation(Player player, String name) {
        UUID uuid = player.getUniqueId();
        firstPoints.put(uuid, null);
        pendingMines.put(uuid, name);
        player.sendMessage(getMessage("select-first-point"));
    }

    public void confirmMine(Player player) {
        UUID uuid = player.getUniqueId();
        if (!firstPoints.containsKey(uuid) || !secondPoints.containsKey(uuid)) {
            player.sendMessage(this.getMessage("points-not-set"));
            return;
        }
        String name = pendingMines.get(uuid);
        MineData mine = new MineData(name, firstPoints.get(uuid), secondPoints.get(uuid));
        mine.running = true;
        mine.pvpEnabled = true;
        mines.put(name, mine);

        createWorldGuardRegion(mine, player);
        mine.createHologram(this);
        saveMines();
        openMineGUI(player, mine);
        clearPending(player);
    }

    private void createWorldGuardRegion(MineData mine, Player player) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(mine.point1.getWorld()));
        if (regions == null) {
            player.sendMessage(getMessage("region-manager-not-found"));
            return;
        }

        BlockVector3 min = BlockVector3.at(
                Math.min(mine.point1.getX(), mine.point2.getX()),
                Math.min(mine.point1.getY(), mine.point2.getY()),
                Math.min(mine.point1.getZ(), mine.point2.getZ())
        );
        BlockVector3 max = BlockVector3.at(
                Math.max(mine.point1.getX(), mine.point2.getX()),
                Math.max(mine.point1.getY(), mine.point2.getY()),
                Math.max(mine.point1.getZ(), mine.point2.getZ())
        );

        String regionId = "minimines_" + mine.getName().toLowerCase().replace(" ", "_");
        ProtectedCuboidRegion region = new ProtectedCuboidRegion(regionId, min, max);

        region.setPriority(999);
        region.setFlag(Flags.BLOCK_BREAK, StateFlag.State.ALLOW);
        region.setFlag(Flags.BLOCK_PLACE, StateFlag.State.DENY);
        region.setFlag(Flags.PVP, mine.pvpEnabled ? StateFlag.State.ALLOW : StateFlag.State.DENY);

        regions.addRegion(region);
        player.sendMessage(getMessage("region-created", regionId));
    }

    private void clearPending(Player player) {
        UUID uuid = player.getUniqueId();
        firstPoints.remove(uuid);
        secondPoints.remove(uuid);
        pendingMines.remove(uuid);
    }

    public Map<UUID, Location> getFirstPoints() {
        return firstPoints;
    }

    public Map<UUID, Location> getSecondPoints() {
        return secondPoints;
    }
}