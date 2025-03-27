package com.phantyt.utils.minimines;

import com.phantyt.utils.modules.MiniMinesModule;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.Particle;
import org.bukkit.Sound;

import java.util.*;

public class MineData {
    public String name;
    public Location point1;
    public Location point2;
    public List<Material> keys = new ArrayList<>();
    public Map<Material, Double> percentages = new HashMap<>();
    public long cooldown;
    public Location teleportPoint;
    public boolean notify;
    public long notifyBefore;
    public long timeLeft;
    public boolean running;
    public boolean pvpEnabled;
    public boolean hologramEnabled;
    public String respawnSound = "ENTITY_ENDER_DRAGON_GROWL";
    public String respawnParticle = "EXPLOSION_LARGE";
    public int particleCount = 50; // Количество частиц по умолчанию
    public boolean soundEnabled = true;
    public boolean particleEnabled = true;
    private Hologram hologram;
    private List<String> hologramLines;
    private double hologramYOffset;

    public MineData(String name, Location point1, Location point2) {
        this.name = name;
        this.point1 = point1;
        this.point2 = point2;
        this.hologramEnabled = true;
        this.hologramLines = new ArrayList<>(Arrays.asList(
                "&6Шахта: &e{name}",
                "&7Время до респавна: &e{TIME} сек",
                "&7Всего блоков: &e{BLOCKS_ALL}",
                "&7Осталось блоков: &e{BLOCKS_LOST}"
        ));
        this.hologramYOffset = 2.0;
    }

    public void respawn(MiniMinesModule module) {
        if (point1 == null || point2 == null) {
            Bukkit.broadcastMessage(module.getMessage("points-not-set", name));
            return;
        }
        if (keys.isEmpty()) {
            Bukkit.broadcastMessage(module.getMessage("no-blocks", name));
            return;
        }
        int minX = Math.min(point1.getBlockX(), point2.getBlockX());
        int maxX = Math.max(point1.getBlockX(), point2.getBlockX());
        int minY = Math.min(point1.getBlockY(), point2.getBlockY());
        int maxY = Math.max(point1.getBlockY(), point2.getBlockY());
        int minZ = Math.min(point1.getBlockZ(), point2.getBlockZ());
        int maxZ = Math.max(point1.getBlockZ(), point2.getBlockZ());

        World world = point1.getWorld();
        Random random = new Random();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Material block = chooseBlock(random);
                    world.getBlockAt(x, y, z).setType(block);
                }
            }
        }

        // Воспроизведение звука и частиц от голограммы
        if (soundEnabled) {
            try {
                Sound sound = Sound.valueOf(respawnSound);
                world.playSound(new Location(world, (minX + maxX) / 2.0, maxY + 1, (minZ + maxZ) / 2.0), sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("Invalid sound name for mine " + name + ": " + respawnSound);
            }
        }

        if (particleEnabled && hologram != null) {
            try {
                Particle particle = Particle.valueOf(respawnParticle);
                spawnFireworkParticles(world, hologram.getLocation(), particle);
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("Invalid particle name for mine " + name + ": " + respawnParticle);
            }
        }

        timeLeft = cooldown;
    }

    private void spawnFireworkParticles(World world, Location hologramLocation, Particle particle) {
        // Эффект салюта: частицы разлетаются от голограммы
        world.spawnParticle(particle, hologramLocation, particleCount, 0.5, 0.5, 0.5, 0.1);
    }

    private Material chooseBlock(Random random) {
        double total = percentages.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total == 0) return keys.get(random.nextInt(keys.size()));
        double roll = random.nextDouble() * total;
        double current = 0;
        for (Map.Entry<Material, Double> entry : percentages.entrySet()) {
            current += entry.getValue();
            if (roll <= current) return entry.getKey();
        }
        return keys.get(0);
    }

    public void startTimer(MiniMinesModule module) {
        if (keys.isEmpty()) {
            Bukkit.broadcastMessage(module.getMessage("no-blocks", name));
            return;
        }
        running = true;
    }

    public void stopTimer() {
        running = false;
    }

    public void tick(MiniMinesModule module) {
        if (!running) return;
        if (point1 == null || point2 == null) {
            Bukkit.broadcastMessage(module.getMessage("points-not-set", name));
            running = false;
            return;
        }
        if (timeLeft > 0) {
            timeLeft--;
            if (notify && timeLeft == notifyBefore) {
                Bukkit.broadcastMessage(module.getMessage("notify", name, String.valueOf(notifyBefore)));
            }
            if (timeLeft == 0) {
                respawn(module);
            }
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void setTeleportPoint(Location loc) {
        teleportPoint = loc;
    }

    public Location getTeleportPoint() {
        return teleportPoint;
    }

    public String getName() {
        return name;
    }

    public void createHologram(MiniMinesModule module) {
        if (point1 == null || point2 == null) return;
        if (!hologramEnabled) return;

        removeHologram();

        double centerX = (point1.getX() + point2.getX()) / 2.0 + 0.5;
        double centerZ = (point1.getZ() + point2.getZ()) / 2.0 + 0.5;
        double maxY = Math.max(point1.getY(), point2.getY()) + 0.5;
        Location hologramLocation = new Location(point1.getWorld(), centerX, maxY + hologramYOffset, centerZ);

        String hologramName = "minimines_" + name.toLowerCase().replace(" ", "_");
        hologram = DHAPI.createHologram(hologramName, hologramLocation, false, hologramLines);
        module.addHologramName(hologramName);
    }

    public void updateHologram(MiniMinesModule module) {
        if (!hologramEnabled || hologram == null) return;

        List<String> updatedLines = new ArrayList<>();
        for (String line : hologramLines) {
            line = line.replace("{name}", name)
                    .replace("{TIME}", String.valueOf(timeLeft))
                    .replace("{BLOCKS_ALL}", String.valueOf(getTotalBlocks()))
                    .replace("{BLOCKS_LOST}", String.valueOf(getRemainingBlocks()));
            updatedLines.add(line);
        }
        DHAPI.setHologramLines(hologram, updatedLines);
    }

    public void removeHologram() {
        if (hologram != null) {
            String hologramName = "minimines_" + name.toLowerCase().replace(" ", "_");
            DHAPI.removeHologram(hologramName);
            hologram = null;
        }
    }

    public int getTotalBlocks() {
        if (point1 == null || point2 == null) return 0;
        int minX = Math.min(point1.getBlockX(), point2.getBlockX());
        int maxX = Math.max(point1.getBlockX(), point2.getBlockX());
        int minY = Math.min(point1.getBlockY(), point2.getBlockY());
        int maxY = Math.max(point1.getBlockY(), point2.getBlockY());
        int minZ = Math.min(point1.getBlockZ(), point2.getBlockZ());
        int maxZ = Math.max(point1.getBlockZ(), point2.getBlockZ());

        return (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    public int getRemainingBlocks() {
        if (point1 == null || point2 == null) return 0;
        int minX = Math.min(point1.getBlockX(), point2.getBlockX());
        int maxX = Math.max(point1.getBlockX(), point2.getBlockX());
        int minY = Math.min(point1.getBlockY(), point2.getBlockY());
        int maxY = Math.max(point1.getBlockY(), point2.getBlockY());
        int minZ = Math.min(point1.getBlockZ(), point2.getBlockZ());
        int maxZ = Math.max(point1.getBlockZ(), point2.getBlockZ());

        World world = point1.getWorld();
        int remaining = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() != Material.AIR) {
                        remaining++;
                    }
                }
            }
        }
        return remaining;
    }

    public boolean isHologramEnabled() {
        return hologramEnabled;
    }

    public void setHologramEnabled(boolean enabled) {
        this.hologramEnabled = enabled;
    }

    public List<String> getHologramLines() {
        return hologramLines;
    }

    public void setHologramLines(List<String> lines) {
        this.hologramLines = lines;
    }

    public double getHologramYOffset() {
        return hologramYOffset;
    }

    public void setHologramYOffset(double yOffset) {
        this.hologramYOffset = yOffset;
    }

    public int getParticleCount() {
        return particleCount;
    }

    public void setParticleCount(int count) {
        this.particleCount = count;
    }
}