package com.kolensdeepsleep.managers;

import com.kolensdeepsleep.KolensDeepSleep;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages cooldowns for the /bed command
 */
public class CooldownManager {
    private final KolensDeepSleep plugin;
    private final Map<UUID, Long> cooldowns;
    private final Map<UUID, Integer> perNightUsage;
    private final Map<UUID, Long> lastNightTime;
    
    public CooldownManager(KolensDeepSleep plugin) {
        this.plugin = plugin;
        this.cooldowns = new HashMap<>();
        this.perNightUsage = new HashMap<>();
        this.lastNightTime = new HashMap<>();
    }
    
    /**
     * Check if player is on cooldown
     * Returns remaining seconds if on cooldown, 0 if not
     */
    public long getCooldownRemaining(Player player) {
        if (player.hasPermission("deepsleep.bypass.cooldown")) {
            return 0;
        }
        
        if (!plugin.getConfig().getBoolean("cooldown.enabled", true)) {
            return 0;
        }
        
        UUID uuid = player.getUniqueId();
        if (!cooldowns.containsKey(uuid)) {
            return 0;
        }
        
        long cooldownEnd = cooldowns.get(uuid);
        long now = System.currentTimeMillis();
        
        if (now >= cooldownEnd) {
            cooldowns.remove(uuid);
            return 0;
        }
        
        return (cooldownEnd - now) / 1000;
    }
    
    /**
     * Set cooldown for a player
     */
    public void setCooldown(Player player) {
        if (player.hasPermission("deepsleep.bypass.cooldown")) {
            return;
        }
        
        if (!plugin.getConfig().getBoolean("cooldown.enabled", true)) {
            return;
        }
        
        int duration = plugin.getConfig().getInt("cooldown.duration", 60);
        long cooldownEnd = System.currentTimeMillis() + (duration * 1000L);
        cooldowns.put(player.getUniqueId(), cooldownEnd);
    }
    
    /**
     * Check if player has exceeded per-night usage limit
     * Returns remaining uses if limited, -1 if no limit or exceeded
     */
    public int checkPerNightLimit(Player player) {
        if (!plugin.getConfig().getBoolean("cooldown.per-night-limit.enabled", false)) {
            return -1; // No limit
        }
        
        int maxUses = plugin.getConfig().getInt("cooldown.per-night-limit.max-uses", 3);
        UUID uuid = player.getUniqueId();
        
        // Get current night cycle
        long currentNightCycle = getNightCycle(player.getWorld().getTime());
        
        // Reset counter if it's a new night
        if (!lastNightTime.containsKey(uuid) || lastNightTime.get(uuid) != currentNightCycle) {
            perNightUsage.put(uuid, 0);
            lastNightTime.put(uuid, currentNightCycle);
        }
        
        int uses = perNightUsage.getOrDefault(uuid, 0);
        
        if (uses >= maxUses) {
            return 0; // Limit exceeded
        }
        
        return maxUses - uses; // Remaining uses
    }
    
    /**
     * Increment per-night usage counter
     */
    public void incrementPerNightUsage(Player player) {
        if (!plugin.getConfig().getBoolean("cooldown.per-night-limit.enabled", false)) {
            return;
        }
        
        UUID uuid = player.getUniqueId();
        long currentNightCycle = getNightCycle(player.getWorld().getTime());
        
        // Ensure we're tracking the current night
        if (!lastNightTime.containsKey(uuid) || lastNightTime.get(uuid) != currentNightCycle) {
            perNightUsage.put(uuid, 1);
            lastNightTime.put(uuid, currentNightCycle);
        } else {
            perNightUsage.put(uuid, perNightUsage.getOrDefault(uuid, 0) + 1);
        }
    }
    
    /**
     * Get current night cycle number (increments each night)
     */
    private long getNightCycle(long worldTime) {
        // Each full day-night cycle is 24000 ticks
        // Night starts at 12541 and ends at 23458
        return worldTime / 24000;
    }
    
    /**
     * Clear cooldown for a player
     */
    public void clearCooldown(UUID uuid) {
        cooldowns.remove(uuid);
    }
    
    /**
     * Clear all cooldowns
     */
    public void clearAllCooldowns() {
        cooldowns.clear();
    }
    
    /**
     * Clear per-night usage for a player
     */
    public void clearPerNightUsage(UUID uuid) {
        perNightUsage.remove(uuid);
        lastNightTime.remove(uuid);
    }
    
    /**
     * Clear all per-night usage data
     */
    public void clearAllPerNightUsage() {
        perNightUsage.clear();
        lastNightTime.clear();
    }
    
    /**
     * Cleanup cooldowns on player quit
     */
    public void cleanup(UUID uuid) {
        // Keep cooldowns and per-night usage even after logout
        // They will naturally expire or reset
    }
    
    /**
     * Get current usage count for a player
     */
    public int getCurrentUsage(Player player) {
        UUID uuid = player.getUniqueId();
        long currentNightCycle = getNightCycle(player.getWorld().getTime());
        
        if (!lastNightTime.containsKey(uuid) || lastNightTime.get(uuid) != currentNightCycle) {
            return 0;
        }
        
        return perNightUsage.getOrDefault(uuid, 0);
    }
}