package com.phantyt.utils.commands;

import com.phantyt.utils.modules.BlockCommandsModule;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BlockCommandsCommands implements CommandExecutor {
    private final BlockCommandsModule module;

    public BlockCommandsCommands(BlockCommandsModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("blockcommands")) return true;

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Только игроки могут использовать эту команду!");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("blockcommands.create")) {
            player.sendMessage(ChatColor.RED + "У вас нет прав!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "Используйте: /blockcommands create");
            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            module.startCreation(player);
            return true;
        }

        return true;
    }

    public void handleChat(Player player, String message) {
        try {
            int radius = Integer.parseInt(message);
            if (radius < 1 || radius > 50) {
                player.sendMessage(ChatColor.RED + "Радиус должен быть от 1 до 50!");
                return;
            }
            module.setRadius(player, radius);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Введите число для радиуса!");
        }
    }
}