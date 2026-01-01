package com.kolensdeepsleep.commands;

import com.kolensdeepsleep.KolensDeepSleep;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles the /deepsleep admin command
 */
public class AdminCommand implements CommandExecutor, TabCompleter {
    private final KolensDeepSleep plugin;
    
    public AdminCommand(KolensDeepSleep plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        if (!sender.hasPermission("deepsleep.admin")) {
            Map<String, String> placeholders = new HashMap<>();
            plugin.getMessageUtil().sendMessage(sender, "commands.no-permission", placeholders);
            return true;
        }
        
        // Show usage if no args
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload" -> handleReload(sender);
            case "status" -> handleStatus(sender);
            case "skip" -> handleSkip(sender, args);
            case "cancel" -> handleCancel(sender, args);
            case "debug" -> handleDebug(sender);
            case "version" -> handleVersion(sender);
            default -> sendUsage(sender, label);
        }
        
        return true;
    }
    
    /**
     * Handle reload subcommand
     */
    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("deepsleep.admin.reload")) {
            plugin.getMessageUtil().sendMessage(sender, "commands.no-permission", new HashMap<>());
            return;
        }
        
        plugin.reload();
        plugin.getMessageUtil().sendMessage(sender, "commands.admin.reload", new HashMap<>());
    }
    
    /**
     * Handle status subcommand
     */
    private void handleStatus(CommandSender sender) {
        if (!sender.hasPermission("deepsleep.admin.status")) {
            plugin.getMessageUtil().sendMessage(sender, "commands.no-permission", new HashMap<>());
            return;
        }
        
        sender.sendMessage("§b=== DeepSleep Status ===");
        
        for (World world : Bukkit.getWorlds()) {
            if (!plugin.getConfigUtil().isWorldEnabled(world)) {
                continue;
            }
            
            int sleeping = plugin.getSleepManager().getSleepingCount(world);
            int eligible = plugin.getSleepManager().getEligiblePlayerCount(world);
            double percentage = eligible > 0 ? (double) sleeping / eligible * 100 : 0;
            boolean isNight = plugin.getSleepManager().isNightTime(world);
            boolean skipActive = plugin.getSleepManager().isSkipInProgress(world);
            
            sender.sendMessage(String.format("§7%s: §f%d§7/§f%d §7(§e%.0f%%§7) %s %s",
                    world.getName(),
                    sleeping,
                    eligible,
                    percentage,
                    isNight ? "§a[Night]" : "§7[Day]",
                    skipActive ? "§6[Skipping]" : ""
            ));
        }
        
        sender.sendMessage(String.format("§7Active Beds: §f%d", plugin.getBedManager().getActiveBedCount()));
    }
    
    /**
     * Handle skip subcommand
     */
    private void handleSkip(CommandSender sender, String[] args) {
        if (!sender.hasPermission("deepsleep.admin.skip")) {
            plugin.getMessageUtil().sendMessage(sender, "commands.no-permission", new HashMap<>());
            return;
        }
        
        World world;
        
        if (args.length < 2) {
            // If sender is player, use their world
            if (sender instanceof Player player) {
                world = player.getWorld();
            } else {
                sender.sendMessage("§cUsage: /deepsleep skip <world>");
                return;
            }
        } else {
            // Get world from argument
            world = Bukkit.getWorld(args[1]);
            if (world == null) {
                sender.sendMessage("§cWorld not found: " + args[1]);
                return;
            }
        }
        
        if (!plugin.getSleepManager().isNightTime(world)) {
            sender.sendMessage("§cIt's not night time in " + world.getName());
            return;
        }
        
        plugin.getSleepManager().forceNightSkip(world);
        plugin.getMessageUtil().sendMessage(sender, "commands.admin.skip-forced", new HashMap<>());
    }
    
    /**
     * Handle cancel subcommand
     */
    private void handleCancel(CommandSender sender, String[] args) {
        if (!sender.hasPermission("deepsleep.admin.cancel")) {
            plugin.getMessageUtil().sendMessage(sender, "commands.no-permission", new HashMap<>());
            return;
        }
        
        World world;
        
        if (args.length < 2) {
            // If sender is player, use their world
            if (sender instanceof Player player) {
                world = player.getWorld();
            } else {
                sender.sendMessage("§cUsage: /deepsleep cancel <world>");
                return;
            }
        } else {
            // Get world from argument
            world = Bukkit.getWorld(args[1]);
            if (world == null) {
                sender.sendMessage("§cWorld not found: " + args[1]);
                return;
            }
        }
        
        boolean cancelled = plugin.getSleepManager().cancelNightSkip(world);
        
        if (cancelled) {
            plugin.getMessageUtil().sendMessage(sender, "commands.admin.skip-cancelled", new HashMap<>());
        } else {
            plugin.getMessageUtil().sendMessage(sender, "commands.admin.no-skip-active", new HashMap<>());
        }
    }
    
    /**
     * Handle debug subcommand
     */
    private void handleDebug(CommandSender sender) {
        if (!sender.hasPermission("deepsleep.admin.debug")) {
            plugin.getMessageUtil().sendMessage(sender, "commands.no-permission", new HashMap<>());
            return;
        }
        
        boolean currentDebug = plugin.getConfig().getBoolean("general.debug", false);
        boolean newDebug = !currentDebug;
        
        plugin.getConfig().set("general.debug", newDebug);
        plugin.saveConfig();
        
        String messagePath = newDebug ? "commands.admin.debug-enabled" : "commands.admin.debug-disabled";
        plugin.getMessageUtil().sendMessage(sender, messagePath, new HashMap<>());
    }
    
    /**
     * Handle version subcommand
     * NEW: Shows current version and update status
     */
    private void handleVersion(CommandSender sender) {
        if (!sender.hasPermission("deepsleep.admin.version")) {
            plugin.getMessageUtil().sendMessage(sender, "commands.no-permission", new HashMap<>());
            return;
        }
        
        sender.sendMessage("§b=== DeepSleep Version Info ===");
        sender.sendMessage("§7Current: §f" + plugin.getDescription().getVersion());
        
        if (plugin.getUpdateChecker() != null && plugin.getUpdateChecker().hasChecked()) {
            if (plugin.getUpdateChecker().isUpdateAvailable()) {
                sender.sendMessage("§7Latest: §a" + plugin.getUpdateChecker().getLatestVersion() + " §e(Update Available!)");
                
                String repo = plugin.getConfig().getString("update-checker.github-repo", "KolenMG/Kolen-sDeepSleep");
                sender.sendMessage("§7Download: §bhttps://github.com/" + repo + "/releases");
            } else {
                sender.sendMessage("§7Status: §aUp to date!");
            }
        } else {
            sender.sendMessage("§7Status: §7Checking...");
            sender.sendMessage("§7Tip: Wait a few seconds and run the command again");
        }
    }
    
    /**
     * Send usage message
     */
    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage("§b=== DeepSleep Admin Commands ===");
        sender.sendMessage("§7/" + label + " reload §8- §fReload configuration");
        sender.sendMessage("§7/" + label + " status §8- §fView sleep status");
        sender.sendMessage("§7/" + label + " skip [world] §8- §fForce night skip");
        sender.sendMessage("§7/" + label + " cancel [world] §8- §fCancel night skip");
        sender.sendMessage("§7/" + label + " debug §8- §fToggle debug mode");
        sender.sendMessage("§7/" + label + " version §8- §fCheck plugin version");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("deepsleep.admin")) {
            return Collections.emptyList();
        }
        
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("reload", "status", "skip", "cancel", "debug", "version");
            return subCommands.stream()
                    .filter(sub -> sub.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        if (args.length == 2 && (args[0].equalsIgnoreCase("skip") || args[0].equalsIgnoreCase("cancel"))) {
            return Bukkit.getWorlds().stream()
                    .map(World::getName)
                    .filter(name -> name.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        return Collections.emptyList();
    }
}