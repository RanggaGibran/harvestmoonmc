package id.rnggagib.managers;

import id.rnggagib.HarvestMoonMC;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Random;

public class HarvestLimitManager {
    private final HarvestMoonMC plugin;
    private final Random random = new Random();
    // Maps for tracking harvests removed - unlimited harvesting
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
     * Now just a placeholder since limits are removed
     */
    private void scheduleNextReset() {
        // No need to schedule limit resets as harvesting is now unlimited
        if (resetTask != null) {
            resetTask.cancel();
            resetTask = null;
        }
        plugin.getLogger().info("Harvest limits have been disabled - unlimited harvesting enabled");
    }
    
    /**
     * Resets harvest limits for all players
     */    public void resetAllHarvestLimits() {
        // No-op method - limits are disabled
        plugin.getLogger().info("Harvest limits reset triggered but ignored (unlimited harvesting enabled)");
    }
      /**
     * Checks if a player can harvest more crops
     * @param player The player
     * @return true if player is below their harvest limit
     */
    public boolean canHarvest(Player player) {
        // Always return true - unlimited harvesting
        return true;
    }
      /**
     * Gets the remaining harvest count for a player
     * @param player The player
     * @return Remaining harvests allowed
     */
    public int getRemainingHarvests(Player player) {
        // Always return the maximum integer value for unlimited harvests
        return Integer.MAX_VALUE;
    }
      /**
     * Increments a player's harvest count
     * @param player The player
     * @param amount Amount to increment by
     * @return Remaining harvests allowed
     */    public int incrementHarvestCount(Player player, int amount) {
        // Don't track harvest counts at all - fully unlimited
        return Integer.MAX_VALUE;
    }
      /**
     * Gets the current harvest limit for a player
     * @param player The player
     * @return The player's harvest limit
     */
    public int getHarvestLimit(Player player) {
        // Return the maximum integer value for unlimited harvests
        return Integer.MAX_VALUE;
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