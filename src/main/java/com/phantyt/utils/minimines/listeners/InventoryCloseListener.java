package com.phantyt.utils.minimines.listeners;

import com.phantyt.utils.modules.MiniMinesModule;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class InventoryCloseListener implements Listener {
    private final MiniMinesModule module;

    public InventoryCloseListener(MiniMinesModule module) {
        this.module = module;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        String title = event.getView().getTitle();
        if (title.equals("§6MiniMines - Шахты")) {
            InventoryClickListener listener = module.getInventoryClickListener();
            if (listener.getPreviousMenu().containsKey(event.getPlayer().getUniqueId())) {
                listener.getPreviousMenu().remove(event.getPlayer().getUniqueId());
            }
        }
    }
}