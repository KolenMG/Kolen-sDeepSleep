package com.kolensdeepsleep.hooks;

import com.kolensdeepsleep.KolensDeepSleep;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI integration for custom placeholders
 */
public class PlaceholderAPIHook extends PlaceholderExpansion {
    private final KolensDeepSleep plugin;
    
    public PlaceholderAPIHook(KolensDeepSleep plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "deepsleep";
    }
    
    @Override
    public @NotNull String getAuthor() {
        return "Kolen";
    }
    
    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer == null || !offlinePlayer.isOnline()) {
            return "";
        }
        
        Player player = offlinePlayer.getPlayer();
        if (player == null) {
            return "";
        }
        
        World world = player.getWorld();
        
        return switch (params.toLowerCase()) {
            case "sleeping" -> String.valueOf(plugin.getSleepManager().getSleepingCount(world));
            case "eligible" -> String.valueOf(plugin.getSleepManager().getEligiblePlayerCount(world));
            case "percentage" -> {
                int sleeping = plugin.getSleepManager().getSleepingCount(world);
                int eligible = plugin.getSleepManager().getEligiblePlayerCount(world);
                double percentage = eligible > 0 ? (double) sleeping / eligible * 100 : 0;
                yield String.format("%.0f", percentage);
            }
            case "cooldown" -> {
                long remaining = plugin.getCooldownManager().getCooldownRemaining(player);
                yield plugin.getMessageUtil().formatTime(remaining);
            }
            case "cooldown_seconds" -> String.valueOf(plugin.getCooldownManager().getCooldownRemaining(player));
            case "is_night" -> String.valueOf(plugin.getSleepManager().isNightTime(world));
            case "has_bed" -> String.valueOf(plugin.getBedManager().hasTemporaryBed(player.getUniqueId()));
            case "skip_active" -> String.valueOf(plugin.getSleepManager().isSkipInProgress(world));
            default -> null;
        };
    }
}