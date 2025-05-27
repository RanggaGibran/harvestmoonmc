package id.rnggagib.utils;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CropUtils {
    private static final List<Material> SUPPORTED_CROPS = Arrays.asList(
            Material.WHEAT, 
            Material.CARROTS, 
            Material.POTATOES, 
            Material.BEETROOTS
    );
    
    private static final Map<Material, Material> CROP_TO_DROP = new HashMap<>();
    private static final Map<Material, String> CROP_NAMES = new HashMap<>();
    private static final Map<Material, Integer> BASE_DROP_AMOUNTS = new HashMap<>();
    
    static {
        // Initialize crop to drop mapping
        CROP_TO_DROP.put(Material.WHEAT, Material.WHEAT);
        CROP_TO_DROP.put(Material.CARROTS, Material.CARROT);
        CROP_TO_DROP.put(Material.POTATOES, Material.POTATO);
        CROP_TO_DROP.put(Material.BEETROOTS, Material.BEETROOT);
        
        // Initialize crop names
        CROP_NAMES.put(Material.WHEAT, "Gandum");
        CROP_NAMES.put(Material.CARROTS, "Wortel");
        CROP_NAMES.put(Material.POTATOES, "Kentang");
        CROP_NAMES.put(Material.BEETROOTS, "Bit");
        
        // Initialize base drop amounts
        BASE_DROP_AMOUNTS.put(Material.WHEAT, 1);
        BASE_DROP_AMOUNTS.put(Material.CARROTS, 2);
        BASE_DROP_AMOUNTS.put(Material.POTATOES, 2);
        BASE_DROP_AMOUNTS.put(Material.BEETROOTS, 1);
    }
    
    public static boolean isSupportedCrop(Material material) {
        return SUPPORTED_CROPS.contains(material);
    }
    
    public static Material getCropDrop(Material cropType) {
        return CROP_TO_DROP.getOrDefault(cropType, Material.WHEAT);
    }
    
    public static String getCropName(Material cropType) {
        return CROP_NAMES.getOrDefault(cropType, "Unknown");
    }
    
    public static int getBaseDropAmount(Material cropType) {
        return BASE_DROP_AMOUNTS.getOrDefault(cropType, 1);
    }
}