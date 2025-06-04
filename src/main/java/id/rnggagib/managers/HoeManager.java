package id.rnggagib.managers;

import id.rnggagib.HarvestMoonMC;
import id.rnggagib.models.CustomHoe;
import id.rnggagib.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import id.rnggagib.models.FarmingRegion; // Add this
import id.rnggagib.utils.CropUtils;     // Add this
import id.rnggagib.utils.QualityUtils;  // Add this
import id.rnggagib.listeners.FarmingListener; // Add this

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class HoeManager {
    private final HarvestMoonMC plugin;
    private final Map<String, CustomHoe> hoes = new HashMap<>();
    private File hoesFile;
    private FileConfiguration hoesConfig;
    
    // NamespacedKeys for persistent data
    private final NamespacedKey hoeIdKey;
    private final NamespacedKey hoeUsesKey;
    private final Map<UUID, Map<String, Long>> abilityCooldowns = new HashMap<>(); // PlayerUUID -> HoeID -> EndTimeMillis

    public HoeManager(HarvestMoonMC plugin) {
        this.plugin = plugin;
        this.hoeIdKey = new NamespacedKey(plugin, "hoe_id");
        this.hoeUsesKey = new NamespacedKey(plugin, "hoe_uses");
        setupHoesFile();
        loadHoes();
    }
    
    /**
     * Sets up the hoes.yml file
     */
    private void setupHoesFile() {
        hoesFile = new File(plugin.getDataFolder(), "hoes.yml");
        if (!hoesFile.exists()) {
            hoesFile.getParentFile().mkdirs();
            plugin.saveResource("hoes.yml", false);
        }
        
        hoesConfig = YamlConfiguration.loadConfiguration(hoesFile);
        
        // Check if the file is empty or invalid, load defaults if needed
        if (!hoesConfig.contains("hoes")) {
            // Load default config from jar
            InputStream defaultStream = plugin.getResource("hoes.yml");
            if (defaultStream != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defaultStream));
                hoesConfig.setDefaults(defaultConfig);
                hoesConfig.options().copyDefaults(true);
                try {
                    hoesConfig.save(hoesFile);
                } catch (IOException e) {
                    plugin.getLogger().severe("Could not save default hoes.yml: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Loads all custom hoes from config
     */
    private void loadHoes() {
        hoes.clear();
        ConfigurationSection hoesSection = hoesConfig.getConfigurationSection("hoes");
        if (hoesSection == null) {
            plugin.getLogger().warning("No hoes section found in hoes.yml");
            return;
        }

        for (String hoeId : hoesSection.getKeys(false)) {
            ConfigurationSection hoeSection = hoesSection.getConfigurationSection(hoeId);
            if (hoeSection == null) continue;

            CustomHoe hoe = new CustomHoe(hoeId);
            hoe.setName(hoeSection.getString("name", "&cUnnamed Hoe"));
            try {
                hoe.setMaterial(Material.valueOf(hoeSection.getString("material", "IRON_HOE").toUpperCase()));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material for hoe " + hoeId + ". Defaulting to IRON_HOE.");
                hoe.setMaterial(Material.IRON_HOE);
            }
            hoe.setCustomModelData(hoeSection.getInt("custom_model_data", 0));
            hoe.setLore(hoeSection.getStringList("lore"));
            hoe.setHarvestMultiplier(hoeSection.getInt("harvest_multiplier", 1));
            hoe.setGlow(hoeSection.getBoolean("glow", false));
            hoe.setDurability(hoeSection.getInt("durability", 100)); // Load durability, 0 or -1 for infinite

            ConfigurationSection areaSection = hoeSection.getConfigurationSection("area");
            if (areaSection != null) {
                hoe.setAreaWidth(areaSection.getInt("width", 1));
                hoe.setAreaHeight(areaSection.getInt("height", 1));
            }
            hoe.setAutoReplant(hoeSection.getBoolean("auto_replant", false));

            ConfigurationSection upgradeSection = hoeSection.getConfigurationSection("upgrade");
            if (upgradeSection != null) {
                hoe.setNextTier(upgradeSection.getString("next_tier"));
                hoe.setUpgradeCost(upgradeSection.getDouble("cost", 0));
                Map<Material, Integer> materials = new HashMap<>();
                ConfigurationSection materialSection = upgradeSection.getConfigurationSection("materials");
                if (materialSection != null) {
                    for (String matKey : materialSection.getKeys(false)) {
                        try {
                            materials.put(Material.valueOf(matKey.toUpperCase()), materialSection.getInt(matKey));
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Invalid upgrade material " + matKey + " for hoe " + hoeId);
                        }
                    }
                }
                hoe.setUpgradeMaterials(materials);
            }

            ConfigurationSection saSection = hoeSection.getConfigurationSection("special_ability");
            if (saSection != null) {
                hoe.setSpecialAbilityEnabled(saSection.getBoolean("enabled", false));
                hoe.setSpecialAbilityType(saSection.getString("type", "divine_harvest")); // Load type
                hoe.setSpecialAbilityRadius(saSection.getInt("radius", 0));
                hoe.setSpecialAbilityCooldown(saSection.getInt("cooldown", 60));
                hoe.setSpecialAbilityCost(saSection.getDouble("cost", 0.0));
                hoe.setSpecialAbilityLoreText(saSection.getString("lore_text", "")); // Load lore text
                hoe.setSpecialAbilityParticleType(saSection.getString("particle_type", plugin.getConfig().getString("farming.default_special_ability.particle_type", "SMOKE_NORMAL")));
                hoe.setSpecialAbilitySoundActivate(saSection.getString("sound_activate", plugin.getConfig().getString("farming.default_special_ability.sound_activate", "ENTITY_PLAYER_ATTACK_SWEEP")));
                hoe.setSpecialAbilitySoundImpact(saSection.getString("sound_impact", plugin.getConfig().getString("farming.default_special_ability.sound_impact", "BLOCK_GRASS_BREAK")));
            }

            hoes.put(hoeId, hoe);
            // plugin.getLogger().info("Loaded custom hoe: " + hoeId); // Keep if desired
        }
    }
    
    /**
     * Gets a custom hoe by its ID
     * @param id The hoe ID
     * @return The CustomHoe, or null if not found
     */
    public CustomHoe getHoeById(String id) {
        return hoes.get(id);
    }
    
    /**
     * Checks if an ItemStack is a custom hoe
     * @param item The item to check
     * @return The custom hoe or null if not a custom hoe
     */
    public CustomHoe getHoeFromItemStack(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        
        if (container.has(hoeIdKey, PersistentDataType.STRING)) {
            String hoeId = container.get(hoeIdKey, PersistentDataType.STRING);
            CustomHoe hoe = getHoeById(hoeId);
            
            // Load uses count
            if (hoe != null && container.has(hoeUsesKey, PersistentDataType.INTEGER)) {
                int uses = container.get(hoeUsesKey, PersistentDataType.INTEGER);
                hoe.setUses(uses);
            }
            
            return hoe;
        }
        
        return null;
    }
    
    /**
     * Creates a new custom hoe ItemStack
     * @param hoeId The hoe ID
     * @return The created ItemStack, or null if the hoe ID is invalid
     */
    public ItemStack createHoe(String hoeId) {
        CustomHoe hoe = getHoeById(hoeId);
        if (hoe == null) return null;

        return hoe.createItemStack(this.plugin, hoeIdKey, hoeUsesKey); // Pass this.plugin
    }
    
    /**
     * Updates a hoe's durability
     * @param player The player using the hoe
     * @param item The hoe item to update
     * @return The updated item, or null if broken
     */
    public ItemStack useDurability(Player player, ItemStack item) {
        CustomHoe hoe = getHoeFromItemStack(item);
        if (hoe == null) return item; // Not a custom hoe

        if (hoe.getDurability() <= 0) { // Infinite durability
             // Optionally update lore if uses are tracked for display, but item doesn't break
            return hoe.updateDurability(item, hoeUsesKey); // This will refresh lore if needed
        }

        // For finite durability (though we made them infinite, this path won't be hit with current hoes.yml)
        int currentUses = item.getItemMeta().getPersistentDataContainer().getOrDefault(hoeUsesKey, PersistentDataType.INTEGER, 0);
        currentUses++;
        
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(hoeUsesKey, PersistentDataType.INTEGER, currentUses);
        item.setItemMeta(meta); // Apply meta before updating lore via CustomHoe

        // Update lore and check for breakage
        item = hoe.updateDurability(item, hoeUsesKey); // CustomHoe.updateDurability will use the new uses count

        if (hoe.isBroken()) { // CustomHoe.isBroken will return false if durability <= 0
            // This part should not be reached if all hoes are infinite
            player.getInventory().setItemInMainHand(null);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + "&cYour farming hoe broke!"));
            return null;
        }
        return item;
    }
    
    /**
     * Gets all blocks in the area of effect of a hoe
     * @param centerBlock The center block
     * @param hoe The custom hoe
     * @param player The player using the hoe
     * @return List of blocks in the area
     */
    public List<Block> getAreaBlocks(Block centerBlock, CustomHoe hoe, Player player) {
        List<Block> blocks = new ArrayList<>();
        
        // If it's a 1x1 area, just return the center block
        if (hoe.getAreaWidth() <= 1 && hoe.getAreaHeight() <= 1) {
            blocks.add(centerBlock);
            return blocks;
        }
        
        // Get player facing direction
        BlockFace direction = getPlayerFacing(player);
        
        // Calculate the area based on facing direction
        int width = hoe.getAreaWidth();
        int height = hoe.getAreaHeight();
        
        // Width is always perpendicular to facing direction
        // Height is always along facing direction
        
        // For odd dimensions, player is in the center
        // For even dimensions, player is offset by 1 in the negative direction
        
        int widthOffset = width / 2;
        int heightOffset = height / 2;
        
        for (int y = -heightOffset; y <= height - heightOffset - 1; y++) {
            for (int x = -widthOffset; x <= width - widthOffset - 1; x++) {
                Block block;
                
                switch (direction) {
                    case NORTH:
                        block = centerBlock.getRelative(x, 0, -y);
                        break;
                    case SOUTH:
                        block = centerBlock.getRelative(-x, 0, y);
                        break;
                    case EAST:
                        block = centerBlock.getRelative(y, 0, x);
                        break;
                    case WEST:
                        block = centerBlock.getRelative(-y, 0, -x);
                        break;
                    default:
                        continue;
                }
                
                blocks.add(block);
            }
        }
        
        return blocks;
    }
    
    /**
     * Gets the player's facing direction
     * @param player The player
     * @return The BlockFace representing the direction
     */
    private BlockFace getPlayerFacing(Player player) {
        float yaw = player.getLocation().getYaw();
        if (yaw < 0) {
            yaw += 360;
        }
        
        if (yaw >= 315 || yaw < 45) {
            return BlockFace.SOUTH;
        } else if (yaw < 135) {
            return BlockFace.WEST;
        } else if (yaw < 225) {
            return BlockFace.NORTH;
        } else {
            return BlockFace.EAST;
        }
    }
    
    /**
     * Attempts to upgrade a hoe
     * @param currentHoe The current hoe ItemStack
     * @param player The player upgrading the hoe
     * @return true if upgrade was successful, false otherwise
     */
    public boolean upgradeHoe(ItemStack currentHoe, Player player) {
        // Get the current hoe data
        CustomHoe hoe = getHoeFromItemStack(currentHoe);
        if (hoe == null) return false;
        
        // Check if there's a next tier
        String nextTierId = hoe.getNextTier();
        if (nextTierId == null || nextTierId.isEmpty()) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                    "&cThis hoe cannot be upgraded further!"));
            return false;
        }
        
        // Get the next tier hoe
        CustomHoe nextTier = getHoeById(nextTierId);
        if (nextTier == null) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                    "&cUpgrade not available!"));
            return false;
        }
        
        // Check if player has enough money
        double cost = hoe.getUpgradeCost();
        if (cost > 0) {
            if (!plugin.getEconomyManager().hasMoney(player, cost)) {
                player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                        "&cYou need " + plugin.getEconomyManager().format(cost) + " to upgrade this hoe!"));
                return false;
            }
        }
        
        // Check if player has the required materials
        Map<Material, Integer> requiredMaterials = hoe.getUpgradeMaterials();
        Map<Integer, ItemStack> toReturn = new HashMap<>();
        
        // Check if player has all required materials
        for (Map.Entry<Material, Integer> entry : requiredMaterials.entrySet()) {
            Material material = entry.getKey();
            int requiredAmount = entry.getValue();
            
            // Count how many of this material the player has
            int playerAmount = 0;
            HashMap<Integer, ? extends ItemStack> items = player.getInventory().all(material);
            
            for (ItemStack item : items.values()) {
                playerAmount += item.getAmount();
                
                if (playerAmount >= requiredAmount) {
                    break;
                }
            }
            
            if (playerAmount < requiredAmount) {
                player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                        "&cYou need " + requiredAmount + "x " + material.toString() + " to upgrade this hoe!"));
                return false;
            }
        }
        
        // At this point, the player has everything needed for the upgrade
        
        // Take the money
        if (cost > 0) {
            plugin.getEconomyManager().withdrawMoney(player, cost);
        }
        
        // Take the materials
        for (Map.Entry<Material, Integer> entry : requiredMaterials.entrySet()) {
            Material material = entry.getKey();
            int requiredAmount = entry.getValue();
            
            // Remove the items
            int remainingToRemove = requiredAmount;
            for (Map.Entry<Integer, ? extends ItemStack> invEntry : player.getInventory().all(material).entrySet()) {
                int slot = invEntry.getKey();
                ItemStack item = invEntry.getValue();
                
                if (item.getAmount() <= remainingToRemove) {
                    remainingToRemove -= item.getAmount();
                    player.getInventory().setItem(slot, null);
                } else {
                    item.setAmount(item.getAmount() - remainingToRemove);
                    remainingToRemove = 0;
                }
                
                if (remainingToRemove <= 0) {
                    break;
                }
            }
        }
        
        // Create the new hoe
        ItemStack upgradedHoe = createHoe(nextTierId);
        
        // Replace the old hoe with the new one
        player.getInventory().setItemInMainHand(upgradedHoe);
        
        // Send a success message
        player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                "&aYour hoe has been upgraded to " + nextTier.getName() + "&a!"));
        
        return true;
    }
    
    /**
     * Reloads the hoes configuration
     */
    public void reloadConfig() {
        hoesConfig = YamlConfiguration.loadConfiguration(hoesFile);
        loadHoes();
    }
    
    /**
     * Gets all available hoe IDs
     * @return Set of hoe IDs
     */
    public Set<String> getAllHoeIds() {
        return hoes.keySet();
    }
    
    public void activateSpecialAbility(Player player, CustomHoe hoe, ItemStack hoeItem) {
        if (!hoe.isSpecialAbilityEnabled() || hoe.getSpecialAbilityRadius() <= 0) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.special_ability_not_available")));
            return;
        }

        if (!player.hasPermission("dragonfarm.hoe.specialability")) { // General permission
             player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.special_ability_no_permission")));
            return;
        }
        
        FarmingRegion currentRegion = plugin.getRegionManager().getRegionAt(player.getLocation());
        if (currentRegion == null) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") +
                    plugin.getConfig().getString("messages.custom_hoe_outside_farm_region"))); // Use the more general message
            return;
        }

        long currentTime = System.currentTimeMillis();
        Map<String, Long> playerCooldowns = abilityCooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        long cooldownEndTime = playerCooldowns.getOrDefault(hoe.getId(), 0L);

        if (currentTime < cooldownEndTime) {
            long timeLeft = (cooldownEndTime - currentTime) / 1000;
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") +
                    plugin.getConfig().getString("messages.special_ability_cooldown").replace("%time%", String.valueOf(timeLeft))));
            return;
        }

        if (hoe.getSpecialAbilityCost() > 0) {
            if (!plugin.getEconomyManager().hasEnoughMoney(player, hoe.getSpecialAbilityCost())) {
                player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") +
                        plugin.getConfig().getString("messages.special_ability_no_money")
                                .replace("%cost%", plugin.getEconomyManager().format(hoe.getSpecialAbilityCost()))));
                return;
            }
            plugin.getEconomyManager().withdrawMoney(player, hoe.getSpecialAbilityCost());
        }

        // Perform Visuals & Action based on type
        if ("bomb".equalsIgnoreCase(hoe.getSpecialAbilityType())) {
            performBombAbilityVisualsAndAction(player, hoe, hoeItem);
        } else { // Default to divine_harvest (apple)
            performDivineHarvestVisualsAndAction(player, hoe, hoeItem);
        }


        if (hoe.getSpecialAbilityCooldown() > 0) {
            playerCooldowns.put(hoe.getId(), currentTime + (hoe.getSpecialAbilityCooldown() * 1000L));
        }
        
        player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + plugin.getConfig().getString("messages.special_ability_activated")));
        try {
            Sound activationSound = Sound.valueOf(hoe.getSpecialAbilitySoundActivate().toUpperCase());
            player.playSound(player.getLocation(), activationSound, 1f, 1f);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid activation sound for hoe " + hoe.getId() + ": " + hoe.getSpecialAbilitySoundActivate());
        }
    }

    private void performBombAbilityVisualsAndAction(Player player, CustomHoe hoe, ItemStack hoeItem) {
        Location playerLoc = player.getLocation();
        double fallHeight = plugin.getConfig().getDouble("farming.default_special_ability.apple_fall_height", 2.0); // Can be adjusted for bomb
        double fallSpeed = plugin.getConfig().getDouble("farming.default_special_ability.apple_fall_speed", -0.3); // Can be adjusted

        Location tntSpawnLoc = playerLoc.clone().add(0, fallHeight, 0);
        Item tntEntity = player.getWorld().dropItem(tntSpawnLoc, new ItemStack(Material.TNT)); // Visual TNT
        tntEntity.setVelocity(new Vector(0, fallSpeed, 0));
        tntEntity.setPickupDelay(Integer.MAX_VALUE);
        tntEntity.setGravity(true);
        tntEntity.setInvulnerable(true); // So it doesn't get destroyed by other means

        Particle determinedParticleEffect;
        try {
            determinedParticleEffect = Particle.valueOf(hoe.getSpecialAbilityParticleType().toUpperCase()); // Use hoe's particle for trail
        } catch (IllegalArgumentException e) {
            determinedParticleEffect = Particle.SMOKE_NORMAL;
        }
        final Particle trailParticleEffect = determinedParticleEffect;

        new BukkitRunnable() {
            int ticksLived = 0;
            @Override
            public void run() {
                if (!tntEntity.isValid() || tntEntity.isOnGround() || ticksLived > 80) { // Max 4 seconds or on ground
                    Location impactLocation = tntEntity.isValid() ? tntEntity.getLocation() : player.getLocation();
                    if (tntEntity.isValid()) tntEntity.remove();

                    // Explosion visual
                    player.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, impactLocation, 1, 0, 0, 0, 0);
                    player.getWorld().spawnParticle(Particle.LAVA, impactLocation, 30, 0.5, 0.5, 0.5, 0.01); // Some fiery particles
                    try {
                        // Use a more explosive sound, or keep generic impact sound
                        Sound impactSound = Sound.ENTITY_GENERIC_EXPLODE; // Changed for bomb
                        player.playSound(impactLocation, impactSound, 1f, 1.0f);
                    } catch (IllegalArgumentException e) {
                         plugin.getLogger().warning("Invalid impact sound for BOMB: " + e.getMessage());
                    }
                    
                    performCircularHarvest(player, hoe, hoeItem, impactLocation);
                    this.cancel();
                    return;
                }
                if (ticksLived % 3 == 0) {
                    player.getWorld().spawnParticle(trailParticleEffect, tntEntity.getLocation(), 3, 0.1, 0.1, 0.1, 0.01);
                }
                ticksLived++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
    
    private void performDivineHarvestVisualsAndAction(Player player, CustomHoe hoe, ItemStack hoeItem) {
        Location playerLoc = player.getLocation();
        double fallHeight = plugin.getConfig().getDouble("farming.default_special_ability.apple_fall_height", 3.0);
        double fallSpeed = plugin.getConfig().getDouble("farming.default_special_ability.apple_fall_speed", -0.4);

        Location appleSpawnLoc = playerLoc.clone().add(0, fallHeight, 0);
        Item appleEntity = player.getWorld().dropItem(appleSpawnLoc, new ItemStack(Material.APPLE));
        appleEntity.setVelocity(new Vector(0, fallSpeed, 0));
        appleEntity.setPickupDelay(Integer.MAX_VALUE);
        appleEntity.setGravity(true);

        Particle determinedParticleEffect; // Temporary variable
        try {
            determinedParticleEffect = Particle.valueOf(hoe.getSpecialAbilityParticleType().toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid particle type for hoe " + hoe.getId() + ": " + hoe.getSpecialAbilityParticleType() + ". Defaulting to SMOKE_NORMAL.");
            determinedParticleEffect = Particle.SMOKE_NORMAL;
        }

        final Particle finalParticleEffect = determinedParticleEffect; // Explicitly final variable

        new BukkitRunnable() {
            int ticksLived = 0;
            @Override
            public void run() {
                if (!appleEntity.isValid() || appleEntity.isOnGround() || ticksLived > 80) { // Max 4 seconds or on ground
                    Location impactLocation = appleEntity.isValid() ? appleEntity.getLocation() : player.getLocation(); // Fallback to player loc
                    if (appleEntity.isValid()) appleEntity.remove();

                    player.getWorld().spawnParticle(finalParticleEffect, impactLocation, 50, 0.5, 0.2, 0.5, 0.05); // Use the final variable
                    try {
                        Sound impactSound = Sound.valueOf(hoe.getSpecialAbilitySoundImpact().toUpperCase());
                        player.playSound(impactLocation, impactSound, 1f, 1.2f);
                    } catch (IllegalArgumentException e) {
                         plugin.getLogger().warning("Invalid impact sound for hoe " + hoe.getId() + ": " + hoe.getSpecialAbilitySoundImpact());
                    }
                    
                    performCircularHarvest(player, hoe, hoeItem, impactLocation);
                    this.cancel();
                    return;
                }
                if (ticksLived % 3 == 0) { // Spawn trail particles less frequently
                    player.getWorld().spawnParticle(finalParticleEffect, appleEntity.getLocation(), 3, 0.1, 0.1, 0.1, 0.01); // Use the final variable
                }
                ticksLived++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void performCircularHarvest(Player player, CustomHoe currentHoe, ItemStack hoeItem, Location center) {
        int radius = currentHoe.getSpecialAbilityRadius();
        FarmingListener farmingListener = plugin.getFarmingListener(); // Get listener instance
        if (farmingListener == null) {
            plugin.getLogger().severe("FarmingListener instance is null. Cannot perform special harvest.");
            return;
        }


        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z <= radius * radius) {
                    Block targetBlock = center.getWorld().getBlockAt(center.getBlockX() + x, center.getBlockY(), center.getBlockZ() + z);
                    
                    // Try to find the actual crop block, could be at y, y+1, or y-1 from impact point
                    Block cropBlock = null;
                    for (int yOffset = -1; yOffset <= 1; yOffset++) {
                        Block checkBlock = center.getWorld().getBlockAt(targetBlock.getX(), targetBlock.getY() + yOffset, targetBlock.getZ());
                        if (CropUtils.isSupportedCrop(checkBlock.getType()) && checkBlock.getRelative(BlockFace.DOWN).getType() == Material.FARMLAND) {
                            cropBlock = checkBlock;
                            break;
                        }
                    }

                    if (cropBlock != null && cropBlock.getBlockData() instanceof Ageable) {
                        Ageable ageable = (Ageable) cropBlock.getBlockData();
                        if (ageable.getAge() == ageable.getMaximumAge()) {
                            // Check region again for the specific cropBlock
                            FarmingRegion cropRegion = plugin.getRegionManager().getRegionAt(cropBlock.getLocation());
                            if (cropRegion == null) continue; // Skip if this specific crop is not in a farming region

                            if (plugin.getHarvestLimitManager().canHarvest(player)) {
                                Material originalCropType = cropBlock.getType();

                                // Simplified harvest logic (no Zonk for ability for now)
                                Map<String, Object> qualityInfo = QualityUtils.calculateQuality(plugin, originalCropType, hoeItem, player);
                                ItemStack customDrop = farmingListener.createCustomCrop( // Use public method from FarmingListener
                                    originalCropType,
                                    (String) qualityInfo.get("tier"),
                                    (String) qualityInfo.get("displayName"),
                                    (String) qualityInfo.get("colorCode"),
                                    (Double) qualityInfo.get("price")
                                );
                                int dropAmount = farmingListener.calculateDropAmount(hoeItem, originalCropType); // Use public method
                                if (currentHoe != null) { // Apply hoe's own multiplier
                                    dropAmount *= currentHoe.getHarvestMultiplier();
                                }
                                customDrop.setAmount(dropAmount);

                                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(customDrop);
                                for (ItemStack itemLeft : leftover.values()) {
                                    player.getWorld().dropItemNaturally(cropBlock.getLocation().add(0.5,0.5,0.5), itemLeft);
                                }

                                plugin.getHarvestLimitManager().incrementHarvestCount(player, dropAmount);
                                double xpMultiplier = QualityUtils.getXpMultiplier((String) qualityInfo.get("tier"));
                                int xpGained = (int) Math.round(plugin.getSkillManager().getBaseXp(originalCropType) * xpMultiplier);
                                plugin.getSkillManager().addXp(player, xpGained);

                                // Replant
                                cropBlock.setType(originalCropType);
                                Ageable newCropData = (Ageable) cropBlock.getBlockData();
                                newCropData.setAge(0);
                                cropBlock.setBlockData(newCropData);
                                farmingListener.startGrowthAnimation(cropBlock, originalCropType); // Use public method
                            }
                        }
                    }
                }
            }
        }
    }
}