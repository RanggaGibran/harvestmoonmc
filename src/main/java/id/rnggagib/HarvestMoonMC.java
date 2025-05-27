package id.rnggagib;

import id.rnggagib.commands.HMCCommandExecutor;
import id.rnggagib.listeners.FarmingListener;
import id.rnggagib.listeners.WandListener;
import id.rnggagib.managers.EconomyManager;
import id.rnggagib.managers.RegionManager;
import id.rnggagib.managers.SelectionManager;
import id.rnggagib.gui.ShopGUI;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/*
 * harvestmoonmc java plugin
 */
public class HarvestMoonMC extends JavaPlugin {
    private static final Logger LOGGER = Logger.getLogger("harvestmoonmc");
    private static HarvestMoonMC instance;
    
    private SelectionManager selectionManager;
    private RegionManager regionManager;
    private EconomyManager economyManager;
    private ShopGUI shopGUI;
    private FileConfiguration config;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config if it doesn't exist
        saveDefaultConfig();
        config = getConfig();
        
        // Initialize managers
        selectionManager = new SelectionManager();
        regionManager = new RegionManager(this);
        economyManager = new EconomyManager(this);
        
        // Initialize shop GUI
        shopGUI = new ShopGUI(this);
        
        // Load farm regions
        regionManager.loadRegions();
        
        // Register commands
        getCommand("hmc").setExecutor(new HMCCommandExecutor(this));
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new WandListener(this), this);
        getServer().getPluginManager().registerEvents(new FarmingListener(this), this);
        
        LOGGER.info("harvestmoonmc enabled");
    }
    
    // Update onDisable method to ensure proper shutdown
    @Override
    public void onDisable() {
        // Save farm regions and properly shut down
        regionManager.shutdown();
        
        LOGGER.info("harvestmoonmc disabled");
    }
    
    public static HarvestMoonMC getInstance() {
        return instance;
    }
    
    public SelectionManager getSelectionManager() {
        return selectionManager;
    }
    
    public RegionManager getRegionManager() {
        return regionManager;
    }
    
    public EconomyManager getEconomyManager() {
        return economyManager;
    }
    
    public ShopGUI getShopGUI() {
        return shopGUI;
    }
}
