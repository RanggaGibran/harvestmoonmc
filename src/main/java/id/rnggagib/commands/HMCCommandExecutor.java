package id.rnggagib.commands;

import id.rnggagib.HarvestMoonMC;
import id.rnggagib.models.FarmingRegion;
import id.rnggagib.utils.MessageUtils;
import id.rnggagib.utils.WandUtils;
import id.rnggagib.utils.CropPriceUtils;
import id.rnggagib.gui.ShopGUI;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class HMCCommandExecutor implements CommandExecutor {
    private final HarvestMoonMC plugin;

    public HMCCommandExecutor(HarvestMoonMC plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be executed by a player.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "wand":
                return handleWandCommand(player);
            case "create":
                if (args.length < 2) {
                    player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                            "Usage: /hmc create <region_name>"));
                    return true;
                }
                return handleCreateCommand(player, args[1]);
            case "list":
                return handleListCommand(player);
            case "info":
                if (args.length < 2) {
                    player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                            "Usage: /hmc info <region_name>"));
                    return true;
                }
                return handleInfoCommand(player, args[1]);
            case "delete":
                if (args.length < 2) {
                    player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                            "Usage: /hmc delete <region_name>"));
                    return true;
                }
                return handleDeleteCommand(player, args[1]);
            case "sell":
                return handleSellCommand(player, args.length > 1 ? args[1] : null);
            case "shop":
                return handleShopCommand(player);
            case "help":
            default:
                sendHelpMessage(player);
                return true;
        }
    }

    private boolean handleWandCommand(Player player) {
        if (!player.hasPermission("harvestmoonmc.admin.wand")) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                    plugin.getConfig().getString("messages.no_permission")));
            return true;
        }

        player.getInventory().addItem(WandUtils.createWand());
        player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                plugin.getConfig().getString("messages.wand_given")));
        return true;
    }

    private boolean handleCreateCommand(Player player, String regionName) {
        if (!player.hasPermission("harvestmoonmc.admin.create")) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                    plugin.getConfig().getString("messages.no_permission")));
            return true;
        }

        if (plugin.getRegionManager().regionExists(regionName)) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                    plugin.getConfig().getString("messages.region_exists").replace("%name%", regionName)));
            return true;
        }

        if (!plugin.getSelectionManager().hasCompleteSelection(player)) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                    plugin.getConfig().getString("messages.no_selection")));
            return true;
        }

        Location pos1 = plugin.getSelectionManager().getPos1(player);
        Location pos2 = plugin.getSelectionManager().getPos2(player);
        
        FarmingRegion region = new FarmingRegion(regionName, pos1, pos2);
        plugin.getRegionManager().addRegion(region);
        plugin.getRegionManager().saveRegions();
        
        plugin.getSelectionManager().clearSelection(player);
        
        player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                plugin.getConfig().getString("messages.region_created").replace("%name%", regionName)));
        return true;
    }

    private boolean handleListCommand(Player player) {
        if (!player.hasPermission("harvestmoonmc.admin.list")) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                    plugin.getConfig().getString("messages.no_permission")));
            return true;
        }

        player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + "Farm Regions:"));
        
        if (plugin.getRegionManager().getAllRegions().isEmpty()) {
            player.sendMessage(MessageUtils.colorize("&7  No regions created yet."));
            return true;
        }
        
        for (String name : plugin.getRegionManager().getAllRegions().keySet()) {
            player.sendMessage(MessageUtils.colorize("&a  - " + name));
        }
        
        return true;
    }

    private boolean handleInfoCommand(Player player, String regionName) {
        if (!player.hasPermission("harvestmoonmc.admin.info")) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                    plugin.getConfig().getString("messages.no_permission")));
            return true;
        }

        FarmingRegion region = plugin.getRegionManager().getRegion(regionName);
        
        if (region == null) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                    "&cRegion '" + regionName + "' does not exist."));
            return true;
        }
        
        player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + "Region Info: &e" + regionName));
        player.sendMessage(MessageUtils.colorize("&7World: &f" + region.getWorld().getName()));
        
        Location min = region.getMin();
        Location max = region.getMax();
        
        player.sendMessage(MessageUtils.colorize("&7Min: &f" + min.getBlockX() + ", " + min.getBlockY() + ", " + min.getBlockZ()));
        player.sendMessage(MessageUtils.colorize("&7Max: &f" + max.getBlockX() + ", " + max.getBlockY() + ", " + max.getBlockZ()));
        
        int volume = (max.getBlockX() - min.getBlockX() + 1) * 
                     (max.getBlockY() - min.getBlockY() + 1) * 
                     (max.getBlockZ() - min.getBlockZ() + 1);
        
        player.sendMessage(MessageUtils.colorize("&7Volume: &f" + volume + " blocks"));
        
        return true;
    }

    private boolean handleDeleteCommand(Player player, String regionName) {
        if (!player.hasPermission("harvestmoonmc.admin.delete")) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                    plugin.getConfig().getString("messages.no_permission")));
            return true;
        }

        if (!plugin.getRegionManager().regionExists(regionName)) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                    "&cRegion '" + regionName + "' does not exist."));
            return true;
        }
        
        plugin.getRegionManager().removeRegion(regionName);
        plugin.getRegionManager().saveRegions();
        
        player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                "&aRegion '" + regionName + "' has been deleted."));
        
        return true;
    }

    private boolean handleSellCommand(Player player, String arg) {
        if (!player.hasPermission("harvestmoonmc.sell")) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                    plugin.getConfig().getString("messages.no_permission")));
            return true;
        }
        
        if (!plugin.getEconomyManager().isEnabled()) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                    "&cEconomy system is not enabled. Cannot sell items."));
            return true;
        }
        
        if (arg != null && arg.equalsIgnoreCase("all")) {
            return sellAllCrops(player);
        } else if (arg != null && arg.equalsIgnoreCase("hand")) {
            return sellHandItem(player);
        } else {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                    "&7Usage: /hmc sell <hand|all>"));
            return true;
        }
    }

    private boolean sellHandItem(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item == null || item.getType().isAir()) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                    "&cYou're not holding any item!"));
            return true;
        }
        
        double price = CropPriceUtils.getItemPrice(item);
        
        if (price <= 0) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                    "&cThis item cannot be sold!"));
            return true;
        }
        
        double totalPrice = price * item.getAmount();
        
        // Remove the item
        player.getInventory().setItemInMainHand(null);
        
        // Give money to player
        plugin.getEconomyManager().depositMoney(player, totalPrice);
        
        // Send confirmation message
        player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                "&aSold &f" + item.getAmount() + "x " + item.getItemMeta().getDisplayName() + 
                " &afor &6" + plugin.getEconomyManager().format(totalPrice)));
        
        return true;
    }

    private boolean sellAllCrops(Player player) {
        double totalEarnings = 0;
        int totalItems = 0;
        
        // Check all items in inventory
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            
            if (item == null || item.getType().isAir()) {
                continue;
            }
            
            double price = CropPriceUtils.getItemPrice(item);
            
            if (price <= 0) {
                continue;
            }
            
            double itemTotal = price * item.getAmount();
            totalEarnings += itemTotal;
            totalItems += item.getAmount();
            
            // Remove the item
            player.getInventory().setItem(i, null);
        }
        
        if (totalItems == 0) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                    "&cYou don't have any sellable crops in your inventory!"));
            return true;
        }
        
        // Give money to player
        plugin.getEconomyManager().depositMoney(player, totalEarnings);
        
        // Send confirmation message
        player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                "&aSold &f" + totalItems + " &aitems for &6" + 
                plugin.getEconomyManager().format(totalEarnings)));
        
        return true;
    }

    private boolean handleShopCommand(Player player) {
        if (!player.hasPermission("harvestmoonmc.shop")) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                    plugin.getConfig().getString("messages.no_permission")));
            return true;
        }
        
        if (!plugin.getEconomyManager().isEnabled()) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                    "&cEconomy system is not enabled. Cannot open shop."));
            return true;
        }
        
        // Open shop GUI
        new ShopGUI(plugin).openShop(player);
        
        return true;
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(MessageUtils.colorize("&6==== HarvestMoonMC Help ===="));
        player.sendMessage(MessageUtils.colorize("&e/hmc wand &7- Get a region selection wand"));
        player.sendMessage(MessageUtils.colorize("&e/hmc create <name> &7- Create a farming region"));
        player.sendMessage(MessageUtils.colorize("&e/hmc list &7- List all farming regions"));
        player.sendMessage(MessageUtils.colorize("&e/hmc info <name> &7- View region details"));
        player.sendMessage(MessageUtils.colorize("&e/hmc delete <name> &7- Delete a farming region"));
        player.sendMessage(MessageUtils.colorize("&e/hmc sell <hand|all> &7- Sell crops for money"));
        player.sendMessage(MessageUtils.colorize("&e/hmc shop &7- Open the shop GUI"));
        player.sendMessage(MessageUtils.colorize("&e/hmc help &7- Show this help message"));
    }
}