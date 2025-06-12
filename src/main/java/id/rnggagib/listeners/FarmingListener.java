package id.rnggagib.listeners;

import id.rnggagib.HarvestMoonMC;
import id.rnggagib.models.FarmingRegion;
import id.rnggagib.models.CustomHoe;
import id.rnggagib.utils.CropUtils;
import id.rnggagib.utils.MessageUtils;
import id.rnggagib.utils.QualityUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
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

    // Method startGrowthAnimation tetap sama
    public void startGrowthAnimation(Block block, Material cropMaterial) {
        if (!plugin.getConfig().getBoolean("farming.enable_animations", true)) {
            // Jika animasi dinonaktifkan, langsung set ke usia maksimal
            if (CropUtils.isSupportedCrop(cropMaterial) && block.getBlockData() instanceof Ageable) {
                Ageable ageable = (Ageable) block.getBlockData();
                if (block.getType() == cropMaterial) { // Pastikan block masih crop yang benar
                    ageable.setAge(ageable.getMaximumAge());
                    block.setBlockData(ageable);
                    block.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY,
                        block.getLocation().add(0.5, 0.5, 0.5), 5, 0.2, 0.2, 0.2, 0.01);
                }
            }
            return;
        }

        if (!CropUtils.isSupportedCrop(cropMaterial) || !(block.getBlockData() instanceof Ageable)) {
            return;
        }

        // Pastikan block adalah cropMaterial yang benar sebelum memulai
        if (block.getType() != cropMaterial) {
            block.setType(cropMaterial); // Set ke tipe crop yang benar jika belum
            Ageable ageableData = (Ageable) block.getBlockData();
            ageableData.setAge(0); // Mulai dari usia 0
            block.setBlockData(ageableData);
        } else {
            // Jika sudah cropMaterial, pastikan usianya 0
            Ageable ageableData = (Ageable) block.getBlockData();
            if (ageableData.getAge() != 0) {
                ageableData.setAge(0);
                block.setBlockData(ageableData);
            }
        }

        Ageable initialAgeable = (Ageable) block.getBlockData();
        final int maxAge = initialAgeable.getMaximumAge();
        final int animationDelay = plugin.getConfig().getInt("farming.animation_delay_ticks", 10); // Default 10 tick

        new BukkitRunnable() {
            int currentAge = 0; // Animasi dimulai dari usia 0

            @Override
            public void run() {
                if (block.getType() != cropMaterial || !(block.getBlockData() instanceof Ageable)) {
                    this.cancel(); // Block berubah, hentikan animasi
                    return;
                }

                Ageable currentStageAgeable = (Ageable) block.getBlockData();

                if (currentAge < maxAge) {
                    currentAge++;
                    currentStageAgeable.setAge(currentAge);
                    block.setBlockData(currentStageAgeable);
                    // Optional: partikel untuk setiap tahap
                    // block.getWorld().spawnParticle(Particle.COMPOSTER, block.getLocation().add(0.5, 0.7, 0.5), 1, 0.1, 0.1, 0.1, 0);
                } else {
                    // Mencapai usia maksimal
                    block.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY,
                        block.getLocation().add(0.5, 0.5, 0.5), 10, 0.3, 0.3, 0.3, 0.01);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, animationDelay, animationDelay);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.CREATIVE || player.hasPermission("dragfarm.admin.bypass")) {
            return;
        }        ItemStack heldItemForCheck = player.getInventory().getItemInMainHand();
        CustomHoe customHoeForCheck = plugin.getHoeManager().getHoeFromItemStack(heldItemForCheck);
        FarmingRegion region = plugin.getRegionManager().getRegionAt(block.getLocation());
        
        // Check if the block is farmable soil (with crop above)
        boolean isFarmableArea = (block.getType() == Material.FARMLAND) || 
                                (block.getRelative(0, -1, 0).getType() == Material.FARMLAND) ||
                                CropUtils.isSupportedCrop(block.getType());
        
        // For custom hoes, allow use in farm-like areas even without defined regions
        if (customHoeForCheck != null) {
            if (!isFarmableArea && region == null) { // Custom hoe used OUTSIDE a farming area
                player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") +
                        plugin.getConfig().getString("messages.custom_hoe_outside_farm_region")));
                event.setCancelled(true);
                return;
            }
            // If we have a custom hoe and we're in a farmable area, treat it as if we're in a region
            if (isFarmableArea && region == null) {
                // Continue with farming mechanics as if we're in a region
            } else if (region == null) {
                // Not in a region or farmable area with custom hoe
                return;
            }
        } else if (region == null) { // Not in a farming region, and not a custom hoe
            return; // Allow default behavior or other plugins
        }
        
        // From here, we are INSIDE a farming region
        event.setCancelled(true); // Default cancel for farming regions
        
        Material originalCropType = block.getType();

        if (CropUtils.isSupportedCrop(originalCropType) && block.getBlockData() instanceof Ageable) {
            // event.setCancelled(false); // No longer needed as we handle drops directly
            // event.setCancelled(true);  // Already set above

            if (processingBlocks.contains(block)) {
                return;
            }
            processingBlocks.add(block);

            Ageable ageable = (Ageable) block.getBlockData();
            boolean isMature = ageable.getAge() == ageable.getMaximumAge();

            ItemStack heldItem = player.getInventory().getItemInMainHand(); // Re-get in case it changed, though unlikely here
            if (!isValidFarmingTool(heldItem)) { // This now checks for custom hoe
                Material itemType = heldItem.getType();
                if (itemType == Material.WOODEN_HOE || itemType == Material.STONE_HOE ||
                    itemType == Material.IRON_HOE || itemType == Material.GOLDEN_HOE ||
                    itemType == Material.DIAMOND_HOE || itemType == Material.NETHERITE_HOE) {
                    player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") +
                            plugin.getConfig().getString("messages.vanilla_hoe_in_farm_region")));
                } else {
                     player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") +
                            plugin.getConfig().getString("messages.need_hoe"))); // "need_hoe" implies custom now
                }
                processingBlocks.remove(block);
                return;
            }            if (isMature) {
                // Harvest limit check removed - unlimited harvesting enabled
                
                double zonkChancePercent = plugin.getConfig().getDouble("farming.zonk_chance_percent", 0.0);
                boolean isZonk = random.nextDouble() * 100 < zonkChancePercent;

                id.rnggagib.models.CustomHoe customHoe = plugin.getHoeManager().getHoeFromItemStack(heldItem);

                if (isZonk) {
                    // player.sendMessage(...) // ZONK MESSAGE REMOVED

                    block.setType(originalCropType);
                    Ageable newMainCropData = (Ageable) block.getBlockData();
                    newMainCropData.setAge(0);
                    block.setBlockData(newMainCropData);
                    startGrowthAnimation(block, originalCropType);

                    if (customHoe != null && (customHoe.getAreaWidth() > 1 || customHoe.getAreaHeight() > 1)) {
                        List<Block> areaBlocks = plugin.getHoeManager().getAreaBlocks(block, customHoe, player);
                        for (Block areaBlock : areaBlocks) {
                            if (areaBlock.equals(block)) continue;
                            Material areaOriginalCropType = areaBlock.getType();
                            if (CropUtils.isSupportedCrop(areaOriginalCropType) && areaBlock.getBlockData() instanceof Ageable) {
                                Ageable areaAgeable = (Ageable) areaBlock.getBlockData();
                                if (areaAgeable.getAge() == areaAgeable.getMaximumAge()) {
                                    if (processingBlocks.contains(areaBlock)) continue;
                                    processingBlocks.add(areaBlock);

                                    areaBlock.setType(areaOriginalCropType);
                                    Ageable newAreaCropData = (Ageable) areaBlock.getBlockData();
                                    newAreaCropData.setAge(0);
                                    areaBlock.setBlockData(newAreaCropData);
                                    startGrowthAnimation(areaBlock, areaOriginalCropType);
                                    
                                    processingBlocks.remove(areaBlock);
                                }
                            }
                        }
                    }
                    processingBlocks.remove(block);
                    return;
                }

                // --- JIKA TIDAK ZONK, LANJUTKAN PANEN NORMAL ---
                Material cropType = block.getType(); // originalCropType bisa digunakan di sini
                
                // customHoe sudah didapatkan di atas sebelum cek zonk
                
                Map<String, Object> qualityInfo = QualityUtils.calculateQuality(plugin, cropType, heldItem, player);
                String qualityTier = (String) qualityInfo.get("tier");
                String displayName = (String) qualityInfo.get("displayName");
                String colorCode = (String) qualityInfo.get("colorCode");
                double price = (Double) qualityInfo.get("price");
                
                ItemStack customCropItem = createCustomCrop(cropType, qualityTier, displayName, colorCode, price);
                int dropAmount = calculateDropAmount(heldItem, cropType);
                
                if (customHoe != null) {
                    dropAmount *= customHoe.getHarvestMultiplier();
                    ItemStack updatedHoe = plugin.getHoeManager().useDurability(player, heldItem); // Pass player here
                    if (updatedHoe == null) {
                        // This logic is now handled within useDurability if the hoe breaks.
                        // player.getInventory().setItemInMainHand(null);
                        // player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                        // player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                        //         "&cYour farming hoe broke!"));
                        // The item in hand will be set to null by useDurability if it broke.
                    } else {
                        player.getInventory().setItemInMainHand(updatedHoe);
                    }
                }
                  customCropItem.setAmount(dropAmount);
                // Drop items naturally on ground instead of adding to inventory
                block.getWorld().dropItemNaturally(block.getLocation(), customCropItem);
                
                plugin.getHarvestLimitManager().incrementHarvestCount(player, dropAmount);
                double xpMultiplier = QualityUtils.getXpMultiplier(qualityTier);
                int xpGained = (int) Math.round(plugin.getSkillManager().getBaseXp(cropType) * xpMultiplier);
                plugin.getSkillManager().addXp(player, xpGained);
                
                // Tanam ulang blok utama (setelah panen berhasil)
                block.setType(originalCropType);
                Ageable newCropData = (Ageable) block.getBlockData();
                newCropData.setAge(0);
                block.setBlockData(newCropData);
                startGrowthAnimation(block, originalCropType);

                // Panen AoE (jika tidak zonk)
                if (customHoe != null && (customHoe.getAreaWidth() > 1 || customHoe.getAreaHeight() > 1)) {
                    List<Block> areaBlocks = plugin.getHoeManager().getAreaBlocks(block, customHoe, player);
                    for (Block areaBlock : areaBlocks) {
                        if (areaBlock.equals(block)) continue;
                        Material areaOriginalCropType = areaBlock.getType();
                        if (CropUtils.isSupportedCrop(areaOriginalCropType) && areaBlock.getBlockData() instanceof Ageable) {
                            Ageable areaAgeable = (Ageable) areaBlock.getBlockData();
                            if (areaAgeable.getAge() == areaAgeable.getMaximumAge()) {
                                if (processingBlocks.contains(areaBlock)) continue;
                                processingBlocks.add(areaBlock);

                                // Logika panen untuk blok AoE
                                Map<String, Object> areaQualityInfo = QualityUtils.calculateQuality(plugin, areaOriginalCropType, heldItem, player);
                                ItemStack areaCropItem = createCustomCrop(
                                        areaOriginalCropType, 
                                        (String) areaQualityInfo.get("tier"),
                                        (String) areaQualityInfo.get("displayName"),
                                        (String) areaQualityInfo.get("colorCode"),
                                        (Double) areaQualityInfo.get("price")
                                );
                                int areaDropAmount = calculateDropAmount(heldItem, areaOriginalCropType);
                                if (customHoe != null) { // Perlu cek lagi karena bisa jadi hoe utama pecah
                                     areaDropAmount *= customHoe.getHarvestMultiplier();
                                }                                areaCropItem.setAmount(areaDropAmount);
                                // Drop AoE items naturally on ground instead of adding to inventory
                                areaBlock.getWorld().dropItemNaturally(areaBlock.getLocation(), areaCropItem);
                                plugin.getHarvestLimitManager().incrementHarvestCount(player, areaDropAmount);
                                double areaXpMultiplier = QualityUtils.getXpMultiplier((String) areaQualityInfo.get("tier"));
                                int areaXpGained = (int) Math.round(plugin.getSkillManager().getBaseXp(areaOriginalCropType) * areaXpMultiplier);
                                plugin.getSkillManager().addXp(player, areaXpGained);
                                
                                // Tanam ulang blok AoE
                                areaBlock.setType(areaOriginalCropType);
                                Ageable newAreaCropData = (Ageable) areaBlock.getBlockData();
                                newAreaCropData.setAge(0);
                                areaBlock.setBlockData(newAreaCropData);
                                startGrowthAnimation(areaBlock, areaOriginalCropType);

                                processingBlocks.remove(areaBlock);
                            }
                        }
                    }
                }
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);
                processingBlocks.remove(block); // Hapus blok utama dari processing
            } else { // Tanaman belum matang
                // player.sendMessage(...) // "NOT READY TO HARVEST" MESSAGE REMOVED
                processingBlocks.remove(block); // Still remove from processing if not mature
            }
            // Ensure processingBlocks.remove(block) is called in all paths if block was added.
            // It's generally handled within the if(isMature) branches.
            // If not mature, it's removed. If mature and zonk, removed. If mature and success, removed.
            // The return after the main if (CropUtils.isSupportedCrop...) is fine.
            return; 
        } else if (region != null) { 
             // This part is for when it's in a farming region but NOT a supported crop type.
             // No message needed here by default unless you want to tell them they can't break *this specific block*
             // event.setCancelled(true); // Already set at the start of region check
        }
        // If it falls through, event was cancelled if in region, or returned if not.
    }

    // Method createCustomCrop tetap sama
    public ItemStack createCustomCrop(Material cropType, String qualityTier, String displayName, String colorCode, double price) {
        Material dropType = CropUtils.getCropDrop(cropType);
        ItemStack customItem = new ItemStack(dropType);
        ItemMeta meta = customItem.getItemMeta();
        
        meta.setDisplayName(MessageUtils.colorize(colorCode + CropUtils.getCropName(cropType) + " " + displayName));
        
        List<String> lore = new ArrayList<>();
        lore.add(MessageUtils.colorize("&7Kualitas: " + colorCode + displayName));
        lore.add(MessageUtils.colorize("&7Harga Jual: &6" + price + " Koin"));
        meta.setLore(lore);
        
        customItem.setItemMeta(meta);
        return customItem;
    }

    // Method calculateDropAmount tetap sama
    public int calculateDropAmount(ItemStack tool, Material cropType) {
        int baseAmount = CropUtils.getBaseDropAmount(cropType);
        
        if (tool != null && tool.getEnchantments().containsKey(Enchantment.LOOT_BONUS_BLOCKS)) {
            int fortuneLevel = tool.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS);
            for (int i = 0; i < fortuneLevel; i++) {
                if (random.nextInt(100) < (33 + (fortuneLevel * 10))) { // Peluang meningkat dengan level
                    baseAmount++;
                }
            }
        }
        return baseAmount;
    }

    // Method isValidFarmingTool tetap sama
    private boolean isValidFarmingTool(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        // Only custom hoes are valid tools in farming regions now
        return plugin.getHoeManager().getHoeFromItemStack(item) != null;
    }
}