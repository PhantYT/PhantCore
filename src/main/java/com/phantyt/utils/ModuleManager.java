package com.phantyt.utils;

import com.phantyt.PhantCore;
import com.phantyt.utils.commands.ModuleCommands;
import com.phantyt.utils.modules.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    private final PhantCore plugin;
    private final FileConfiguration languageConfig;
    private final List<Module> modules = new ArrayList<>();

    public ModuleManager(PhantCore plugin) {
        this.plugin = plugin;

        File languageFile = new File(plugin.getDataFolder(), "language.yml");
        if (!languageFile.exists()) {
            plugin.saveResource("language.yml", false); // Сохраняем только если файла нет
        }
        this.languageConfig = YamlConfiguration.loadConfiguration(languageFile);

        new ModuleCommands(this);

        registerModules();
        loadModules();
    }

    private void registerModules() {
        modules.add(new PhysicsModule(plugin));
        modules.add(new WorldManagerModule());
        modules.add(new ShulkerOpenModule(plugin));
        modules.add(new HotSwapModule(plugin));
        modules.add(new ItemManagerModule(plugin));
        modules.add(new ServerStatusModule(plugin));
        modules.add(new RunesModule(plugin));
        modules.add(new DonateCaseModule(plugin));
        modules.add(new BlockParticlesModule(plugin));
        modules.add(new BlockCommandsModule(plugin));
        modules.add(new ErrorsDeleteModule(plugin));
        modules.add(new MiniMinesModule(plugin));
    }


    private void loadModules() {
        for (Module module : modules) {
            String moduleName = module.getName().toLowerCase();
            boolean isEnabled = plugin.getConfig().getBoolean("modules." + moduleName, true);
            if (isEnabled) {
                module.enable(plugin);
                plugin.getLogger().info(getMessage("module-enabled", moduleName));
            } else {
                plugin.getLogger().info(getMessage("module-disabled", moduleName));
            }
        }
    }

    private String getMessage(String path, String... replacements) {
        List<String> messageList = languageConfig.getStringList(path);
        if (messageList.isEmpty()) {
            return "Ошибка: сообщение '" + path + "' не найдено в language.yml";
        }
        StringBuilder message = new StringBuilder();
        for (String line : messageList) {
            String formattedLine = line;
            for (int i = 0; i < replacements.length; i++) {
                formattedLine = formattedLine.replace("{" + i + "}", replacements[i]);
            }
            message.append(formattedLine).append("\n");
        }
        return message.toString().trim();
    }

    public List<Module> getModules() {
        return modules;
    }
}