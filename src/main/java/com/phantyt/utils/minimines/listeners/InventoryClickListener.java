package com.phantyt.utils.minimines.listeners;

import com.phantyt.utils.modules.MiniMinesModule;
import com.phantyt.utils.minimines.MineData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldedit.bukkit.BukkitAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InventoryClickListener implements Listener {
    private final MiniMinesModule module;
    private final Map<UUID, String> previousMenu = new HashMap<>();
    private final Map<UUID, MineData> pendingCooldown = new HashMap<>();
    private final Map<UUID, MineData> pendingNotifyTime = new HashMap<>();
    private final Map<UUID, MineData> pendingRename = new HashMap<>();
    private final Map<UUID, Material> pendingBlockPercent = new HashMap<>();
    private final Map<UUID, MineData> pendingBlockAdd = new HashMap<>();
    private final Map<UUID, MineData> pendingSound = new HashMap<>();
    private final Map<UUID, MineData> pendingParticle = new HashMap<>();
    private final Map<UUID, MineData> pendingParticleCount = new HashMap<>();

    public InventoryClickListener(MiniMinesModule module) {
        this.module = module;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        UUID uuid = player.getUniqueId();
        String title = event.getView().getTitle();

        if (!title.startsWith("§6MiniMines - ")) {
            return;
        }

        if (event.getClickedInventory() == null || event.getClickedInventory().equals(player.getInventory())) {
            return;
        }

        if (event.getSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }

        if (title.startsWith("§6MiniMines - Шахты")) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType() == Material.AIR) return;

            int slot = event.getSlot();
            if (slot < module.ITEMS_PER_PAGE) {
                String mineName = item.getItemMeta().getDisplayName().substring(2);
                MineData mine = module.getMines().get(mineName);
                if (mine != null) {
                    previousMenu.put(uuid, title);
                    module.openMineGUI(player, mine);
                }
            } else if (slot == 45 && item.getType() == Material.ARROW) {
                int currentPage = module.playerPage.getOrDefault(uuid, 0);
                module.playerPage.put(uuid, currentPage - 1);
                module.openMainGUI(player);
            } else if (slot == 53 && item.getType() == Material.ARROW) {
                int currentPage = module.playerPage.getOrDefault(uuid, 0);
                module.playerPage.put(uuid, currentPage + 1);
                module.openMainGUI(player);
            }
            return;
        }

        if (title.startsWith("§6MiniMines - ") && !title.contains(" - Блок")) {
            int slot = event.getSlot();
            ItemStack clickedItem = event.getCurrentItem();
            String mineName = title.split(" - ")[1];
            MineData mine = module.getMines().get(mineName);

            if (slot >= 18 && slot <= 26) {
                event.setCancelled(true);
                if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

                if (clickedItem.getType() == Material.BARRIER) {
                    pendingBlockAdd.put(uuid, mine);
                    player.closeInventory();
                    player.sendMessage(module.getMessage("enter-block-id"));
                } else if (mine.keys.contains(clickedItem.getType())) {
                    previousMenu.put(uuid, title);
                    module.openBlockGUI(player, mine, clickedItem.getType());
                }
                return;
            }

            event.setCancelled(true);
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            switch (slot) {
                case 27:
                    if (mine.notify) {
                        mine.notify = false;
                        mine.notifyBefore = 0;
                        module.saveMines();
                        player.sendMessage(module.getMessage("notify-disabled", mineName));
                        module.openMineGUI(player, mine);
                    } else {
                        player.sendMessage(module.getMessage("notify-already-disabled", mineName));
                    }
                    break;
                case 28:
                    pendingCooldown.put(uuid, mine);
                    player.closeInventory();
                    player.sendMessage(module.getMessage("enter-cooldown"));
                    break;
                case 29:
                    mine.respawn(module);
                    player.sendMessage(module.getMessage("mine-respawned", mineName));
                    break;
                case 30:
                    // Удаляем голограмму
                    mine.removeHologram();

                    // Удаляем регион WorldGuard
                    RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
                    RegionManager regions = container.get(BukkitAdapter.adapt(mine.point1.getWorld()));
                    if (regions != null) {
                        String regionId = "minimines_" + mineName.toLowerCase().replace(" ", "_");
                        ProtectedRegion region = regions.getRegion(regionId);
                        if (region != null) {
                            regions.removeRegion(regionId);
                        }
                    }

                    // Удаляем шахту из внутренней карты
                    module.getMines().remove(mineName);

                    // Удаляем секцию шахты из minesConfig
                    module.minesConfig.set(mineName, null);

                    // Сохраняем изменения в mines.yml
                    module.saveMines();

                    // Уведомляем игрока и закрываем инвентарь
                    player.sendMessage(module.getMessage("mine-deleted", mineName));
                    player.closeInventory();
                    break;
                case 31:
                    if (mine.getTeleportPoint() != null) {
                        player.teleport(mine.getTeleportPoint());
                        player.sendMessage(module.getMessage("teleported", mineName));
                    } else {
                        player.sendMessage(module.getMessage("no-teleport-point"));
                    }
                    break;
                case 32:
                    mine.setTeleportPoint(player.getLocation());
                    module.saveMines();
                    player.sendMessage(module.getMessage("teleport-set", mineName));
                    break;
                case 33:
                    mine.startTimer(module);
                    player.sendMessage(module.getMessage("timer-started", mineName));
                    break;
                case 34:
                    mine.stopTimer();
                    player.sendMessage(module.getMessage("timer-stopped", mineName));
                    break;
                case 35:
                    pendingNotifyTime.put(uuid, mine);
                    player.closeInventory();
                    player.sendMessage(module.getMessage("enter-notify-time"));
                    break;
                case 36:
                    mine.pvpEnabled = !mine.pvpEnabled;
                    module.saveMines();
                    updateWorldGuardRegionPvp(mine, mine.pvpEnabled);
                    player.sendMessage(module.getMessage("pvp-toggled", mineName, mine.pvpEnabled ? "включено" : "выключено"));
                    module.openMineGUI(player, mine);
                    break;
                case 37:
                    mine.setHologramEnabled(!mine.isHologramEnabled());
                    if (mine.isHologramEnabled()) {
                        mine.createHologram(module);
                    } else {
                        mine.removeHologram();
                    }
                    module.saveMines();
                    player.sendMessage(module.getMessage("hologram-toggled", mineName, mine.isHologramEnabled() ? "включена" : "выключена"));
                    module.openMineGUI(player, mine);
                    break;
                case 38:
                    if (mine.soundEnabled) {
                        mine.soundEnabled = false;
                        module.saveMines();
                        player.sendMessage(module.getMessage("sound-disabled", mineName));
                    } else {
                        pendingSound.put(uuid, mine);
                        player.closeInventory();
                        player.sendMessage(module.getMessage("enter-sound"));
                    }
                    module.openMineGUI(player, mine);
                    break;
                case 39:
                    if (mine.particleEnabled) {
                        mine.particleEnabled = false;
                        module.saveMines();
                        player.sendMessage(module.getMessage("particle-disabled", mineName));
                    } else {
                        pendingParticle.put(uuid, mine);
                        player.closeInventory();
                        player.sendMessage(module.getMessage("enter-particle"));
                    }
                    module.openMineGUI(player, mine);
                    break;
                case 40:
                    pendingParticleCount.put(uuid, mine);
                    player.closeInventory();
                    player.sendMessage(module.getMessage("enter-particle-count"));
                    break;
            }
        } else if (title.startsWith("§6MiniMines - Блок")) {
            event.setCancelled(true);
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            String[] split = title.split(" - Блок ");
            String mineName = split[1].split(" в ")[0];
            Material block = Material.valueOf(split[1].split(" в ")[1]);
            MineData mine = module.getMines().get(mineName);

            switch (event.getSlot()) {
                case 2:
                    pendingBlockPercent.put(uuid, block);
                    player.closeInventory();
                    player.sendMessage(module.getMessage("enter-percent"));
                    break;
                case 6:
                    mine.keys.remove(block);
                    mine.percentages.remove(block);
                    module.saveMines();
                    player.sendMessage(module.getMessage("block-removed", block.name()));
                    module.openMineGUI(player, mine);
                    break;
            }
        }
    }

    private void updateWorldGuardRegionPvp(MineData mine, boolean pvpEnabled) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(mine.point1.getWorld()));
        if (regions == null) return;

        String regionId = "minimines_" + mine.getName().toLowerCase().replace(" ", "_");
        ProtectedRegion region = regions.getRegion(regionId);
        if (region != null) {
            region.setFlag(Flags.PVP, pvpEnabled ? StateFlag.State.ALLOW : StateFlag.State.DENY);
        }
    }

    public Map<UUID, String> getPreviousMenu() {
        return previousMenu;
    }

    public Map<UUID, MineData> getPendingCooldown() {
        return pendingCooldown;
    }

    public Map<UUID, MineData> getPendingNotifyTime() {
        return pendingNotifyTime;
    }

    public Map<UUID, MineData> getPendingRename() {
        return pendingRename;
    }

    public Map<UUID, Material> getPendingBlockPercent() {
        return pendingBlockPercent;
    }

    public Map<UUID, MineData> getPendingBlockAdd() {
        return pendingBlockAdd;
    }

    public Map<UUID, MineData> getPendingSound() {
        return pendingSound;
    }

    public Map<UUID, MineData> getPendingParticle() {
        return pendingParticle;
    }

    public Map<UUID, MineData> getPendingParticleCount() {
        return pendingParticleCount;
    }
}