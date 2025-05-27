package id.rnggagib.managers;

import id.rnggagib.HarvestMoonMC;
import id.rnggagib.models.PlayerSkill;
import id.rnggagib.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class SkillManager {
    private final HarvestMoonMC plugin;
    private final Map<UUID, PlayerSkill> playerSkills = new HashMap<>();
    private File skillsFile;
    private FileConfiguration skillsConfig;
    private BukkitTask autoSaveTask;
    private static final int AUTO_SAVE_INTERVAL = 5 * 60 * 20; // 5 minutes in ticks
    
    public SkillManager(HarvestMoonMC plugin) {
        this.plugin = plugin;
        this.setupSkillsFile();
        this.loadSkills();
        this.startAutoSave();
    }
    
    /**
     * Sets up the dedicated skills file
     */
    private void setupSkillsFile() {
        // Create data directory if it doesn't exist
        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        // Set up skills file
        skillsFile = new File(dataFolder, "skills.yml");
        if (!skillsFile.exists()) {
            try {
                skillsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create skills.yml", e);
            }
        }
        
        // Load skills config
        skillsConfig = YamlConfiguration.loadConfiguration(skillsFile);
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
            this::saveSkills, 
            AUTO_SAVE_INTERVAL, 
            AUTO_SAVE_INTERVAL
        );
    }
    
    /**
     * Loads player skills from the skills file
     */
    public void loadSkills() {
        playerSkills.clear();
        
        try {
            ConfigurationSection playersSection = skillsConfig.getConfigurationSection("players");
            
            if (playersSection == null) {
                plugin.getLogger().info("No player skills found to load.");
                return;
            }
            
            for (String uuidKey : playersSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidKey);
                    int xp = playersSection.getInt(uuidKey + ".xp", 0);
                    int level = playersSection.getInt(uuidKey + ".level", 1);
                    
                    PlayerSkill skill = new PlayerSkill(uuid);
                    skill.setXp(xp);
                    skill.setLevel(level);
                    
                    // Tambahkan pemuatan total panen
                    int totalHarvests = playersSection.getInt(uuidKey + ".harvests", 0);
                    skill.addHarvests(totalHarvests);
                    
                    playerSkills.put(uuid, skill);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error loading skills for player: " + uuidKey, e);
                }
            }
            
            plugin.getLogger().info("Loaded skills for " + playerSkills.size() + " players");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading player skills", e);
        }
    }
    
    /**
     * Saves all player skills to the skills file
     */
    public void saveSkills() {
        try {
            // Clear existing skills data
            skillsConfig.set("players", null);
            
            // Save each player's skills
            for (Map.Entry<UUID, PlayerSkill> entry : playerSkills.entrySet()) {
                UUID uuid = entry.getKey();
                PlayerSkill skill = entry.getValue();
                
                String path = "players." + uuid.toString();
                skillsConfig.set(path + ".xp", skill.getXp());
                skillsConfig.set(path + ".level", skill.getLevel());
                
                // Jika pemain online, simpan namanya sebagai referensi
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    skillsConfig.set(path + ".name", player.getName());
                }
                
                // Simpan total panen
                skillsConfig.set(path + ".harvests", skill.getTotalHarvests());
            }
            
            // Save the config file
            skillsConfig.save(skillsFile);
            
            plugin.getLogger().info("Saved skills for " + playerSkills.size() + " players");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save player skills to " + skillsFile, e);
        }
    }
    
    /**
     * Gets a player's skill, creating it if it doesn't exist
     */
    public PlayerSkill getSkill(Player player) {
        return getSkill(player.getUniqueId());
    }
    
    /**
     * Gets a player's skill by UUID, creating it if it doesn't exist
     */
    public PlayerSkill getSkill(UUID uuid) {
        PlayerSkill skill = playerSkills.get(uuid);
        
        if (skill == null) {
            skill = new PlayerSkill(uuid);
            playerSkills.put(uuid, skill);
        }
        
        return skill;
    }
    
    /**
     * Awards farming XP to a player
     * @param player The player
     * @param amount The amount of XP
     * @return true if the player leveled up
     */
    public boolean awardXp(Player player, int amount) {
        PlayerSkill skill = getSkill(player);
        boolean leveledUp = skill.addXp(amount);

        if (leveledUp) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") +
                    "&aLevel up! Farming Skill naik ke level &6" + skill.getLevel() + "&a!"));
            player.playSound(player.getLocation(), "entity.player.levelup", 1.0f, 1.0f);
        }
        return leveledUp;
    }
    
    /**
     * Gets the quality bonus multiplier based on player level
     * @param player The player
     * @return The quality multiplier (1.0 = no bonus)
     */
    public double getQualityMultiplier(Player player) {
        PlayerSkill skill = getSkill(player);
        int level = skill.getLevel();
        
        // Base multiplier from config or default formula
        double baseMultiplier = plugin.getConfig().getDouble("skills.quality_multiplier_per_level", 0.02);
        
        // Calculate bonus (e.g., 2% bonus per level)
        double bonus = 1.0 + (level - 1) * baseMultiplier;
        
        // Cap the bonus if needed
        double maxBonus = plugin.getConfig().getDouble("skills.max_quality_multiplier", 2.0);
        return Math.min(bonus, maxBonus);
    }
    
    /**
     * Gets the XP gain for harvesting a crop with a specific quality
     * @param cropType The type of crop
     * @param quality The quality tier of the crop
     * @return The XP amount to award
     */
    public int getCropXp(Material cropType, String quality) {
        // Base XP from config or default values
        int baseXp = plugin.getConfig().getInt("skills.base_xp." + cropType.toString(), 1);
        
        // Quality multiplier from config or default values
        double qualityMultiplier = plugin.getConfig().getDouble("skills.quality_xp_multiplier." + quality, 1.0);
        
        // Calculate final XP
        return (int) Math.round(baseXp * qualityMultiplier);
    }
    
    /**
     * Gets the XP required for the next level
     * @param level Current level
     * @return XP required for next level, or -1 if at max level
     */
    public int getXpForNextLevel(int level) {
        if (level >= PlayerSkill.getMaxLevel()) return -1;
        
        // Get the player skill empty instance just to access the XP requirements
        PlayerSkill dummySkill = new PlayerSkill(UUID.randomUUID());
        dummySkill.setLevel(level);
        return dummySkill.getXpForNextLevel();
    }
    
    /**
     * Shutdown method to ensure data is saved
     */
    public void shutdown() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }
        saveSkills();
    }
}