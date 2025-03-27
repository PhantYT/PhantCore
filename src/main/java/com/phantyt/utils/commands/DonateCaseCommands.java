package com.phantyt.utils.commands;

import com.phantyt.utils.modules.DonateCaseModule;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class DonateCaseCommands implements CommandExecutor, TabCompleter {
    private final DonateCaseModule module;

    public DonateCaseCommands(DonateCaseModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(module.getMessage("error.player-only"));
            return true;
        }

        Player player = (Player) sender;
        FileConfiguration config = module.getConfig();
        FileConfiguration casesConfig = module.getCasesConfig();

        if (args.length == 0) {
            player.sendMessage(module.getMessage("error.invalid-usage"));
            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            if (!player.hasPermission("donatecase.create")) {
                player.sendMessage(module.getMessage("error.no-permission"));
                return true;
            }

            if (args.length < 3) {
                player.sendMessage(module.getMessage("error.create-usage"));
                return true;
            }

            String caseName = args[1];
            if (!casesConfig.contains("cases." + caseName)) {
                player.sendMessage(module.getMessage("error.case-not-found").replace("%case%", caseName));
                return true;
            }

            int slot;
            try {
                slot = Integer.parseInt(args[2]);
                if (slot < 0 || slot >= 54) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                player.sendMessage(module.getMessage("error.invalid-slot"));
                return true;
            }

            Block targetBlock = player.getTargetBlock(null, 5);
            if (targetBlock == null || targetBlock.getType() == Material.AIR) {
                player.sendMessage(module.getMessage("error.no-block"));
                return true;
            }

            targetBlock.setType(Material.PURPLE_SHULKER_BOX);
            Location loc = targetBlock.getLocation();
            String locKey = loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
            config.set("case-locations." + locKey, caseName);
            config.set("case-slots." + caseName, slot);
            module.saveConfig();
            module.createHologram(caseName, loc.clone().add(0.5, 0, 0.5));
            player.sendMessage(module.getMessage("success.case-created").replace("%case%", caseName).replace("%slot%", String.valueOf(slot)));
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            if (!player.hasPermission("donatecase.give")) {
                player.sendMessage(module.getMessage("error.no-permission"));
                return true;
            }

            if (args.length < 4) {
                player.sendMessage(module.getMessage("error.give-usage"));
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(module.getMessage("error.player-not-found").replace("%player%", args[1]));
                return true;
            }

            String caseName = args[2];
            if (!casesConfig.contains("cases." + caseName)) {
                player.sendMessage(module.getMessage("error.case-not-found").replace("%case%", caseName));
                return true;
            }

            int amount;
            try {
                amount = Integer.parseInt(args[3]);
                if (amount <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                player.sendMessage(module.getMessage("error.invalid-amount"));
                return true;
            }

            FileConfiguration playerConfig = module.getPlayerConfig();
            int currentAmount = playerConfig.getInt("player-cases." + target.getUniqueId() + "." + caseName, 0);
            playerConfig.set("player-cases." + target.getUniqueId() + "." + caseName, currentAmount + amount);
            module.savePlayerConfig();
            player.sendMessage(module.getMessage("success.give-success").replace("%amount%", String.valueOf(amount)).replace("%case%", caseName).replace("%player%", target.getName()));
            target.sendMessage(module.getMessage("success.receive-success").replace("%amount%", String.valueOf(amount)).replace("%case%", caseName));
            return true;
        }

        player.sendMessage(module.getMessage("error.invalid-usage"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("create");
            completions.add("give");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
            FileConfiguration casesConfig = module.getCasesConfig();
            completions.addAll(casesConfig.getConfigurationSection("cases").getKeys(false));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            FileConfiguration casesConfig = module.getCasesConfig();
            completions.addAll(casesConfig.getConfigurationSection("cases").getKeys(false));
        }
        return completions;
    }
}