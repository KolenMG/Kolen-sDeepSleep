package com.kolensdeepsleep.util;

import com.kolensdeepsleep.KolensDeepSleep;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Utility class for configuration management
 */
public class ConfigUtil {
    private final KolensDeepSleep plugin;
    private FileConfiguration messagesConfig;
    
    public ConfigUtil(KolensDeepSleep plugin) {
        this.plugin = plugin;
        loadConfigs();
    }
    
    /**
     * Load all configuration files
     */
    public void loadConfigs() {
        // Save default configs if they don't exist
        plugin.saveDefaultConfig();
        saveMessagesConfig();
        
        // Reload configs
        plugin.reloadConfig();
        loadMessagesConfig();
        
        // Validate configuration
        validateConfig();
    }
    
    /**
     * Save default messages.yml
     */
    private void saveMessagesConfig() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
    }
    
    /**
     * Load messages.yml
     */
    private void loadMessagesConfig() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }
    
    /**
     * Get messages configuration
     */
    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }
    
    /**
     * Reload all configurations
     */
    public void reload() {
        loadConfigs();
    }
    
    /**
     * Validate configuration values
     */
    private void validateConfig() {
        FileConfiguration config = plugin.getConfig();
        List<String> warnings = new ArrayList<>();
        
        // Validate threshold
        String thresholdType = config.getString("sleep.threshold-type", "percentage");
        if (!thresholdType.equals("fixed") && !thresholdType.equals("percentage")) {
            warnings.add("Invalid threshold-type: " + thresholdType + ". Using 'percentage'.");
        }
        
        int thresholdValue = config.getInt("sleep.threshold-value", 50);
        if (thresholdType.equals("percentage") && (thresholdValue < 0 || thresholdValue > 100)) {
            warnings.add("Percentage threshold must be between 0 and 100. Current: " + thresholdValue);
        }
        
        // Validate animation speed
        int animationSpeed = config.getInt("animation.speed", 100);
        if (animationSpeed < 1 || animationSpeed > 1000) {
            warnings.add("Animation speed should be between 1 and 1000. Current: " + animationSpeed);
        }
        
        // Validate cooldown
        int cooldown = config.getInt("cooldown.duration", 60);
        if (cooldown < 0) {
            warnings.add("Cooldown duration cannot be negative. Current: " + cooldown);
        }
        
        // Validate combat timeout
        int combatTimeout = config.getInt("restrictions.combat.timeout", 10);
        if (combatTimeout < 0) {
            warnings.add("Combat timeout cannot be negative. Current: " + combatTimeout);
        }
        
        // Log warnings
        if (!warnings.isEmpty()) {
            plugin.getLogger().warning("Configuration validation warnings:");
            warnings.forEach(warning -> plugin.getLogger().warning("  - " + warning));
        }
    }
    
    /**
     * Check if a world is enabled for sleep mechanics
     */
    public boolean isWorldEnabled(World world) {
        List<String> enabledWorlds = plugin.getConfig().getStringList("worlds.enabled-worlds");
        return enabledWorlds.contains(world.getName());
    }
    
    /**
     * Get threshold type for a world
     */
    public String getThresholdType(World world) {
        ConfigurationSection overrides = plugin.getConfig().getConfigurationSection("worlds.overrides." + world.getName());
        if (overrides != null && overrides.contains("threshold-type")) {
            return overrides.getString("threshold-type");
        }
        return plugin.getConfig().getString("sleep.threshold-type", "percentage");
    }
    
    /**
     * Get threshold value for a world
     */
    public int getThresholdValue(World world) {
        ConfigurationSection overrides = plugin.getConfig().getConfigurationSection("worlds.overrides." + world.getName());
        if (overrides != null && overrides.contains("threshold-value")) {
            return overrides.getInt("threshold-value");
        }
        return plugin.getConfig().getInt("sleep.threshold-value", 50);
    }
    
    /**
     * Get blocked materials for bed placement
     */
    public Set<Material> getBlockedMaterials() {
        return plugin.getConfig().getStringList("placement.blocked-blocks").stream()
                .map(name -> {
                    try {
                        return Material.valueOf(name);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid material in blocked-blocks: " + name);
                        return null;
                    }
                })
                .filter(material -> material != null)
                .collect(Collectors.toSet());
    }
    
    /**
     * Check if debug mode is enabled
     */
    public boolean isDebugEnabled() {
        return plugin.getConfig().getBoolean("general.debug", false);
    }
    
    /**
     * Get bed color from config
     */
    public String getBedColor() {
        return plugin.getConfig().getString("general.bed-color", "RED").toUpperCase();
    }
}