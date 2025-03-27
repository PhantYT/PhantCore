package com.phantyt.utils.modules;

import com.phantyt.PhantCore;
import com.phantyt.utils.Module;
import com.phantyt.utils.commands.ServerStatusCommands;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ServerStatusModule implements Module, Listener {
    private final PhantCore plugin;
    private FileConfiguration config;
    private FileConfiguration languageConfig;
    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private final Map<UUID, Boolean> actionBars = new HashMap<>();
    private final Map<UUID, Boolean> bossBarStates = new HashMap<>(); // Состояние боссбара
    private final Map<UUID, Boolean> actionBarStates = new HashMap<>(); // Состояние актионбара
    private final DecimalFormat decimalFormat = new DecimalFormat("#.##");
    private BukkitRunnable statusTask;

    public ServerStatusModule(PhantCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "ServerStatus";
    }

    @Override
    public void enable(PhantCore plugin) {
        // Загрузка основного конфига
        File configFile = new File(plugin.getDataFolder(), "serverstatus/config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("serverstatus/config.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);

        // Загрузка языкового файла
        File languageFile = new File(plugin.getDataFolder(), "serverstatus/language.yml");
        if (!languageFile.exists()) {
            plugin.saveResource("serverstatus/language.yml", false);
        }
        this.languageConfig = YamlConfiguration.loadConfiguration(languageFile);

        // Регистрация событий и команд
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getCommand("serverstatus").setExecutor(new ServerStatusCommands(this));

        // Восстановление состояния для онлайн-игроков при включении модуля
        for (Player player : Bukkit.getOnlinePlayers()) {
            restorePlayerStatus(player);
        }
    }

    @Override
    public void disable(PhantCore plugin) {
        stopAllBossBars();
        stopAllActionBars();
        if (statusTask != null) {
            statusTask.cancel();
            statusTask = null;
        }
        plugin.getCommand("serverstatus").setExecutor(null);
        org.bukkit.event.HandlerList.unregisterAll(this);
        // Не очищаем bossBarStates и actionBarStates, чтобы сохранить их между перезаходами
    }

    // Событие входа игрока
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        restorePlayerStatus(player);
    }

    // Событие выхода игрока
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        stopBossBar(player); // Убираем боссбар из активных, но сохраняем состояние
        stopActionBar(player); // Убираем актионбар из активных, но сохраняем состояние
    }

    public void startBossBar(Player player) {
        if (bossBars.containsKey(player.getUniqueId())) {
            return;
        }

        BossBar bossBar = Bukkit.createBossBar(
                formatStatus(config.getString("bossbar.format")),
                BarColor.GREEN,
                BarStyle.SEGMENTED_10
        );
        bossBar.addPlayer(player);
        bossBars.put(player.getUniqueId(), bossBar);
        bossBarStates.put(player.getUniqueId(), true); // Сохраняем состояние
        startStatusTask();
    }

    public void startActionBar(Player player) {
        if (actionBars.containsKey(player.getUniqueId())) {
            return;
        }

        actionBars.put(player.getUniqueId(), true);
        actionBarStates.put(player.getUniqueId(), true); // Сохраняем состояние
        startStatusTask();
    }

    public void stopBossBar(Player player) {
        BossBar bossBar = bossBars.remove(player.getUniqueId());
        if (bossBar != null) {
            bossBar.removePlayer(player);
        }
        bossBarStates.put(player.getUniqueId(), false); // Сохраняем состояние
    }

    public void stopActionBar(Player player) {
        actionBars.remove(player.getUniqueId());
        actionBarStates.put(player.getUniqueId(), false); // Сохраняем состояние
    }

    private void stopAllBossBars() {
        for (BossBar bossBar : bossBars.values()) {
            bossBar.removeAll();
        }
        bossBars.clear();
    }

    private void stopAllActionBars() {
        actionBars.clear();
    }

    private void startStatusTask() {
        if (statusTask == null && (!bossBars.isEmpty() || !actionBars.isEmpty())) {
            statusTask = new BukkitRunnable() {
                @Override
                public void run() {
                    updateStatus();
                }
            };
            statusTask.runTaskTimer(plugin, 0L, config.getLong("update-interval", 20L));
        }
    }

    private void updateStatus() {
        double tps = Bukkit.getServer().getTPS()[0];
        double mspt = ((org.bukkit.Server) Bukkit.getServer()).getAverageTickTime(); // Paper API

        String bossBarFormatted = formatStatus(config.getString("bossbar.format"))
                .replace("{TPS}", decimalFormat.format(tps))
                .replace("{MSPT}", decimalFormat.format(mspt));
        for (BossBar bossBar : bossBars.values()) {
            bossBar.setTitle(bossBarFormatted);
            bossBar.setProgress(Math.min(tps / 20.0, 1.0));
            if (tps >= 18.0) {
                bossBar.setColor(BarColor.GREEN);
            } else if (tps >= 15.0) {
                bossBar.setColor(BarColor.YELLOW);
            } else {
                bossBar.setColor(BarColor.RED);
            }
        }

        String actionBarFormatted = formatStatus(config.getString("actionbar.format"))
                .replace("{TPS}", decimalFormat.format(tps))
                .replace("{MSPT}", decimalFormat.format(mspt));
        for (UUID uuid : actionBars.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.sendActionBar(actionBarFormatted);
            } else {
                actionBars.remove(uuid);
            }
        }

        if (bossBars.isEmpty() && actionBars.isEmpty() && statusTask != null) {
            statusTask.cancel();
            statusTask = null;
        }
    }

    private String formatStatus(String format) {
        return format.replace("&", "§");
    }

    public String getMessage(String path, String... replacements) {
        List<String> messageList = languageConfig.getStringList(path);
        if (messageList.isEmpty()) {
            return "Сообщение не найдено: " + path;
        }
        StringBuilder message = new StringBuilder();
        for (String line : messageList) {
            String formattedLine = line;
            for (int i = 0; i < replacements.length; i++) {
                formattedLine = formattedLine.replace("{" + i + "}", replacements[i]);
            }
            message.append(formattedLine).append("\n");
        }
        return message.toString().replace("&", "§").trim();
    }

    // Восстановление состояния игрока из HashMap
    private void restorePlayerStatus(Player player) {
        UUID uuid = player.getUniqueId();
        if (bossBarStates.getOrDefault(uuid, false)) {
            startBossBar(player);
        }
        if (actionBarStates.getOrDefault(uuid, false)) {
            startActionBar(player);
        }
    }
}