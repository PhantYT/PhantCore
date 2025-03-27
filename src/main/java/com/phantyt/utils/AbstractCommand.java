package com.phantyt.utils;

import com.phantyt.PhantCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public abstract class AbstractCommand implements CommandExecutor {
    protected final String commandName;
    protected final PhantCore plugin;

    public AbstractCommand(PhantCore plugin, String commandName) {
        this.plugin = plugin;
        this.commandName = commandName;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase(commandName)) {
            return false;
        }
        return execute(sender, args);
    }

    protected abstract boolean execute(CommandSender sender, String[] args);
}