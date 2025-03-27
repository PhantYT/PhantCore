package com.phantyt.utils.commands;

import com.phantyt.utils.modules.MiniMinesModule;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MiniMinesCommands implements CommandExecutor, TabCompleter {
    private final MiniMinesModule module;

    public MiniMinesCommands(MiniMinesModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(module.getMessage("only-players"));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("minimines.use")) {
            player.sendMessage(module.getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(module.getMessage("usage"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                if (args.length < 2) {
                    player.sendMessage(module.getMessage("create-usage"));
                    return true;
                }
                module.startMineCreation(player, args[1]);
                break;
            case "confirm":
                module.confirmMine(player);
                break;
            case "gui":
                module.openMainGUI(player);
                break;
            default:
                player.sendMessage(module.getMessage("usage"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player) || !sender.hasPermission("minimines.use")) {
            return new ArrayList<>();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Предлагаем основные подкоманды
            completions.addAll(Arrays.asList("create", "confirm", "gui"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
            // Для "create" можно предложить пример имени шахты, но обычно имя вводится вручную
            completions.add("<mine_name>");
        }

        // Фильтруем варианты по введённому тексту
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}