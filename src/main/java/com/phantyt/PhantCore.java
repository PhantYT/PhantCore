package com.phantyt;

import com.phantyt.utils.ModuleManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class PhantCore extends JavaPlugin {
    private static PhantCore instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Подключение конкретной реализации команд
        new ModuleManager(this);

        getLogger().info("PhantCore успешно запущен!");
    }

    @Override
    public void onDisable() {
    }

    public static PhantCore getInstance() {
        return instance;
    }
}