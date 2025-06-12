package id.rnggagib.listeners;

import id.rnggagib.HarvestMoonMC;
import id.rnggagib.models.CustomHoe;
import id.rnggagib.models.FarmingRegion; // Add this
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import id.rnggagib.utils.MessageUtils;

public class HoeInteractListener implements Listener {
    private final HarvestMoonMC plugin;

    public HoeInteractListener(HarvestMoonMC plugin) {
        this.plugin = plugin;
    }    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        CustomHoe hoe = plugin.getHoeManager().getHoeFromItemStack(item);
        
        if (hoe == null) {
            // Not a custom hoe, do nothing
            return;
        }
        
        // Shift+Left Click for skill activation
        if ((event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) 
                && player.isSneaking()) {
            
            FarmingRegion currentRegion = plugin.getRegionManager().getRegionAt(player.getLocation());
            
            // Check if special ability is available and can be activated
            if (hoe.isSpecialAbilityEnabled() && hoe.getSpecialAbilityRadius() > 0) {
                // Check if player is in a region or standing near farmland
                boolean isInFarmArea = currentRegion != null || 
                                      player.getLocation().getBlock().getRelative(0, -1, 0).getType() == org.bukkit.Material.FARMLAND;
                
                if (!isInFarmArea) {
                    player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") +
                            plugin.getConfig().getString("messages.custom_hoe_outside_farm_region")));
                    event.setCancelled(true);
                    return;
                }
                
                // Activate special ability
                event.setCancelled(true);
                plugin.getHoeManager().activateSpecialAbility(player, hoe, item);
            }
        }
        // Shift+Right Click for upgrading
        else if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
                && player.isSneaking()) {
            
            // Check if the hoe can be upgraded
            if (hoe.getNextTier() != null) {
                // For upgrade GUI, it's okay to open it anywhere, the actual upgrade action will check materials.
                event.setCancelled(true);
                
                if (!player.hasPermission("dragonfarm.hoe.upgrade")) {
                    player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") +
                            plugin.getConfig().getString("messages.no_permission")));
                    return;
                }
                
                plugin.getHoeShopGUI().openUpgradeShop(player, item);
                return;
            }
            
            // If it's a custom hoe but no upgrade available, and they are outside a farm region, send the message
            FarmingRegion currentRegion = plugin.getRegionManager().getRegionAt(player.getLocation());
            boolean isInFarmArea = currentRegion != null || 
                                  player.getLocation().getBlock().getRelative(0, -1, 0).getType() == org.bukkit.Material.FARMLAND;
            
            if (!isInFarmArea) {
                 player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") +
                            plugin.getConfig().getString("messages.custom_hoe_outside_farm_region")));
            }
            
            // Cancel default hoe actions if it's a custom hoe
            event.setCancelled(true);
        }
    }
}