package id.rnggagib.listeners;

import id.rnggagib.HarvestMoonMC;
import id.rnggagib.utils.MessageUtils;
import id.rnggagib.utils.WandUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class WandListener implements Listener {
    private final HarvestMoonMC plugin;
    
    public WandListener(HarvestMoonMC plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (!WandUtils.isWand(item)) {
            return;
        }
        
        if (!player.hasPermission("harvestmoonmc.admin.wand")) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                    plugin.getConfig().getString("messages.no_permission")));
            return;
        }
        
        // Cancel the event to prevent block interaction
        event.setCancelled(true);
        
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            Location location = event.getClickedBlock().getLocation();
            plugin.getSelectionManager().setPos1(player, location);
            
            String message = plugin.getConfig().getString("messages.pos1_selected")
                    .replace("%x%", String.valueOf(location.getBlockX()))
                    .replace("%y%", String.valueOf(location.getBlockY()))
                    .replace("%z%", String.valueOf(location.getBlockZ()));
            
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + message));
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Location location = event.getClickedBlock().getLocation();
            plugin.getSelectionManager().setPos2(player, location);
            
            String message = plugin.getConfig().getString("messages.pos2_selected")
                    .replace("%x%", String.valueOf(location.getBlockX()))
                    .replace("%y%", String.valueOf(location.getBlockY()))
                    .replace("%z%", String.valueOf(location.getBlockZ()));
            
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + message));
        }
    }
}