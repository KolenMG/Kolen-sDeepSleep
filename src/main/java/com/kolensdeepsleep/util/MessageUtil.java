package com.kolensdeepsleep.util;

import com.kolensdeepsleep.KolensDeepSleep;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for message formatting and sending
 */
public class MessageUtil {
    private final KolensDeepSleep plugin;
    private final MiniMessage miniMessage;
    
    public MessageUtil(KolensDeepSleep plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }
    
    /**
     * Get a message from messages.yml
     */
    public String getMessage(String path) {
        FileConfiguration config = plugin.getConfigUtil().getMessagesConfig();
        return config.getString(path, "Message not found: " + path);
    }
    
    /**
     * Get a message with prefix
     */
    public String getMessageWithPrefix(String path) {
        String prefix = getMessage("prefix");
        String message = getMessage(path);
        return prefix + message;
    }
    
    /**
     * Send a message to a player
     */
    public void sendMessage(CommandSender sender, String path) {
        sendMessage(sender, path, new HashMap<>());
    }
    
    /**
     * Send a message to a player with placeholders
     */
    public void sendMessage(CommandSender sender, String path, Map<String, String> placeholders) {
        String message = getMessageWithPrefix(path);
        message = replacePlaceholders(message, placeholders);
        
        Component component = miniMessage.deserialize(message);
        sender.sendMessage(component);
    }
    
    /**
     * Send a raw message without prefix
     */
    public void sendRawMessage(CommandSender sender, String path, Map<String, String> placeholders) {
        String message = getMessage(path);
        message = replacePlaceholders(message, placeholders);
        
        Component component = miniMessage.deserialize(message);
        sender.sendMessage(component);
    }
    
    /**
     * Send an action bar message
     */
    public void sendActionBar(Player player, String path, Map<String, String> placeholders) {
        String message = getMessage(path);
        message = replacePlaceholders(message, placeholders);
        
        Component component = miniMessage.deserialize(message);
        player.sendActionBar(component);
    }
    
    /**
     * Parse a message with MiniMessage format
     */
    public Component parseMessage(String message) {
        return miniMessage.deserialize(message);
    }
    
    /**
     * Replace placeholders in a message
     */
    private String replacePlaceholders(String message, Map<String, String> placeholders) {
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }
    
    /**
     * Format time remaining (seconds to readable format)
     */
    public String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            long minutes = TimeUnit.SECONDS.toMinutes(seconds);
            long remainingSeconds = seconds - TimeUnit.MINUTES.toSeconds(minutes);
            return minutes + "m " + remainingSeconds + "s";
        } else {
            long hours = TimeUnit.SECONDS.toHours(seconds);
            long remainingMinutes = TimeUnit.SECONDS.toMinutes(seconds) - TimeUnit.HOURS.toMinutes(hours);
            return hours + "h " + remainingMinutes + "m";
        }
    }
    
    /**
     * Format percentage
     */
    public String formatPercentage(double value) {
        return String.format("%.0f", value);
    }
    
    /**
     * Send debug message if debug is enabled
     */
    public void debug(String message) {
        if (plugin.getConfigUtil().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }
    
    /**
     * Send debug message with placeholders
     */
    public void debug(String path, Map<String, String> placeholders) {
        if (plugin.getConfigUtil().isDebugEnabled()) {
            String message = getMessage(path);
            message = replacePlaceholders(message, placeholders);
            
            // Strip MiniMessage formatting for console
            Component component = miniMessage.deserialize(message);
            String plain = LegacyComponentSerializer.legacySection().serialize(component);
            plugin.getLogger().info(plain);
        }
    }
    
    /**
     * Broadcast a message to all players
     */
    public void broadcast(String path, Map<String, String> placeholders) {
        String message = getMessage(path);
        message = replacePlaceholders(message, placeholders);
        
        Component component = miniMessage.deserialize(message);
        plugin.getServer().broadcast(component);
    }
    
    /**
     * Create a placeholder map
     */
    public Map<String, String> createPlaceholders() {
        return new HashMap<>();
    }
}