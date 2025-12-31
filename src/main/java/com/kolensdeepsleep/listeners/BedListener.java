package com.kolensdeepsleep.listeners;

import com.kolensdeepsleep.KolensDeepSleep;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import java.util.UUID;

import java.util.Iterator;

/**
 * Protects temporary beds from breaking and unauthorized interaction
 */
public class BedListener implements Listener {
    private final KolensDeepSleep plugin;
    
    public BedListener(KolensDeepSleep plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle breaking temporary beds - allow it but don't drop items
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        
        // Check if block is a temporary bed
        if (plugin.getBedManager().isTemporaryBed(block)) {
            // Don't drop items
            event.setDropItems(false);
            
            // Remove the bed from tracking
            plugin.getBedManager().removeBedByLocation(block.getLocation());
            
            // Wake up the player if they're sleeping
            UUID ownerUUID = plugin.getBedManager().getBedOwner(block.getLocation());
            if (ownerUUID != null) {
                Player owner = plugin.getServer().getPlayer(ownerUUID);
                if (owner != null && owner.isSleeping()) {
                    owner.wakeup(false);
                    plugin.getSleepManager().removeSleepingPlayer(owner);
                }
            }
        }
    }
    
    /**
     * Prevent interaction with temporary beds by non-owners
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        
        if (block == null || !block.getType().name().endsWith("_BED")) {
            return;
        }
        
        // Check if it's a temporary bed
        if (plugin.getBedManager().isTemporaryBed(block)) {
            Player player = event.getPlayer();
            
            // Check ownership
            java.util.UUID owner = plugin.getBedManager().getBedOwner(block.getLocation());
            if (owner != null && !owner.equals(player.getUniqueId())) {
                event.setCancelled(true);
                
                // Optionally send message
                if (player.hasPermission("deepsleep.admin")) {
                    player.sendMessage("§cThis bed belongs to another player.");
                }
            }
        }
    }
    
    /**
     * Prevent explosions from destroying temporary beds
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        // Remove temporary beds from explosion block list
        Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (plugin.getBedManager().isTemporaryBed(block)) {
                iterator.remove();
            }
        }
    }
    
    /**
     * Prevent entity explosions from destroying temporary beds
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        // Remove temporary beds from explosion block list
        Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (plugin.getBedManager().isTemporaryBed(block)) {
                iterator.remove();
            }
        }
    }
    
    /**
     * Cleanup beds when world unloads
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldUnload(WorldUnloadEvent event) {
        plugin.getBedManager().cleanupUnloadedWorlds();
    }
}