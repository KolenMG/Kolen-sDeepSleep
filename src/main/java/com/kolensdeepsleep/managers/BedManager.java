package com.kolensdeepsleep.managers;

import com.kolensdeepsleep.KolensDeepSleep;
import com.kolensdeepsleep.util.BedData;
import com.kolensdeepsleep.validators.BedPlacementValidator;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Manages temporary bed spawning and cleanup
 */
public class BedManager {
    private final KolensDeepSleep plugin;
    private final BedPlacementValidator validator;
    final Map<UUID, BedData> playerBeds; // Package-private for SleepManager access
    private final Map<Location, UUID> bedLocations;
    
    public BedManager(KolensDeepSleep plugin) {
        this.plugin = plugin;
        this.validator = new BedPlacementValidator(plugin);
        this.playerBeds = new HashMap<>();
        this.bedLocations = new HashMap<>();
    }
    
    /**
     * Spawn a temporary bed for a player
     * Returns true if successful
     */
    public boolean spawnBed(Player player) {
        // Check if player already has a bed
        if (playerBeds.containsKey(player.getUniqueId())) {
            return false;
        }
        
        // Find safe location
        Location bedLocation = validator.findSafeBedLocation(player);
        if (bedLocation == null) {
            return false;
        }
        
        // Get bed color from config
        String colorName = plugin.getConfigUtil().getBedColor();
        Material bedMaterial;
        try {
            bedMaterial = Material.valueOf(colorName + "_BED");
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid bed color: " + colorName + ". Using RED_BED.");
            bedMaterial = Material.RED_BED;
        }
        
        // Get facing direction
        BlockFace facing = validator.getCardinalDirection(player);
        
        // Place bed (both halves)
        Block footBlock = bedLocation.getBlock();
        Block headBlock = footBlock.getRelative(facing);
        
        // Set foot of bed
        footBlock.setType(bedMaterial, false); // false = don't apply physics yet
        Bed footBedData = (Bed) bedMaterial.createBlockData();
        footBedData.setPart(Bed.Part.FOOT);
        footBedData.setFacing(facing);
        footBlock.setBlockData(footBedData, false);
        
        // Set head of bed
        headBlock.setType(bedMaterial, false); // false = don't apply physics yet
        Bed headBedData = (Bed) bedMaterial.createBlockData();
        headBedData.setPart(Bed.Part.HEAD);
        headBedData.setFacing(facing);
        headBlock.setBlockData(headBedData, true); // true = apply physics now
        
        // Store bed data
        Set<Location> bedBlocks = new HashSet<>();
        bedBlocks.add(footBlock.getLocation());
        bedBlocks.add(headBlock.getLocation());
        
        BedData bedData = new BedData(player.getUniqueId(), bedBlocks, player.getWorld());
        playerBeds.put(player.getUniqueId(), bedData);
        
        // Map locations to player
        bedLocations.put(footBlock.getLocation(), player.getUniqueId());
        bedLocations.put(headBlock.getLocation(), player.getUniqueId());
        
        // Play sound
        playBedSound(player, "sounds.bed-spawn");
        plugin.getVisualEffectsManager().spawnBedParticles(footBlock.getLocation());
        // Debug message
        Map<String, String> debugPlaceholders = new HashMap<>();
        debugPlaceholders.put("x", String.valueOf(footBlock.getX()));
        debugPlaceholders.put("y", String.valueOf(footBlock.getY()));
        debugPlaceholders.put("z", String.valueOf(footBlock.getZ()));
        debugPlaceholders.put("world", player.getWorld().getName());
        plugin.getMessageUtil().debug("debug.bed-spawned", debugPlaceholders);
        
        // Force player to sleep
        //plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
        //    if (player.isOnline()) {
        //        player.sleep(footBlock.getLocation(), true);
        //        playBedSound(player, "sounds.sleep-start");
        //    }
        //}, 1L);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                // Reuse existing footBlock variable
                Bed bedBlockData = (Bed) footBlock.getBlockData();
        
                // Offset to HEAD block
                Location sleepLocation = footBlock.getLocation().clone();
                if (bedBlockData.getPart() == Bed.Part.FOOT) {
                    sleepLocation.add(bedBlockData.getFacing().getModX(), 0, bedBlockData.getFacing().getModZ());
                }
        
                player.sleep(sleepLocation, true);
                playBedSound(player, "sounds.sleep-start");
            }
        }, 1L);
        
        
        return true;
    }
    
    /**
     * Remove a player's bed
     */
    public void removeBed(UUID playerUUID) {
        BedData bedData = playerBeds.get(playerUUID);
        if (bedData == null) {
            return;
        }
        
        // Remove blocks
        for (Location loc : bedData.getBedBlocks()) {
            Block block = loc.getBlock();
            if (block.getType().name().endsWith("_BED")) {
                block.setType(Material.AIR, false); // Don't drop items
            }
            bedLocations.remove(loc);
        }
        
        // Remove from tracking
        playerBeds.remove(playerUUID);
        
        // Debug message
        if (!bedData.getBedBlocks().isEmpty()) {
            Location firstLoc = bedData.getBedBlocks().iterator().next();
            Map<String, String> debugPlaceholders = new HashMap<>();
            debugPlaceholders.put("x", String.valueOf(firstLoc.getBlockX()));
            debugPlaceholders.put("y", String.valueOf(firstLoc.getBlockY()));
            debugPlaceholders.put("z", String.valueOf(firstLoc.getBlockZ()));
            debugPlaceholders.put("world", bedData.getWorld().getName());
            plugin.getMessageUtil().debug("debug.bed-removed", debugPlaceholders);
        }
    }
    
    /**
     * Remove a bed by location
     */
    public void removeBedByLocation(Location location) {
        UUID ownerUUID = bedLocations.get(location);
        if (ownerUUID != null) {
            removeBed(ownerUUID);
        }
    }
    
    /**
     * Check if a location is a temporary bed
     */
    public boolean isTemporaryBed(Location location) {
        return bedLocations.containsKey(location);
    }
    
    /**
     * Check if a block is a temporary bed
     */
    public boolean isTemporaryBed(Block block) {
        return isTemporaryBed(block.getLocation());
    }
    
    /**
     * Get the owner of a temporary bed
     */
    public UUID getBedOwner(Location location) {
        return bedLocations.get(location);
    }
    
    /**
     * Get a player's bed data
     */
    public BedData getPlayerBed(UUID playerUUID) {
        return playerBeds.get(playerUUID);
    }
    
    /**
     * Check if a player has a temporary bed
     */
    public boolean hasTemporaryBed(UUID playerUUID) {
        return playerBeds.containsKey(playerUUID);
    }
    
    /**
     * Cleanup all temporary beds
     */
    public void cleanupAll() {
        int count = playerBeds.size();
        
        // Create a copy to avoid concurrent modification
        Set<UUID> players = new HashSet<>(playerBeds.keySet());
        
        for (UUID uuid : players) {
            removeBed(uuid);
        }
        
        if (count > 0) {
            Map<String, String> debugPlaceholders = new HashMap<>();
            debugPlaceholders.put("count", String.valueOf(count));
            plugin.getMessageUtil().debug("debug.cleanup-performed", debugPlaceholders);
        }
    }
    
    /**
     * Cleanup beds in unloaded worlds
     */
    public void cleanupUnloadedWorlds() {
        Set<UUID> toRemove = new HashSet<>();
        
        for (Map.Entry<UUID, BedData> entry : playerBeds.entrySet()) {
            if (!plugin.getServer().getWorlds().contains(entry.getValue().getWorld())) {
                toRemove.add(entry.getKey());
            }
        }
        
        toRemove.forEach(this::removeBed);
    }
    
    /**
     * Get bed placement validator
     */
    public BedPlacementValidator getValidator() {
        return validator;
    }
    
    /**
     * Play a bed-related sound
     */
    private void playBedSound(Player player, String configPath) {
        if (!plugin.getConfig().getBoolean(configPath + ".enabled", true)) {
            return;
        }
        
        String soundName = plugin.getConfig().getString(configPath + ".sound", "BLOCK_WOOL_PLACE");
        float volume = (float) plugin.getConfig().getDouble(configPath + ".volume", 1.0);
        float pitch = (float) plugin.getConfig().getDouble(configPath + ".pitch", 1.0);
        
        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound: " + soundName);
        }
    }
    
    /**
     * Get all active beds count
     */
    public int getActiveBedCount() {
        return playerBeds.size();
    }
}