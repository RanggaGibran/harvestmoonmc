package id.rnggagib.listeners;

import id.rnggagib.HarvestMoonMC;
import id.rnggagib.models.FarmingRegion;
import id.rnggagib.utils.CropUtils;
import id.rnggagib.utils.MessageUtils;
import id.rnggagib.utils.QualityUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class FarmingListener implements Listener {
    private final HarvestMoonMC plugin;
    private final Random random = new Random();
    private final Set<Block> processingBlocks = new HashSet<>();

    public FarmingListener(HarvestMoonMC plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        // Prevent duplicate processing
        if (processingBlocks.contains(block)) {
            event.setCancelled(true);
            return;
        }

        // Check if the block is inside a farming region
        FarmingRegion region = plugin.getRegionManager().getRegionAt(block.getLocation());
        if (region == null) {
            return; // Not in a farming region, normal behavior
        }

        // Check if the block is a supported crop
        if (!CropUtils.isSupportedCrop(block.getType())) {
            return; // Not a supported crop, normal behavior
        }

        // Get the crop's growth stage
        Ageable ageable = (Ageable) block.getBlockData();
        boolean isMature = ageable.getAge() == ageable.getMaximumAge();

        if (isMature) {
            // Cancel the default drop
            event.setCancelled(true);
            processingBlocks.add(block);

            // Create custom crop drop
            Material cropType = block.getType();
            ItemStack heldItem = player.getInventory().getItemInMainHand();
            
            // Calculate crop quality and create custom drop
            Map<String, Object> qualityInfo = QualityUtils.calculateQuality(plugin, cropType, heldItem);
            String qualityTier = (String) qualityInfo.get("tier");
            String displayName = (String) qualityInfo.get("displayName");
            String colorCode = (String) qualityInfo.get("colorCode");
            double price = (Double) qualityInfo.get("price");
            
            // Create the custom crop item
            ItemStack customCrop = createCustomCrop(cropType, qualityTier, displayName, colorCode, price);
            
            // Calculate drop amount (consider Fortune enchantment)
            int dropAmount = calculateDropAmount(heldItem, cropType);
            customCrop.setAmount(dropAmount);
            
            // Give item to player
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(customCrop);
            
            // Drop any items that didn't fit in inventory
            for (ItemStack item : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
            
            // Reset crop age to 0 (seedling)
            Material cropMaterial = block.getType();
            Ageable newAgeable = (Ageable) cropMaterial.createBlockData();
            newAgeable.setAge(0);
            block.setBlockData(newAgeable);

            // Start growth animation
            startGrowthAnimation(block);
        } else {
            // Cancel the event if the crop is not mature to prevent breaking
            event.setCancelled(true);
            
            // Send message to player (optional)
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                    "&cTanaman belum siap dipanen!"));
        }
    }

    private void startGrowthAnimation(Block block) {
        Material cropType = block.getType();
        Ageable ageable = (Ageable) block.getBlockData();
        int maxAge = ageable.getMaximumAge();
        
        // Schedule task to animate growth
        new BukkitRunnable() {
            int currentAge = 0;
            int animationDelay = plugin.getConfig().getInt("farming.animation_delay_ticks", 5);

            @Override
            public void run() {
                // Check if block still exists and is still a crop
                if (!block.getType().equals(cropType) || currentAge >= maxAge) {
                    processingBlocks.remove(block);
                    this.cancel();
                    return;
                }

                // Increment the age
                currentAge++;
                if (currentAge <= maxAge) {
                    Ageable blockData = (Ageable) block.getBlockData();
                    blockData.setAge(currentAge);
                    block.setBlockData(blockData);
                } else {
                    processingBlocks.remove(block);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 5L, 10L); // Run every 10 ticks (0.5 seconds), starting 5 ticks later
    }

    private ItemStack createCustomCrop(Material cropType, String qualityTier, String displayName, String colorCode, double price) {
        Material dropType = CropUtils.getCropDrop(cropType);
        ItemStack customItem = new ItemStack(dropType);
        ItemMeta meta = customItem.getItemMeta();
        
        // Set display name with quality
        meta.setDisplayName(MessageUtils.colorize(colorCode + CropUtils.getCropName(cropType) + " " + displayName));
        
        // Set lore with quality and price
        List<String> lore = new ArrayList<>();
        lore.add(MessageUtils.colorize("&7Kualitas: " + colorCode + displayName));
        lore.add(MessageUtils.colorize("&7Harga Jual: &6" + price + " Koin"));
        meta.setLore(lore);
        
        customItem.setItemMeta(meta);
        return customItem;
    }

    private int calculateDropAmount(ItemStack tool, Material cropType) {
        int baseAmount = CropUtils.getBaseDropAmount(cropType);
        
        // Check if tool has Fortune enchantment
        if (tool != null && tool.getEnchantments().containsKey(Enchantment.LOOT_BONUS_BLOCKS)) {
            int fortuneLevel = tool.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS);
            
            // Fortune formula: random chance to increase drops by level
            for (int i = 0; i < fortuneLevel; i++) {
                if (random.nextFloat() <= 0.4f) { // 40% chance per level
                    baseAmount++;
                }
            }
        }
        
        return baseAmount;
    }
}