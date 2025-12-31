package com.kolensdeepsleep.listeners;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.kolensdeepsleep.KolensDeepSleep;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

/**
 * Handles player-related events for sleep mechanics
 */
public class PlayerListener implements Listener {
    private final KolensDeepSleep plugin;
    
    public PlayerListener(KolensDeepSleep plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle player entering bed
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        Player player = event.getPlayer();
        
        // Check if it's a temporary bed
        if (plugin.getBedManager().isTemporaryBed(event.getBed())) {
            // Verify ownership
            if (!plugin.getBedManager().getBedOwner(event.getBed().getLocation())
                    .equals(player.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
            
            // Add to sleeping players list
            plugin.getSleepManager().addSleepingPlayer(player);
        } else {
            // Regular bed - check if we should track it
            if (plugin.getConfig().getBoolean("sleep.track-regular-beds", true)) {
                plugin.getSleepManager().addSleepingPlayer(player);
            }
        }
    }
    
    /**
     * Handle player leaving bed
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerBedLeave(PlayerBedLeaveEvent event) {
        Player player = event.getPlayer();
        
        // Remove from sleeping list
        plugin.getSleepManager().removeSleepingPlayer(player);
        
        // Remove temporary bed if player owns one
        if (plugin.getBedManager().hasTemporaryBed(player.getUniqueId())) {
            // Small delay to prevent bed removal before player fully wakes
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!player.isSleeping()) {
                    plugin.getBedManager().removeBed(player.getUniqueId());
                }
            }, 5L);
        }
    }
    
    /**
     * Handle player quit
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Remove from sleeping list
        plugin.getSleepManager().removeSleepingPlayer(player);
        
        // Remove temporary bed
        plugin.getBedManager().removeBed(player.getUniqueId());
        
        // Cleanup cooldowns (they persist through sessions)
        // plugin.getCooldownManager().cleanup(player.getUniqueId());
    }
    
    /**
     * Handle player death
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        // Remove from sleeping list
        plugin.getSleepManager().removeSleepingPlayer(player);
        
        // Remove temporary bed
        plugin.getBedManager().removeBed(player.getUniqueId());
    }
    
    /**
     * Handle player respawn
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        // Ensure bed is removed
        plugin.getBedManager().removeBed(player.getUniqueId());
    }
    
    /**
     * Handle player changing worlds
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        
        // Remove from old world's sleeping list
        plugin.getSleepManager().removeSleepingPlayer(player);
        
        // Remove temporary bed from old world
        plugin.getBedManager().removeBed(player.getUniqueId());
    }
    
    /**
     * Handle player teleport
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        
        // If teleporting to different world, cleanup bed
        if (!event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            plugin.getBedManager().removeBed(player.getUniqueId());
            plugin.getSleepManager().removeSleepingPlayer(player);
        }
        
        // If player is sleeping and teleports, cleanup
        if (player.isSleeping()) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!player.isSleeping()) {
                    plugin.getBedManager().removeBed(player.getUniqueId());
                }
            });
        }
    }
    
    /**
     * Handle player kick
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        
        // Remove from sleeping list
        plugin.getSleepManager().removeSleepingPlayer(player);
        
        // Remove temporary bed
        plugin.getBedManager().removeBed(player.getUniqueId());
    }
}