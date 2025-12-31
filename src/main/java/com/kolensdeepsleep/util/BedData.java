package com.kolensdeepsleep.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Stores information about a temporary bed spawned by the plugin
 */
public class BedData {
    private final UUID playerUUID;
    private final Set<Location> bedBlocks; // Both halves of the bed
    private final World world;
    private final long spawnTime;
    
    public BedData(UUID playerUUID, Set<Location> bedBlocks, World world) {
        this.playerUUID = playerUUID;
        this.bedBlocks = new HashSet<>(bedBlocks);
        this.world = world;
        this.spawnTime = System.currentTimeMillis();
    }
    
    public UUID getPlayerUUID() {
        return playerUUID;
    }
    
    public Set<Location> getBedBlocks() {
        return new HashSet<>(bedBlocks);
    }
    
    public World getWorld() {
        return world;
    }
    
    public long getSpawnTime() {
        return spawnTime;
    }
    
    /**
     * Check if a location belongs to this bed
     */
    public boolean containsLocation(Location location) {
        return bedBlocks.stream()
                .anyMatch(loc -> loc.getBlockX() == location.getBlockX() 
                        && loc.getBlockY() == location.getBlockY() 
                        && loc.getBlockZ() == location.getBlockZ()
                        && loc.getWorld().equals(location.getWorld()));
    }
    
    /**
     * Check if a block belongs to this bed
     */
    public boolean containsBlock(Block block) {
        return containsLocation(block.getLocation());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BedData bedData = (BedData) o;
        return playerUUID.equals(bedData.playerUUID);
    }
    
    @Override
    public int hashCode() {
        return playerUUID.hashCode();
    }
}