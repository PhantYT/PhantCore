package com.phantyt.utils.modules;

import com.phantyt.PhantCore;
import com.phantyt.utils.Module;
import com.phantyt.utils.commands.WorldCommands;
import com.phantyt.utils.world.WorldLoader;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;

public class WorldManagerModule implements Module {
    private WorldCommands worldCommands;

    @Override
    public String getName() {
        return "WorldManager";
    }

    @Override
    public void enable(PhantCore plugin) {
        File configFile = new File(plugin.getDataFolder(), "worldmanager/config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("worldmanager/config.yml", false); // Сохраняем только если файла нет
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        // Автозагрузка миров
        if (config.getBoolean("autoload.enabled", false)) {
            List<String> worldsToLoad = config.getStringList("autoload.worlds");
            WorldLoader worldLoader = new WorldLoader();
            for (String worldName : worldsToLoad) {
                if (worldLoader.loadWorld(worldName) != null) {
                } else {
                }
            }
        }

        this.worldCommands = new WorldCommands(plugin);
    }

    @Override
    public void disable(PhantCore plugin) {
        if (worldCommands != null) {
            worldCommands.unregisterCommands(plugin);
        } else {
        }
    }
}