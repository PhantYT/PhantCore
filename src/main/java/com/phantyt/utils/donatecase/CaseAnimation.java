package com.phantyt.utils.donatecase;

import com.phantyt.utils.modules.DonateCaseModule;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CaseAnimation {
    private final Player player;
    private final Location originalLocation;
    private final Location animationLocation;
    private final ConfigurationSection rewards;
    private final DonateCaseListener listener;
    private final String caseName;
    private final DonateCaseModule module;
    private ItemStack winningItem;
    private final List<Hologram> rewardHolograms = new ArrayList<>();
    private Hologram winningHologram;
    private Hologram nameHologram;
    private final Random random = new Random();

    public CaseAnimation(Player player, Location shulkerLocation, ConfigurationSection rewards, DonateCaseListener listener, String caseName, DonateCaseModule module) {
        this.player = player;
        this.originalLocation = player.getLocation().clone();

        // Читаем локацию из cases.yml для конкретного кейса
        ConfigurationSection config = module.getCasesConfig().getConfigurationSection("cases." + caseName + ".animation-location");
        if (config != null) {
            String worldName = config.getString("world", player.getWorld().getName());
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                world = player.getWorld(); // Если мир не найден, используем мир игрока
            }
            double x = config.getDouble("x", shulkerLocation.getX());
            double y = config.getDouble("y", shulkerLocation.getY() + 4);
            double z = config.getDouble("z", shulkerLocation.getZ());
            float yaw = (float) config.getDouble("yaw", 0);
            float pitch = (float) config.getDouble("pitch", 0);
            this.animationLocation = new Location(world, x, y, z, yaw, pitch);
        } else {
            this.animationLocation = shulkerLocation.clone().add(0, 4, 0); // Запасной вариант
        }

        this.rewards = rewards;
        this.listener = listener;
        this.caseName = caseName;
        this.module = module;
    }

    public void start() {
        module.getPlugin().getLogger().info("[DonateCase] Начало анимации для " + player.getName());
        module.removeHologram(animationLocation.clone().subtract(0.5, 0.5, 0.5));
        player.teleport(animationLocation);
        winningItem = determineWinningItem();
        listener.onAnimationStart(player);

        String texture = module.getCasesConfig().getString("cases." + caseName + ".default-item-texture",
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOThkYWExZTNlZDk0ZmYzZTMzZTFkNGM2ZTQzZjAyNGM0N2Q3OGE1N2JhNGQzOGU3NWU3YzkyNjQxMDYifX19");

        for (int i = 0; i < 25; i++) {
            String hologramLine;
            if (i < 12) {
                hologramLine = "#ICON: PLAYER_HEAD (" + texture + ")";
            } else {
                hologramLine = "#SMALLHEAD: PLAYER_HEAD (" + texture + ")";
            }
            Hologram hologram = DHAPI.createHologram("reward_" + player.getUniqueId() + "_" + i, animationLocation.clone());
            DHAPI.setHologramLines(hologram, List.of(hologramLine));
            rewardHolograms.add(hologram);
        }

        new BukkitRunnable() {
            int ticks = 0;
            final int duration = 100;
            final List<Vector> positions = new ArrayList<>();
            final List<Vector> velocities = new ArrayList<>();
            final List<Double> rotationAngles = new ArrayList<>();
            final double minDistanceFromPlayer = 2.0;

            {
                for (int i = 0; i < rewardHolograms.size(); i++) {
                    Vector pos;
                    do {
                        pos = new Vector(
                                random.nextDouble() * 6 - 3,
                                random.nextDouble() * 4,
                                random.nextDouble() * 6 - 3
                        );
                    } while (pos.length() < minDistanceFromPlayer);
                    positions.add(pos);
                    velocities.add(new Vector(
                            random.nextDouble() * 0.3 - 0.15,
                            random.nextDouble() * 0.3 - 0.15,
                            random.nextDouble() * 0.3 - 0.15
                    ));
                    rotationAngles.add(random.nextDouble() * 2 * Math.PI);
                }
            }

            @Override
            public void run() {
                if (ticks >= duration) {
                    finishAnimation();
                    cancel();
                    return;
                }

                for (int i = 0; i < rewardHolograms.size(); i++) {
                    Hologram hologram = rewardHolograms.get(i);
                    Vector pos = positions.get(i);
                    Vector vel = velocities.get(i);
                    double angle = rotationAngles.get(i);

                    pos.add(vel);

                    if (pos.getX() > 5 || pos.getX() < -5) vel.setX(-vel.getX());
                    if (pos.getY() > 4 || pos.getY() < 0) vel.setY(-vel.getY());
                    if (pos.getZ() > 5 || pos.getZ() < -5) vel.setZ(-vel.getZ());

                    if (pos.length() < minDistanceFromPlayer) {
                        pos.normalize().multiply(minDistanceFromPlayer);
                    }

                    Location newLoc = animationLocation.clone().add(pos);
                    DHAPI.moveHologram(hologram, newLoc);

                    angle += 0.1;
                    rotationAngles.set(i, angle);

                    for (int j = 0; j < 4; j++) {
                        double particleAngle = angle + (j * Math.PI / 2);
                        double particleX = 0.3 * Math.cos(particleAngle);
                        double particleZ = 0.3 * Math.sin(particleAngle);
                        Location particleLoc = newLoc.clone().add(particleX, 0, particleZ);
                        player.getWorld().spawnParticle(Particle.REDSTONE, particleLoc, 1, 0, 0, 0, 0,
                                new Particle.DustOptions(Color.fromRGB(random.nextInt(255), random.nextInt(255), random.nextInt(255)), 1.0f));
                    }

                    player.getWorld().spawnParticle(Particle.REDSTONE, newLoc, 3, 0.1, 0.1, 0.1, 0,
                            new Particle.DustOptions(Color.fromRGB(random.nextInt(255), random.nextInt(255), random.nextInt(255)), 1.0f));
                }

                if (ticks % 10 == 0) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.0f);
                }

                ticks++;
            }
        }.runTaskTimer(module.getPlugin(), 0L, 1L);
    }

    private void finishAnimation() {
        for (Hologram hologram : rewardHolograms) {
            hologram.delete();
        }
        rewardHolograms.clear();

        Location winLoc = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(2)).add(0, 0.5, 0);
        String texture = module.getCasesConfig().getString("cases." + caseName + ".rewards." + getWinningRewardKey() + ".item-texture",
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOThkYWExZTNlZDk0ZmYzZTMzZTFkNGM2ZTQzZjAyNGM0N2Q3OGE1N2JhNGQzOGU3NWU3YzkyNjQxMDYifX19");
        winningHologram = DHAPI.createHologram("win_" + player.getUniqueId(), winLoc);
        DHAPI.setHologramLines(winningHologram, List.of("#HEAD: PLAYER_HEAD (" + texture + ")"));

        Location nameLoc = winLoc.clone().subtract(0, 0.7, 0);
        nameHologram = DHAPI.createHologram("win_name_" + player.getUniqueId(), nameLoc);
        DHAPI.setHologramLines(nameHologram, List.of(winningItem.getItemMeta().getDisplayName()));

        player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, winLoc, 10, 0.2, 0.2, 0.2, 0.05);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        new BukkitRunnable() {
            int winTicks = 0;
            final int winDuration = 60;

            @Override
            public void run() {
                if (winTicks >= winDuration) {
                    winningHologram.delete();
                    nameHologram.delete();
                    player.teleport(originalLocation);
                    module.restoreHologram(caseName, animationLocation.clone().subtract(0.5, 0.5, 0.5));
                    listener.onAnimationFinish(player);
                    listener.onDisplayFinish(player);
                    onRewardReceived();
                    cancel();
                    return;
                }

                Location newWinLoc = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(2)).add(0, 0.5, 0);
                DHAPI.moveHologram(winningHologram, newWinLoc);
                Location newNameLoc = newWinLoc.clone().subtract(0, 0.7, 0);
                DHAPI.moveHologram(nameHologram, newNameLoc);
                winTicks++;
            }
        }.runTaskTimer(module.getPlugin(), 0L, 1L);
    }

    private String getWinningRewardKey() {
        String rewardName = winningItem.getItemMeta().getDisplayName();
        for (String key : rewards.getKeys(false)) {
            String configuredRewardName = rewards.getString(key + ".name");
            if (configuredRewardName != null && configuredRewardName.equals(ChatColor.stripColor(rewardName))) {
                return key;
            }
        }
        return rewards.getKeys(false).iterator().next();
    }

    private ItemStack determineWinningItem() {
        List<String> rewardKeys = new ArrayList<>(rewards.getKeys(false));
        if (rewardKeys.isEmpty()) {
            ItemStack fallback = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = fallback.getItemMeta();
            meta.setDisplayName("Ошибка: Награды не найдены");
            fallback.setItemMeta(meta);
            return fallback;
        }

        double totalChance = rewardKeys.stream()
                .mapToDouble(key -> rewards.getDouble(key + ".chance"))
                .sum();

        if (totalChance <= 0) {
            ItemStack fallback = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = fallback.getItemMeta();
            meta.setDisplayName("Ошибка: Суммарный шанс равен 0");
            fallback.setItemMeta(meta);
            return fallback;
        }

        double roll = random.nextDouble() * totalChance;
        double current = 0;

        for (String key : rewardKeys) {
            current += rewards.getDouble(key + ".chance");
            if (roll <= current) {
                ItemStack item = module.getCasesConfig().getItemStack("cases." + caseName + ".rewards." + key + ".item");
                if (item == null) {
                    item = new ItemStack(Material.PLAYER_HEAD);
                }
                ItemMeta meta = item.getItemMeta();
                String rewardName = rewards.getString(key + ".name", "Неизвестная награда");
                meta.setDisplayName(rewardName);
                List<String> lore = new ArrayList<>();
                lore.add("Вы выиграли!");
                meta.setLore(lore);
                item.setItemMeta(meta);
                return item;
            }
        }

        String firstKey = rewardKeys.get(0);
        ItemStack fallback = module.getCasesConfig().getItemStack("cases." + caseName + ".rewards." + firstKey + ".item");
        if (fallback == null) {
            module.getPlugin().getLogger().warning("[DonateCase] Не удалось загрузить награду для первой награды: " + firstKey);
            fallback = new ItemStack(Material.PLAYER_HEAD);
        }
        ItemMeta meta = fallback.getItemMeta();
        String firstRewardName = rewards.getString(firstKey + ".name", "Неизвестная награда");
        meta.setDisplayName(firstRewardName);
        List<String> lore = new ArrayList<>();
        lore.add("Вы выиграли!");
        meta.setLore(lore);
        fallback.setItemMeta(meta);
        return fallback;
    }

    private void onRewardReceived() {
        if (winningItem == null || winningItem.getItemMeta() == null) {
            player.sendMessage(module.getMessage("error.no-winning-item"));
            return;
        }

        String rewardName = winningItem.getItemMeta().getDisplayName();
        boolean rewardFound = false;

        for (String key : rewards.getKeys(false)) {
            String configuredRewardName = rewards.getString(key + ".name");
            if (configuredRewardName != null && configuredRewardName.equals(ChatColor.stripColor(rewardName))) {
                String command = rewards.getString(key + ".command");
                if (command != null) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
                    String coloredRewardName = ChatColor.translateAlternateColorCodes('&', rewardName);
                    String message = module.getMessage("success.won-reward").replace("%reward%", coloredRewardName);
                    player.sendMessage(message);
                    rewardFound = true;
                    break;
                }
            }
        }

        if (!rewardFound) {
            String coloredRewardName = ChatColor.translateAlternateColorCodes('&', rewardName);
            String message = module.getMessage("error.reward-not-found").replace("%reward%", coloredRewardName);
            player.sendMessage(message);
        }
    }

    public ItemStack getWinningItem() {
        return winningItem;
    }
}