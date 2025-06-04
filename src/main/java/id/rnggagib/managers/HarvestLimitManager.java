package id.rnggagib.managers;

import id.rnggagib.HarvestMoonMC;
import id.rnggagib.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class HarvestLimitManager {
    private final HarvestMoonMC plugin;
    private final Random random = new Random();
    private final Map<UUID, Integer> playerHarvestCounts = new HashMap<>();
    private final Map<UUID, Integer> playerHarvestLimits = new HashMap<>();
    private BukkitTask resetTask;
    
    // Default values
    private int minHarvestLimit = 300;
    private int maxHarvestLimit = 500;
    private int minResetTimeMinutes = 60; // 1 hour
    private int maxResetTimeMinutes = 180; // 3 hours
    
    public HarvestLimitManager(HarvestMoonMC plugin) {
        this.plugin = plugin;
        loadConfig();
        scheduleNextReset();
    }
    
    /**
     * Loads harvest limit settings from config
     */
    private void loadConfig() {
        minHarvestLimit = plugin.getConfig().getInt("harvest.min_limit", 300);
        maxHarvestLimit = plugin.getConfig().getInt("harvest.max_limit", 500);
        minResetTimeMinutes = plugin.getConfig().getInt("harvest.min_reset_time_minutes", 60);
        maxResetTimeMinutes = plugin.getConfig().getInt("harvest.max_reset_time_minutes", 180);
    }
    
    /**
     * Schedules the next harvest limit reset for all players
     */
    private void scheduleNextReset() {
        if (resetTask != null) {
            resetTask.cancel();
        }
        
        int resetTimeMinutes = minResetTimeMinutes + random.nextInt(maxResetTimeMinutes - minResetTimeMinutes + 1);
        long resetTimeTicks = resetTimeMinutes * 60L * 20L; // Convert to ticks
        
        plugin.getLogger().info("Next harvest limit reset scheduled in " + resetTimeMinutes + " minutes");
        
        resetTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            resetAllHarvestLimits();
            scheduleNextReset();
        }, resetTimeTicks);
    }
    
    /**
     * Resets harvest limits for all players
     */
    public void resetAllHarvestLimits() {
        playerHarvestCounts.clear();
        playerHarvestLimits.clear();
        
        // Notify online players about the reset
        String resetMessage = plugin.getConfig().getString("messages.harvest_limit_reset", 
                "&aKuota panen Anda telah direset!");
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + resetMessage));
        }
        
        plugin.getLogger().info("Reset harvest limits for all players");
    }
    
    /**
     * Checks if a player can harvest more crops
     * @param player The player
     * @return true if player is below their harvest limit
     */
    public boolean canHarvest(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Admins and creative mode players bypass limits
        if (player.hasPermission("harvestmoonmc.admin.bypass") || 
            player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return true;
        }
        
        // Get current harvest count or initialize to 0
        int currentCount = playerHarvestCounts.getOrDefault(playerId, 0);
        
        // Get harvest limit or generate a new one
        int limit = playerHarvestLimits.computeIfAbsent(playerId, uuid -> 
            minHarvestLimit + random.nextInt(maxHarvestLimit - minHarvestLimit + 1)
        );
        
        return currentCount < limit;
    }
    
    /**
     * Gets the remaining harvest count for a player
     * @param player The player
     * @return Remaining harvests allowed
     */
    public int getRemainingHarvests(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Admins and creative mode players have unlimited harvests
        if (player.hasPermission("harvestmoonmc.admin.bypass") || 
            player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return Integer.MAX_VALUE;
        }
        
        int currentCount = playerHarvestCounts.getOrDefault(playerId, 0);
        int limit = playerHarvestLimits.computeIfAbsent(playerId, uuid -> 
            minHarvestLimit + random.nextInt(maxHarvestLimit - minHarvestLimit + 1)
        );
        
        return Math.max(0, limit - currentCount);
    }
    
    /**
     * Increments a player's harvest count
     * @param player The player
     * @param amount Amount to increment by
     * @return Remaining harvests allowed
     */
    public int incrementHarvestCount(Player player, int amount) {
        UUID playerId = player.getUniqueId();
        
        // Admins and creative mode players don't consume harvest count
        if (player.hasPermission("harvestmoonmc.admin.bypass") || 
            player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return Integer.MAX_VALUE;
        }
        
        int currentCount = playerHarvestCounts.getOrDefault(playerId, 0);
        int newCount = currentCount + amount;
        playerHarvestCounts.put(playerId, newCount);
        
        int limit = playerHarvestLimits.computeIfAbsent(playerId, uuid -> 
            minHarvestLimit + random.nextInt(maxHarvestLimit - minHarvestLimit + 1)
        );
        
        return Math.max(0, limit - newCount);
    }
    
    /**
     * Gets the current harvest limit for a player
     * @param player The player
     * @return The player's harvest limit
     */
    public int getHarvestLimit(Player player) {
        UUID playerId = player.getUniqueId();
        
        return playerHarvestLimits.computeIfAbsent(playerId, uuid -> 
            minHarvestLimit + random.nextInt(maxHarvestLimit - minHarvestLimit + 1)
        );
    }
    
    /**
     * Reloads settings from config
     */
    public void reloadConfig() {
        loadConfig();
        plugin.getLogger().info("Harvest limit configuration reloaded");
    }
    
    /**
     * Shuts down the harvest limit manager
     */
    public void shutdown() {
        if (resetTask != null) {
            resetTask.cancel();
            resetTask = null;
        }
    }
}