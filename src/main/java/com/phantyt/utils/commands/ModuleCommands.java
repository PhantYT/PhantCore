package com.phantyt.utils.commands;

import com.phantyt.PhantCore;
import com.phantyt.utils.ModuleManager;
import com.phantyt.utils.Module;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ModuleCommands implements CommandExecutor, TabCompleter {
    private final ModuleManager moduleManager;
    private final PhantCore plugin;
    private FileConfiguration languageConfig;

    public ModuleCommands(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
        this.plugin = PhantCore.getInstance();

        File languageFile = new File(plugin.getDataFolder(), "language.yml");
        if (!languageFile.exists()) {
            plugin.saveResource("language.yml", false);
        }
        this.languageConfig = YamlConfiguration.loadConfiguration(languageFile);

        if (plugin.getCommand("phantcore") != null) {
            plugin.getCommand("phantcore").setExecutor(this);
            plugin.getCommand("phantcore").setTabCompleter(this);
        } else {
            plugin.getLogger().severe("Команда 'phantcore' не найдена в plugin.yml!");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("phantcore.admin")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(getMessage("usage"));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            reloadAll();
            sender.sendMessage(getMessage("reload-all"));
            return true;
        }

        if (args[0].equalsIgnoreCase("module")) {
            if (args.length < 3) {
                sender.sendMessage(getMessage("usage-module"));
                return true;
            }

            String moduleName = args[1].toLowerCase();
            String action = args[2].toLowerCase();

            switch (action) {
                case "reload":
                    if (reloadModule(moduleName)) {
                        sender.sendMessage(getMessage("module-reloaded", moduleName));
                    } else {
                        sender.sendMessage(getMessage("module-not-found-or-disabled", moduleName));
                    }
                    break;
                case "on":
                    if (enableModule(moduleName)) {
                        sender.sendMessage(getMessage("module-enabled", moduleName));
                    } else {
                        sender.sendMessage(getMessage("module-not-found-or-enabled", moduleName));
                    }
                    break;
                case "off":
                    if (disableModule(moduleName)) {
                        sender.sendMessage(getMessage("module-disabled", moduleName));
                    } else {
                        sender.sendMessage(getMessage("module-not-found-or-disabled-already", moduleName));
                    }
                    break;
                default:
                    sender.sendMessage(getMessage("unknown-action", action));
                    break;
            }
            return true;
        }

        sender.sendMessage(getMessage("usage"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("phantcore.admin")) {
            return new ArrayList<>();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("reload");
            completions.add("module");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("module")) {
            completions.addAll(getModuleNames());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("module")) {
            completions.add("reload");
            completions.add("on");
            completions.add("off");
        }

        return filterCompletions(completions, args[args.length - 1]);
    }

    private List<String> filterCompletions(List<String> completions, String input) {
        List<String> filtered = new ArrayList<>();
        for (String completion : completions) {
            if (completion.toLowerCase().startsWith(input.toLowerCase())) {
                filtered.add(completion);
            }
        }
        return filtered;
    }

    private void reloadAll() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        plugin.reloadConfig(); // Перезагружаем основной конфиг
        reloadLanguageConfig();
        reloadAllModules();
    }

    private void reloadAllModules() {
        FileConfiguration config = plugin.getConfig();
        for (Module module : moduleManager.getModules()) {
            String moduleName = module.getName().toLowerCase();
            boolean isEnabled = config.getBoolean("modules." + moduleName, true);
            if (isEnabled) {
                module.enable(plugin);
                plugin.getLogger().info(getMessage("module-reloaded", moduleName));
            } else {
                module.disable(plugin); // Отключаем модуль, если в конфиге false
                plugin.getLogger().info(getMessage("module-disabled", moduleName));
            }
        }
    }

    private boolean reloadModule(String moduleName) {
        Module module = findModule(moduleName);
        if (module == null) {
            return false;
        }
        FileConfiguration config = plugin.getConfig();
        if (config.getBoolean("modules." + moduleName.toLowerCase(), true)) {
            module.enable(plugin);
            plugin.getLogger().info(getMessage("module-reloaded", moduleName));
            return true;
        } else {
            module.disable(plugin); // Отключаем модуль при reload, если он выключен в конфиге
            plugin.getLogger().info(getMessage("module-disabled", moduleName));
            return true;
        }
    }

    private boolean enableModule(String moduleName) {
        Module module = findModule(moduleName);
        if (module == null) {
            return false;
        }
        FileConfiguration config = plugin.getConfig();
        String configKey = "modules." + moduleName.toLowerCase();
        if (!config.getBoolean(configKey, true)) {
            config.set(configKey, true);
            saveConfig();
            module.enable(plugin);
            plugin.getLogger().info(getMessage("module-enabled", moduleName));
            return true;
        }
        return false;
    }

    private boolean disableModule(String moduleName) {
        Module module = findModule(moduleName);
        if (module == null) {
            return false;
        }
        FileConfiguration config = plugin.getConfig();
        String configKey = "modules." + moduleName.toLowerCase();
        if (config.getBoolean(configKey, true)) {
            config.set(configKey, false);
            saveConfig();
            module.disable(plugin);
            plugin.getLogger().info(getMessage("module-disabled", moduleName));
            return true;
        }
        return false;
    }

    private Module findModule(String name) {
        for (Module module : moduleManager.getModules()) {
            if (module.getName().equalsIgnoreCase(name)) {
                return module;
            }
        }
        return null;
    }

    private void saveConfig() {
        try {
            plugin.getConfig().save(new File(plugin.getDataFolder(), "config.yml"));
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить config.yml: " + e.getMessage());
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

    private List<String> getModuleNames() {
        List<String> names = new ArrayList<>();
        for (Module module : moduleManager.getModules()) {
            names.add(module.getName().toLowerCase());
        }
        return names;
    }

    private void reloadLanguageConfig() {
        File languageFile = new File(plugin.getDataFolder(), "language.yml");
        if (!languageFile.exists()) {
            plugin.saveResource("language.yml", false);
        }
        this.languageConfig = YamlConfiguration.loadConfiguration(languageFile);
    }
}