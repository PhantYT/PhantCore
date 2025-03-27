package com.phantyt.utils.modules;

import com.phantyt.PhantCore;
import com.phantyt.utils.Module;
import com.phantyt.utils.commands.DonateCaseCommands;
import com.phantyt.utils.donatecase.DonateCaseListener;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DonateCaseModule implements Module {
    private final PhantCore plugin;
    private FileConfiguration config;
    private FileConfiguration casesConfig;
    private FileConfiguration languageConfig;
    private FileConfiguration playerConfig;
    private File playerFile;
    private final Map<String, Hologram> holograms = new HashMap<>();
    private final List<String> hologramNames = new ArrayList<>();
    private DonateCaseCommands commandHandler;

    public DonateCaseModule(PhantCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "DonateCase";
    }

    @Override
    public void enable(PhantCore plugin) {
        File configFile = new File(plugin.getDataFolder(), "donatecase/config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("donatecase/config.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);

        File casesFile = new File(plugin.getDataFolder(), "donatecase/cases.yml");
        if (!casesFile.exists()) {
            plugin.saveResource("donatecase/cases.yml", false);
        }
        this.casesConfig = YamlConfiguration.loadConfiguration(casesFile);

        File languageFile = new File(plugin.getDataFolder(), "donatecase/language.yml");
        if (!languageFile.exists()) {
            plugin.saveResource("donatecase/language.yml", false);
        }
        this.languageConfig = YamlConfiguration.loadConfiguration(languageFile);

        this.playerFile = new File(plugin.getDataFolder(), "donatecase/player.yml");
        if (!playerFile.exists()) {
            try {
                playerFile.getParentFile().mkdirs();
                playerFile.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.playerConfig = YamlConfiguration.loadConfiguration(playerFile);

        commandHandler = new DonateCaseCommands(this);
        PluginCommand donateCaseCommand = plugin.getCommand("donatecase");
        if (donateCaseCommand != null) {
            donateCaseCommand.setExecutor(commandHandler);
            donateCaseCommand.setTabCompleter(commandHandler);
        }

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new DonateCaseListener(this), plugin);

        clearAllHolograms();
        loadHolograms();
    }

    @Override
    public void disable(PhantCore plugin) {
        clearAllHolograms();

        org.bukkit.event.HandlerList.unregisterAll(plugin);

        PluginCommand donateCaseCommand = plugin.getCommand("donatecase");
        if (donateCaseCommand != null) {
            donateCaseCommand.setExecutor(null);
            donateCaseCommand.setTabCompleter(null);
        }

        saveConfig();
        savePlayerConfig();

        this.commandHandler = null;
        this.config = null;
        this.casesConfig = null;
        this.languageConfig = null;
        this.playerConfig = null;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getCasesConfig() {
        return casesConfig;
    }

    public FileConfiguration getLanguageConfig() {
        return languageConfig;
    }

    public FileConfiguration getPlayerConfig() {
        return playerConfig;
    }

    public PhantCore getPlugin() {
        return plugin;
    }

    public void saveConfig() {
        try {
            config.save(new File(plugin.getDataFolder(), "donatecase/config.yml"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveCasesConfig() {
        try {
            casesConfig.save(new File(plugin.getDataFolder(), "donatecase/cases.yml"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void savePlayerConfig() {
        try {
            playerConfig.save(playerFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getMessage(String key) {
        List<String> messages = languageConfig.getStringList(key);
        if (messages.isEmpty()) return "§cСообщение не найдено: " + key;
        return String.join("\n", messages);
    }

    private void loadHolograms() {
        ConfigurationSection locations = config.getConfigurationSection("case-locations");
        if (locations != null) {
            for (String locKey : locations.getKeys(false)) {
                String caseName = locations.getString(locKey);
                String[] parts = locKey.split("_");
                Location baseLoc = new Location(Bukkit.getWorld(parts[0]),
                        Double.parseDouble(parts[1]),
                        Double.parseDouble(parts[2]),
                        Double.parseDouble(parts[3])).add(0.5, 0, 0.5);

                createHologram(caseName, baseLoc);
            }
        }
    }

    public void createHologram(String caseName, Location baseLoc) {
        String holoKey = locKey(baseLoc);

        Hologram existingHologram = DHAPI.getHologram(holoKey);
        if (existingHologram != null) {
            existingHologram.delete();
            hologramNames.remove(holoKey);
        }
        holograms.remove(holoKey);

        double offsetX = casesConfig.getDouble("cases." + caseName + ".hologram-offset.x", 0.0);
        double offsetY = casesConfig.getDouble("cases." + caseName + ".hologram-offset.y", 2.0);
        double offsetZ = casesConfig.getDouble("cases." + caseName + ".hologram-offset.z", 0.0);
        Location holoLoc = baseLoc.clone().add(offsetX, offsetY, offsetZ);

        List<String> lines = casesConfig.getStringList("cases." + caseName + ".hologram");
        if (lines.isEmpty()) {
            lines.add(ChatColor.translateAlternateColorCodes('&', "&5&l" + caseName));
        }

        try {
            Hologram hologram = DHAPI.createHologram(holoKey, holoLoc);
            DHAPI.setHologramLines(hologram, lines);
            holograms.put(holoKey, hologram);
            hologramNames.add(holoKey);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Failed to create hologram at " + holoKey + ": " + e.getMessage());
        }
    }

    public void removeHologram(Location loc) {
        String holoKey = locKey(loc);
        Hologram hologram = holograms.remove(holoKey);
        if (hologram != null) {
            hologram.delete();
            hologramNames.remove(holoKey);
        }
        Hologram existingHologram = DHAPI.getHologram(holoKey);
        if (existingHologram != null) {
            existingHologram.delete();
            hologramNames.remove(holoKey);
        }
    }

    public void restoreHologram(String caseName, Location baseLoc) {
        ConfigurationSection locations = config.getConfigurationSection("case-locations");
        if (locations != null) {
            Location centeredLoc = new Location(
                    baseLoc.getWorld(),
                    baseLoc.getBlockX() + 0.5,
                    baseLoc.getBlockY(),
                    baseLoc.getBlockZ() + 0.5
            );
            String locKey = locKey(centeredLoc);
            if (locations.contains(locKey)) {
                createHologram(caseName, centeredLoc);
            }
        }
    }

    private void clearAllHolograms() {
        for (Hologram hologram : holograms.values()) {
            if (hologram != null) {
                hologram.delete();
            }
        }
        holograms.clear();

        for (String name : new ArrayList<>(hologramNames)) {
            Hologram hologram = DHAPI.getHologram(name);
            if (hologram != null) {
                hologram.delete();
            }
        }
        hologramNames.clear();
    }

    private String locKey(Location loc) {
        return loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
    }

    public void addHologramName(String name) {
        if (!hologramNames.contains(name)) {
            hologramNames.add(name);
        }
    }

    public void removeHologramName(String name) {
        hologramNames.remove(name);
        Hologram hologram = DHAPI.getHologram(name);
        if (hologram != null) {
            hologram.delete();
        }
    }
}