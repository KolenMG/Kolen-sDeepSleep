package com.kolensdeepsleep.commands;

import com.kolensdeepsleep.KolensDeepSleep;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles the /bed command
 */
public class BedCommand implements CommandExecutor {
    private final KolensDeepSleep plugin;
    
    public BedCommand(KolensDeepSleep plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if sender is a player
        if (!(sender instanceof Player player)) {
            Map<String, String> placeholders = new HashMap<>();
            plugin.getMessageUtil().sendMessage(sender, "commands.player-only", placeholders);
            return true;
        }
        
        // Check permission
        String permission = plugin.getConfig().getString("general.permission", "deepsleep.bed");
        if (!player.hasPermission(permission)) {
            Map<String, String> placeholders = new HashMap<>();
            plugin.getMessageUtil().sendMessage(player, "commands.no-permission", placeholders);
            return true;
        }
        
        // Check if player already has a bed
        if (plugin.getBedManager().hasTemporaryBed(player.getUniqueId())) {
            Map<String, String> placeholders = new HashMap<>();
            plugin.getMessageUtil().sendMessage(player, "errors.already-sleeping", placeholders);
            return true;
        }
        
        // Check if it's night time
        if (!plugin.getSleepManager().isNightTime(player.getWorld())) {
            Map<String, String> placeholders = new HashMap<>();
            plugin.getMessageUtil().sendMessage(player, "errors.not-night", placeholders);
            return true;
        }
        
        // Validate placement conditions
        String validationError = plugin.getBedManager().getValidator().validate(player);
        if (validationError != null) {
            Map<String, String> placeholders = new HashMap<>();
            
            // Special handling for combat error
            if (validationError.equals("errors.in-combat")) {
                long remaining = plugin.getCombatListener().getRemainingCombatTime(player);
                placeholders.put("time", plugin.getMessageUtil().formatTime(remaining));
            }
            
            plugin.getMessageUtil().sendMessage(player, validationError, placeholders);
            return true;
        }
        
        // Check cooldown
        long cooldownRemaining = plugin.getCooldownManager().getCooldownRemaining(player);
        if (cooldownRemaining > 0) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("time", plugin.getMessageUtil().formatTime(cooldownRemaining));
            plugin.getMessageUtil().sendMessage(player, "commands.bed.cooldown", placeholders);
            return true;
        }
        
        // Check per-night limit
        int remainingUses = plugin.getCooldownManager().checkPerNightLimit(player);
        if (remainingUses == 0) {
            Map<String, String> placeholders = new HashMap<>();
            int maxUses = plugin.getConfig().getInt("cooldown.per-night-limit.max-uses", 3);
            int currentUses = plugin.getCooldownManager().getCurrentUsage(player);
            placeholders.put("uses", String.valueOf(currentUses));
            placeholders.put("max", String.valueOf(maxUses));
            plugin.getMessageUtil().sendMessage(player, "commands.bed.per-night-limit", placeholders);
            return true;
        }
        
        // Attempt to spawn bed
        boolean success = plugin.getBedManager().spawnBed(player);
        
        if (success) {
            // Set cooldown
            plugin.getCooldownManager().setCooldown(player);
            
            // Increment per-night usage
            plugin.getCooldownManager().incrementPerNightUsage(player);
            
            // Send success message
            Map<String, String> placeholders = new HashMap<>();
            plugin.getMessageUtil().sendMessage(player, "commands.bed.success", placeholders);
        } else {
            // Failed to find safe spot
            Map<String, String> placeholders = new HashMap<>();
            plugin.getMessageUtil().sendMessage(player, "errors.no-safe-spot", placeholders);
        }
        
        return true;
    }
}