package id.rnggagib.models;

import java.util.UUID;

public class PlayerSkill {
    private final UUID playerId;
    private int xp;
    private int level;
    private int totalHarvests;
      // XP needed for each level (increased for slower leveling)
    private static final int[] XP_REQUIREMENTS = {
        0,      // Level 1 (starting level)
        200,    // Level 2 (was 100)
        500,    // Level 3 (was 250)
        1000,   // Level 4 (was 500)
        2000,   // Level 5 (was 1000)
        4000,   // Level 6 (was 2000)
        7000,   // Level 7 (was 3500)
        11000,  // Level 8 (was 5500)
        16000,  // Level 9 (was 8000)
        22000,  // Level 10 (was 11000)
        29000,  // Level 11 (was 14500)
        37000,  // Level 12 (was 18500)
        46000,  // Level 13 (was 23000)
        56000,  // Level 14 (was 28000)
        67000,  // Level 15 (was 33500)
        79000,  // Level 16 (was 39500)
        92000,  // Level 17 (was 46000)
        106000, // Level 18 (was 53000)
        121000, // Level 19 (was 60500)
        137000, // Level 20 (was 68500)
        154000, // Level 21 (was 77000)
        172000, // Level 22 (was 86000)
        191000, // Level 23 (was 95500)
        211000, // Level 24 (was 105500)
        232000, // Level 25 (was 116000)
        254000, // Level 26 (was 127000)
        277000, // Level 27 (was 138500)
        301000, // Level 28 (was 150500)
        326000, // Level 29 (was 163000)
        352000, // Level 30 (was 176000)
    };
    
    private static final int MAX_LEVEL = 30;
    
    public PlayerSkill(UUID playerId) {
        this.playerId = playerId;
        this.xp = 0;
        this.level = 1;
        this.totalHarvests = 0;
    }
    
    /**
     * Adds XP to the player's skill and checks for level up
     * @param amount The amount of XP to add
     * @return true if the player leveled up
     */
    public boolean addXp(int amount) {
        int oldLevel = level;
        
        xp += amount;
        updateLevel();
        
        return level > oldLevel;
    }
    
    /**
     * Updates the player's level based on their current XP
     */
    private void updateLevel() {
        if (level >= MAX_LEVEL) return; // Already max level
        
        while (level < MAX_LEVEL && xp >= getXpForNextLevel()) {
            level++;
        }
    }
    
    /**
     * Gets the XP required for the next level
     * @return The XP requirement or -1 if at max level
     */
    public int getXpForNextLevel() {
        if (level >= MAX_LEVEL) return -1;
        return XP_REQUIREMENTS[level];
    }
    
    /**
     * Gets the XP progress towards the next level (0-1)
     * @return The progress as a decimal (0.0 to 1.0)
     */
    public double getProgressToNextLevel() {
        if (level >= MAX_LEVEL) return 1.0;
        
        int currentLevelXp = level > 1 ? XP_REQUIREMENTS[level - 1] : 0;
        int nextLevelXp = XP_REQUIREMENTS[level];
        int xpForLevel = nextLevelXp - currentLevelXp;
        int xpProgress = xp - currentLevelXp;
        
        return (double) xpProgress / xpForLevel;
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public int getXp() {
        return xp;
    }
    
    public void setXp(int xp) {
        this.xp = xp;
        updateLevel();
    }
    
    public int getLevel() {
        return level;
    }
    
    public void setLevel(int level) {
        this.level = Math.min(Math.max(1, level), MAX_LEVEL);
    }
    
    public int getXpForCurrentLevel() {
        if (level <= 1) return 0;
        return XP_REQUIREMENTS[level - 1];
    }
    
    public static int getMaxLevel() {
        return MAX_LEVEL;
    }
    
    /**
     * Gets the total number of harvests by this player
     * @return Total harvests count
     */
    public int getTotalHarvests() {
        return totalHarvests;
    }

    /**
     * Increments the total harvest count by 1
     */
    public void incrementHarvests() {
        totalHarvests++;
    }

    /**
     * Increments the total harvest count by specified amount
     * @param amount Amount to add
     */
    public void addHarvests(int amount) {
        totalHarvests += amount;
    }
}