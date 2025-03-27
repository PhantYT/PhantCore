package com.phantyt.utils.minimines.listeners;

import com.phantyt.PhantCore;
import com.phantyt.utils.modules.MiniMinesModule;
import com.phantyt.utils.minimines.MineData;
import org.bukkit.scheduler.BukkitRunnable;

public class HologramUpdateScheduler extends BukkitRunnable {
    private final MiniMinesModule module;

    public HologramUpdateScheduler(MiniMinesModule module, PhantCore plugin) {
        this.module = module;
        this.runTaskTimer(plugin, 0L, 1L); // Обновляем каждые 1 тик (0.05 секунды)
    }

    @Override
    public void run() {
        for (MineData mine : module.getMines().values()) {
            if (mine.isRunning()) {
                mine.updateHologram(module);
            }
        }
    }

    public void cancelTask() {
        this.cancel();
    }
}