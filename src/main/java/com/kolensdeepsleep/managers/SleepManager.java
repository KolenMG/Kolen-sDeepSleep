package com.kolensdeepsleep.managers;

import com.kolensdeepsleep.KolensDeepSleep;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * Manages sleep tracking and night skip logic
 */
public class SleepManager {
    private final KolensDeepSleep plugin;
    private final Map<World, Set<UUID>> sleepingPlayers;
    private final Map<World, Boolean> skipInProgress;
    
    public SleepManager(KolensDeepSleep plugin) {
        this.plugin = plugin;
        this.sleepingPlayers = new HashMap<>();
        this.skipInProgress = new HashMap<>();
        
        // Start progress display task
        startProgressDisplayTask();
        
        // Start day time cleanup task
        startDayCleanupTask();
    }
    
    /**
     * Add a player to sleeping list
     */
    public void addSleepingPlayer(Player player) {
        World world = player.getWorld();
        sleepingPlayers.computeIfAbsent(world, k -> new HashSet<>()).add(player.getUniqueId());
        
        // Broadcast join message
        broadcastSleepUpdate(world, player.getName(), true);
        
        // Debug
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getName());
        plugin.getMessageUtil().debug("debug.player-sleep-start", placeholders);
        
        // Check if threshold is met
        checkThreshold(world);
    }
    
    /**
     * Remove a player from sleeping list
     */
    public void removeSleepingPlayer(Player player) {
        World world = player.getWorld();
        Set<UUID> sleeping = sleepingPlayers.get(world);
        
        if (sleeping != null && sleeping.remove(player.getUniqueId())) {
            // Broadcast leave message
            broadcastSleepUpdate(world, player.getName(), false);
            
            // Debug
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", player.getName());
            plugin.getMessageUtil().debug("debug.player-sleep-end", placeholders);
        }
    }
    
    /**
     * Check if threshold is met for night skip
     */
    private void checkThreshold(World world) {
        if (skipInProgress.getOrDefault(world, false)) {
            return; // Skip already in progress
        }
        
        if (!isNightTime(world)) {
            return; // Not night time
        }
        
        int sleeping = getSleepingCount(world);
        int eligible = getEligiblePlayerCount(world);
        
        if (eligible == 0) {
            return;
        }
        
        boolean thresholdMet = isThresholdMet(world, sleeping, eligible);
        
        // Debug
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("sleeping", String.valueOf(sleeping));
        placeholders.put("eligible", String.valueOf(eligible));
        placeholders.put("percentage", String.valueOf((sleeping * 100) / eligible));
        placeholders.put("threshold", getThresholdString(world));
        plugin.getMessageUtil().debug("debug.threshold-check", placeholders);
        
        if (thresholdMet) {
            initiateNightSkip(world);
        }
    }
    
    /**
     * Check if threshold is met
     */
    private boolean isThresholdMet(World world, int sleeping, int eligible) {
        String type = plugin.getConfigUtil().getThresholdType(world);
        int value = plugin.getConfigUtil().getThresholdValue(world);
        
        if (type.equalsIgnoreCase("percentage")) {
            double percentage = (double) sleeping / eligible * 100;
            return percentage >= value;
        } else {
            return sleeping >= value;
        }
    }
    
    /**
     * Get threshold as string for display
     */
    private String getThresholdString(World world) {
        String type = plugin.getConfigUtil().getThresholdType(world);
        int value = plugin.getConfigUtil().getThresholdValue(world);
        
        if (type.equalsIgnoreCase("percentage")) {
            return value + "%";
        } else {
            return String.valueOf(value);
        }
    }
    
    /**
     * Initiate night skip
     */
    private void initiateNightSkip(World world) {
        skipInProgress.put(world, true);
        
        // Broadcast threshold reached message
        Map<String, String> placeholders = new HashMap<>();
        plugin.getMessageUtil().broadcast("sleep.threshold-reached", placeholders);
        
        // Start animation
        plugin.getAnimationManager().startNightSkip(world, () -> {
            completeNightSkip(world);
        });
    }
    
    /**
     * Complete night skip
     */
    private void completeNightSkip(World world) {
        skipInProgress.put(world, false);
        
        // Wake up all sleeping players
        Set<UUID> sleeping = new HashSet<>(sleepingPlayers.getOrDefault(world, new HashSet<>()));
        for (UUID uuid : sleeping) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                wakePlayer(player);
            }
        }
        
        // Clear sleeping list
        sleepingPlayers.remove(world);
        
        // Broadcast completion
        Map<String, String> placeholders = new HashMap<>();
        plugin.getMessageUtil().broadcast("animation.complete", placeholders);
    }
    
    /**
     * Wake a player and apply effects
     */
    private void wakePlayer(Player player) {
        // Check if player has a temporary bed
        boolean hasTemporaryBed = plugin.getBedManager().hasTemporaryBed(player.getUniqueId());
        
        // Remove temporary bed if exists
        if (hasTemporaryBed) {
            plugin.getBedManager().removeBed(player.getUniqueId());
        }
        
        // Apply wake-up effects (for all players)
        applyWakeUpEffects(player);
        
        // Send wake-up message
        Map<String, String> placeholders = new HashMap<>();
        plugin.getMessageUtil().sendMessage(player, "sleep.wake-up", placeholders);
        
        // Play sound
        playWakeSound(player);
    }
    
    /**
     * Apply wake-up effects to player
     */
    private void applyWakeUpEffects(Player player) {
        if (!plugin.getConfig().getBoolean("effects.enabled", true)) {
            return;
        }
        
        var effectsSection = plugin.getConfig().getConfigurationSection("effects.effects-list");
        if (effectsSection == null) {
            return;
        }
        
        for (String effectName : effectsSection.getKeys(false)) {
            try {
                PotionEffectType effectType = PotionEffectType.getByName(effectName);
                if (effectType == null) {
                    continue;
                }
                
                int duration = effectsSection.getInt(effectName + ".duration", 5) * 20;
                int amplifier = effectsSection.getInt(effectName + ".amplifier", 0);
                
                player.addPotionEffect(new PotionEffect(effectType, duration, amplifier, true, false));
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid effect: " + effectName);
            }
        }
    }
    
    /**
     * Play wake-up sound
     */
    private void playWakeSound(Player player) {
        if (!plugin.getConfig().getBoolean("sounds.wake-up.enabled", true)) {
            return;
        }
        
        String soundName = plugin.getConfig().getString("sounds.wake-up.sound", "ENTITY_PLAYER_LEVELUP");
        float volume = (float) plugin.getConfig().getDouble("sounds.wake-up.volume", 0.5);
        float pitch = (float) plugin.getConfig().getDouble("sounds.wake-up.pitch", 1.5);
        
        try {
            player.playSound(player.getLocation(), org.bukkit.Sound.valueOf(soundName), volume, pitch);
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid wake sound: " + soundName);
        }
    }
    
    /**
     * Get number of sleeping players in a world
     */
    public int getSleepingCount(World world) {
        return sleepingPlayers.getOrDefault(world, Collections.emptySet()).size();
    }
    
    /**
     * Get number of eligible players in a world
     */
    public int getEligiblePlayerCount(World world) {
        int count = 0;
        
        for (Player player : world.getPlayers()) {
            if (isPlayerEligible(player)) {
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * Check if player is eligible for sleep counting
     */
    private boolean isPlayerEligible(Player player) {
        // Check if world is enabled
        if (!plugin.getConfigUtil().isWorldEnabled(player.getWorld())) {
            return false;
        }
        
        // Check spectator mode
        if (!plugin.getConfig().getBoolean("sleep.count-spectators", false)) {
            if (player.getGameMode() == GameMode.SPECTATOR) {
                return false;
            }
        }
        
        // Check vanished players
        if (!plugin.getConfig().getBoolean("sleep.count-vanished", false)) {
            if (player.hasMetadata("vanished")) {
                return false;
            }
        }
        
        // Check AFK players (hook integration)
        if (!plugin.getConfig().getBoolean("sleep.count-afk", false)) {
            if (plugin.getHookManager().isPlayerAFK(player)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Check if it's night time in a world
     */
    public boolean isNightTime(World world) {
        long time = world.getTime();
        int nightStart = plugin.getConfig().getInt("sleep.night-range.start", 12541);
        int nightEnd = plugin.getConfig().getInt("sleep.night-range.end", 23458);
        
        return time >= nightStart && time <= nightEnd;
    }
    
    /**
     * Broadcast sleep update to world
     */
    private void broadcastSleepUpdate(World world, String playerName, boolean joined) {
        int sleeping = getSleepingCount(world);
        int eligible = getEligiblePlayerCount(world);
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", playerName);
        placeholders.put("sleeping", String.valueOf(sleeping));
        placeholders.put("eligible", String.valueOf(eligible));
        
        String messagePath = joined ? "sleep.player-entered" : "sleep.player-left";
        
        for (Player player : world.getPlayers()) {
            plugin.getMessageUtil().sendRawMessage(player, messagePath, placeholders);
        }
    }
    
    /**
     * Start progress display task
     */
    private void startProgressDisplayTask() {
        int interval = plugin.getConfig().getInt("display.update-interval", 20);
        
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (World world : sleepingPlayers.keySet()) {
                if (getSleepingCount(world) > 0) {
                    displayProgress(world);
                    
                    // Spawn sleeping particles for all sleeping players
                    Set<UUID> sleeping = sleepingPlayers.get(world);
                    if (sleeping != null) {
                        for (UUID uuid : sleeping) {
                            Player player = Bukkit.getPlayer(uuid);
                            if (player != null && player.isSleeping()) {
                                plugin.getVisualEffectsManager().spawnSleepingParticles(player);
                            }
                        }
                    }
                }
            }
        }, interval, interval);
    }

    /**
     * Start day time cleanup task - removes all beds when it becomes day
     */
    private void startDayCleanupTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (World world : Bukkit.getWorlds()) {
                if (!plugin.getConfigUtil().isWorldEnabled(world)) {
                    continue;
                }
                
                // If it's day time, cleanup all beds in this world
                if (!isNightTime(world)) {
                    // Get all players with beds in this world
                    plugin.getBedManager().playerBeds.entrySet().stream()
                        .filter(entry -> entry.getValue().getWorld().equals(world))
                        .map(Map.Entry::getKey)
                        .forEach(uuid -> {
                            Player player = Bukkit.getPlayer(uuid);
                            if (player != null && player.isSleeping()) {
                                player.wakeup(false);
                            }
                            if (player != null) {
                                plugin.getSleepManager().removeSleepingPlayer(player);
                            }
                            plugin.getBedManager().removeBed(uuid);
                        });
                }
            }
        }, 20L, 20L); // Check every second
    }
    
    /**
     * Display sleep progress
     */
    private void displayProgress(World world) {
        String displayType = plugin.getConfig().getString("display.type", "action_bar");
        
        if (displayType.equalsIgnoreCase("none")) {
            return;
        }
        
        int sleeping = getSleepingCount(world);
        int eligible = getEligiblePlayerCount(world);
        double percentage = eligible > 0 ? (double) sleeping / eligible * 100 : 0;
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("sleeping", String.valueOf(sleeping));
        placeholders.put("eligible", String.valueOf(eligible));
        placeholders.put("percentage", plugin.getMessageUtil().formatPercentage(percentage));
        
        String messagePath = "sleep.progress." + displayType.toLowerCase().replace("_", "-");
        
        for (Player player : world.getPlayers()) {
            if (displayType.equalsIgnoreCase("action_bar")) {
                plugin.getMessageUtil().sendActionBar(player, messagePath, placeholders);
            } else {
                plugin.getMessageUtil().sendRawMessage(player, messagePath, placeholders);
            }
        }
    }
    
    /**
     * Force night skip in a world
     */
    public void forceNightSkip(World world) {
        if (!isNightTime(world)) {
            return;
        }
        
        initiateNightSkip(world);
    }
    
    /**
     * Cancel ongoing night skip
     */
    public boolean cancelNightSkip(World world) {
        if (!skipInProgress.getOrDefault(world, false)) {
            return false;
        }
        
        plugin.getAnimationManager().cancelNightSkip(world);
        skipInProgress.put(world, false);
        return true;
    }
    
    /**
     * Check if skip is in progress
     */
    public boolean isSkipInProgress(World world) {
        return skipInProgress.getOrDefault(world, false);
    }
    
    /**
     * Cleanup on disable
     */
    public void cleanup() {
        sleepingPlayers.clear();
        skipInProgress.clear();
    }
}