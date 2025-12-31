package com.kolensdeepsleep.managers;

import com.kolensdeepsleep.KolensDeepSleep;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages smooth time transition animation for night skip
 */
public class AnimationManager {
    private final KolensDeepSleep plugin;
    private final Map<World, BukkitTask> activeTasks;
    private final Map<World, Runnable> completionCallbacks;
    
    public AnimationManager(KolensDeepSleep plugin) {
        this.plugin = plugin;
        this.activeTasks = new HashMap<>();
        this.completionCallbacks = new HashMap<>();
    }
    
    /**
     * Start night skip animation
     */
    public void startNightSkip(World world, Runnable onComplete) {
        if (activeTasks.containsKey(world)) {
            return; // Animation already in progress
        }
        
        if (!plugin.getConfig().getBoolean("animation.enabled", true)) {
            // Skip animation, set time directly
            skipToMorning(world);
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }
        
        completionCallbacks.put(world, onComplete);
        
        // Clear weather if configured
        if (plugin.getConfig().getBoolean("animation.clear-weather", true)) {
            world.setStorm(false);
        }
        
        if (plugin.getConfig().getBoolean("animation.clear-thunder", true)) {
            world.setThundering(false);
        }
        
        // Get animation settings
        int speed = plugin.getConfig().getInt("animation.speed", 100);
        int interval = plugin.getConfig().getInt("animation.interval", 1);
        
        // Target time (morning)
        final long targetTime = 0; // Morning starts at 0
        
        // Start animation task
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long currentTime = world.getTime();
            
            // If we've reached morning, complete the animation
            if (currentTime >= 23458 || currentTime < 12541) {
                completeAnimation(world);
                return;
            }
            
            // Advance time
            long newTime = currentTime + speed;
            
            // Handle day wrap-around
            if (newTime >= 24000) {
                newTime = 0;
            }
            
            world.setTime(newTime);
            
        }, 0L, interval);
        
        activeTasks.put(world, task);
        plugin.getVisualEffectsManager().startTimeProgressionTitle(world);
        // Broadcast animation start
        Map<String, String> placeholders = new HashMap<>();
        plugin.getMessageUtil().broadcast("animation.starting", placeholders);
    }
    
    /**
     * Complete the animation
     */
    private void completeAnimation(World world) {
        // Cancel task
        BukkitTask task = activeTasks.remove(world);
        if (task != null) {
            task.cancel();
        }
		// Stop time progression title
        plugin.getVisualEffectsManager().stopTimeProgressionTitle(world);
        // Ensure time is set to morning
        skipToMorning(world);
        
        // Run completion callback
        Runnable callback = completionCallbacks.remove(world);
        if (callback != null) {
            callback.run();
        }
    }
    
    /**
     * Cancel ongoing animation
     */
    public void cancelNightSkip(World world) {
        BukkitTask task = activeTasks.remove(world);
        if (task != null) {
            task.cancel();
        }
        // Stop time progression title
        plugin.getVisualEffectsManager().stopTimeProgressionTitle(world);
        completionCallbacks.remove(world);
    }
    
    /**
     * Skip to morning instantly (no animation)
     */
    private void skipToMorning(World world) {
        world.setTime(0); // Set to morning
        
        // Clear weather if configured
        if (plugin.getConfig().getBoolean("animation.clear-weather", true)) {
            world.setStorm(false);
        }
        
        if (plugin.getConfig().getBoolean("animation.clear-thunder", true)) {
            world.setThundering(false);
        }
    }
    
    /**
     * Check if animation is active
     */
    public boolean isAnimationActive(World world) {
        return activeTasks.containsKey(world);
    }
    
    /**
     * Cleanup all animations
     */
    public void cleanup() {
        // Cancel all active tasks
        for (BukkitTask task : activeTasks.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        
        activeTasks.clear();
        completionCallbacks.clear();
    }
}