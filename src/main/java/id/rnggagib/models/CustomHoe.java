package id.rnggagib.models;

import id.rnggagib.HarvestMoonMC;
import id.rnggagib.utils.MessageUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.Particle; // Add this import
import org.bukkit.Sound;    // Add this import

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomHoe {
    private String id;
    private String name;
    private Material material;
    private int customModelData;
    private List<String> lore;
    private int harvestMultiplier;
    private boolean glow;
    private int areaWidth = 1;
    private int areaHeight = 1;
    private boolean autoReplant;
    private int durability; // Can be 0 or -1 for infinite
    private int uses = 0; // Still track uses if needed for other things

    // Upgrade info
    private String nextTier;
    private double upgradeCost;
    private Map<Material, Integer> upgradeMaterials = new HashMap<>();

    // Special Ability Fields
    private boolean specialAbilityEnabled = false;
    private int specialAbilityRadius = 0;
    private int specialAbilityCooldown = 0; // in seconds
    private double specialAbilityCost = 0.0;
    private String specialAbilityParticleType = "SMOKE_NORMAL";
    private String specialAbilitySoundActivate = "ENTITY_PLAYER_ATTACK_SWEEP";
    private String specialAbilitySoundImpact = "BLOCK_GRASS_BREAK";
    private String specialAbilityType = "divine_harvest"; // Default type
    private String specialAbilityLoreText = "";


    // Keys for persistent data
    private static final String HOE_ID_KEY = "dragfarm.hoe.id";
    private static final String HOE_USES_KEY = "dragfarm.hoe.uses";

    public CustomHoe(String id) {
        this.id = id;
    }

    // Getters and setters
    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Material getMaterial() { return material; }
    public void setMaterial(Material material) { this.material = material; }
    public int getCustomModelData() { return customModelData; }
    public void setCustomModelData(int customModelData) { this.customModelData = customModelData; }
    public List<String> getLore() { return lore; }
    public void setLore(List<String> lore) { this.lore = lore; }
    public int getHarvestMultiplier() { return harvestMultiplier; }
    public void setHarvestMultiplier(int harvestMultiplier) { this.harvestMultiplier = harvestMultiplier; }
    public boolean isGlow() { return glow; }
    public void setGlow(boolean glow) { this.glow = glow; }
    public int getAreaWidth() { return areaWidth; }
    public void setAreaWidth(int areaWidth) { this.areaWidth = areaWidth; }
    public int getAreaHeight() { return areaHeight; }
    public void setAreaHeight(int areaHeight) { this.areaHeight = areaHeight; }
    public boolean isAutoReplant() { return autoReplant; }
    public void setAutoReplant(boolean autoReplant) { this.autoReplant = autoReplant; }
    public String getNextTier() { return nextTier; }
    public void setNextTier(String nextTier) { this.nextTier = nextTier; }
    public double getUpgradeCost() { return upgradeCost; }
    public void setUpgradeCost(double upgradeCost) { this.upgradeCost = upgradeCost; }
    public Map<Material, Integer> getUpgradeMaterials() { return upgradeMaterials; }
    public void setUpgradeMaterials(Map<Material, Integer> upgradeMaterials) { this.upgradeMaterials = upgradeMaterials; }
    public int getDurability() { return durability; }
    public void setDurability(int durability) { this.durability = durability; }
    public int getUses() { return uses; }
    public void setUses(int uses) { this.uses = uses; }

    public boolean isSpecialAbilityEnabled() { return specialAbilityEnabled; }
    public void setSpecialAbilityEnabled(boolean specialAbilityEnabled) { this.specialAbilityEnabled = specialAbilityEnabled; }
    public int getSpecialAbilityRadius() { return specialAbilityRadius; }
    public void setSpecialAbilityRadius(int specialAbilityRadius) { this.specialAbilityRadius = specialAbilityRadius; }
    public int getSpecialAbilityCooldown() { return specialAbilityCooldown; }
    public void setSpecialAbilityCooldown(int specialAbilityCooldown) { this.specialAbilityCooldown = specialAbilityCooldown; }
    public double getSpecialAbilityCost() { return specialAbilityCost; }
    public void setSpecialAbilityCost(double specialAbilityCost) { this.specialAbilityCost = specialAbilityCost; }
    public String getSpecialAbilityParticleType() { return specialAbilityParticleType; }
    public void setSpecialAbilityParticleType(String particleType) { this.specialAbilityParticleType = particleType; }
    public String getSpecialAbilitySoundActivate() { return specialAbilitySoundActivate; }
    public void setSpecialAbilitySoundActivate(String sound) { this.specialAbilitySoundActivate = sound; }
    public String getSpecialAbilitySoundImpact() { return specialAbilitySoundImpact; }
    public void setSpecialAbilitySoundImpact(String sound) { this.specialAbilitySoundImpact = sound; }
    public String getSpecialAbilityType() { return specialAbilityType; }
    public void setSpecialAbilityType(String specialAbilityType) { this.specialAbilityType = specialAbilityType; }
    public String getSpecialAbilityLoreText() { return specialAbilityLoreText; }
    public void setSpecialAbilityLoreText(String specialAbilityLoreText) { this.specialAbilityLoreText = specialAbilityLoreText; }


    /**
     * Creates an ItemStack representing this custom hoe
     * @param plugin The plugin instance
     * @param hoeIdKey The NamespacedKey for the hoe ID
     * @param hoeUsesKey The NamespacedKey for the hoe uses
     * @return The created ItemStack
     */
    public ItemStack createItemStack(HarvestMoonMC plugin, NamespacedKey hoeIdKey, NamespacedKey hoeUsesKey) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(MessageUtils.colorize(name));

        List<String> finalLore = new ArrayList<>();
        List<String> configLoreFormat = plugin.getConfig().getStringList("customization.hoes.lore_format");

        if (!configLoreFormat.isEmpty()) {
            for (String line : configLoreFormat) {
                String processedLine = line;
                processedLine = processedLine.replace("{multiplier}", String.valueOf(harvestMultiplier));

                if (durability <= 0) {
                    processedLine = processedLine.replace("{durability_text}", MessageUtils.colorize("&7Durability: &aInfinite"));
                } else {
                    processedLine = processedLine.replace("{durability_text}", MessageUtils.colorize("&7Durability: &a" + (durability - uses) + "/" + durability + " uses"));
                }

                if (areaWidth > 1 || areaHeight > 1) {
                    processedLine = processedLine.replace("{area_text}", MessageUtils.colorize(plugin.getConfig().getString("customization.hoes.area_text", "&7Area: &a{width}x{height}")
                            .replace("{width}", String.valueOf(areaWidth))
                            .replace("{height}", String.valueOf(areaHeight))));
                } else {
                    processedLine = processedLine.replace("{area_text}", ""); // Remove placeholder if no area
                }

                if (autoReplant) {
                    processedLine = processedLine.replace("{replant_text}", MessageUtils.colorize(plugin.getConfig().getString("customization.hoes.replant_text", "&7Auto-replant: &aEnabled")));
                } else {
                     processedLine = processedLine.replace("{replant_text}", MessageUtils.colorize(plugin.getConfig().getString("customization.hoes.no_replant_text", "&7Auto-replant: &cDisabled")));
                }
                
                if (isSpecialAbilityEnabled() && specialAbilityRadius > 0 && specialAbilityLoreText != null && !specialAbilityLoreText.isEmpty()) {
                    processedLine = processedLine.replace("{special_ability_text}", MessageUtils.colorize(specialAbilityLoreText));
                } else {
                    processedLine = processedLine.replace("{special_ability_text}", "");
                }

                if (!processedLine.trim().isEmpty()) { // Add line if not empty after replacements
                    finalLore.add(MessageUtils.colorize(processedLine));
                }
            }
        } else { // Fallback to direct lore from hoes.yml if format is missing
            for (String line : lore) {
                finalLore.add(MessageUtils.colorize(line));
            }
            // Manual durability for fallback
            if (durability <= 0) {
                finalLore.add(MessageUtils.colorize("&7Durability: &aInfinite"));
            } else {
                finalLore.add(MessageUtils.colorize("&7Durability: &a" + (durability - uses) + "/" + durability + " uses"));
            }
            if (isSpecialAbilityEnabled() && specialAbilityRadius > 0 && specialAbilityLoreText != null && !specialAbilityLoreText.isEmpty()) {
                 finalLore.add(MessageUtils.colorize(specialAbilityLoreText));
            }
        }
        
        meta.setLore(finalLore);

        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }
        if (glow) {
            meta.addEnchant(Enchantment.LUCK, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(hoeIdKey, PersistentDataType.STRING, id);
        container.set(hoeUsesKey, PersistentDataType.INTEGER, uses); // Still store uses

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Updates the durability information on the item lore
     * @param item The ItemStack to update
     * @param hoeUsesKey The key for the uses data
     * @return The updated ItemStack
     */
    public ItemStack updateDurability(ItemStack item, NamespacedKey hoeUsesKey) {
        if (item == null || !item.hasItemMeta() || durability <= 0) { // If infinite durability, no update needed
            return item;
        }

        ItemMeta meta = item.getItemMeta();
        // uses++; // Increment uses if you want to track it even for infinite hoes for other purposes

        // Update lore
        List<String> newLore = new ArrayList<>();
        List<String> configLoreFormat = HarvestMoonMC.getInstance().getConfig().getStringList("customization.hoes.lore_format");

        if (!configLoreFormat.isEmpty()) {
            for (String line : configLoreFormat) {
                String processedLine = line;
                processedLine = processedLine.replace("{multiplier}", String.valueOf(harvestMultiplier));
                
                if (this.durability <= 0) { // Check instance's durability field
                    processedLine = processedLine.replace("{durability_text}", MessageUtils.colorize("&7Durability: &aInfinite"));
                } else {
                    processedLine = processedLine.replace("{durability_text}", MessageUtils.colorize("&7Durability: &a" + (this.durability - uses) + "/" + this.durability + " uses"));
                }

                if (areaWidth > 1 || areaHeight > 1) {
                     processedLine = processedLine.replace("{area_text}", MessageUtils.colorize(HarvestMoonMC.getInstance().getConfig().getString("customization.hoes.area_text", "&7Area: &a{width}x{height}")
                            .replace("{width}", String.valueOf(areaWidth))
                            .replace("{height}", String.valueOf(areaHeight))));
                } else {
                    processedLine = processedLine.replace("{area_text}", "");
                }
                if (autoReplant) {
                    processedLine = processedLine.replace("{replant_text}", MessageUtils.colorize(HarvestMoonMC.getInstance().getConfig().getString("customization.hoes.replant_text", "&7Auto-replant: &aEnabled")));
                } else {
                    processedLine = processedLine.replace("{replant_text}", MessageUtils.colorize(HarvestMoonMC.getInstance().getConfig().getString("customization.hoes.no_replant_text", "&7Auto-replant: &cDisabled")));
                }
                 if (isSpecialAbilityEnabled() && specialAbilityRadius > 0 && specialAbilityLoreText != null && !specialAbilityLoreText.isEmpty()) {
                    processedLine = processedLine.replace("{special_ability_text}", MessageUtils.colorize(specialAbilityLoreText));
                } else {
                    processedLine = processedLine.replace("{special_ability_text}", "");
                }

                if (!processedLine.trim().isEmpty()) {
                    newLore.add(MessageUtils.colorize(processedLine));
                }
            }
        } else {
            // Fallback to direct lore (less ideal for dynamic updates)
            for(String currentLine : lore) newLore.add(MessageUtils.colorize(currentLine));
            if (this.durability <= 0) {
                 newLore.add(MessageUtils.colorize("&7Durability: &aInfinite"));
            } else {
                 newLore.add(MessageUtils.colorize("&7Durability: &a" + (this.durability - uses) + "/" + this.durability + " uses"));
            }
            if (isSpecialAbilityEnabled() && specialAbilityRadius > 0 && specialAbilityLoreText != null && !specialAbilityLoreText.isEmpty()) {
                 newLore.add(MessageUtils.colorize(specialAbilityLoreText));
            }
        }
        meta.setLore(newLore);

        // Update uses in persistent data
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(hoeUsesKey, PersistentDataType.INTEGER, uses);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Checks if the custom hoe is broken (out of durability)
     * @return true if broken, false otherwise
     */
    public boolean isBroken() {
        if (durability <= 0) return false; // Infinite durability
        return uses >= durability;
    }
}