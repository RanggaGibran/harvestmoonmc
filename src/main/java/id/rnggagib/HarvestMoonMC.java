package id.rnggagib;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import id.rnggagib.commands.HMCCommandExecutor;
import id.rnggagib.commands.HoeShopCommandExecutor;
import id.rnggagib.commands.DragFarmTabCompleter;
import id.rnggagib.commands.FarmCommandExecutor; // Add this import
import id.rnggagib.listeners.AdminJoinListener;
import id.rnggagib.listeners.FarmingListener;
import id.rnggagib.listeners.WandListener;
import id.rnggagib.listeners.HoeInteractListener;
import id.rnggagib.managers.EconomyManager;
import id.rnggagib.managers.EventManager;
import id.rnggagib.managers.RegionManager;
import id.rnggagib.managers.SelectionManager;
import id.rnggagib.managers.SkillManager;
import id.rnggagib.managers.WorldGuardManager;
import id.rnggagib.gui.ShopGUI;
import id.rnggagib.gui.HoeShopGUI;
import id.rnggagib.managers.HarvestLimitManager;
import id.rnggagib.managers.LicenseManager;
import id.rnggagib.managers.PlaceholderManager;
import id.rnggagib.managers.HoeManager;
import id.rnggagib.managers.DiscordWebhookManager; // Add this import
import org.bukkit.Bukkit; // Add this import if not present
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
    private static final Logger LOGGER = Logger.getLogger("dragfarm");
    private static HarvestMoonMC instance;
    
    private SelectionManager selectionManager;
    private RegionManager regionManager;
    private EconomyManager economyManager;
    private SkillManager skillManager;
    private ShopGUI shopGUI;
    private HoeShopGUI hoeShopGUI;
    private FileConfiguration config;
    private WorldGuardManager worldGuardManager;
    private EventManager eventManager;
    private HarvestLimitManager harvestLimitManager; // Add this field to the class
    private LicenseManager licenseManager;
    private HoeManager hoeManager; // Tambahkan field untuk HoeManager
    private FarmingListener farmingListener; // Add this
    private DiscordWebhookManager discordWebhookManager; // Add this field

    // Deklarasi flag sebagai field statis
    public static StateFlag ALLOW_WHEAT_FARMING;
    
    @Override
    public void onLoad() {
        // Daftarkan WorldGuard flag sebelum WorldGuard sepenuhnya diaktifkan
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            getLogger().info("WorldGuard detected, registering custom flag...");
            
            FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
            try {
                // Buat flag baru
                StateFlag flag = new StateFlag("allow-wheat-farming", true); // default true
                registry.register(flag);
                ALLOW_WHEAT_FARMING = flag; // Simpan flag untuk digunakan nanti
                getLogger().info("Custom WorldGuard flag 'allow-wheat-farming' registered.");
            } catch (FlagConflictException e) {
                // Flag sudah ada, coba dapatkan instance yang ada
                Flag<?> existingFlag = registry.get("allow-wheat-farming");
                if (existingFlag instanceof StateFlag) {
                    ALLOW_WHEAT_FARMING = (StateFlag) existingFlag;
                    getLogger().info("Custom WorldGuard flag 'allow-wheat-farming' already registered, using existing.");
                } else {
                    getLogger().severe("Flag 'allow-wheat-farming' already exists but is not a StateFlag!");
                }
            } catch (Exception e) {
                getLogger().log(java.util.logging.Level.SEVERE, "Error registering WorldGuard flag", e);
            }
        }
    }
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config if it doesn't exist
        saveDefaultConfig();
        config = getConfig();
        
        // Inisialisasi LicenseManager dan validasi lisensi sebelum plugin berjalan
        licenseManager = new LicenseManager(this);
        licenseManager.initialize(); // This will eventually call completePluginInitialization
    }

    public void completePluginInitialization() {
        // This method is called by LicenseManager after IP is fetched and license is valid.
        // Run on main thread for Bukkit API calls during manager initializations.
        Bukkit.getScheduler().runTask(this, () -> {
            if (!licenseManager.isValidated()) { // Double check, though LicenseManager should have disabled if not
                getLogger().severe("Attempted to complete initialization with invalid license. Disabling.");
                Bukkit.getPluginManager().disablePlugin(this);
                return;
            }

            // Initialize managers
            selectionManager = new SelectionManager();
            regionManager = new RegionManager(this);
            economyManager = new EconomyManager(this);
            skillManager = new SkillManager(this);
            hoeManager = new HoeManager(this);
            eventManager = new EventManager(this); // Initialize EventManager
            harvestLimitManager = new HarvestLimitManager(this); // Initialize HarvestLimitManager
            
            this.discordWebhookManager = new DiscordWebhookManager(this); // Initialize Discord Manager FIRST
            
            // Initialize GUIs
            shopGUI = new ShopGUI(this);
            hoeShopGUI = new HoeShopGUI(this);
            
            // Load farm regions
            regionManager.loadRegions();
            
            // Register command executor and tab completer
            getCommand("dragfarm").setExecutor(new HMCCommandExecutor(this));
            getCommand("dragfarm").setTabCompleter(new DragFarmTabCompleter(this));
            getCommand("hoeshop").setExecutor(new HoeShopCommandExecutor(this));
            getCommand("farm").setExecutor(new FarmCommandExecutor(this)); // Register new command

            // Register listeners
            this.farmingListener = new FarmingListener(this);
            getServer().getPluginManager().registerEvents(this.farmingListener, this);
            getServer().getPluginManager().registerEvents(new WandListener(this), this);
            // Register AdminJoinListener
            getServer().getPluginManager().registerEvents(new AdminJoinListener(this), this);
            // Register HoeInteractListener
            getServer().getPluginManager().registerEvents(new HoeInteractListener(this), this);
            
            // Initialize WorldGuard manager
            if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
                worldGuardManager = new WorldGuardManager(this);
                // getLogger().info("WorldGuard integration initialized"); // Logging is fine here
            } else {
                // getLogger().info("WorldGuard not found, integration disabled");
            }
            
            // Register PlaceholderAPI expansion if it's present
            if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                // getLogger().info("PlaceholderAPI found, registering placeholders...");
                new PlaceholderManager(this).register();
            }

            // Send Discord Notifications AFTER other managers are ready (especially LicenseManager for IP)
            this.discordWebhookManager.sendDownloadNotification();
            this.discordWebhookManager.sendStatsNotification();
            this.discordWebhookManager.startPeriodicOpInfoUpdate();
            
            getLogger().info("DragFarm enabled successfully with valid license and all components initialized.");
        });
    }
    
    // Update onDisable method to ensure proper shutdown
    @Override
    public void onDisable() {
        if (discordWebhookManager != null) discordWebhookManager.shutdown(); // Shutdown Discord first
        if (regionManager != null) regionManager.shutdown();
        if (skillManager != null) skillManager.shutdown();
        if (eventManager != null) eventManager.shutdown();
        if (harvestLimitManager != null) harvestLimitManager.shutdown();
        getLogger().info("DragFarm disabled.");
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
    
    public HoeShopGUI getHoeShopGUI() {
        return hoeShopGUI;
    }
    
    public WorldGuardManager getWorldGuardManager() {
        return worldGuardManager;
    }
    
    public EventManager getEventManager() {
        return eventManager;
    }
    
    public HarvestLimitManager getHarvestLimitManager() { // Add getter method
        return harvestLimitManager;
    }

    public LicenseManager getLicenseManager() {
        return licenseManager;
    }

    public HoeManager getHoeManager() {
        return hoeManager;
    }

    public FarmingListener getFarmingListener() { // Add this getter
        return farmingListener;
    }

    public DiscordWebhookManager getDiscordWebhookManager() { // Add getter if needed elsewhere
        return discordWebhookManager;
    }
}
