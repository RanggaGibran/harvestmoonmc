package id.rnggagib.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class WandUtils {
    
    public static ItemStack createWand() {
        ItemStack wand = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = wand.getItemMeta();
        
        meta.setDisplayName(MessageUtils.colorize("&6&lFarm Region Selector"));
        
        List<String> lore = new ArrayList<>();
        lore.add(MessageUtils.colorize("&7Left-click to select position 1"));
        lore.add(MessageUtils.colorize("&7Right-click to select position 2"));
        lore.add(MessageUtils.colorize("&7Then use &e/df create <name>&7 to create a region"));
        
        meta.setLore(lore);
        wand.setItemMeta(meta);
        
        return wand;
    }
    
    public static boolean isWand(ItemStack item) {
        if (item == null || item.getType() != Material.BLAZE_ROD) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }
        
        return MessageUtils.stripColor(meta.getDisplayName()).equals("Farm Region Selector");
    }
}