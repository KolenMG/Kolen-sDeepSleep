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
    private boolean hasChecked = false;
    
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
            plugin.getLogger().info("Update checker is disabled in config.");
            return;
        }
        
        if (githubRepo == null || githubRepo.isEmpty() || githubRepo.equals("YourUsername/KolensDeepSleep")) {
            plugin.getLogger().warning("GitHub repository not configured in config.yml!");
            plugin.getLogger().warning("Please set 'update-checker.github-repo' to 'KolenMG/Kolen-sDeepSleep'");
            return;
        }
        
        plugin.getLogger().info("Checking for updates from: " + githubRepo);
        
        // Run async to avoid blocking server startup
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                latestVersion = fetchLatestVersion();
                
                if (latestVersion != null && !latestVersion.isEmpty()) {
                    plugin.getLogger().info("Current version: " + currentVersion);
                    plugin.getLogger().info("Latest version: " + latestVersion);
                    
                    updateAvailable = isNewerVersion(currentVersion, latestVersion);
                    hasChecked = true;
                    
                    // Log result on main thread
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (updateAvailable) {
                            logUpdateAvailable();
                        } else {
                            plugin.getLogger().info("You are running the latest version!");
                        }
                    });
                } else {
                    plugin.getLogger().warning("Could not fetch latest version from GitHub.");
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to check for updates: " + e.getMessage());
                if (plugin.getConfig().getBoolean("general.debug", false)) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    /**
     * Fetch the latest version from GitHub API
     */
    private String fetchLatestVersion() throws Exception {
        String apiUrl = "https://api.github.com/repos/" + githubRepo + "/releases/latest";
        
        plugin.getLogger().info("Fetching from: " + apiUrl);
        
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000); // Increased timeout
        connection.setReadTimeout(10000);
        connection.setRequestProperty("User-Agent", "KolensDeepSleep-UpdateChecker");
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
        
        int responseCode = connection.getResponseCode();
        plugin.getLogger().info("GitHub API response code: " + responseCode);
        
        if (responseCode == 200) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            String json = response.toString();
            
            // Debug: log first 200 chars of response
            if (plugin.getConfig().getBoolean("general.debug", false)) {
                plugin.getLogger().info("API Response preview: " + json.substring(0, Math.min(200, json.length())));
            }
            
            // Parse JSON response (simple parsing for "tag_name")
            int tagNameIndex = json.indexOf("\"tag_name\"");
            
            if (tagNameIndex != -1) {
                int startQuote = json.indexOf("\"", tagNameIndex + 11);
                int endQuote = json.indexOf("\"", startQuote + 1);
                
                if (startQuote != -1 && endQuote != -1) {
                    String version = json.substring(startQuote + 1, endQuote);
                    // Remove 'v' prefix if present (e.g., "v1.0.0" -> "1.0.0")
                    version = version.startsWith("v") ? version.substring(1) : version;
                    plugin.getLogger().info("Extracted version: " + version);
                    return version;
                }
            }
            
            plugin.getLogger().warning("Could not find 'tag_name' in GitHub API response");
        } else if (responseCode == 404) {
            plugin.getLogger().warning("Repository not found or no releases available: " + githubRepo);
        } else if (responseCode == 403) {
            plugin.getLogger().warning("GitHub API rate limit exceeded. Try again later.");
        } else {
            plugin.getLogger().warning("Unexpected response code from GitHub API: " + responseCode);
        }
        
        return null;
    }
    
    /**
     * Compare version strings
     * @return true if newVersion is newer than currentVersion
     */
    private boolean isNewerVersion(String current, String latest) {
        try {
            // Remove any non-numeric prefixes or suffixes for comparison
            String cleanCurrent = cleanVersion(current);
            String cleanLatest = cleanVersion(latest);
            
            plugin.getLogger().info("Comparing versions: " + cleanCurrent + " vs " + cleanLatest);
            
            String[] currentParts = cleanCurrent.split("\\.");
            String[] latestParts = cleanLatest.split("\\.");
            
            int length = Math.max(currentParts.length, latestParts.length);
            
            for (int i = 0; i < length; i++) {
                int currentPart = i < currentParts.length ? parseVersionPart(currentParts[i]) : 0;
                int latestPart = i < latestParts.length ? parseVersionPart(latestParts[i]) : 0;
                
                plugin.getLogger().info("Part " + i + ": current=" + currentPart + ", latest=" + latestPart);
                
                if (latestPart > currentPart) {
                    plugin.getLogger().info("Update available: latest version is newer");
                    return true;
                } else if (latestPart < currentPart) {
                    plugin.getLogger().info("Current version is newer than latest release");
                    return false;
                }
            }
            
            plugin.getLogger().info("Versions are equal");
            return false; // Versions are equal
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to compare versions: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Clean version string (remove prefixes like 'v' and suffixes like '-SNAPSHOT')
     */
    private String cleanVersion(String version) {
        // Remove 'v' prefix
        if (version.startsWith("v") || version.startsWith("V")) {
            version = version.substring(1);
        }
        
        // Remove suffix like '-SNAPSHOT', '-BETA', etc.
        int dashIndex = version.indexOf('-');
        if (dashIndex != -1) {
            version = version.substring(0, dashIndex);
        }
        
        return version;
    }
    
    /**
     * Parse version part (handles numbers and removes non-numeric suffixes)
     */
    private int parseVersionPart(String part) {
        try {
            // Remove non-numeric characters (e.g., "1a" -> "1")
            String numericPart = part.replaceAll("[^0-9].*", "");
            return numericPart.isEmpty() ? 0 : Integer.parseInt(numericPart);
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Failed to parse version part: " + part);
            return 0;
        }
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
    }
    
    /**
     * Check if update is available (returns false if not yet checked)
     */
    public boolean isUpdateAvailable() {
        return hasChecked && updateAvailable;
    }
    
    /**
     * Get latest version (null if not yet checked)
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
    
    /**
     * Check if the update checker has completed its check
     */
    public boolean hasChecked() {
        return hasChecked;
    }
}