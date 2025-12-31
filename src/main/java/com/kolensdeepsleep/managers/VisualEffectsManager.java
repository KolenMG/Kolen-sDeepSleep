package com.kolensdeepsleep.managers;

import com.kolensdeepsleep.KolensDeepSleep;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages visual effects during sleep and night skip
 */
public class VisualEffectsManager {
    private final KolensDeepSleep plugin;
    private final Map<World, BukkitTask> titleTasks;
    
    public VisualEffectsManager(KolensDeepSleep plugin) {
        this.plugin = plugin;
        this.titleTasks = new HashMap<>();
    }
    
    /**
     * Spawn particles around a bed when it's placed
     */
    public void spawnBedParticles(Location bedLocation) {
        if (!plugin.getConfig().getBoolean("visual-effects.particles.bed-spawn", true)) {
            return;
        }
        
        World world = bedLocation.getWorld();
        if (world == null) return;
        
        // Sparkle particles around the bed
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 20) { // 1 second
                    cancel();
                    return;
                }
                
                // Create a circle of particles around the bed
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 * i) / 8;
                    double x = bedLocation.getX() + 0.5 + Math.cos(angle) * 1.5;
                    double z = bedLocation.getZ() + 0.5 + Math.sin(angle) * 1.5;
                    double y = bedLocation.getY() + 0.5;
                    
                    world.spawnParticle(Particle.ENCHANT, x, y, z, 3, 0.1, 0.1, 0.1, 0.5);
                    world.spawnParticle(Particle.END_ROD, x, y, z, 1, 0, 0.2, 0, 0.02);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    /**
     * Spawn particles above a sleeping player
     */
    public void spawnSleepingParticles(Player player) {
        if (!plugin.getConfig().getBoolean("visual-effects.particles.sleeping", true)) {
            return;
        }
        
        Location loc = player.getLocation().add(0, 2, 0);
        
        // Z particles (sleeping indicator)
        player.getWorld().spawnParticle(Particle.ENCHANT, 
            loc.getX(), loc.getY(), loc.getZ(), 
            2, 0.3, 0.3, 0.3, 0.5);
    }
    
    /**
     * Spawn wake-up particles around a player
     */
    public void spawnWakeUpParticles(Player player) {
        if (!plugin.getConfig().getBoolean("visual-effects.particles.wake-up", true)) {
            return;
        }
        
        Location loc = player.getLocation().add(0, 1, 0);
        
        // Sun rays effect
        player.getWorld().spawnParticle(Particle.END_ROD, 
            loc.getX(), loc.getY(), loc.getZ(), 
            20, 0.5, 0.5, 0.5, 0.1);
            
        player.getWorld().spawnParticle(Particle.FIREWORK, 
            loc.getX(), loc.getY(), loc.getZ(), 
            30, 0.3, 0.5, 0.3, 0.05);
    }
    
    /**
     * Start displaying time progression title during night skip
     */
    public void startTimeProgressionTitle(World world) {
        if (!plugin.getConfig().getBoolean("visual-effects.time-display.enabled", true)) {
            return;
        }
        
        // Cancel existing task if any
        stopTimeProgressionTitle(world);
        
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                // Check if night skip is still active
                if (!plugin.getSleepManager().isSkipInProgress(world)) {
                    cancel();
                    titleTasks.remove(world);
                    return;
                }
                
                // Get current world time
                long worldTime = world.getTime();
                String timeString = formatMinecraftTime(worldTime);
                
                // Get sleeping stats
                int sleeping = plugin.getSleepManager().getSleepingCount(world);
                int eligible = plugin.getSleepManager().getEligiblePlayerCount(world);
                
                // Calculate percentage
                double percentage = calculateNightPercentage(worldTime);
                
                // Get format from config
                String titleFormat = plugin.getConfig().getString("visual-effects.time-display.title-format", 
                    "<gold>⏰ %time%</gold>");
                String subtitleFormat = plugin.getConfig().getString("visual-effects.time-display.subtitle-format", 
                    "<aqua>💤 %sleeping%/%eligible% sleeping | %percentage%% complete</aqua>");
                
                // Replace placeholders
                titleFormat = titleFormat
                    .replace("%time%", timeString)
                    .replace("%sleeping%", String.valueOf(sleeping))
                    .replace("%eligible%", String.valueOf(eligible))
                    .replace("%percentage%", String.format("%.0f", percentage));
                    
                subtitleFormat = subtitleFormat
                    .replace("%time%", timeString)
                    .replace("%sleeping%", String.valueOf(sleeping))
                    .replace("%eligible%", String.valueOf(eligible))
                    .replace("%percentage%", String.format("%.0f", percentage));
                
                // Parse with MiniMessage
                Component titleComponent = plugin.getMessageUtil().parseMessage(titleFormat);
                Component subtitleComponent = plugin.getMessageUtil().parseMessage(subtitleFormat);
                
                // Create title with timings
                Title title = Title.title(
                    titleComponent,
                    subtitleComponent,
                    Title.Times.times(
                        Duration.ofMillis(0),      // Fade in
                        Duration.ofMillis(1000),   // Stay
                        Duration.ofMillis(200)     // Fade out
                    )
                );
                
                // Show to all players in the world
                for (Player player : world.getPlayers()) {
                    player.showTitle(title);
                }
            }
        }.runTaskTimer(plugin, 0L, 10L); // Update every 0.5 seconds
        
        titleTasks.put(world, task);
    }
    
    /**
     * Stop displaying time progression title
     */
    public void stopTimeProgressionTitle(World world) {
        BukkitTask task = titleTasks.remove(world);
        if (task != null) {
            task.cancel();
        }
        
        // Clear titles for all players in world
        for (Player player : world.getPlayers()) {
            player.clearTitle();
        }
    }
    
    /**
     * Show morning message to a player
     */
    public void showMorningMessage(Player player) {
        if (!plugin.getConfig().getBoolean("visual-effects.morning-message.enabled", true)) {
            return;
        }
        
        // Get messages from config
        String titleFormat = plugin.getConfig().getString("visual-effects.morning-message.title", 
            "<gradient:yellow:gold>☀ Good Morning!</gradient>");
        String subtitleFormat = plugin.getConfig().getString("visual-effects.morning-message.subtitle", 
            "<green>You slept well!</green>");
        
        Component titleComponent = plugin.getMessageUtil().parseMessage(titleFormat);
        Component subtitleComponent = plugin.getMessageUtil().parseMessage(subtitleFormat);
        
        // Create title with timings
        Title title = Title.title(
            titleComponent,
            subtitleComponent,
            Title.Times.times(
                Duration.ofMillis(500),    // Fade in
                Duration.ofSeconds(3),     // Stay
                Duration.ofMillis(1000)    // Fade out
            )
        );
        
        player.showTitle(title);
        
        // Send detailed message in chat
        if (plugin.getConfig().getBoolean("visual-effects.morning-message.chat-details", true)) {
            Map<String, String> placeholders = new HashMap<>();
            plugin.getMessageUtil().sendRawMessage(player, "sleep.morning-details", placeholders);
        }
    }
    
    /**
     * Format Minecraft time to readable format
     */
    private String formatMinecraftTime(long worldTime) {
        // Minecraft time: 0 = 6 AM, 6000 = noon, 12000 = 6 PM, 18000 = midnight
        // Normalize to 0-24000
        worldTime = worldTime % 24000;
        
        // Calculate hours and minutes
        // 1000 ticks = 1 hour in Minecraft
        double totalMinutes = (worldTime + 6000) / 16.67;
        long hours = (long)(totalMinutes / 60) % 24;
        long minutes = (long)(totalMinutes % 60);
        
        
        // Determine AM/PM
        String period;
        int displayHour;
        
        if (hours == 0) {
            displayHour = 12;
            period = "AM";
        } else if (hours < 12) {
            displayHour = (int) hours;
            period = "AM";
        } else if (hours == 12) {
            displayHour = 12;
            period = "PM";
        } else {
            displayHour = (int) (hours - 12);
            period = "PM";
        }
        
        return String.format("%d:%02d %s", displayHour, minutes, period);
    }
    
    /**
     * Calculate percentage of night completed
     */
    private double calculateNightPercentage(long worldTime) {
        int nightStart = plugin.getConfig().getInt("sleep.night-range.start", 12541);
        int nightEnd = plugin.getConfig().getInt("sleep.night-range.end", 23458);
        
        long nightDuration = nightEnd - nightStart;
        long elapsed = worldTime - nightStart;
        
        if (elapsed < 0) {
            return 0;
        }
        
        return Math.min(100, (elapsed * 100.0) / nightDuration);
    }
    
    /**
     * Cleanup all effects
     */
    public void cleanup() {
        // Cancel all title tasks
        for (BukkitTask task : titleTasks.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        titleTasks.clear();
        
        // Clear titles for all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.clearTitle();
        }
    }
}