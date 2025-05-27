package id.rnggagib.managers;

import id.rnggagib.HarvestMoonMC;
import id.rnggagib.models.FarmingRegion;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class RegionManager {
    private final HarvestMoonMC plugin;
    private final Map<String, FarmingRegion> regions = new HashMap<>();
    private File regionsFile;
    private FileConfiguration regionsConfig;
    private BukkitTask autoSaveTask;
    private static final int AUTO_SAVE_INTERVAL = 15 * 60 * 20; // 15 minutes in ticks
    
    public RegionManager(HarvestMoonMC plugin) {
        this.plugin = plugin;
        this.setupRegionsFile();
        this.startAutoSave();
    }
    
    /**
     * Sets up the dedicated regions file
     */
    private void setupRegionsFile() {
        // Create regions directory if it doesn't exist
        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        // Set up regions file
        regionsFile = new File(dataFolder, "regions.yml");
        if (!regionsFile.exists()) {
            try {
                regionsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create regions.yml", e);
            }
        }
        
        // Load regions config
        regionsConfig = YamlConfiguration.loadConfiguration(regionsFile);
    }
    
    /**
     * Starts the auto-save task
     */
    private void startAutoSave() {
        // Cancel existing task if it exists
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }
        
        // Start new auto-save task
        autoSaveTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
            plugin, 
            this::saveRegions, 
            AUTO_SAVE_INTERVAL, 
            AUTO_SAVE_INTERVAL
        );
    }
    
    /**
     * Loads regions from the dedicated regions file
     */
    public void loadRegions() {
        regions.clear();
        
        try {
            ConfigurationSection regionsSection = regionsConfig.getConfigurationSection("regions");
            
            if (regionsSection == null) {
                plugin.getLogger().info("No regions found to load.");
                return;
            }
            
            for (String key : regionsSection.getKeys(false)) {
                try {
                    String worldName = regionsSection.getString(key + ".world");
                    
                    if (plugin.getServer().getWorld(worldName) == null) {
                        plugin.getLogger().warning("Could not load region '" + key + "': World '" + worldName + "' not found");
                        continue;
                    }
                    
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", key);
                    map.put("world", worldName);
                    
                    Map<String, Object> minMap = new HashMap<>();
                    minMap.put("x", regionsSection.getDouble(key + ".min.x"));
                    minMap.put("y", regionsSection.getDouble(key + ".min.y"));
                    minMap.put("z", regionsSection.getDouble(key + ".min.z"));
                    map.put("min", minMap);
                    
                    Map<String, Object> maxMap = new HashMap<>();
                    maxMap.put("x", regionsSection.getDouble(key + ".max.x"));
                    maxMap.put("y", regionsSection.getDouble(key + ".max.y"));
                    maxMap.put("z", regionsSection.getDouble(key + ".max.z"));
                    map.put("max", maxMap);
                    
                    FarmingRegion region = new FarmingRegion(map);
                    regions.put(key, region);
                    
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error loading region: " + key, e);
                }
            }
            
            plugin.getLogger().info("Loaded " + regions.size() + " farming regions");
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading regions", e);
        }
    }
    
    /**
     * Saves regions to the dedicated regions file
     */
    public void saveRegions() {
        try {
            // Clear existing regions
            regionsConfig.set("regions", null);
            
            // Save each region
            for (FarmingRegion region : regions.values()) {
                String name = region.getName();
                
                regionsConfig.set("regions." + name + ".world", region.getWorld().getName());
                
                Location min = region.getMin();
                regionsConfig.set("regions." + name + ".min.x", min.getX());
                regionsConfig.set("regions." + name + ".min.y", min.getY());
                regionsConfig.set("regions." + name + ".min.z", min.getZ());
                
                Location max = region.getMax();
                regionsConfig.set("regions." + name + ".max.x", max.getX());
                regionsConfig.set("regions." + name + ".max.y", max.getY());
                regionsConfig.set("regions." + name + ".max.z", max.getZ());
            }
            
            // Save the config file
            regionsConfig.save(regionsFile);
            
            plugin.getLogger().info("Saved " + regions.size() + " farming regions");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save regions to " + regionsFile, e);
        }
    }
    
    /**
     * Adds a region to the manager
     */
    public void addRegion(FarmingRegion region) {
        regions.put(region.getName(), region);
    }
    
    /**
     * Gets a region by name
     */
    public FarmingRegion getRegion(String name) {
        return regions.get(name);
    }
    
    /**
     * Checks if a region exists by name
     */
    public boolean regionExists(String name) {
        return regions.containsKey(name);
    }
    
    /**
     * Gets the region at a specific location
     */
    public FarmingRegion getRegionAt(Location location) {
        for (FarmingRegion region : regions.values()) {
            if (region.contains(location)) {
                return region;
            }
        }
        return null;
    }
    
    /**
     * Gets all regions
     */
    public Map<String, FarmingRegion> getAllRegions() {
        return regions;
    }
    
    /**
     * Removes a region by name
     */
    public void removeRegion(String name) {
        regions.remove(name);
    }
    
    /**
     * Shutdown method to ensure data is saved
     */
    public void shutdown() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }
        saveRegions();
    }
}