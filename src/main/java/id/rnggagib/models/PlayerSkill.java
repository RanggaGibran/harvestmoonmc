package id.rnggagib.models;

import java.util.UUID;

public class PlayerSkill {
    private final UUID playerId;
    private int xp;
    private int level;
    
    // XP needed for each level
    private static final int[] XP_REQUIREMENTS = {
        0,      // Level 1 (starting level)
        100,    // Level 2
        250,    // Level 3
        500,    // Level 4
        1000,   // Level 5
        2000,   // Level 6
        3500,   // Level 7
        5500,   // Level 8
        8000,   // Level 9
        11000,  // Level 10
        14500,  // Level 11
        18500,  // Level 12
        23000,  // Level 13
        28000,  // Level 14
        33500,  // Level 15
        39500,  // Level 16
        46000,  // Level 17
        53000,  // Level 18
        60500,  // Level 19
        68500,  // Level 20
        77000,  // Level 21
        86000,  // Level 22
        95500,  // Level 23
        105500, // Level 24
        116000, // Level 25
        127000, // Level 26
        138500, // Level 27
        150500, // Level 28
        163000, // Level 29
        176000, // Level 30
    };
    
    private static final int MAX_LEVEL = 30;
    
    public PlayerSkill(UUID playerId) {
        this.playerId = playerId;
        this.xp = 0;
        this.level = 1;
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
}