package com.phantyt.utils.commands;

import com.phantyt.utils.itemmanager.ItemManagerGive;
import com.phantyt.utils.itemmanager.ItemManagerSave;
import com.phantyt.utils.modules.ItemManagerModule;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ItemManagerCommands implements CommandExecutor, TabCompleter {
    private final ItemManagerModule module;
    private final ItemManagerGive give;
    private final ItemManagerSave itemSaver;

    public ItemManagerCommands(ItemManagerModule module) {
        this.module = module;
        this.give = new ItemManagerGive(module);
        this.itemSaver = module.getItemSaver();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(module.getMessage("only-players"));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("itemmanager.use")) {
            player.sendMessage(module.getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            module.openGUI(player); // Открываем GUI по умолчанию
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                if (args.length < 2) {
                    player.sendMessage(module.getMessage("create-usage"));
                    return true;
                }
                if (!player.hasPermission("itemmanager.create")) {
                    player.sendMessage(module.getMessage("no-permission"));
                    return true;
                }
                String id = args[1];
                module.startItemCreation(player, id);
                break;
            case "give":
                if (args.length < 2) {
                    player.sendMessage(module.getMessage("give-usage"));
                    return true;
                }
                if (!player.hasPermission("itemmanager.give")) {
                    player.sendMessage(module.getMessage("no-permission"));
                    return true;
                }
                String giveId = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                give.giveItem(player, giveId);
                break;
            case "attribute":
                if (args.length < 2) {
                    player.sendMessage(module.getMessage("attribute-usage"));
                    return true;
                }
                if (!player.hasPermission("itemmanager.attribute")) {
                    player.sendMessage(module.getMessage("no-permission"));
                    return true;
                }
                switch (args[1].toLowerCase()) {
                    case "add":
                        if (args.length < 6) {
                            player.sendMessage(module.getMessage("attribute-add-usage"));
                            return true;
                        }
                        String slot = args[2].toUpperCase();
                        String attrId = args[3];
                        try {
                            double amount = Double.parseDouble(args[4]);
                            String type = args[5].toLowerCase();
                            if (!slot.equals("HEAD") && !slot.equals("CHEST") && !slot.equals("LEGS") && !slot.equals("FEET") && !slot.equals("HAND")) {
                                player.sendMessage(module.getMessage("invalid-slot"));
                                return true;
                            }
                            if (!type.equals("damage") && !type.equals("armor")) {
                                player.sendMessage(module.getMessage("invalid-attribute-type"));
                                return true;
                            }
                            if (itemSaver.loadItemById(attrId) == null) {
                                player.sendMessage(module.getMessage("item-not-found", attrId));
                                return true;
                            }
                            itemSaver.addAttribute(attrId, slot, amount, type);
                            player.sendMessage(module.getMessage("attribute-added", attrId, slot, type, String.valueOf(amount)));
                        } catch (NumberFormatException e) {
                            player.sendMessage(module.getMessage("invalid-attribute-amount"));
                        }
                        break;
                    default:
                        player.sendMessage(module.getMessage("attribute-usage"));
                }
                break;
            case "gui":
                module.openGUI(player);
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
            completions.add("create");
            completions.add("give");
            completions.add("attribute");
            completions.add("gui");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("create")) {
                completions.add("<id>");
            } else if (args[0].equalsIgnoreCase("give")) {
                completions.addAll(itemSaver.getItemIds());
            } else if (args[0].equalsIgnoreCase("attribute")) {
                completions.add("add");
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("attribute") && args[1].equalsIgnoreCase("add")) {
            completions.add("HEAD");
            completions.add("CHEST");
            completions.add("LEGS");
            completions.add("FEET");
            completions.add("HAND");
        } else if (args.length == 4 && args[0].equalsIgnoreCase("attribute") && args[1].equalsIgnoreCase("add")) {
            completions.addAll(itemSaver.getItemIds());
        } else if (args.length == 6 && args[0].equalsIgnoreCase("attribute") && args[1].equalsIgnoreCase("add")) {
            completions.add("damage");
            completions.add("armor");
        }
        return completions;
    }
}