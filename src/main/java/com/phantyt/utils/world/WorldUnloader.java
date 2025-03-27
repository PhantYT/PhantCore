package com.phantyt.utils.world;

import com.phantyt.PhantCore;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class WorldUnloader {
    private final PhantCore plugin;

    public WorldUnloader() {
        this.plugin = PhantCore.getInstance();
    }

    public boolean unloadWorld(String name, boolean save) {
        World world = plugin.getServer().getWorld(name);
        if (world == null) {
            plugin.getLogger().warning("Мир " + name + " не найден!");
            return false;
        }

        // Проверяем наличие игроков в мире
        if (!world.getPlayers().isEmpty()) {
            plugin.getLogger().warning("Нельзя выгрузить мир " + name + ": в нем находятся игроки!");
            return false;
        }

        boolean success = plugin.getServer().unloadWorld(world, save);
        if (success) {
            plugin.getLogger().info("Мир " + name + " выгружен" + (save ? " с сохранением" : ""));
        } else {
            plugin.getLogger().warning("Не удалось выгрузить мир: " + name);
        }
        return success;
    }
}