package com.phantyt.utils.commands;

import com.phantyt.utils.modules.PhysicsModule;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PhysicsCommands implements CommandExecutor, TabCompleter {
    private final PhysicsModule module;

    public PhysicsCommands(PhysicsModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("physic")) {
            if (args.length == 0) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(module.getMessage("only-players", null));
                    return true;
                }

                Player player = (Player) sender;
                if (!player.hasPermission("physics.control")) {
                    player.sendMessage(module.getMessage("no-permission", null));
                    return true;
                }

                // Открываем GUI
                module.openPhysicsGUI(player);
                return true;
            }

            // Если есть аргументы, показываем помощь
            sender.sendMessage(module.getMessage("command-list-header", null));
            return true;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (cmd.getName().equalsIgnoreCase("physic")) {
            if (args.length == 1) {
                completions.add("help");
            }
        }
        return completions;
    }
}