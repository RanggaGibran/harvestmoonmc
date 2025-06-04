package id.rnggagib.models;

import java.util.UUID;

public class PlayerSkill {
    private final UUID playerId;
    private int xp;
    private int level;
    private int totalHarvests;
    
    // XP needed for each level
    private static final int[] XP_REQUIREMENTS = {
        0,        150,        375,        750,        1500,        3000,        5250,        8250,        12000,        16500,        21750,        27750,        34500,        42000,        50250,        59250,        69000,        79500,        90750,        102750,        115500,        129000,        143250,        158250,        174000,        190500,        207750,        225750,        244500,        264000    };
    
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