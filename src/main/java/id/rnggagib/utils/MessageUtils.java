package id.rnggagib.utils;

import org.bukkit.ChatColor;

public class MessageUtils {
    
    public static String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    public static String stripColor(String message) {
        return ChatColor.stripColor(message);
    }
}