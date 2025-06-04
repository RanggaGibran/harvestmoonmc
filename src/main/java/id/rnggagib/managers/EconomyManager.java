package id.rnggagib.managers;

import id.rnggagib.HarvestMoonMC;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {
    private final HarvestMoonMC plugin;
    private Economy economy = null;
    private boolean enabled = false;

    public EconomyManager(HarvestMoonMC plugin) {
        this.plugin = plugin;
        this.enabled = setupEconomy();
        
        if (enabled) {
            plugin.getLogger().info("Successfully hooked into Vault economy!");
        } else {
            plugin.getLogger().warning("Vault not found or no economy plugin detected. Economy features disabled.");
        }
    }
    
    /**
     * Sets up the economy hook with Vault
     * @return true if successful, false otherwise
     */
    private boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        
        economy = rsp.getProvider();
        return economy != null;
    }
    
    /**
     * Checks if economy is enabled
     * @return true if economy is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Gets the player's balance
     * @param player The player
     * @return The player's balance
     */
    public double getBalance(Player player) {
        if (!enabled) return 0.0;
        return economy.getBalance(player);
    }
    
    /**
     * Deposits money to a player
     * @param player The player
     * @param amount The amount to deposit
     * @return true if successful
     */
    public boolean depositMoney(Player player, double amount) {
        if (!enabled) return false;
        
        EconomyResponse response = economy.depositPlayer(player, amount);
        return response.transactionSuccess();
    }
    
    /**
     * Withdraws money from a player
     * @param player The player
     * @param amount The amount to withdraw
     * @return true if successful
     */
    public boolean withdrawMoney(Player player, double amount) {
        if (!enabled) return false;
        
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }
    
    /**
     * Formats the amount as currency
     * @param amount The amount
     * @return Formatted currency string
     */
    public String format(double amount) {
        if (!enabled) return String.format("%.2f", amount);
        return economy.format(amount);
    }
    
    /**
     * Checks if the player has enough money
     * @param player The player
     * @param amount The amount to check
     * @return true if the player has enough money
     */
    public boolean hasEnoughMoney(Player player, double amount) {
        if (!enabled) return false;
        return economy.has(player, amount);
    }
    
    /**
     * Checks if a player has enough money
     * @param player The player
     * @param amount The amount to check
     * @return true if player has enough money
     */
    public boolean hasMoney(Player player, double amount) {
        return getBalance(player) >= amount;
    }
}