package com.phantyt.utils.world;

import com.phantyt.utils.world.generators.EmptyWorldGenerator;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

public class CustomWorldCreator {
    public enum WorldTemplate {
        NORMAL,
        NETHER,
        END,
        FLAT,
        EMPTY
    }

    public World createWorld(String name, WorldTemplate template) {
        var creator = new WorldCreator(name);

        switch (template) {
            case NORMAL:
                creator.environment(World.Environment.NORMAL);
                creator.type(WorldType.NORMAL);
                break;
            case NETHER:
                creator.environment(World.Environment.NETHER);
                creator.type(WorldType.NORMAL);
                break;
            case END:
                creator.environment(World.Environment.THE_END);
                creator.type(WorldType.NORMAL);
                break;
            case FLAT:
                creator.environment(World.Environment.NORMAL);
                creator.type(WorldType.FLAT);
                break;
            case EMPTY:
                creator.environment(World.Environment.NORMAL);
                creator.type(WorldType.NORMAL);
                creator.generator(new EmptyWorldGenerator());
                break;
        }

        return Bukkit.createWorld(creator);
    }
}