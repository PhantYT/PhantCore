package com.phantyt.utils.world;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.io.File;

public class WorldImporter {
    public World importWorld(String worldName) {
        try {
            File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
            if (!worldFolder.exists() || !worldFolder.isDirectory()) {
                return null;
            }

            WorldCreator worldCreator = new WorldCreator(worldName);
            World world = Bukkit.createWorld(worldCreator);
            return world;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}