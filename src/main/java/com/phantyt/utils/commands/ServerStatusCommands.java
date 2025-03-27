package com.phantyt.utils.commands;

import com.phantyt.utils.modules.ServerStatusModule;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ServerStatusCommands implements CommandExecutor, TabCompleter {
    private final ServerStatusModule module;

    public ServerStatusCommands(ServerStatusModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(module.getMessage("only-players"));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("serverstatus.use")) {
            player.sendMessage(module.getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(module.getMessage("usage"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "bossbar":
                if (!player.hasPermission("serverstatus.bossbar")) {
                    player.sendMessage(module.getMessage("no-permission"));
                    return true;
                }
                module.startBossBar(player);
                player.sendMessage(module.getMessage("bossbar-enabled"));
                break;
            case "actionbar":
                if (!player.hasPermission("serverstatus.actionbar")) {
                    player.sendMessage(module.getMessage("no-permission"));
                    return true;
                }
                module.startActionBar(player);
                player.sendMessage(module.getMessage("actionbar-shown"));
                break;
            case "stop":
                if (!player.hasPermission("serverstatus.bossbar") && !player.hasPermission("serverstatus.actionbar")) {
                    player.sendMessage(module.getMessage("no-permission"));
                    return true;
                }
                module.stopBossBar(player);
                module.stopActionBar(player);
                player.sendMessage(module.getMessage("stopped"));
                break;
            default:
                player.sendMessage(module.getMessage("usage"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("serverstatus.bossbar")) {
                completions.add("bossbar");
            }
            if (sender.hasPermission("serverstatus.actionbar")) {
                completions.add("actionbar");
            }
            if (sender.hasPermission("serverstatus.bossbar") || sender.hasPermission("serverstatus.actionbar")) {
                completions.add("stop");
            }
        }
        return completions;
    }
}