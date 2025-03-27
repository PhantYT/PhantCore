package com.phantyt.utils.world;

import com.phantyt.PhantCore;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class WorldTeleporter {
    private final PhantCore plugin;

    public WorldTeleporter() {
        this.plugin = PhantCore.getInstance();
    }

    public boolean teleportToWorld(Player player, String worldName) {
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("Мир " + worldName + " не найден!");
            return false;
        }

        Location spawnLocation = world.getSpawnLocation();
        boolean success = player.teleport(spawnLocation);

        if (success) {
            plugin.getLogger().info("Игрок " + player.getName() + " телепортирован в " + worldName);
        } else {
            plugin.getLogger().warning("Не удалось телепортировать игрока " + player.getName());
        }
        return success;
    }
}