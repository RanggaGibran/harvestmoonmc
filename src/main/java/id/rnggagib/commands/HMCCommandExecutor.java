package id.rnggagib.commands;

import id.rnggagib.HarvestMoonMC;
import id.rnggagib.models.FarmingRegion;
import id.rnggagib.utils.MessageUtils;
import id.rnggagib.utils.WandUtils;
import id.rnggagib.utils.CropPriceUtils;
import id.rnggagib.gui.ShopGUI;
import id.rnggagib.models.PlayerSkill;
import id.rnggagib.models.CustomHoe;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Random;
import java.util.Set;

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
                            "Usage: /df create <region_name>"));
                    return true;
                }
                return handleCreateCommand(player, args[1]);
            case "list":
                return handleListCommand(player);
            case "info":
                if (args.length < 2) {
                    player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                            "Usage: /df info <region_name>"));
                    return true;
                }
                return handleInfoCommand(player, args[1]);
            case "delete":
                if (args.length < 2) {
                    player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                            "Usage: /df delete <region_name>"));
                    return true;
                }
                return handleDeleteCommand(player, args[1]);
            case "sell":
                return handleSellCommand(player, args.length > 1 ? args[1] : null);
            case "shop":
                return handleShopCommand(player);
            case "hoeshop":
                plugin.getHoeShopGUI().openBuyShop(player);
                return true;
            case "upgradegui":
                ItemStack heldItem = player.getInventory().getItemInMainHand();
                if (heldItem == null || heldItem.getType().isAir()) {
                    player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                            "&cAnda harus memegang custom farming hoe untuk meng-upgrade!"));
                    return true;
                }
                plugin.getHoeShopGUI().openUpgradeShop(player, heldItem);
                return true;
            case "help":
            case "stats":
                return handleStatsCommand(player);
            case "event":
                if (args.length >= 2 && player.hasPermission("harvestmoonmc.admin.event")) {
                    String eventSubCommand = args[1].toLowerCase();
                    switch (eventSubCommand) {
                        case "start":
                            int multiplier = args.length >= 3 ? Integer.parseInt(args[2]) : 0;
                            int durationMinutes = args.length >= 4 ? Integer.parseInt(args[3]) : 0;
                            return handleEventStartCommand(player, multiplier, durationMinutes);
                        case "stop":
                            return handleEventStopCommand(player);
                        case "reload":
                            return handleEventReloadCommand(player);
                        default:
                            return handleEventCommand(player);
                    }
                } else {
                    return handleEventCommand(player);
                }
            case "reload":
            case "reloadconfig":
                return handleReloadCommand(player);
            case "hoe":
            case "tool":
                return handleHoeCommand(player, args);
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
                    "&7Usage: /df sell <hand|all>"));
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
        
        double basePrice = CropPriceUtils.getItemPrice(item);
        
        if (basePrice <= 0) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                    "&cItem ini tidak dapat dijual!"));
            return true;
        }
        
        // Apply the event multiplier
        int priceMultiplier = plugin.getEventManager().getCurrentPriceMultiplier();
        boolean isEventActive = priceMultiplier > 1;
        
        double price = basePrice * priceMultiplier;
        double totalPrice = price * item.getAmount();
        
        // Remove the item
        player.getInventory().setItemInMainHand(null);
        
        // Give money to player
        plugin.getEconomyManager().depositMoney(player, totalPrice);
        
        // Send confirmation message with event bonus info if active
        String message;
        if (isEventActive) {
            message = "&aTerjual &f" + item.getAmount() + "x " + item.getItemMeta().getDisplayName() + 
                    " &aseharga &6" + plugin.getEconomyManager().format(totalPrice) + 
                    " &a(&e" + priceMultiplier + "x &amultiplier event!)";
        } else {
            message = "&aTerjual &f" + item.getAmount() + "x " + item.getItemMeta().getDisplayName() + 
                    " &aseharga &6" + plugin.getEconomyManager().format(totalPrice);
        }
        
        player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + message));
        
        return true;
    }

    private boolean sellAllCrops(Player player) {
        double totalEarnings = 0;
        int totalItems = 0;
        
        // Get the event multiplier
        int priceMultiplier = plugin.getEventManager().getCurrentPriceMultiplier();
        boolean isEventActive = priceMultiplier > 1;
        
        // Check all items in inventory
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            
            if (item == null || item.getType().isAir()) {
                continue;
            }
            
            double basePrice = CropPriceUtils.getItemPrice(item);
            
            if (basePrice <= 0) {
                continue;
            }
            
            // Apply the event multiplier
            double price = basePrice * priceMultiplier;
            double itemTotal = price * item.getAmount();
            totalEarnings += itemTotal;
            totalItems += item.getAmount();
            
            // Remove the item
            player.getInventory().setItem(i, null);
        }
        
        if (totalItems == 0) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                    "&cAnda tidak memiliki hasil panen yang dapat dijual!"));
            return true;
        }
        
        // Give money to player
        plugin.getEconomyManager().depositMoney(player, totalEarnings);
        
        // Send confirmation message with event bonus info if active
        String message;
        if (isEventActive) {
            message = "&aTerjual &f" + totalItems + " &ahasil panen seharga &6" + 
                    plugin.getEconomyManager().format(totalEarnings) + " &a(&e" + priceMultiplier + "x &amultiplier event!)";
        } else {
            message = "&aTerjual &f" + totalItems + " &ahasil panen seharga &6" + 
                    plugin.getEconomyManager().format(totalEarnings);
        }
        
        player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + message));
        
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

    private boolean handleStatsCommand(Player player) {
        if (!player.hasPermission("harvestmoonmc.stats")) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                    plugin.getConfig().getString("messages.no_permission")));
            return true;
        }
        
        PlayerSkill skill = plugin.getSkillManager().getSkill(player);
        int level = skill.getLevel();
        int currentXp = skill.getXp();
        int nextLevelXp = skill.getXpForNextLevel();
        double progress = skill.getProgressToNextLevel();
        double qualityBonus = plugin.getSkillManager().getQualityMultiplier(player);
        
        player.sendMessage(MessageUtils.colorize("&6==== Your Farming Skills ===="));
        player.sendMessage(MessageUtils.colorize("&7Level: &a" + level + (level >= PlayerSkill.getMaxLevel() ? " &7(MAX)" : "")));
        
        if (level < PlayerSkill.getMaxLevel()) {
            player.sendMessage(MessageUtils.colorize("&7XP: &b" + currentXp + "&7/&b" + nextLevelXp + 
                    " &7(" + String.format("%.1f", progress * 100) + "%)"));
                    
            // Create XP bar visualization
            int barLength = 20;
            int filledBars = (int) Math.round(progress * barLength);
            StringBuilder bar = new StringBuilder("&7[");
            
            for (int i = 0; i < barLength; i++) {
                if (i < filledBars) {
                    bar.append("&a▮");
                } else {
                    bar.append("&8▮");
                }
            }
            
            bar.append("&7]");
            player.sendMessage(MessageUtils.colorize(bar.toString()));
        }
        
        // Show bonuses
        player.sendMessage(MessageUtils.colorize("&7Quality Bonus: &6+" + 
                String.format("%.0f", (qualityBonus - 1) * 100) + "% &7chance for better crops"));
        
        return true;
    }

    private boolean handleEventCommand(Player player) {
        if (!player.hasPermission("harvestmoonmc.event")) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                    plugin.getConfig().getString("messages.no_permission")));
            return true;
        }
        
        if (plugin.getEventManager().isEventActive()) {
            int multiplier = plugin.getEventManager().getCurrentPriceMultiplier();
            int remainingSeconds = plugin.getEventManager().getRemainingTime();
            int minutes = remainingSeconds / 60;
            int seconds = remainingSeconds % 60;
            
            player.sendMessage(MessageUtils.colorize("&6==== Event Penjualan Spesial ===="));
            player.sendMessage(MessageUtils.colorize("&7Status: &aAktif"));
            player.sendMessage(MessageUtils.colorize("&7Multiplier: &e" + multiplier + "x"));
            player.sendMessage(MessageUtils.colorize("&7Sisa waktu: &e" + minutes + " menit " + seconds + " detik"));
        } else {
            player.sendMessage(MessageUtils.colorize("&6==== Event Penjualan Spesial ===="));
            player.sendMessage(MessageUtils.colorize("&7Status: &cTidak aktif"));
            player.sendMessage(MessageUtils.colorize("&7Tunggu pengumuman event penjualan spesial berikutnya!"));
        }
        
        return true;
    }

    /**
     * Handles the command to manually start an event
     */
    private boolean handleEventStartCommand(Player player, int requestedMultiplier, int requestedDurationMinutes) {
        if (!player.hasPermission("harvestmoonmc.admin.event")) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                    plugin.getConfig().getString("messages.no_permission")));
            return true;
        }

        // Get default config values
        int defaultMinDuration = plugin.getConfig().getInt("events.min_duration_minutes", 5);
        int defaultMaxDuration = plugin.getConfig().getInt("events.max_duration_minutes", 15);
        int[] possibleMultipliers = plugin.getEventManager().getPossibleMultipliers();
        
        // Use defaults if values aren't specified or are invalid
        int multiplier = requestedMultiplier;
        if (multiplier <= 0) {
            // Pick a random multiplier from config
            multiplier = possibleMultipliers[new Random().nextInt(possibleMultipliers.length)];
        }
        
        int durationMinutes = requestedDurationMinutes;
        if (durationMinutes <= 0) {
            // Random duration between min and max from config
            durationMinutes = defaultMinDuration + new Random().nextInt(defaultMaxDuration - defaultMinDuration + 1);
        }
        
        // Start the event
        boolean started = plugin.getEventManager().startEventManually(multiplier, durationMinutes * 60);
        
        if (started) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                    "&aEvent penjualan spesial berhasil dimulai dengan multiplier " + multiplier + "x selama " + 
                    durationMinutes + " menit!"));
        } else {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                    "&cTidak dapat memulai event. Event lain mungkin sedang berjalan."));
        }
        
        return true;
    }

    /**
     * Handles the command to manually stop an event
     */
    private boolean handleEventStopCommand(Player player) {
        if (!player.hasPermission("harvestmoonmc.admin.event")) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                    plugin.getConfig().getString("messages.no_permission")));
            return true;
        }
        
        boolean stopped = plugin.getEventManager().stopEventManually();
        
        if (stopped) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                    "&aEvent penjualan spesial berhasil dihentikan."));
        } else {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                    "&cTidak ada event yang sedang berjalan."));
        }
        
        return true;
    }

    /**
     * Handles the command to reload event settings
     */
    private boolean handleEventReloadCommand(Player player) {
        if (!player.hasPermission("harvestmoonmc.admin.event")) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                    plugin.getConfig().getString("messages.no_permission")));
            return true;
        }
        
        plugin.getEventManager().reloadConfig();
        
        player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                "&aKonfigurasi event berhasil di-reload."));
        
        return true;
    }

    /**
     * Handles the reload config command
     */
    private boolean handleReloadCommand(Player player) {
        if (!player.hasPermission("harvestmoonmc.admin.reload")) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                    plugin.getConfig().getString("messages.no_permission")));
            return true;
        }
        
        // Reload main plugin config
        plugin.reloadConfig();
        
        // Reload managers that use config values
        if (plugin.getEventManager() != null) {
            plugin.getEventManager().reloadConfig();
        }
        
        if (plugin.getHarvestLimitManager() != null) {
            plugin.getHarvestLimitManager().reloadConfig();
        }
        
        // Add other managers that need config reloading here
        
        player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                "&aKonfigurasi plugin berhasil di-reload!"));
        
        return true;
    }

    private boolean handleHoeCommand(Player player, String[] args) {
        if (args.length < 2) {
            sendHoeHelp(player);
            return true;
        }
        
        String subCommand = args[1].toLowerCase();
        
        switch (subCommand) {
            case "get":
                if (!player.hasPermission("harvestmoonmc.admin.hoe")) {
                    player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                            plugin.getConfig().getString("messages.no_permission")));
                    return true;
                }
                
                if (args.length < 3) {
                    player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                            "&cUsage: /df hoe get <hoe_id>"));
                    return true;
                }
                
                String hoeId = args[2].toLowerCase();
                ItemStack hoe = plugin.getHoeManager().createHoe(hoeId);
                
                if (hoe == null) {
                    player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                            "&cInvalid hoe ID: " + hoeId));
                    return true;
                }
                
                player.getInventory().addItem(hoe);
                player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                        "&aYou received a " + hoe.getItemMeta().getDisplayName() + "&a!"));
                return true;
                
            case "list":
                if (!player.hasPermission("harvestmoonmc.admin.hoe")) {
                    player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                            plugin.getConfig().getString("messages.no_permission")));
                    return true;
                }
                
                Set<String> hoeIds = plugin.getHoeManager().getAllHoeIds();
                player.sendMessage(MessageUtils.colorize("&6==== Custom Hoes ===="));
                for (String id : hoeIds) {
                    player.sendMessage(MessageUtils.colorize("&e- " + id));
                }
                return true;
                
            case "upgrade":
                ItemStack heldItem = player.getInventory().getItemInMainHand();
                CustomHoe customHoe = plugin.getHoeManager().getHoeFromItemStack(heldItem);
                
                if (customHoe == null) {
                    player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                            "&cYou must hold a custom farming hoe to upgrade it!"));
                    return true;
                }
                
                plugin.getHoeManager().upgradeHoe(heldItem, player);
                return true;
                
            case "reload":
                if (!player.hasPermission("harvestmoonmc.admin.reload")) {
                    player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                            plugin.getConfig().getString("messages.no_permission")));
                    return true;
                }
                
                plugin.getHoeManager().reloadConfig();
                player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                        "&aHoes configuration reloaded!"));
                return true;
                
            case "give":
                if (!player.hasPermission("harvestmoonmc.admin.hoe.give")) {
                    player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                            plugin.getConfig().getString("messages.no_permission")));
                    return true;
                }
                
                if (args.length < 4) {
                    player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                            "&cUsage: /df hoe give <player> <hoe_id>"));
                    return true;
                }
                
                // Get target player
                Player targetPlayer = Bukkit.getPlayer(args[2]);
                if (targetPlayer == null) {
                    player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                            "&cPlayer '" + args[2] + "' not found or offline."));
                    return true;
                }
                
                // Get hoe
                String giveHoeId = args[3].toLowerCase();
                ItemStack giveHoe = plugin.getHoeManager().createHoe(giveHoeId);
                
                if (giveHoe == null) {
                    player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                            "&cInvalid hoe ID: " + giveHoeId));
                    return true;
                }
                
                // Give hoe to target player
                targetPlayer.getInventory().addItem(giveHoe);
                
                // Notify both players
                player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                        "&aYou gave " + targetPlayer.getName() + " a " + giveHoe.getItemMeta().getDisplayName() + "&a!"));
                targetPlayer.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                        "&aYou received a " + giveHoe.getItemMeta().getDisplayName() + "&a from " + player.getName() + "!"));
                return true;
                
            default:
                sendHoeHelp(player);
                return true;
        }
    }

    /**
     * Sends hoe command help
     */
    private void sendHoeHelp(Player player) {
        player.sendMessage(MessageUtils.colorize("&6==== Custom Hoes ===="));
        player.sendMessage(MessageUtils.colorize("&e/df hoe upgrade &7- Upgrade your current hoe"));
        player.sendMessage(MessageUtils.colorize("&e/df hoe shop &7- Open hoe shop"));
        
        if (player.hasPermission("harvestmoonmc.admin.hoe")) {
            player.sendMessage(MessageUtils.colorize("&e/df hoe get <id> &7- Get a custom hoe"));
            player.sendMessage(MessageUtils.colorize("&e/df hoe list &7- List all custom hoes"));
            player.sendMessage(MessageUtils.colorize("&e/df hoe reload &7- Reload hoes configuration"));
        }
        
        player.sendMessage(MessageUtils.colorize("&7Tip: Shift + Right-click with a hoe to open upgrade GUI"));
        player.sendMessage(MessageUtils.colorize("&7Tip: Shift + Left-click with a hoe to activate special ability"));
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(MessageUtils.colorize("&6==== DragFarm Help ===="));
        player.sendMessage(MessageUtils.colorize("&7/df shop &f- Buka toko penjualan hasil panen"));
        player.sendMessage(MessageUtils.colorize("&7/df sell hand &f- Jual item di tangan"));
        player.sendMessage(MessageUtils.colorize("&7/df sell all &f- Jual semua hasil panen di inventory"));
        player.sendMessage(MessageUtils.colorize("&7/df stats &f- Lihat statistik farming Anda"));
        player.sendMessage(MessageUtils.colorize("&7/df event &f- Cek status event penjualan spesial"));
        player.sendMessage(MessageUtils.colorize("&7/df hoe &f- Informasi tentang custom hoe"));
        player.sendMessage(MessageUtils.colorize("&7/hoeshop &f- Buka toko cangkul"));
        
        if (player.hasPermission("dragfarm.admin")) {
            player.sendMessage(MessageUtils.colorize("&6==== Admin Commands ===="));
            player.sendMessage(MessageUtils.colorize("&7/df wand &f- Dapatkan region selector wand"));
            player.sendMessage(MessageUtils.colorize("&7/df create <name> &f- Buat region pertanian baru"));
            player.sendMessage(MessageUtils.colorize("&7/df list &f- Tampilkan semua region pertanian"));
            player.sendMessage(MessageUtils.colorize("&7/df info <name> &f- Lihat info region pertanian"));
            player.sendMessage(MessageUtils.colorize("&7/df delete <name> &f- Hapus region pertanian"));
            player.sendMessage(MessageUtils.colorize("&7/df reload &f- Reload plugin config"));
            player.sendMessage(MessageUtils.colorize("&7/df event start <multiplier> <minutes> &f- Mulai event"));
            player.sendMessage(MessageUtils.colorize("&7/df event stop &f- Akhiri event"));
            player.sendMessage(MessageUtils.colorize("&7/df hoe get <id> &f- Dapatkan custom hoe"));
        }
    }
}