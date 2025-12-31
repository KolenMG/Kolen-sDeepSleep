package com.kolensdeepsleep;

import com.kolensdeepsleep.commands.AdminCommand;
import com.kolensdeepsleep.commands.BedCommand;
import com.kolensdeepsleep.hooks.HookManager;
import com.kolensdeepsleep.listeners.BedListener;
import com.kolensdeepsleep.listeners.CombatListener;
import com.kolensdeepsleep.listeners.PlayerListener;
import com.kolensdeepsleep.managers.*;
import com.kolensdeepsleep.util.ConfigUtil;
import com.kolensdeepsleep.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Main plugin class for Kolen's DeepSleep
 * Provides temporary beds and smooth night skip mechanics
 */
public final class KolensDeepSleep extends JavaPlugin {
    
    // Utility classes
    private ConfigUtil configUtil;
    private MessageUtil messageUtil;
    
    // Managers
    private BedManager bedManager;
    private SleepManager sleepManager;
    private AnimationManager animationManager;
    private CooldownManager cooldownManager;
    private HookManager hookManager;
    private VisualEffectsManager visualEffectsManager;
    
    // Listeners
    private CombatListener combatListener;
    private PlayerListener playerListener;
    private BedListener bedListener;
    
    @Override
    public void onEnable() {
        getLogger().info("Enabling Kolen's DeepSleep...");
        
        // Load utilities
        configUtil = new ConfigUtil(this);
        messageUtil = new MessageUtil(this);
        
        // Load managers
        loadManagers();
        
        // Register listeners
        registerListeners();
        
        // Register commands
        registerCommands();
        
        // Load hooks
        hookManager.loadHooks();
        
        // Detect conflicts
        detectConflicts();
        
        // Cleanup any leftover beds from previous sessions
        bedManager.cleanupAll();
        
        getLogger().info("Kolen's DeepSleep enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("Disabling Kolen's DeepSleep...");

        if (visualEffectsManager != null) {
            visualEffectsManager.cleanup();
        }
        // Cleanup all managers
        if (animationManager != null) {
            animationManager.cleanup();
        }
        
        if (sleepManager != null) {
            sleepManager.cleanup();
        }
        
        if (bedManager != null) {
            bedManager.cleanupAll();
        }
        
        if (combatListener != null) {
            combatListener.cleanup();
        }
        
        if (cooldownManager != null) {
            cooldownManager.clearAllCooldowns();
        }
        
        getLogger().info("Kolen's DeepSleep disabled successfully!");
    }
    
    /**
     * Load all managers
     */
    private void loadManagers() {
        hookManager = new HookManager(this);
        cooldownManager = new CooldownManager(this);
        visualEffectsManager = new VisualEffectsManager(this);
        bedManager = new BedManager(this);
        animationManager = new AnimationManager(this);
        sleepManager = new SleepManager(this);
    }
    
    /**
     * Register all event listeners
     */
    private void registerListeners() {
        combatListener = new CombatListener(this);
        playerListener = new PlayerListener(this);
        bedListener = new BedListener(this);
        
        getServer().getPluginManager().registerEvents(combatListener, this);
        getServer().getPluginManager().registerEvents(playerListener, this);
        getServer().getPluginManager().registerEvents(bedListener, this);
    }
    
    /**
     * Register all commands
     */
    private void registerCommands() {
        // Register /bed command
        PluginCommand bedCommand = getCommand("bed");
        if (bedCommand != null) {
            bedCommand.setExecutor(new BedCommand(this));
        } else {
            getLogger().severe("Failed to register /bed command!");
        }
        
        // Register /deepsleep command
        PluginCommand adminCommand = getCommand("deepsleep");
        if (adminCommand != null) {
            adminCommand.setExecutor(new AdminCommand(this));
        } else {
            getLogger().severe("Failed to register /deepsleep command!");
        }
    }
    
    /**
     * Detect conflicting plugins
     */
    private void detectConflicts() {
        if (!getConfig().getBoolean("conflict-detection.enabled", true)) {
            return;
        }
        
        List<String> detectPlugins = getConfig().getStringList("conflict-detection.detect-plugins");
        
        for (String pluginName : detectPlugins) {
            if (Bukkit.getPluginManager().getPlugin(pluginName) != null) {
                getLogger().warning("Detected potentially conflicting plugin: " + pluginName);
                getLogger().warning("This may cause issues with sleep mechanics.");
                
                // Broadcast warning to online admins
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    Bukkit.getOnlinePlayers().stream()
                            .filter(p -> p.hasPermission("deepsleep.admin"))
                            .forEach(admin -> {
                                var placeholders = messageUtil.createPlaceholders();
                                placeholders.put("plugin", pluginName);
                                messageUtil.sendMessage(admin, "warnings.conflict-detected", placeholders);
                            });
                }, 20L); // Delay 1 second to ensure players are loaded
            }
        }
    }
    
    /**
     * Reload plugin configuration
     */
    public void reload() {
        configUtil.reload();
        getLogger().info("Configuration reloaded!");
    }
    
    // Getters for managers and utilities
    
    public ConfigUtil getConfigUtil() {
        return configUtil;
    }
    
    public MessageUtil getMessageUtil() {
        return messageUtil;
    }
    
    public BedManager getBedManager() {
        return bedManager;
    }
    
    public SleepManager getSleepManager() {
        return sleepManager;
    }
    
    public AnimationManager getAnimationManager() {
        return animationManager;
    }
    
    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }
    
    public HookManager getHookManager() {
        return hookManager;
    }
    
    public CombatListener getCombatListener() {
        return combatListener;
    }
    
    public PlayerListener getPlayerListener() {
        return playerListener;
    }
    
    public BedListener getBedListener() {
        return bedListener;
    }
    public VisualEffectsManager getVisualEffectsManager() {
        return visualEffectsManager;
    }
}
