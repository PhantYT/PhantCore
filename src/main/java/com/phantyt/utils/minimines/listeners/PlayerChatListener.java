package com.phantyt.utils.minimines.listeners;

import com.phantyt.utils.modules.MiniMinesModule;
import com.phantyt.utils.minimines.MineData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class PlayerChatListener implements Listener {
    private final MiniMinesModule module;

    public PlayerChatListener(MiniMinesModule module) {
        this.module = module;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        InventoryClickListener listener = module.getInventoryClickListener();
        Player player = event.getPlayer();

        if (listener.getPendingCooldown().containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            MineData mine = listener.getPendingCooldown().get(player.getUniqueId());
            try {
                long cooldown = Long.parseLong(event.getMessage());
                if (cooldown <= 0) {
                    player.sendMessage(module.getMessage("invalid-cooldown"));
                    return;
                }
                mine.cooldown = cooldown;
                mine.timeLeft = cooldown;
                module.saveMines();
                player.sendMessage(module.getMessage("cooldown-set", mine.getName(), String.valueOf(cooldown)));
                Bukkit.getScheduler().runTask(module.getPlugin(), () -> module.openMineGUI(player, mine));
            } catch (NumberFormatException e) {
                player.sendMessage(module.getMessage("invalid-cooldown"));
            } finally {
                listener.getPendingCooldown().remove(player.getUniqueId());
            }
        } else if (listener.getPendingNotifyTime().containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            MineData mine = listener.getPendingNotifyTime().get(player.getUniqueId());
            try {
                long notifyTime = Long.parseLong(event.getMessage());
                if (notifyTime <= 0 || notifyTime > mine.cooldown) {
                    player.sendMessage(module.getMessage("invalid-notify-time"));
                    return;
                }
                mine.notify = true;
                mine.notifyBefore = notifyTime;
                module.saveMines();
                player.sendMessage(module.getMessage("notify-set", mine.getName(), String.valueOf(notifyTime)));
                Bukkit.getScheduler().runTask(module.getPlugin(), () -> module.openMineGUI(player, mine));
            } catch (NumberFormatException e) {
                player.sendMessage(module.getMessage("invalid-notify-time"));
            } finally {
                listener.getPendingNotifyTime().remove(player.getUniqueId());
            }
        } else if (listener.getPendingBlockPercent().containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            Material block = listener.getPendingBlockPercent().get(player.getUniqueId());
            MineData mine = module.getMines().values().stream()
                    .filter(m -> m.keys.contains(block))
                    .findFirst()
                    .orElse(null);
            if (mine == null) return;
            try {
                double percent = Double.parseDouble(event.getMessage());
                if (percent < 0 || percent > 100) {
                    player.sendMessage(module.getMessage("invalid-percent"));
                    return;
                }
                mine.percentages.put(block, percent);
                module.saveMines();
                player.sendMessage(module.getMessage("percent-set", block.name(), String.valueOf(percent)));
                Bukkit.getScheduler().runTask(module.getPlugin(), () -> module.openBlockGUI(player, mine, block));
            } catch (NumberFormatException e) {
                player.sendMessage(module.getMessage("invalid-percent"));
            } finally {
                listener.getPendingBlockPercent().remove(player.getUniqueId());
            }
        } else if (listener.getPendingBlockAdd().containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            MineData mine = listener.getPendingBlockAdd().get(player.getUniqueId());
            try {
                Material block = Material.valueOf(event.getMessage().toUpperCase());
                if (!block.isBlock()) {
                    player.sendMessage(module.getMessage("invalid-block"));
                    return;
                }
                mine.keys.add(block);
                mine.percentages.put(block, 0.0);
                module.saveMines();
                player.sendMessage(module.getMessage("block-added", block.name()));
                Bukkit.getScheduler().runTask(module.getPlugin(), () -> module.openMineGUI(player, mine));
            } catch (IllegalArgumentException e) {
                player.sendMessage(module.getMessage("invalid-block"));
            } finally {
                listener.getPendingBlockAdd().remove(player.getUniqueId());
            }
        } else if (listener.getPendingSound().containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            MineData mine = listener.getPendingSound().get(player.getUniqueId());
            String soundName = event.getMessage().toUpperCase();
            try {
                Sound.valueOf(soundName);
                mine.respawnSound = soundName;
                mine.soundEnabled = true;
                module.saveMines();
                player.sendMessage(module.getMessage("sound-set", mine.getName(), soundName));
                Bukkit.getScheduler().runTask(module.getPlugin(), () -> module.openMineGUI(player, mine));
            } catch (IllegalArgumentException e) {
                player.sendMessage(module.getMessage("invalid-sound"));
            } finally {
                listener.getPendingSound().remove(player.getUniqueId());
            }
        } else if (listener.getPendingParticle().containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            MineData mine = listener.getPendingParticle().get(player.getUniqueId());
            String particleName = event.getMessage().toUpperCase();
            try {
                Particle.valueOf(particleName);
                mine.respawnParticle = particleName;
                mine.particleEnabled = true;
                module.saveMines();
                player.sendMessage(module.getMessage("particle-set", mine.getName(), particleName));
                Bukkit.getScheduler().runTask(module.getPlugin(), () -> module.openMineGUI(player, mine));
            } catch (IllegalArgumentException e) {
                player.sendMessage(module.getMessage("invalid-particle"));
            } finally {
                listener.getPendingParticle().remove(player.getUniqueId());
            }
        } else if (listener.getPendingParticleCount().containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            MineData mine = listener.getPendingParticleCount().get(player.getUniqueId());
            try {
                int count = Integer.parseInt(event.getMessage());
                if (count <= 0) {
                    player.sendMessage(module.getMessage("invalid-particle-count"));
                    return;
                }
                mine.setParticleCount(count);
                module.saveMines();
                player.sendMessage(module.getMessage("particle-count-set", mine.getName(), String.valueOf(count)));
                Bukkit.getScheduler().runTask(module.getPlugin(), () -> module.openMineGUI(player, mine));
            } catch (NumberFormatException e) {
                player.sendMessage(module.getMessage("invalid-particle-count"));
            } finally {
                listener.getPendingParticleCount().remove(player.getUniqueId());
            }
        }
    }
}