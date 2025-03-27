package com.phantyt.utils.commands;

import com.phantyt.utils.modules.RunesModule;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RunesCommands implements CommandExecutor, TabCompleter {
    private final RunesModule module;

    public RunesCommands(RunesModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("runes")) {
            if (args.length == 0) {
                sender.sendMessage(module.getMessage("command-list-header", null));
                sender.sendMessage(module.getMessage("command-list-give", null));
                sender.sendMessage(module.getMessage("command-list-reload", null));
                return true;
            }

            if (args[0].equalsIgnoreCase("give")) {
                if (args.length < 3) {
                    sender.sendMessage(module.getMessage("right-use", null));
                    return true;
                }

                if (!sender.hasPermission("runes.give")) {
                    sender.sendMessage(module.getMessage("permission-needed", null));
                    return true;
                }

                String itemName = args[1].toLowerCase();
                Player targetPlayer = Bukkit.getPlayer(args[2]);
                if (targetPlayer == null) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("player", args[2]);
                    sender.sendMessage(module.getMessage("player-not-found", placeholders));
                    return true;
                }

                if (module.getConfig().contains("items." + itemName)) {
                    ItemStack item = module.createRune(itemName);
                    targetPlayer.getInventory().addItem(item);
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("rune", itemName);
                    placeholders.put("player", targetPlayer.getName());
                    sender.sendMessage(module.getMessage("give-success", placeholders));
                    return true;
                }

                sender.sendMessage(module.getMessage("dont-find", null));
                return true;
            }

            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("runes.reload")) {
                    sender.sendMessage(module.getMessage("permission-needed", null));
                    return true;
                }

                module.enable(module.getPlugin());
                sender.sendMessage(module.getMessage("config-reload", null));
                return true;
            }

            sender.sendMessage(module.getMessage("right-use", null));
            return true;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (cmd.getName().equalsIgnoreCase("runes")) {
            if (!sender.hasPermission("runes.tabcomplete")) {
                return new ArrayList<>();
            }

            List<String> completions = new ArrayList<>();
            if (args.length == 1) {
                completions.add("give");
                completions.add("reload");
                return completions.stream()
                        .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
                return module.getConfig().getConfigurationSection("items").getKeys(false).stream()
                        .map(String::toLowerCase)
                        .filter(item -> item.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return null;
    }
}