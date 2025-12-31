package com.kolensdeepsleep.util;

import com.kolensdeepsleep.KolensDeepSleep;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;

/**
 * Checks for plugin updates from GitHub releases
 */
public class UpdateChecker {
    private final KolensDeepSleep plugin;
    private final String currentVersion;
    private final String githubRepo;
    private String latestVersion;
    private boolean updateAvailable = false;
    
    /**
     * @param plugin The plugin instance
     * @param githubRepo Format: "username/repository" (e.g., "Kolen/DeepSleep")
     */
    public UpdateChecker(KolensDeepSleep plugin, String githubRepo) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
        this.githubRepo = githubRepo;
    }
    
    /**
     * Check for updates asynchronously
     */
    public void checkForUpdates() {
        if (!plugin.getConfig().getBoolean("update-checker.enabled", true)) {
            return;
        }
        
        // Run async to avoid blocking server startup
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                latestVersion = fetchLatestVersion();
                
                if (latestVersion != null && !latestVersion.isEmpty()) {
                    updateAvailable = isNewerVersion(currentVersion, latestVersion);
                    
                    // Log result on main thread
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (updateAvailable) {
                            logUpdateAvailable();
                        } else {
                            plugin.getLogger().info("You are running the latest version!");
                        }
                    });
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to check for updates: " + e.getMessage());
            }
        });
    }
    
    /**
     * Fetch the latest version from GitHub API
     */
    private String fetchLatestVersion() throws Exception {
        String apiUrl = "https://api.github.com/repos/" + githubRepo + "/releases/latest";
        
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestProperty("User-Agent", "KolensDeepSleep-UpdateChecker");
        
        int responseCode = connection.getResponseCode();
        
        if (responseCode == 200) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            // Parse JSON response (simple parsing for "tag_name")
            String json = response.toString();
            int tagNameIndex = json.indexOf("\"tag_name\"");
            
            if (tagNameIndex != -1) {
                int startQuote = json.indexOf("\"", tagNameIndex + 11);
                int endQuote = json.indexOf("\"", startQuote + 1);
                
                if (startQuote != -1 && endQuote != -1) {
                    String version = json.substring(startQuote + 1, endQuote);
                    // Remove 'v' prefix if present (e.g., "v1.0.0" -> "1.0.0")
                    return version.startsWith("v") ? version.substring(1) : version;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Compare version strings
     * @return true if newVersion is newer than currentVersion
     */
    private boolean isNewerVersion(String current, String latest) {
        try {
            String[] currentParts = current.split("\\.");
            String[] latestParts = latest.split("\\.");
            
            int length = Math.max(currentParts.length, latestParts.length);
            
            for (int i = 0; i < length; i++) {
                int currentPart = i < currentParts.length ? parseVersionPart(currentParts[i]) : 0;
                int latestPart = i < latestParts.length ? parseVersionPart(latestParts[i]) : 0;
                
                if (latestPart > currentPart) {
                    return true;
                } else if (latestPart < currentPart) {
                    return false;
                }
            }
            
            return false; // Versions are equal
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to compare versions: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Parse version part (handles numbers and removes non-numeric suffixes)
     */
    private int parseVersionPart(String part) {
        // Remove non-numeric characters (e.g., "1-SNAPSHOT" -> "1")
        String numericPart = part.replaceAll("[^0-9].*", "");
        return numericPart.isEmpty() ? 0 : Integer.parseInt(numericPart);
    }
    
    /**
     * Log update available message
     */
    private void logUpdateAvailable() {
        plugin.getLogger().warning("===============================================");
        plugin.getLogger().warning("  UPDATE AVAILABLE!");
        plugin.getLogger().warning("  Current version: " + currentVersion);
        plugin.getLogger().warning("  Latest version: " + latestVersion);
        plugin.getLogger().warning("  Download: https://github.com/" + githubRepo + "/releases");
        plugin.getLogger().warning("===============================================");
        
        // Notify online admins
        if (plugin.getConfig().getBoolean("update-checker.notify-admins", true)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Bukkit.getOnlinePlayers().stream()
                        .filter(p -> p.hasPermission("deepsleep.admin"))
                        .forEach(admin -> {
                            admin.sendMessage("§e§l[Kolen's DeepSleep] §6A new version is available!");
                            admin.sendMessage("§7Current: §f" + currentVersion + " §7→ Latest: §a" + latestVersion);
                            admin.sendMessage("§7Download: §bhttps://github.com/" + githubRepo + "/releases");
                        });
            }, 60L); // Wait 3 seconds after join
        }
    }
    
    /**
     * Check if update is available
     */
    public boolean isUpdateAvailable() {
        return updateAvailable;
    }
    
    /**
     * Get latest version
     */
    public String getLatestVersion() {
        return latestVersion;
    }
    
    /**
     * Get current version
     */
    public String getCurrentVersion() {
        return currentVersion;
    }
}