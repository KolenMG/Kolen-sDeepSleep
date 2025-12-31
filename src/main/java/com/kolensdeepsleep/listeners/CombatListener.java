package com.kolensdeepsleep.listeners;

import com.kolensdeepsleep.KolensDeepSleep;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks player combat status
 */
public class CombatListener implements Listener {
    private final KolensDeepSleep plugin;
    private final Map<UUID, Long> combatTimestamps;
    
    public CombatListener(KolensDeepSleep plugin) {
        this.plugin = plugin;
        this.combatTimestamps = new HashMap<>();
    }
    
    /**
     * Track when player takes damage
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!plugin.getConfig().getBoolean("restrictions.combat.enabled", true)) {
            return;
        }
        
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        
        // Update combat timestamp
        combatTimestamps.put(player.getUniqueId(), System.currentTimeMillis());
        
        // Debug
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getName());
        plugin.getMessageUtil().debug("debug.combat-tagged", placeholders);
    }
    
    /**
     * Track when player damages another player (PvP)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamagePlayer(EntityDamageByEntityEvent event) {
        if (!plugin.getConfig().getBoolean("restrictions.combat.enabled", true)) {
            return;
        }
        
        if (event.getDamager() instanceof Player damager) {
            combatTimestamps.put(damager.getUniqueId(), System.currentTimeMillis());
            
            // Debug
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", damager.getName());
            plugin.getMessageUtil().debug("debug.combat-tagged", placeholders);
        }
    }
    
    /**
     * Clear combat status on quit
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        combatTimestamps.remove(event.getPlayer().getUniqueId());
    }
    
    /**
     * Check if player is in combat
     * Returns error message key if in combat, null otherwise
     */
    public String checkCombat(Player player) {
        if (!plugin.getConfig().getBoolean("restrictions.combat.enabled", true)) {
            return null;
        }
        
        UUID uuid = player.getUniqueId();
        if (!combatTimestamps.containsKey(uuid)) {
            return null;
        }
        
        long combatTime = combatTimestamps.get(uuid);
        long timeout = plugin.getConfig().getInt("restrictions.combat.timeout", 10) * 1000L;
        long elapsed = System.currentTimeMillis() - combatTime;
        
        if (elapsed >= timeout) {
            combatTimestamps.remove(uuid);
            
            // Debug
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", player.getName());
            plugin.getMessageUtil().debug("debug.combat-expired", placeholders);
            
            return null;
        }
        
        return "errors.in-combat";
    }
    
    /**
     * Get remaining combat time in seconds
     */
    public long getRemainingCombatTime(Player player) {
        UUID uuid = player.getUniqueId();
        if (!combatTimestamps.containsKey(uuid)) {
            return 0;
        }
        
        long combatTime = combatTimestamps.get(uuid);
        long timeout = plugin.getConfig().getInt("restrictions.combat.timeout", 10) * 1000L;
        long elapsed = System.currentTimeMillis() - combatTime;
        
        if (elapsed >= timeout) {
            return 0;
        }
        
        return (timeout - elapsed) / 1000;
    }
    
    /**
     * Clear all combat timestamps
     */
    public void cleanup() {
        combatTimestamps.clear();
    }
}