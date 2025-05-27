package id.rnggagib.utils;

import id.rnggagib.HarvestMoonMC;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;

import java.util.*;

public class QualityUtils {
    
    public static Map<String, Object> calculateQuality(HarvestMoonMC plugin, Material cropType, ItemStack tool, Player player) {
        Map<String, Object> result = new HashMap<>();
        Random random = new Random();
        
        ConfigurationSection qualityTiersSection = plugin.getConfig().getConfigurationSection("grinding_system.quality_tiers");
        if (qualityTiersSection == null) {
            // Default values if configuration doesn't exist
            result.put("tier", "COMMON");
            result.put("displayName", "Common");
            result.put("colorCode", "&f");
            result.put("price", 5.0);
            return result;
        }
        
        // Get all quality tiers
        List<String> tiers = new ArrayList<>(qualityTiersSection.getKeys(false));
        
        // Calculate tool modifier
        double toolModifier = calculateToolModifier(plugin, tool);
        
        // Get weighted tiers based on tool modifier
        List<String> weightedTiers = new ArrayList<>();
        // Apply player skill multiplier to weight chance if player is provided
        if (player != null) {
            double skillMultiplier = plugin.getSkillManager().getQualityMultiplier(player);
            
            for (String tier : tiers) {
                int weight = qualityTiersSection.getInt(tier + ".weight");
                
                // Apply tool modifier to weight chance (better tools = better chance for rare items)
                if (!tier.equals("COMMON")) {
                    weight = (int) (weight * toolModifier);
                    
                    // Apply skill modifier to non-common tiers
                    weight = (int) (weight * skillMultiplier);
                }
                
                // Add the tier to the list multiple times based on its weight
                for (int i = 0; i < weight; i++) {
                    weightedTiers.add(tier);
                }
            }
        } else {
            // Existing code for when player is null
            for (String tier : tiers) {
                int weight = qualityTiersSection.getInt(tier + ".weight");
                
                // Apply tool modifier to weight chance (better tools = better chance for rare items)
                if (!tier.equals("COMMON")) {
                    weight = (int) (weight * toolModifier);
                }
                
                // Add the tier to the list multiple times based on its weight
                for (int i = 0; i < weight; i++) {
                    weightedTiers.add(tier);
                }
            }
        }
        
        // Select a random tier from the weighted list
        String selectedTier = weightedTiers.get(random.nextInt(weightedTiers.size()));
        
        // Get the details for the selected tier
        String displayName = qualityTiersSection.getString(selectedTier + ".display_name", selectedTier);
        String nameSuffix = qualityTiersSection.getString(selectedTier + ".name_suffix", "[" + selectedTier + "]");
        String colorName = qualityTiersSection.getString(selectedTier + ".color", "WHITE");
        ChatColor chatColor;
        try {
            chatColor = ChatColor.valueOf(colorName.toUpperCase());
        } catch (Exception e) {
            chatColor = ChatColor.WHITE;
        }
        double priceMultiplier = qualityTiersSection.getDouble(selectedTier + ".price_multiplier", 1.0);
        
        // Calculate price
        double basePrice = plugin.getConfig().getDouble("grinding_system.base_prices." + cropType.toString(), 5.0);
        double finalPrice = basePrice * priceMultiplier * toolModifier;
        
        // Round price to 2 decimal places
        finalPrice = Math.round(finalPrice * 100.0) / 100.0;
        
        // Set the result
        result.put("tier", selectedTier);
        result.put("displayName", nameSuffix);
        result.put("colorCode", chatColor.toString());
        result.put("price", finalPrice);
        
        return result;
    }
    
    private static double calculateToolModifier(HarvestMoonMC plugin, ItemStack tool) {
        // Default modifier
        double modifier = 1.0;
        
        if (tool == null) {
            return modifier;
        }
        
        // Check tool material modifier
        ConfigurationSection hoeModifiersSection = plugin.getConfig().getConfigurationSection("grinding_system.hoe_modifiers");
        if (hoeModifiersSection != null && hoeModifiersSection.contains(tool.getType().toString())) {
            modifier = hoeModifiersSection.getDouble(tool.getType().toString(), 1.0);
        }
        
        // Check enchantment modifiers
        ConfigurationSection enchantmentModifiersSection = plugin.getConfig().getConfigurationSection("grinding_system.enchantment_modifiers");
        if (enchantmentModifiersSection != null) {
            for (Enchantment enchantment : tool.getEnchantments().keySet()) {
                int level = tool.getEnchantmentLevel(enchantment);
                
                String enchantName = enchantment.getKey().getKey().toUpperCase();
                if (enchantmentModifiersSection.contains(enchantName) && 
                    enchantmentModifiersSection.contains(enchantName + "." + level)) {
                    double enchantModifier = enchantmentModifiersSection.getDouble(enchantName + "." + level, 1.0);
                    modifier *= enchantModifier;
                }
            }
        }
        
        return modifier;
    }
}