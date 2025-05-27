package id.rnggagib.listeners;

import id.rnggagib.HarvestMoonMC;
import id.rnggagib.models.FarmingRegion;
import id.rnggagib.utils.CropUtils;
import id.rnggagib.utils.MessageUtils;
import id.rnggagib.utils.QualityUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;

import java.util.*;

public class FarmingListener implements Listener {
    private final HarvestMoonMC plugin;
    private final Random random = new Random();
    private final Set<Block> processingBlocks = new HashSet<>();

    public FarmingListener(HarvestMoonMC plugin) {
        this.plugin = plugin;
    }

    // Ubah priority dari HIGH menjadi HIGHEST agar dijalankan sebelum WorldGuard
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        // Check if player is in creative mode or has admin permission
        if (player.getGameMode() == GameMode.CREATIVE || player.hasPermission("harvestmoonmc.admin.bypass")) {
            return; // Allow creative mode and admins to break anything
        }

        // Get the farming region at this location
        FarmingRegion region = plugin.getRegionManager().getRegionAt(block.getLocation());
        
        // If it's wheat in a farming region, handle specially
        if (block.getType() == Material.WHEAT && region != null) {
            // Penting: Set event.setCancelled(false) untuk override
            // proteksi WorldGuard yang sudah meng-cancel event
            event.setCancelled(false);
            
            // Lalu cancel kita sendiri agar bisa mengatur custom behavior
            event.setCancelled(true);
            
            // Continue with the normal farming code
            // Prevent duplicate processing
            if (processingBlocks.contains(block)) {
                return;
            }
            
            processingBlocks.add(block);
            
            // Get the crop's growth stage
            Ageable ageable = (Ageable) block.getBlockData();
            boolean isMature = ageable.getAge() == ageable.getMaximumAge();

            if (isMature) {
                // Create custom crop drop
                Material cropType = block.getType();
                ItemStack heldItem = player.getInventory().getItemInMainHand();
                
                // Calculate crop quality and create custom drop
                Map<String, Object> qualityInfo = QualityUtils.calculateQuality(plugin, cropType, heldItem, player);
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
                
                // Award XP to the player based on crop and quality
                int xpAmount = plugin.getSkillManager().getCropXp(cropType, qualityTier);
                boolean leveledUp = plugin.getSkillManager().awardXp(player, xpAmount);

                // Handle XP notification based on config
                String notificationType = plugin.getConfig().getString("skills.xp_notification", "actionbar");
                if (notificationType.equalsIgnoreCase("chat")) {
                    // Send as chat message (original method)
                    player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                            plugin.getConfig().getString("messages.xp_gained").replace("%amount%", String.valueOf(xpAmount))));
                } else if (notificationType.equalsIgnoreCase("actionbar")) {
                    // Send as action bar (less intrusive)
                    player.spigot().sendMessage(
                        net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        net.md_5.bungee.api.chat.TextComponent.fromLegacyText(MessageUtils.colorize("&a+" + xpAmount + " Farming XP"))
                    );
                } else if (!notificationType.equalsIgnoreCase("none")) {
                    // Default to actionbar if invalid option
                    player.spigot().sendMessage(
                        net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        net.md_5.bungee.api.chat.TextComponent.fromLegacyText(MessageUtils.colorize("&a+" + xpAmount + " Farming XP"))
                    );
                }

                // Reset crop age to 0 (seedling)
                Material cropMaterial = block.getType();
                Ageable newAgeable = (Ageable) cropMaterial.createBlockData();
                newAgeable.setAge(0);
                block.setBlockData(newAgeable);

                // Start growth animation
                startGrowthAnimation(block);
            } else {
                // If crop is not mature, just cancel and inform player
                player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                        "&cTanaman belum siap dipanen!"));
                processingBlocks.remove(block);
            }
            return; // Important: exit method after handling wheat
        }
        
        // At this point, it's either:
        // 1. Not wheat
        // 2. Wheat but not in a farming region
        
        // Only allow wheat blocks in farming regions
        if (region != null && CropUtils.isSupportedCrop(block.getType())) {
            // Continue with existing code for supported crops in farming regions
            // [keep all the current farming region handling]
        } else {
            // Cancel any other block breaking attempt
            event.setCancelled(true);
            
            // Only show message if they tried to break something (not just clicking)
            if (!player.isSneaking() && player.getGameMode() != GameMode.SPECTATOR) {
                player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                        "&cAnda hanya dapat memanen tanaman gandum di region pertanian."));
            }
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