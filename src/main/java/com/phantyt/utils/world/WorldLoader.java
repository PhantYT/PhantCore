package com.phantyt.utils.world;

import com.phantyt.PhantCore;
import org.bukkit.World;

public class WorldLoader {
    private final PhantCore plugin;

    public WorldLoader() {
        this.plugin = PhantCore.getInstance();
    }

    public World loadWorld(String name) {
        if (plugin.getServer().getWorld(name) != null) {
            return plugin.getServer().getWorld(name);
        }

        var creator = new org.bukkit.WorldCreator(name);
        World world = creator.createWorld();

        if (world != null) {
            plugin.getLogger().info("Загружен мир: " + name);
        } else {
            plugin.getLogger().warning("Не удалось загрузить мир: " + name);
        }
        return world;
    }
}