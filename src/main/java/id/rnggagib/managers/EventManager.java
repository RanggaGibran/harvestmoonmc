package id.rnggagib.managers;

import id.rnggagib.HarvestMoonMC;
import id.rnggagib.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class EventManager {
    private final HarvestMoonMC plugin;
    private final Random random = new Random();
    
    private BukkitTask eventTask;
    private AtomicBoolean eventActive = new AtomicBoolean(false);
    private int currentMultiplier = 1;
    private long eventEndTime = 0;
    
    // Default config values
    private int minInterval = 40 * 60; // 40 minutes in seconds
    private int maxInterval = 60 * 60; // 60 minutes in seconds
    private int minDuration = 5 * 60;  // 5 minutes in seconds
    private int maxDuration = 15 * 60; // 15 minutes in seconds
    private int[] possibleMultipliers = {2, 3, 4};
    
    public EventManager(HarvestMoonMC plugin) {
        this.plugin = plugin;
        loadConfig();
        startEventScheduler();
    }
    
    /**
     * Loads event settings from config
     */
    private void loadConfig() {
        minInterval = plugin.getConfig().getInt("events.min_interval_minutes", 40) * 60;
        maxInterval = plugin.getConfig().getInt("events.max_interval_minutes", 60) * 60;
        minDuration = plugin.getConfig().getInt("events.min_duration_minutes", 5) * 60;
        maxDuration = plugin.getConfig().getInt("events.max_duration_minutes", 15) * 60;
        
        // Load possible multipliers
        if (plugin.getConfig().contains("events.possible_multipliers")) {
            possibleMultipliers = plugin.getConfig().getIntegerList("events.possible_multipliers")
                    .stream()
                    .mapToInt(Integer::intValue)
                    .toArray();
        }
        
        if (possibleMultipliers.length == 0) {
            // Fallback if config is invalid
            possibleMultipliers = new int[]{2, 3, 4};
        }
    }
    
    /**
     * Starts the scheduler to trigger events
     */
    private void startEventScheduler() {
        if (eventTask != null) {
            eventTask.cancel();
        }
        
        int initialDelay = getRandomInterval();
        plugin.getLogger().info("Event penjualan spesial berikutnya akan terjadi dalam " + (initialDelay / 60) + " menit");
        
        eventTask = Bukkit.getScheduler().runTaskLater(plugin, this::scheduleNextEvent, initialDelay * 20L);
    }
    
    /**
     * Schedules the next event
     */
    private void scheduleNextEvent() {
        // Start current event
        startEvent();
        
        // Schedule next event
        int nextInterval = getRandomInterval();
        eventTask = Bukkit.getScheduler().runTaskLater(plugin, this::scheduleNextEvent, 
                (nextInterval + getRemainingTime()) * 20L);
        
        plugin.getLogger().info("Event penjualan spesial berikutnya akan terjadi dalam " + 
                (nextInterval / 60) + " menit setelah event saat ini selesai");
    }
    
    /**
     * Gets a random interval in seconds for the next event
     */
    private int getRandomInterval() {
        return minInterval + random.nextInt(maxInterval - minInterval + 1);
    }
    
    /**
     * Gets a random duration for the event in seconds
     */
    private int getRandomDuration() {
        return minDuration + random.nextInt(maxDuration - minDuration + 1);
    }
    
    /**
     * Starts a special sale event
     */
    private void startEvent() {
        // Set the multiplier
        currentMultiplier = possibleMultipliers[random.nextInt(possibleMultipliers.length)];
        
        // Set the event duration
        int durationInSeconds = getRandomDuration();
        eventEndTime = System.currentTimeMillis() + (durationInSeconds * 1000L);
        
        // Set the event as active
        eventActive.set(true);
        
        // Announce the event
        String message = plugin.getConfig().getString("messages.event_start", 
                "&6&l✨ EVENT SPESIAL! &e&lJual tanaman dengan harga {multiplier}x lipat selama {duration} menit!");
        message = message
                .replace("{multiplier}", String.valueOf(currentMultiplier))
                .replace("{duration}", String.valueOf(durationInSeconds / 60));
        
        broadcastEventMessage(message);
        
        // Schedule end of event
        Bukkit.getScheduler().runTaskLater(plugin, this::endEvent, durationInSeconds * 20L);
        
        plugin.getLogger().info("Event penjualan spesial dimulai dengan multiplier " + currentMultiplier + "x selama " + 
                (durationInSeconds / 60) + " menit");
    }
    
    /**
     * Ends the current event
     */
    private void endEvent() {
        if (eventActive.getAndSet(false)) {
            // Announce end of event
            String message = plugin.getConfig().getString("messages.event_end", 
                    "&6&l✨ Event penjualan spesial telah berakhir!");
            broadcastEventMessage(message);
            
            // Reset multiplier
            currentMultiplier = 1;
            
            plugin.getLogger().info("Event penjualan spesial telah berakhir");
        }
    }
    
    /**
     * Broadcasts a message to all online players
     */
    private void broadcastEventMessage(String message) {
        String colorizedMessage = MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + message);
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(colorizedMessage);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }
    }
    
    /**
     * Gets the current price multiplier
     * @return The multiplier value
     */
    public int getCurrentPriceMultiplier() {
        return eventActive.get() ? currentMultiplier : 1;
    }
    
    /**
     * Checks if there is an active sale event
     * @return True if an event is active
     */
    public boolean isEventActive() {
        return eventActive.get();
    }
    
    /**
     * Gets the remaining time of the event in seconds
     * @return Seconds remaining, or 0 if no event is active
     */
    public int getRemainingTime() {
        if (!eventActive.get()) return 0;
        
        long remainingMillis = eventEndTime - System.currentTimeMillis();
        return Math.max(0, (int)(remainingMillis / 1000));
    }
    
    /**
     * Shuts down the event manager
     */
    public void shutdown() {
        if (eventTask != null) {
            eventTask.cancel();
            eventTask = null;
        }
        
        // End any active event
        if (eventActive.get()) {
            eventActive.set(false);
            currentMultiplier = 1;
        }
    }
    
    /**
     * Gets the array of possible multipliers
     */
    public int[] getPossibleMultipliers() {
        return possibleMultipliers;
    }

    /**
     * Manually starts an event with specified parameters
     * @param multiplier The price multiplier to use
     * @param durationSeconds The duration in seconds
     * @return true if event started successfully, false if an event is already running
     */
    public boolean startEventManually(int multiplier, int durationSeconds) {
        if (eventActive.get()) {
            return false; // Can't start an event if one is already active
        }
        
        // Cancel the current scheduled task if any
        if (eventTask != null) {
            eventTask.cancel();
            eventTask = null;
        }
        
        // Set multiplier
        currentMultiplier = multiplier;
        
        // Set duration
        int durationInSeconds = durationSeconds;
        eventEndTime = System.currentTimeMillis() + (durationInSeconds * 1000L);
        
        // Set event as active
        eventActive.set(true);
        
        // Announce the event
        String message = plugin.getConfig().getString("messages.event_start", 
                "&6&l✨ EVENT SPESIAL! &e&lJual tanaman dengan harga {multiplier}x lipat selama {duration} menit!");
        message = message
                .replace("{multiplier}", String.valueOf(currentMultiplier))
                .replace("{duration}", String.valueOf(durationInSeconds / 60));
        
        broadcastEventMessage(message);
        
        // Schedule end of event
        Bukkit.getScheduler().runTaskLater(plugin, this::endEvent, durationInSeconds * 20L);
        
        // Schedule next event after manual event ends + random interval
        int nextInterval = getRandomInterval();
        eventTask = Bukkit.getScheduler().runTaskLater(plugin, this::scheduleNextEvent, 
                (durationInSeconds + nextInterval) * 20L);
        
        plugin.getLogger().info("Event penjualan spesial dimulai secara manual dengan multiplier " + 
                currentMultiplier + "x selama " + (durationInSeconds / 60) + " menit");
        
        return true;
    }

    /**
     * Manually stops the current event
     * @return true if an event was stopped, false if no event was running
     */
    public boolean stopEventManually() {
        if (!eventActive.get()) {
            return false;
        }
        
        // End the event
        endEvent();
        
        // Schedule next random event
        if (eventTask != null) {
            eventTask.cancel();
        }
        
        int nextInterval = getRandomInterval();
        eventTask = Bukkit.getScheduler().runTaskLater(plugin, this::scheduleNextEvent, nextInterval * 20L);
        
        plugin.getLogger().info("Event penjualan spesial dihentikan secara manual. Event berikutnya dalam " + 
                (nextInterval / 60) + " menit");
        
        return true;
    }

    /**
     * Reloads event configuration from config file
     */
    public void reloadConfig() {
        loadConfig();
        plugin.getLogger().info("Event configuration reloaded");
    }
}