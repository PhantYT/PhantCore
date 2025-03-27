package com.phantyt.utils.world;

import com.phantyt.PhantCore;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WorldList {
    private final PhantCore plugin;

    public WorldList() {
        this.plugin = PhantCore.getInstance();
    }

    public void showWorldList(CommandSender sender) {
        // Получаем список загруженных миров
        List<World> loadedWorlds = plugin.getServer().getWorlds();
        List<String> loadedWorldNames = new ArrayList<>();
        for (World world : loadedWorlds) {
            loadedWorldNames.add(world.getName());
        }

        // Получаем список всех миров (папок в директории сервера)
        File serverFolder = new File(".");
        File[] files = serverFolder.listFiles();
        List<String> allWorlds = new ArrayList<>();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory() && isWorldFolder(file)) {
                    allWorlds.add(file.getName());
                }
            }
        }

        // Отправляем форматированный список
        sender.sendMessage(""); // Пустая строка сверху
        sender.sendMessage(" §x§5§5§F§F§5§5§l⎛ §e§lᴡᴏʀʟᴅ ʟɪsᴛ"); // Заголовок

        // Выводим загруженные миры
        sender.sendMessage(" §x§5§5§F§F§5§5§l⎜ §a§lЗагруженные:");
        for (String worldName : loadedWorldNames) {
            sender.sendMessage(" §x§5§5§F§F§5§5§l⎜ §7- " + worldName + " §a[ON]");
        }

        // Выводим отгруженные миры
        sender.sendMessage(" §x§5§5§F§F§5§5§l⎜ §c§lОтгруженные:");
        for (String worldName : allWorlds) {
            if (!loadedWorldNames.contains(worldName)) {
                sender.sendMessage(" §x§5§5§F§F§5§5§l⎜ §7- " + worldName + " §c[OFF]");
            }
        }

        sender.sendMessage(" §x§5§5§F§F§5§5§l⎝ §fВсего миров: " + allWorlds.size());
        sender.sendMessage(""); // Пустая строка снизу
    }

    private boolean isWorldFolder(File folder) {
        // Проверяем, является ли папка папкой мира (содержит level.dat)
        File levelDat = new File(folder, "level.dat");
        return levelDat.exists();
    }
}