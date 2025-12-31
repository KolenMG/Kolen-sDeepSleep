package com.kolensdeepsleep.hooks;

import com.kolensdeepsleep.KolensDeepSleep;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * WorldGuard integration for region protection
 */
public class WorldGuardHook {
    private final KolensDeepSleep plugin;
    private boolean enabled;
    
    public WorldGuardHook(KolensDeepSleep plugin) {
        this.plugin = plugin;
        this.enabled = true;
    }
    
    /**
     * Check if WorldGuard hook is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Check if a player can place a bed at a location
     */
    public boolean canPlaceBed(Player player, Location location) {
        if (!enabled) {
            return true;
        }
        
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();
            LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
            
            com.sk89q.worldedit.util.Location wgLocation = BukkitAdapter.adapt(location);
            
            // Check BUILD flag
            return query.testState(wgLocation, localPlayer, Flags.BUILD);
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking WorldGuard region: " + e.getMessage());
            enabled = false;
            return true; // Fail open
        }
    }
}