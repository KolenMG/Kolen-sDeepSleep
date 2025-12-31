package com.kolensdeepsleep.hooks;

import com.kolensdeepsleep.KolensDeepSleep;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages all external plugin hooks
 */
public class HookManager {
    private final KolensDeepSleep plugin;
    private WorldGuardHook worldGuardHook;
    private PlaceholderAPIHook placeholderAPIHook;
    
    public HookManager(KolensDeepSleep plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Load all available hooks
     */
    public void loadHooks() {
        // Load WorldGuard hook
        if (plugin.getConfig().getBoolean("hooks.worldguard", true)) {
            if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
                try {
                    worldGuardHook = new WorldGuardHook(plugin);
                    plugin.getLogger().info("Hooked into WorldGuard successfully!");
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to hook into WorldGuard: " + e.getMessage());
                    sendHookFailedWarning("WorldGuard");
                }
            }
        }
        
        // Load PlaceholderAPI hook
        if (plugin.getConfig().getBoolean("hooks.placeholderapi", true)) {
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                try {
                    placeholderAPIHook = new PlaceholderAPIHook(plugin);
                    placeholderAPIHook.register();
                    plugin.getLogger().info("Hooked into PlaceholderAPI successfully!");
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to hook into PlaceholderAPI: " + e.getMessage());
                    sendHookFailedWarning("PlaceholderAPI");
                }
            }
        }
    }
    
    /**
     * Check if a player is AFK
     * Supports Essentials and CMI
     */
    public boolean isPlayerAFK(Player player) {
        if (!plugin.getConfig().getBoolean("hooks.afk-detection", true)) {
            return false;
        }
        
        // Check Essentials
        if (Bukkit.getPluginManager().getPlugin("Essentials") != null) {
            try {
                return player.hasMetadata("afk");
            } catch (Exception e) {
                // Ignore
            }
        }
        
        // Check CMI
        if (Bukkit.getPluginManager().getPlugin("CMI") != null) {
            try {
                // CMI integration would go here
                // This requires CMI API dependency
                // For now, return false
            } catch (Exception e) {
                // Ignore
            }
        }
        
        return false;
    }
    
    /**
     * Get WorldGuard hook
     */
    public WorldGuardHook getWorldGuardHook() {
        return worldGuardHook;
    }
    
    /**
     * Get PlaceholderAPI hook
     */
    public PlaceholderAPIHook getPlaceholderAPIHook() {
        return placeholderAPIHook;
    }
    
    /**
     * Send hook failed warning to admins
     */
    private void sendHookFailedWarning(String pluginName) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.hasPermission("deepsleep.admin"))
                    .forEach(admin -> {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("plugin", pluginName);
                        plugin.getMessageUtil().sendMessage(admin, "warnings.hook-failed", placeholders);
                    });
        }, 20L);
    }
    
    /**
     * Unload all hooks
     */
    public void unloadHooks() {
        if (placeholderAPIHook != null) {
            placeholderAPIHook.unregister();
        }
    }
}