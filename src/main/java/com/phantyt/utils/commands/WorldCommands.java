package com.phantyt.utils.commands;

import com.phantyt.PhantCore;
import com.phantyt.utils.AbstractCommand;
import com.phantyt.utils.world.*;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WorldCommands extends AbstractCommand implements TabCompleter {
    private final CustomWorldCreator worldCreator;
    private final WorldLoader worldLoader;
    private final WorldUnloader worldUnloader;
    private final WorldTeleporter worldTeleporter;
    private final WorldDeleter worldDeleter;
    private final WorldList worldList;
    private final WorldImporter worldImporter;
    private final String pluginName = "PhantCore";
    private final FileConfiguration languageConfig;
    private final boolean isModuleEnabled;

    public WorldCommands(PhantCore plugin) {
        super(plugin, "world");
        this.worldCreator = new CustomWorldCreator();
        this.worldLoader = new WorldLoader();
        this.worldUnloader = new WorldUnloader();
        this.worldTeleporter = new WorldTeleporter();
        this.worldDeleter = new WorldDeleter();
        this.worldList = new WorldList();
        this.worldImporter = new WorldImporter(); // Добавлено

        File languageFile = new File(plugin.getDataFolder(), "worldmanager/language.yml");
        if (!languageFile.exists()) {
            plugin.saveResource("worldmanager/language.yml", false);
        }
        this.languageConfig = YamlConfiguration.loadConfiguration(languageFile);

        this.isModuleEnabled = plugin.getConfig().getBoolean("modules.worldmanager", true);

        if (plugin.getCommand("world") != null) {
            plugin.getCommand("world").setExecutor(this);
            plugin.getCommand("world").setTabCompleter(this);
        }
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (!isModuleEnabled) {
            sender.sendMessage(getMessage("module-disabled"));
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "create":
                return handleCreate(sender, args);
            case "load":
                return handleLoad(sender, args);
            case "unload":
                return handleUnload(sender, args);
            case "tp":
                return handleTeleport(sender, args);
            case "delete":
                return handleDelete(sender, args);
            case "list":
                return handleList(sender, args);
            case "import":
                return handleImport(sender, args);
            default:
                sendUnknownCommand(sender, subCommand);
                return true;
        }
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("phantcore.world.create")) {
            sendNoPermission(sender);
            return true;
        }
        if (args.length < 3) {
            sendUsage(sender, "create");
            return true;
        }
        String worldName = args[1];
        try {
            CustomWorldCreator.WorldTemplate template = CustomWorldCreator.WorldTemplate.valueOf(args[2].toUpperCase());
            World world = worldCreator.createWorld(worldName, template);
            if (world != null) {
                sender.sendMessage(getMessage("success.create", worldName));
            } else {
                sender.sendMessage(getMessage("error.create-failed", worldName));
            }
        } catch (IllegalArgumentException e) {
            sender.sendMessage(getMessage("error.invalid-type"));
        }
        return true;
    }

    private boolean handleLoad(CommandSender sender, String[] args) {
        if (!sender.hasPermission("phantcore.world.load")) {
            sendNoPermission(sender);
            return true;
        }
        if (args.length < 2) {
            sendUsage(sender, "load");
            return true;
        }
        String worldName = args[1];
        World world = worldLoader.loadWorld(worldName);
        if (world != null) {
            sender.sendMessage(getMessage("success.load", worldName));
        } else {
            sender.sendMessage(getMessage("error.load-failed", worldName));
        }
        return true;
    }

    private boolean handleUnload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("phantcore.world.unload")) {
            sendNoPermission(sender);
            return true;
        }
        if (args.length < 2) {
            sendUsage(sender, "unload");
            return true;
        }
        String worldName = args[1];
        boolean success = worldUnloader.unloadWorld(worldName, true);
        if (success) {
            sender.sendMessage(getMessage("success.unload", worldName));
        } else {
            World world = plugin.getServer().getWorld(worldName);
            if (world != null && !world.getPlayers().isEmpty()) {
                sender.sendMessage(getMessage("error.unload-players", worldName));
            } else {
                sender.sendMessage(getMessage("error.unload-failed", worldName));
            }
        }
        return true;
    }

    private boolean handleTeleport(CommandSender sender, String[] args) {
        if (!sender.hasPermission("phantcore.world.tp")) {
            sendNoPermission(sender);
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("error.players-only"));
            return true;
        }
        if (args.length < 2) {
            sendUsage(sender, "tp");
            return true;
        }
        Player player = (Player) sender;
        String worldName = args[1];
        boolean success = worldTeleporter.teleportToWorld(player, worldName);
        if (success) {
            sender.sendMessage(getMessage("success.tp", worldName));
        } else {
            sender.sendMessage(getMessage("error.tp-failed", worldName));
        }
        return true;
    }

    private boolean handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("phantcore.world.delete")) {
            sendNoPermission(sender);
            return true;
        }
        if (args.length < 2) {
            sendUsage(sender, "delete");
            return true;
        }
        String worldName = args[1];
        boolean success = worldDeleter.deleteWorld(worldName);
        if (success) {
            sender.sendMessage(getMessage("success.delete", worldName));
        } else {
            World world = plugin.getServer().getWorld(worldName);
            if (world != null && !world.getPlayers().isEmpty()) {
                sender.sendMessage(getMessage("error.delete-players", worldName));
            } else {
                sender.sendMessage(getMessage("error.delete-failed", worldName));
            }
        }
        return true;
    }

    private boolean handleList(CommandSender sender, String[] args) {
        if (!sender.hasPermission("phantcore.world.list")) {
            sendNoPermission(sender);
            return true;
        }
        worldList.showWorldList(sender);
        return true;
    }

    private boolean handleImport(CommandSender sender, String[] args) {
        if (!sender.hasPermission("phantcore.world.import")) {
            sendNoPermission(sender);
            return true;
        }
        if (args.length < 2) {
            sendUsage(sender, "import");
            return true;
        }
        String worldName = args[1];
        World world = worldImporter.importWorld(worldName);
        if (world != null) {
            sender.sendMessage(getMessage("success.import", worldName));
        } else {
            sender.sendMessage(getMessage("error.import-failed", worldName));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!isModuleEnabled) {
            return new ArrayList<>();
        }
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(Arrays.asList("create", "load", "unload", "tp", "delete", "list", "import")); // Добавлено "import"
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "create":
                    completions.add("<world_name>");
                    break;
                case "load":
                case "unload":
                case "tp":
                case "delete":
                case "list":
                case "import": // Добавлено
                    plugin.getServer().getWorlds().forEach(world -> completions.add(world.getName()));
                    break;
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            completions.addAll(Arrays.asList("normal", "nether", "end", "flat", "empty"));
        }
        return completions;
    }

    public void unregisterCommands(PhantCore plugin) {
        if (plugin.getCommand("world") != null) {
            plugin.getCommand("world").setExecutor(null);
            plugin.getCommand("world").setTabCompleter(null);
        } else {
        }
    }

    private String[] getMessage(String path, String... replacements) {
        List<String> messageList = languageConfig.getStringList(path);
        if (messageList.isEmpty()) {
            return new String[]{"", "§cОшибка: сообщение '" + path + "' не найдено в language.yml", ""};
        }
        String[] messageArray = messageList.toArray(new String[0]);
        for (int i = 0; i < messageArray.length; i++) {
            for (int j = 0; j < replacements.length; j++) {
                messageArray[i] = messageArray[i].replace("{" + j + "}", replacements[j]);
            }
        }
        return messageArray;
    }

    private void sendNoPermission(CommandSender sender) {
        sender.sendMessage(getMessage("error.no-permission"));
    }

    private void sendUnknownCommand(CommandSender sender, String subCommand) {
        sender.sendMessage(getMessage("error.unknown-command", subCommand));
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(getMessage("usage.main"));
    }

    private void sendUsage(CommandSender sender, String subCommand) {
        sender.sendMessage(getMessage("usage." + subCommand));
    }
}