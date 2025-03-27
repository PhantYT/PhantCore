package com.phantyt.utils.world;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.File;

public class WorldDeleter {
    public boolean deleteWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);

        // Проверяем наличие игроков, если мир загружен
        if (world != null && !world.getPlayers().isEmpty()) {
            return false; // Нельзя удалить мир с игроками
        }

        // Выгрузка мира перед удалением, если он загружен
        if (world != null) {
            boolean unloaded = Bukkit.unloadWorld(world, true);
            if (!unloaded) {
                return false; // Не удалось выгрузить мир
            }
        }

        // Удаление папки мира
        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        return deleteDirectory(worldFolder);
    }

    private boolean deleteDirectory(File directory) {
        if (!directory.exists()) {
            return true;
        }
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        return directory.delete();
    }
}