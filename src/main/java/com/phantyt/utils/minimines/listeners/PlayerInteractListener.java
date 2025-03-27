package com.phantyt.utils.minimines.listeners;

import com.phantyt.utils.modules.MiniMinesModule;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class PlayerInteractListener implements Listener {
    private final MiniMinesModule module;

    public PlayerInteractListener(MiniMinesModule module) {
        this.module = module;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().toString().contains("CLICK_BLOCK")) return;

        Location location = event.getClickedBlock().getLocation();
        if (module.getFirstPoints().containsKey(event.getPlayer().getUniqueId())) {
            if (module.getFirstPoints().get(event.getPlayer().getUniqueId()) == null) {
                module.getFirstPoints().put(event.getPlayer().getUniqueId(), location);
                event.getPlayer().sendMessage(module.getMessage("first-point-set"));
                event.getPlayer().sendMessage(module.getMessage("select-second-point"));
                event.setCancelled(true);
            } else if (module.getSecondPoints().get(event.getPlayer().getUniqueId()) == null) {
                module.getSecondPoints().put(event.getPlayer().getUniqueId(), location);
                event.getPlayer().sendMessage(module.getMessage("second-point-set"));
                event.getPlayer().sendMessage(module.getMessage("confirm-mine"));
                event.setCancelled(true);
            }
        }
    }
}