package com.phantyt.utils.modules;

import com.phantyt.PhantCore;
import com.phantyt.utils.Module;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import com.phantyt.utils.commands.BlockCommandsCommands;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BlockCommandsModule implements Module, Listener {
    private final PhantCore plugin;
    private FileConfiguration config;
    private FileConfiguration commandsConfig;
    private final Map<UUID, CreationState> creationStates = new ConcurrentHashMap<>();
    private final Map<Location, CommandBlockData> commandBlocks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> playerCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> teleportLock = new ConcurrentHashMap<>();
    private int globalCooldown;
    private BlockCommandsCommands commands;
    private volatile int particleTaskId = -1;
    private volatile boolean isRunning = false;
    private String radiusPrompt;
    private String selectBlockPrompt;
    private String selectBlocksMessage;
    private String invalidNumberMessage;
    private String numberRequiredMessage;
    private String creationSuccessMessage;
    private String worldNotFoundMessage;
    private String teleportErrorMessage;
    private String cooldownMessage;
    private String noPermissionMessage;

    public BlockCommandsModule(PhantCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "BlockCommands";
    }

    @Override
    public void enable(PhantCore plugin) {
        File configFile = new File(plugin.getDataFolder(), "blockcommands/config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("blockcommands/config.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);
        this.globalCooldown = config.getInt("global_cooldown", 60);
        loadMessages();

        File commandsFile = new File(plugin.getDataFolder(), "blockcommands/commands.yml");
        if (!commandsFile.exists()) {
            plugin.saveResource("blockcommands/commands.yml", false);
        }
        this.commandsConfig = YamlConfiguration.loadConfiguration(commandsFile);

        loadCommandBlocks();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.commands = new BlockCommandsCommands(this);
        plugin.getCommand("blockcommands").setExecutor(commands);

        isRunning = true;
        if (plugin.isEnabled()) {
            particleTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::spawnParticlesAsync, 0L, 10L).getTaskId();
        }
    }

    private void loadMessages() {
        radiusPrompt = ChatColor.translateAlternateColorCodes('&', config.getString("messages.radius_prompt", "&eВведите радиус в чат:"));
        selectBlockPrompt = ChatColor.translateAlternateColorCodes('&', config.getString("messages.select_block_prompt", "&eКликните по блоку для выбора центра:"));
        selectBlocksMessage = ChatColor.translateAlternateColorCodes('&', config.getString("messages.select_blocks_message", "&7Выберите типы блоков (введите номер для переключения, 0 для подтверждения):"));
        invalidNumberMessage = ChatColor.translateAlternateColorCodes('&', config.getString("messages.invalid_number_message", "&cНеверный номер!"));
        numberRequiredMessage = ChatColor.translateAlternateColorCodes('&', config.getString("messages.number_required_message", "&cВведите число!"));
        creationSuccessMessage = ChatColor.translateAlternateColorCodes('&', config.getString("messages.creation_success_message", "&aУспешно создано! Настройте команды, пермишен, сообщение и звуки в blockcommands/commands.yml"));
        worldNotFoundMessage = ChatColor.translateAlternateColorCodes('&', config.getString("messages.world_not_found_message", "&cОшибка: мир 'box' не найден!"));
        teleportErrorMessage = ChatColor.translateAlternateColorCodes('&', config.getString("messages.teleport_error_message", "&cОшибка в команде телепортации: "));
        cooldownMessage = ChatColor.translateAlternateColorCodes('&', config.getString("messages.cooldown_message", "&cПодождите {time} сек. перед повторным использованием портала!"));
        noPermissionMessage = ChatColor.translateAlternateColorCodes('&', config.getString("messages.no_permission_message", "&cВы не можете войти в этот портал!"));
    }

    @Override
    public void disable(PhantCore plugin) {
        saveCommands();
        plugin.getCommand("blockcommands").setExecutor(null);
        org.bukkit.event.HandlerList.unregisterAll(this);

        // Properly shut down the async particle task
        isRunning = false;
        if (particleTaskId != -1) {
            Bukkit.getScheduler().cancelTask(particleTaskId);
            particleTaskId = -1;
            // Wait briefly to ensure the async task has a chance to stop
            try {
                Thread.sleep(100); // Give async tasks a moment to terminate
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void loadCommandBlocks() {
        for (String key : commandsConfig.getKeys(false)) {
            Location loc = (Location) commandsConfig.get(key + ".location");
            int radius = commandsConfig.getInt(key + ".radius", 5);
            List<String> commands = commandsConfig.getStringList(key + ".commands");
            String particleStr = commandsConfig.getString(key + ".particle", "END_ROD");
            Particle particle = particleStr.equalsIgnoreCase("NULL") ? null : Particle.valueOf(particleStr.toUpperCase());
            List<Material> materials = new ArrayList<>();
            for (String mat : commandsConfig.getStringList(key + ".materials")) {
                materials.add(Material.valueOf(mat));
            }
            String permission = commandsConfig.getString(key + ".permission", "blockcommands.event." + key);
            String actionBarMessage = commandsConfig.getString(key + ".actionbar", "Вы вошли в портал!");
            String enterSoundStr = commandsConfig.getString(key + ".enter_sound", "BLOCK_PORTAL_TRIGGER");
            String exitSoundStr = commandsConfig.getString(key + ".exit_sound", "BLOCK_PORTAL_TRAVEL");
            Sound enterSound = Sound.valueOf(enterSoundStr.toUpperCase());
            Sound exitSound = Sound.valueOf(exitSoundStr.toUpperCase());
            CommandBlockData data = new CommandBlockData(commands, materials, radius, particle, permission, actionBarMessage, enterSound, exitSound);
            commandBlocks.put(loc, data);
        }
    }

    private void saveCommands() {
        int i = 0;
        for (Map.Entry<Location, CommandBlockData> entry : commandBlocks.entrySet()) {
            String key = "event_" + i++;
            commandsConfig.set(key + ".location", entry.getKey());
            commandsConfig.set(key + ".radius", entry.getValue().radius);
            commandsConfig.set(key + ".commands", entry.getValue().commands);
            commandsConfig.set(key + ".particle", entry.getValue().particle != null ? entry.getValue().particle.name() : "NULL");
            commandsConfig.set(key + ".permission", entry.getValue().permission);
            commandsConfig.set(key + ".actionbar", entry.getValue().actionBarMessage);
            commandsConfig.set(key + ".enter_sound", entry.getValue().enterSound.name());
            commandsConfig.set(key + ".exit_sound", entry.getValue().exitSound.name());
            List<String> mats = new ArrayList<>();
            for (Material mat : entry.getValue().materials) {
                mats.add(mat.name());
            }
            commandsConfig.set(key + ".materials", mats);
        }
        try {
            commandsConfig.save(new File(plugin.getDataFolder(), "blockcommands/commands.yml"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String locToString(Location loc) {
        return loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
    }

    public void startCreation(Player player) {
        player.sendMessage(radiusPrompt);
        creationStates.put(player.getUniqueId(), new CreationState());
    }

    public void setRadius(Player player, int radius) {
        CreationState state = creationStates.get(player.getUniqueId());
        if (state == null) return;
        state.radius = radius;
        player.sendMessage(selectBlockPrompt);
    }

    public void selectBlock(Player player, Block block) {
        CreationState state = creationStates.get(player.getUniqueId());
        if (state == null) return;
        state.center = block.getLocation();
        state.findBlocksInRadius();
        sendBlockSelectionMessage(player);
    }

    private void sendBlockSelectionMessage(Player player) {
        CreationState state = creationStates.get(player.getUniqueId());
        if (state == null) return;

        player.sendMessage(selectBlocksMessage);
        int index = 1;
        state.materialList.clear();
        for (Map.Entry<Material, Boolean> entry : state.selectedMaterials.entrySet()) {
            state.materialList.add(entry.getKey());
            String status = entry.getValue() ? ChatColor.GREEN + "[Включено]" : ChatColor.RED + "[Выключено]";
            player.sendMessage(ChatColor.WHITE + "" + index + ". " + entry.getKey().name() + " " + status);
            index++;
        }
    }

    public void toggleMaterialByIndex(Player player, int index) {
        CreationState state = creationStates.get(player.getUniqueId());
        if (state == null || index < 1 || index > state.materialList.size()) {
            player.sendMessage(invalidNumberMessage);
            return;
        }

        Material material = state.materialList.get(index - 1);
        boolean current = state.selectedMaterials.getOrDefault(material, false);
        state.selectedMaterials.put(material, !current);

        for (Block block : state.keys) {
            if (block.getType() == material) {
                if (!current) {
                    state.originalBlocks.put(block.getLocation(), block.getType());
                    block.setType(Material.BEDROCK);
                } else if (state.originalBlocks.containsKey(block.getLocation())) {
                    block.setType(state.originalBlocks.get(block.getLocation()));
                }
            }
        }
        sendBlockSelectionMessage(player);
    }

    public void confirmSelection(Player player) {
        CreationState state = creationStates.get(player.getUniqueId());
        if (state == null) return;

        for (Map.Entry<Location, Material> entry : state.originalBlocks.entrySet()) {
            entry.getKey().getBlock().setType(entry.getValue());
        }

        List<Material> selected = new ArrayList<>();
        for (Map.Entry<Material, Boolean> entry : state.selectedMaterials.entrySet()) {
            if (entry.getValue()) selected.add(entry.getKey());
        }

        String permission = "blockcommands.event.event_" + commandBlocks.size();
        String actionBarMessage = "Вы вошли в портал " + commandBlocks.size() + "!";
        CommandBlockData data = new CommandBlockData(new ArrayList<>(), selected, state.radius, Particle.END_ROD,
                permission, actionBarMessage, Sound.BLOCK_PORTAL_TRIGGER, Sound.BLOCK_PORTAL_TRAVEL);
        commandBlocks.put(state.center, data);
        saveCommands();

        creationStates.remove(player.getUniqueId());
        player.sendMessage(creationSuccessMessage);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block == null) return;

        CreationState state = creationStates.get(player.getUniqueId());
        if (state != null && state.radius > 0 && state.center == null) {
            event.setCancelled(true);
            selectBlock(player, block);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        if (to == null || !to.getWorld().getName().equals("box")) return;

        UUID playerId = player.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (Map.Entry<Location, CommandBlockData> entry : commandBlocks.entrySet()) {
                Location center = entry.getKey();
                CommandBlockData data = entry.getValue();

                if (!to.getWorld().equals(center.getWorld())) continue;

                boolean inRange = to.distanceSquared(center) <= (data.radius + 0.5) * (data.radius + 0.5);

                if (inRange) {
                    World world = center.getWorld();
                    int radius = data.radius;
                    int x = center.getBlockX();
                    int y = center.getBlockY();
                    int z = center.getBlockZ();

                    for (int dx = -radius; dx <= radius; dx++) {
                        for (int dy = -radius; dy <= radius; dy++) {
                            for (int dz = -radius; dz <= radius; dz++) {
                                Block block = world.getBlockAt(x + dx, y + dy, z + dz);
                                if (block.getLocation().distanceSquared(center) <= radius * radius && data.materials.contains(block.getType())) {
                                    Location blockLoc = block.getLocation().add(0.5, 0.5, 0.5);
                                    if (isWithinDistance(to, blockLoc)) {
                                        if (!player.hasPermission(data.permission)) {
                                            Bukkit.getScheduler().runTask(plugin, () -> denyAccess(player));
                                            return;
                                        }
                                        if (canUsePortal(player) && !data.activePlayers.contains(playerId)) {
                                            data.activePlayers.add(playerId);
                                            Bukkit.getScheduler().runTask(plugin, () -> executeCommands(player, data));
                                            return; // Выходим после первой активации
                                        } else if (!canUsePortal(player) && !data.activePlayers.contains(playerId)) {
                                            Bukkit.getScheduler().runTask(plugin, () -> showCooldownMessage(player));
                                            return;
                                        }
                                        return; // Если игрок уже активировал портал, ничего не делаем
                                    }
                                }
                            }
                        }
                    }

                    // Проверяем соседние блоки
                    for (int dx = -radius; dx <= radius; dx++) {
                        for (int dy = -radius; dy <= radius; dy++) {
                            for (int dz = -radius; dz <= radius; dz++) {
                                Block block = world.getBlockAt(x + dx, y + dy, z + dz);
                                if (block.getLocation().distanceSquared(center) <= radius * radius && data.materials.contains(block.getType())) {
                                    for (BlockFace face : BlockFace.values()) {
                                        Block neighbor = block.getRelative(face);
                                        if (neighbor.getType().isSolid() && data.materials.contains(neighbor.getType())) {
                                            Location neighborLoc = neighbor.getLocation().add(0.5, 0.5, 0.5);
                                            if (isWithinDistance(to, neighborLoc)) {
                                                if (!player.hasPermission(data.permission)) {
                                                    Bukkit.getScheduler().runTask(plugin, () -> denyAccess(player));
                                                    return;
                                                }
                                                if (canUsePortal(player) && !data.activePlayers.contains(playerId)) {
                                                    data.activePlayers.add(playerId);
                                                    Bukkit.getScheduler().runTask(plugin, () -> executeCommands(player, data));
                                                    return; // Выходим после первой активации
                                                } else if (!canUsePortal(player) && !data.activePlayers.contains(playerId)) {
                                                    Bukkit.getScheduler().runTask(plugin, () -> showCooldownMessage(player));
                                                    return;
                                                }
                                                return; // Если игрок уже активировал портал, ничего не делаем
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (data.activePlayers.contains(playerId)) {
                    // Игрок вышел из зоны портала, сбрасываем состояние
                    data.activePlayers.remove(playerId);
                }
            }
        });
    }

    private boolean isWithinDistance(Location playerLoc, Location blockLoc) {
        return playerLoc.distanceSquared(blockLoc) <= 4.0;
    }

    private boolean canUsePortal(Player player) {
        long currentTime = System.currentTimeMillis() / 1000;
        Long lastUse = playerCooldowns.get(player.getUniqueId());
        Long lastTeleport = teleportLock.get(player.getUniqueId());

        if (lastTeleport != null && (currentTime - lastTeleport) < 1) {
            return false;
        }

        return lastUse == null || (currentTime - lastUse) >= globalCooldown;
    }

    private void denyAccess(Player player) {
        player.spawnParticle(Particle.SMOKE_LARGE, player.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(noPermissionMessage));
    }

    private void showCooldownMessage(Player player) {
        long currentTime = System.currentTimeMillis() / 1000;
        Long lastUse = playerCooldowns.get(player.getUniqueId());
        if (lastUse != null) {
            long remaining = globalCooldown - (currentTime - lastUse);
            String message = cooldownMessage.replace("{time}", String.valueOf(remaining));
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
            player.spawnParticle(Particle.SMOKE_LARGE, player.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
        }
    }

    private void executeCommands(Player player, CommandBlockData data) {
        World boxWorld = Bukkit.getWorld("box");
        if (boxWorld == null) {
            player.sendMessage(worldNotFoundMessage);
            return;
        }

        // Находим центр портала из commandBlocks
        Location center = null;
        for (Map.Entry<Location, CommandBlockData> entry : commandBlocks.entrySet()) {
            if (entry.getValue() == data) {
                center = entry.getKey();
                break;
            }
        }
        if (center != null) {
        } else {
        }

        player.playSound(player.getLocation(), data.enterSound, 1.0f, 1.0f);
        String formattedMessage = ChatColor.translateAlternateColorCodes('&', data.actionBarMessage.replace("{player}", player.getName()));
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(formattedMessage));

        long currentTime = System.currentTimeMillis() / 1000;
        playerCooldowns.put(player.getUniqueId(), currentTime);
        teleportLock.put(player.getUniqueId(), currentTime);

        for (String command : data.commands) {
            String formatted = command.replace("{player}", player.getName());

            if (formatted.toLowerCase().startsWith("minecraft:tp") || formatted.toLowerCase().startsWith("tp")) {
                String[] parts = formatted.split("\\s+");
                if (parts.length >= 4) {
                    try {
                        int startIndex = formatted.toLowerCase().startsWith("minecraft:tp") ? 2 : 1;
                        double x = Double.parseDouble(parts[startIndex]);
                        double y = Double.parseDouble(parts[startIndex + 1]);
                        double z = Double.parseDouble(parts[startIndex + 2]);
                        float yaw = parts.length >= startIndex + 5 ? Float.parseFloat(parts[startIndex + 3]) : 0.0f;
                        float pitch = parts.length >= startIndex + 5 ? Float.parseFloat(parts[startIndex + 4]) : 0.0f;

                        Location targetLocation = new Location(boxWorld, x, y, z, yaw, pitch);
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            player.teleport(targetLocation);
                            player.playSound(targetLocation, data.exitSound, 0.5f, 1.0f);
                        });
                        continue;
                    } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                        player.sendMessage(teleportErrorMessage + formatted);
                        continue;
                    }
                }
            }

            Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formatted));
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!creationStates.containsKey(player.getUniqueId())) return;

        event.setCancelled(true);
        Bukkit.getScheduler().runTask(plugin, () -> {
            CreationState state = creationStates.get(player.getUniqueId());
            if (state != null && state.center == null) {
                commands.handleChat(player, event.getMessage());
            } else {
                try {
                    int number = Integer.parseInt(event.getMessage());
                    if (number == 0) {
                        confirmSelection(player);
                    } else {
                        toggleMaterialByIndex(player, number);
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(numberRequiredMessage);
                }
            }
        });
    }

    private void spawnParticlesAsync() {
        if (!isRunning || !plugin.isEnabled()) return;

        Map<Location, ParticleData> particleData = new HashMap<>();
        for (Map.Entry<Location, CommandBlockData> entry : commandBlocks.entrySet()) {
            Location center = entry.getKey();
            CommandBlockData data = entry.getValue();
            if (data.particle == null) continue;

            World world = center.getWorld();
            if (world == null) continue; // Skip if world is unloaded

            int radius = data.radius;
            int x = center.getBlockX();
            int y = center.getBlockY();
            int z = center.getBlockZ();

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        Location loc = new Location(world, x + dx + 0.5, y + dy + 0.5, z + dz + 0.5);
                        if (loc.distanceSquared(center) <= radius * radius) {
                            // Check if plugin is still enabled before accessing block
                            if (!isRunning || !plugin.isEnabled()) return;
                            if (data.materials.contains(loc.getBlock().getType())) {
                                particleData.put(loc, new ParticleData(data.particle, 2));
                            }
                        }
                    }
                }
            }
        }

        if (!particleData.isEmpty() && isRunning && plugin.isEnabled()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!isRunning || !plugin.isEnabled()) return;
                for (Map.Entry<Location, ParticleData> entry : particleData.entrySet()) {
                    Location loc = entry.getKey();
                    ParticleData pd = entry.getValue();
                    World world = loc.getWorld();
                    if (world != null) { // Ensure world is still loaded
                        world.spawnParticle(pd.particle, loc, pd.count, 0.1, 0.1, 0.1, 0);
                    }
                }
            });
        }
    }

    public BlockCommandsCommands getCommands() {
        return commands;
    }

    private class CreationState {
        int radius;
        Location center;
        Map<Material, Boolean> selectedMaterials = new HashMap<>();
        Map<Location, Material> originalBlocks = new HashMap<>();
        Set<Block> keys = new HashSet<>();
        List<Material> materialList = new ArrayList<>();

        void findBlocksInRadius() {
            World world = center.getWorld();
            int x = center.getBlockX();
            int y = center.getBlockY();
            int z = center.getBlockZ();

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        Block block = world.getBlockAt(x + dx, y + dy, z + dz);
                        if (block.getLocation().distanceSquared(center) <= radius * radius) {
                            selectedMaterials.putIfAbsent(block.getType(), false);
                            keys.add(block);
                        }
                    }
                }
            }
        }
    }

    private class CommandBlockData {
        List<String> commands;
        Set<Material> materials;
        int radius;
        Particle particle;
        String permission;
        String actionBarMessage;
        Sound enterSound;
        Sound exitSound;
        Set<UUID> activePlayers;

        CommandBlockData(List<String> commands, List<Material> materials, int radius, Particle particle,
                         String permission, String actionBarMessage, Sound enterSound, Sound exitSound) {
            this.commands = commands;
            this.materials = new HashSet<>(materials);
            this.radius = radius;
            this.particle = particle;
            this.permission = permission;
            this.actionBarMessage = actionBarMessage;
            this.enterSound = enterSound;
            this.exitSound = exitSound;
            this.activePlayers = ConcurrentHashMap.newKeySet();
        }
    }

    private static class ParticleData {
        Particle particle;
        int count;

        ParticleData(Particle particle, int count) {
            this.particle = particle;
            this.count = count;
        }
    }
}