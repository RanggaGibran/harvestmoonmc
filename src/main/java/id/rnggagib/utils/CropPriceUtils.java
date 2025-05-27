package id.rnggagib.utils;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CropPriceUtils {
    // Ubah pattern agar tidak mencari §6 yang sudah di-strip
    private static final Pattern PRICE_PATTERN = Pattern.compile("Harga Jual: ([0-9.]+) Koin");
    
    /**
     * Extracts the price from a custom crop item's lore
     * @param item The item to check
     * @return The price or -1 if not a custom crop
     */
    public static double getItemPrice(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return -1;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) {
            return -1;
        }
        
        List<String> lore = meta.getLore();
        
        for (String line : lore) {
            String stripped = MessageUtils.stripColor(line);
            Matcher matcher = PRICE_PATTERN.matcher(stripped);
            if (matcher.find()) {
                try {
                    return Double.parseDouble(matcher.group(1));
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
        }
        
        return -1;
    }
    
    /**
     * Checks if an item is a sellable crop
     * @param item The item to check
     * @return true if the item is a sellable crop
     */
    public static boolean isSellableCrop(ItemStack item) {
        return getItemPrice(item) > 0;
    }
    
    /**
     * Debug method to check why an item isn't detected as sellable
     * Useful for troubleshooting
     */
    public static String getDebugInfo(ItemStack item) {
        if (item == null) return "Item is null";
        if (!item.hasItemMeta()) return "Item has no meta";
        
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return "Item has no lore";
        
        List<String> lore = meta.getLore();
        StringBuilder debug = new StringBuilder("Lore lines:");
        for (String line : lore) {
            debug.append("\n- Raw: '").append(line).append("'");
            debug.append("\n  Stripped: '").append(MessageUtils.stripColor(line)).append("'");
        }
        
        return debug.toString();
    }
}