package com.kolensdeepsleep.validators;

import com.kolensdeepsleep.KolensDeepSleep;
import com.kolensdeepsleep.hooks.WorldGuardHook;
import com.kolensdeepsleep.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

/**
 * Validates bed placement conditions
 */
public class BedPlacementValidator {
    private final KolensDeepSleep plugin;
    private final MessageUtil messageUtil;
    private final Set<Material> blockedMaterials;
    
    public BedPlacementValidator(KolensDeepSleep plugin) {
        this.plugin = plugin;
        this.messageUtil = plugin.getMessageUtil();
        this.blockedMaterials = plugin.getConfigUtil().getBlockedMaterials();
    }
    
    /**
     * Validate all conditions for bed placement
     * Returns null if valid, error message key if invalid
     */
    public String validate(Player player) {
        FileConfiguration config = plugin.getConfig();
        
        // Check if player is on ground
        if (config.getBoolean("restrictions.require-on-ground", true) && !player.isOnGround()) {
            return "errors.not-on-ground";
        }
        
        // Check if player is flying
        if (config.getBoolean("restrictions.block-flying", true) && player.isFlying()) {
            return "errors.flying";
        }
        
        // Check if player is swimming
        if (config.getBoolean("restrictions.block-swimming", true) && player.isSwimming()) {
            return "errors.swimming";
        }
        
        // Check if player is gliding
        if (config.getBoolean("restrictions.block-gliding", true) && player.isGliding()) {
            return "errors.gliding";
        }
        
        // Check if player is in vehicle
        if (config.getBoolean("restrictions.block-in-vehicle", true) && player.isInsideVehicle()) {
            return "errors.in-vehicle";
        }
        
        // Check combat status
        if (!player.hasPermission("deepsleep.bypass.combat")) {
            String combatError = plugin.getCombatListener().checkCombat(player);
            if (combatError != null) {
                return combatError;
            }
        }
        
        // Check nearby hostile mobs
        if (!player.hasPermission("deepsleep.bypass.restrictions")) {
            if (config.getBoolean("restrictions.hostile-mobs.enabled", true)) {
                if (hasNearbyHostileMobs(player)) {
                    return "errors.hostile-mobs";
                }
            }
        }
        
        // Check WorldGuard regions
        if (!player.hasPermission("deepsleep.bypass.restrictions")) {
            if (config.getBoolean("restrictions.regions.worldguard", true)) {
                WorldGuardHook wgHook = plugin.getHookManager().getWorldGuardHook();
                if (wgHook != null && wgHook.isEnabled()) {
                    if (!wgHook.canPlaceBed(player, player.getLocation())) {
                        return "errors.protected-region";
                    }
                }
            }
        }
        
        // Check if world is enabled
        if (!plugin.getConfigUtil().isWorldEnabled(player.getWorld())) {
            return "errors.world-disabled";
        }
        
        // All checks passed
        return null;
    }
    
    /**
     * Find a safe location to place a bed near the player
     */
    public Location findSafeBedLocation(Player player) {
        Location playerLoc = player.getLocation();
        BlockFace facing = getCardinalDirection(player);
        int searchRadius = plugin.getConfig().getInt("placement.search-radius", 3);
        
        // Try to place bed in front of player first
        Location bedLoc = findSafeLocationInDirection(playerLoc, facing);
        if (bedLoc != null) {
            return bedLoc;
        }
        
        // Search in a radius around player
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int z = -searchRadius; z <= searchRadius; z++) {
                for (int y = -1; y <= 1; y++) {
                    Location testLoc = playerLoc.clone().add(x, y, z);
                    if (isSafeBedLocation(testLoc, facing)) {
                        return testLoc;
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Find safe location in a specific direction
     */
    private Location findSafeLocationInDirection(Location start, BlockFace facing) {
        for (int distance = 1; distance <= 3; distance++) {
            Location testLoc = start.clone().add(
                facing.getModX() * distance,
                0,
                facing.getModZ() * distance
            );
            
            if (isSafeBedLocation(testLoc, facing)) {
                return testLoc;
            }
        }
        return null;
    }
    
    /**
     * Check if a location is safe for bed placement
     */
    private boolean isSafeBedLocation(Location location, BlockFace facing) {
        Block block = location.getBlock();
        Block below = block.getRelative(BlockFace.DOWN);
        Block above = block.getRelative(BlockFace.UP);
        
        // Check if ground is solid
        if (!below.getType().isSolid() || !below.getType().isOccluding()) {
            return false;
        }
        
        // Check if placement area is air
        if (!block.getType().isAir() || !above.getType().isAir()) {
            return false;
        }
        
        // Check second half of bed
        Block secondHalf = block.getRelative(facing);
        Block secondAbove = secondHalf.getRelative(BlockFace.UP);
        Block secondBelow = secondHalf.getRelative(BlockFace.DOWN);
        
        if (!secondHalf.getType().isAir() || !secondAbove.getType().isAir()) {
            return false;
        }
        
        if (!secondBelow.getType().isSolid() || !secondBelow.getType().isOccluding()) {
            return false;
        }
        
        // Check for blocked materials nearby
        for (BlockFace face : Arrays.asList(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.DOWN)) {
            Block nearby = block.getRelative(face);
            if (blockedMaterials.contains(nearby.getType())) {
                return false;
            }
            
            Block nearbySecond = secondHalf.getRelative(face);
            if (blockedMaterials.contains(nearbySecond.getType())) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Get cardinal direction player is facing
     */
    public static BlockFace getCardinalDirection(Player player) {
        float yaw = player.getLocation().getYaw();
        yaw = (yaw + 360) % 360; // Normalize to 0-360
        
        if (yaw >= 315 || yaw < 45) {
            return BlockFace.SOUTH;
        } else if (yaw >= 45 && yaw < 135) {
            return BlockFace.WEST;
        } else if (yaw >= 135 && yaw < 225) {
            return BlockFace.NORTH;
        } else {
            return BlockFace.EAST;
        }
    }
    
    
    /**
     * Check if there are hostile mobs nearby
     */
    private boolean hasNearbyHostileMobs(Player player) {
        double radius = plugin.getConfig().getDouble("restrictions.hostile-mobs.radius", 16.0);
        Collection<Entity> nearby = player.getLocation().getWorld()
                .getNearbyEntities(player.getLocation(), radius, radius, radius);
        
        for (Entity entity : nearby) {
            if (entity instanceof Monster) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get the opposite block face
     */
    public BlockFace getOpposite(BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.NORTH;
            case EAST -> BlockFace.WEST;
            case WEST -> BlockFace.EAST;
            default -> face;
        };
    }
}