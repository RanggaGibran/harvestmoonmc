package id.rnggagib;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import id.rnggagib.commands.HMCCommandExecutor;
import id.rnggagib.listeners.FarmingListener;
import id.rnggagib.listeners.WandListener;
import id.rnggagib.managers.EconomyManager;
import id.rnggagib.managers.RegionManager;
import id.rnggagib.managers.SelectionManager;
import id.rnggagib.managers.SkillManager;
import id.rnggagib.managers.WorldGuardManager;
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
    private SkillManager skillManager;
    private ShopGUI shopGUI;
    private FileConfiguration config;
    private WorldGuardManager worldGuardManager;
    
    // Deklarasi flag sebagai field statis
    public static StateFlag ALLOW_WHEAT_FARMING;
    
    @Override
    public void onLoad() {
        // Daftarkan WorldGuard flag sebelum WorldGuard sepenuhnya diaktifkan
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            getLogger().info("WorldGuard detected, registering custom flag...");
            
            FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
            try {
                // Buat flag dengan nama "allow-wheat-farming", default false
                StateFlag flag = new StateFlag("allow-wheat-farming", false);
                registry.register(flag);
                ALLOW_WHEAT_FARMING = flag;
                getLogger().info("Successfully registered WorldGuard flag: allow-wheat-farming");
            } catch (FlagConflictException e) {
                // Jika flag sudah ada, gunakan flag yang sudah ada
                Flag<?> existing = registry.get("allow-wheat-farming");
                if (existing instanceof StateFlag) {
                    ALLOW_WHEAT_FARMING = (StateFlag) existing;
                    getLogger().info("Using existing WorldGuard flag: allow-wheat-farming");
                } else {
                    getLogger().warning("Conflict with existing flag of different type: allow-wheat-farming");
                }
            }
        }
    }
    
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
        skillManager = new SkillManager(this);
        
        // Initialize shop GUI
        shopGUI = new ShopGUI(this);
        
        // Load farm regions
        regionManager.loadRegions();
        
        // Register commands
        getCommand("hmc").setExecutor(new HMCCommandExecutor(this));
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new WandListener(this), this);
        getServer().getPluginManager().registerEvents(new FarmingListener(this), this);
        
        // Initialize WorldGuard manager
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            worldGuardManager = new WorldGuardManager(this);
            getLogger().info("WorldGuard integration initialized");
        } else {
            getLogger().info("WorldGuard not found, integration disabled");
        }
        
        LOGGER.info("harvestmoonmc enabled");
    }
    
    // Update onDisable method to ensure proper shutdown
    @Override
    public void onDisable() {
        // Save farm regions and properly shut down
        regionManager.shutdown();
        skillManager.shutdown();
        
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
    
    public SkillManager getSkillManager() {
        return skillManager;
    }
    
    public ShopGUI getShopGUI() {
        return shopGUI;
    }
    
    public WorldGuardManager getWorldGuardManager() {
        return worldGuardManager;
    }
}
