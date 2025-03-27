package com.phantyt.utils.minimines.listeners;

import com.phantyt.PhantCore;
import com.phantyt.utils.modules.MiniMinesModule;
import com.phantyt.utils.minimines.MineData;
import org.bukkit.scheduler.BukkitRunnable;

public class TaskScheduler extends BukkitRunnable {
    private final MiniMinesModule module;

    public TaskScheduler(MiniMinesModule module, PhantCore plugin) {
        this.module = module;
        this.runTaskTimer(plugin, 0L, 20L); // Обновляем каждые 20 тиков (1 секунда)
    }

    @Override
    public void run() {
        for (MineData mine : module.getMines().values()) {
            if (mine.isRunning()) {
                mine.tick(module);
            }
        }
    }

    public void cancelTask() {
        this.cancel();
    }
}