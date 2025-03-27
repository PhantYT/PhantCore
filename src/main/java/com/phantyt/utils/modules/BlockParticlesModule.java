package com.phantyt.utils.modules;

import com.phantyt.PhantCore;
import com.phantyt.utils.Module;
import com.phantyt.utils.commands.BlockParticlesCommands;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BlockParticlesModule implements Module, Listener {
    private final PhantCore plugin;
    private FileConfiguration config;
    private FileConfiguration languageConfig;
    private File configFile;
    private final Map<Location, ParticleEffect> particleEffects = new ConcurrentHashMap<>();
    private final Map<UUID, String> pendingActions = new ConcurrentHashMap<>();
    private static final int MAX_DISTANCE = 32; // Максимальная дистанция видимости частиц
    private volatile boolean isRunning = false;

    public BlockParticlesModule(PhantCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "BlockParticles";
    }

    @Override
    public void enable(PhantCore plugin) {
        // Инициализация файла конфигурации
        configFile = new File(plugin.getDataFolder(), "blockparticles/config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("blockparticles/config.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);

        // Инициализация файла языка
        File languageFile = new File(plugin.getDataFolder(), "blockparticles/language.yml");
        if (!languageFile.exists()) {
            plugin.saveResource("blockparticles/language.yml", false);
        }
        this.languageConfig = YamlConfiguration.loadConfiguration(languageFile);

        // Регистрация событий и команд
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getCommand("blockparticles").setExecutor(new BlockParticlesCommands(this));

        loadParticleEffects();
        startParticleProcessor();
    }

    @Override
    public void disable(PhantCore plugin) {
        isRunning = false;
        particleEffects.clear();
        plugin.getCommand("blockparticles").setExecutor(null);
        org.bukkit.event.HandlerList.unregisterAll(this);
        saveConfig(); // Синхронное сохранение при отключении
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        pendingActions.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String action = pendingActions.get(uuid);
        if (action == null || event.getClickedBlock() == null) return;

        Location blockLocation = event.getClickedBlock().getLocation();
        Location effectLocation = blockLocation.clone().add(0.5, 0.5, 0.5);
        event.setCancelled(true);

        if (action.startsWith("create")) {
            String[] parts = action.split(" ");
            Particle particle = Particle.valueOf(parts[1]);
            long interval = Long.parseLong(parts[2]);
            startParticleEffect(blockLocation, effectLocation, particle, interval);
            player.sendMessage(getMessage("effect-created", particle.name(), String.valueOf(interval), blockLocation.getWorld().getName()));
        } else if (action.equals("delete")) {
            stopParticleEffect(blockLocation);
            player.sendMessage(getMessage("effect-deleted"));
        } else if (action.equals("move")) {
            moveParticleEffect(player, blockLocation);
        }

        pendingActions.remove(uuid);
    }

    public void startParticleEffect(Location blockLocation, Location effectLocation, Particle particle, long interval) {
        // Удаляем существующий эффект, если он есть
        ParticleEffect existing = particleEffects.remove(blockLocation);
        if (existing != null) existing.isActive = false;

        ParticleEffect effect = new ParticleEffect(blockLocation.getWorld(), blockLocation, effectLocation, particle, interval);
        particleEffects.put(blockLocation, effect);
        saveParticleEffect(blockLocation, particle, interval); // Синхронное сохранение
    }

    public void stopParticleEffect(Location location) {
        ParticleEffect effect = particleEffects.remove(location);
        if (effect != null) {
            effect.isActive = false;
            removeParticleEffect(location); // Синхронное удаление
        }
    }

    public void moveParticleEffect(Player player, Location newBlockLocation) {
        Iterator<Map.Entry<Location, ParticleEffect>> iterator = particleEffects.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Location, ParticleEffect> entry = iterator.next();
            ParticleEffect oldEffect = entry.getValue();
            iterator.remove();
            oldEffect.isActive = false;

            Location newEffectLocation = newBlockLocation.clone().add(0.5, 0.5, 0.5);
            startParticleEffect(newBlockLocation, newEffectLocation, oldEffect.particle, oldEffect.interval);
            removeParticleEffect(entry.getKey());
            player.sendMessage(getMessage("effect-moved", newBlockLocation.getWorld().getName()));
            return;
        }
        player.sendMessage(getMessage("no-effects"));
    }

    private void startParticleProcessor() {
        isRunning = true;
        new BukkitRunnable() {
            private long tickCounter = 0;

            @Override
            public void run() {
                if (!isRunning || particleEffects.isEmpty()) return;

                tickCounter++;
                Iterator<Map.Entry<Location, ParticleEffect>> iterator = particleEffects.entrySet().iterator();

                while (iterator.hasNext()) {
                    ParticleEffect effect = iterator.next().getValue();
                    if (effect.isActive && shouldSpawn(effect) &&
                            (tickCounter % effect.interval) == 0) {
                        effect.process();
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private boolean shouldSpawn(ParticleEffect effect) {
        for (Player player : effect.world.getPlayers()) {
            if (player.getLocation().distanceSquared(effect.effectLocation) <= MAX_DISTANCE * MAX_DISTANCE) {
                return true;
            }
        }
        return false;
    }

    private void loadParticleEffects() {
        ConfigurationSection worldsSection = config.getConfigurationSection("particle-effects");
        if (worldsSection == null) return;

        for (String worldName : worldsSection.getKeys(false)) {
            World world = Bukkit.getWorld(worldName);
            if (world == null) continue;

            ConfigurationSection locationsSection = worldsSection.getConfigurationSection(worldName);
            if (locationsSection == null) continue;

            for (String locationKey : locationsSection.getKeys(false)) {
                ConfigurationSection locData = locationsSection.getConfigurationSection(locationKey);
                if (locData == null) continue;

                try {
                    double x = locData.getDouble("x");
                    double y = locData.getDouble("y");
                    double z = locData.getDouble("z");
                    Location blockLocation = new Location(world, x, y, z);
                    Location effectLocation = blockLocation.clone().add(0.5, 0.5, 0.5);

                    String particleName = locData.getString("particle");
                    long interval = Math.max(1, locData.getLong("interval", 1L)); // Минимум 1 тик
                    Particle particle = Particle.valueOf(particleName);

                    startParticleEffect(blockLocation, effectLocation, particle, interval);
                } catch (Exception ignored) {}
            }
        }
    }

    private void saveParticleEffect(Location location, Particle particle, long interval) {
        String worldName = location.getWorld().getName();
        String locKey = String.format("%d_%d_%d", location.getBlockX(), location.getBlockY(), location.getBlockZ());
        String path = "particle-effects." + worldName + "." + locKey;

        // Убеждаемся, что секция мира существует
        ConfigurationSection worldSection = config.getConfigurationSection("particle-effects." + worldName);
        if (worldSection == null) {
            config.createSection("particle-effects." + worldName);
        }

        config.set(path + ".x", location.getX());
        config.set(path + ".y", location.getY());
        config.set(path + ".z", location.getZ());
        config.set(path + ".particle", particle.name());
        config.set(path + ".interval", interval);

        saveConfig();
    }

    private void removeParticleEffect(Location location) {
        String worldName = location.getWorld().getName();
        String locKey = String.format("%d_%d_%d", location.getBlockX(), location.getBlockY(), location.getBlockZ());
        String path = "particle-effects." + worldName + "." + locKey;

        config.set(path, null);

        // Удаляем пустую секцию мира, если она осталась
        ConfigurationSection worldSection = config.getConfigurationSection("particle-effects." + worldName);
        if (worldSection != null && worldSection.getKeys(false).isEmpty()) {
            config.set("particle-effects." + worldName, null);
        }

        saveConfig();
    }

    private void saveConfig() {
        try {
            // Проверяем корневую секцию перед сохранением
            ConfigurationSection effectsSection = config.getConfigurationSection("particle-effects");
            if (effectsSection == null || effectsSection.getKeys(false).isEmpty()) {
                config.set("particle-effects", new HashMap<>()); // Устанавливаем пустую секцию, чтобы избежать null
            }
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось сохранить конфиг blockparticles: " + e.getMessage());
        }
    }

    public String getMessage(String path, String... replacements) {
        List<String> messageList = languageConfig.getStringList(path);
        if (messageList.isEmpty()) {
            return "Сообщение не найдено: " + path;
        }
        StringBuilder message = new StringBuilder();
        for (String line : messageList) {
            String formattedLine = line;
            for (int i = 0; i < replacements.length; i++) {
                formattedLine = formattedLine.replace("{" + i + "}", replacements[i]);
            }
            message.append(formattedLine).append("\n");
        }
        return message.toString().trim();
    }

    public void setPendingAction(Player player, String action) {
        pendingActions.put(player.getUniqueId(), action);
    }

    private class ParticleEffect {
        private final World world;
        private final Location blockLocation;
        private final Location effectLocation;
        private final Particle particle;
        private final long interval;
        private volatile boolean isActive = true;

        ParticleEffect(World world, Location blockLocation, Location effectLocation, Particle particle, long interval) {
            this.world = world;
            this.blockLocation = blockLocation;
            this.effectLocation = effectLocation;
            this.particle = particle;
            this.interval = Math.max(1, interval);
        }

        void process() {
            if (!isActive || !effectLocation.getChunk().isLoaded()) return;

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (isActive) {
                    world.spawnParticle(particle, effectLocation, 1, 0.2, 0.2, 0.2, 0.1);
                }
            });
        }
    }
}