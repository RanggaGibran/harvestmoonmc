package id.rnggagib.gui;

import id.rnggagib.HarvestMoonMC;
import id.rnggagib.utils.CropPriceUtils;
import id.rnggagib.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopGUI implements Listener {
    private final HarvestMoonMC plugin;
    private final Map<Player, Inventory> openShops = new HashMap<>();
    private static final int SHOP_SIZE = 54; // 6 rows
    private static final int INFO_SLOT = 4;
    private static final int SELL_ALL_SLOT = 49;
    
    public ShopGUI(HarvestMoonMC plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Opens the shop GUI for a player
     * @param player The player
     */
    public void openShop(Player player) {
        Inventory shopInventory = Bukkit.createInventory(null, SHOP_SIZE, "HarvestMoonMC Shop");
        
        // Add info item
        ItemStack infoItem = createInfoItem(player);
        shopInventory.setItem(INFO_SLOT, infoItem);
        
        // Add sell all button
        ItemStack sellAllItem = new ItemStack(Material.GOLD_INGOT);
        ItemMeta sellAllMeta = sellAllItem.getItemMeta();
        sellAllMeta.setDisplayName(MessageUtils.colorize("&6&lSell All Crops"));
        List<String> sellAllLore = new ArrayList<>();
        sellAllLore.add(MessageUtils.colorize("&7Click to sell all crops"));
        sellAllLore.add(MessageUtils.colorize("&7in your inventory"));
        sellAllMeta.setLore(sellAllLore);
        sellAllItem.setItemMeta(sellAllMeta);
        shopInventory.setItem(SELL_ALL_SLOT, sellAllItem);
        
        // Add glass pane dividers
        ItemStack divider = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta dividerMeta = divider.getItemMeta();
        dividerMeta.setDisplayName(" ");
        divider.setItemMeta(dividerMeta);
        
        for (int i = 0; i < 9; i++) {
            shopInventory.setItem(i, divider);
        }
        for (int i = 45; i < 54; i++) {
            if (i != SELL_ALL_SLOT) {
                shopInventory.setItem(i, divider);
            }
        }
        
        openShops.put(player, shopInventory);
        player.openInventory(shopInventory);
    }
    
    /**
     * Creates the info item for the shop
     * @param player The player
     * @return The info item
     */
    private ItemStack createInfoItem(Player player) {
        ItemStack infoItem = new ItemStack(Material.BOOK);
        ItemMeta meta = infoItem.getItemMeta();
        meta.setDisplayName(MessageUtils.colorize("&e&lHarvestMoonMC Shop"));
        
        List<String> lore = new ArrayList<>();
        lore.add(MessageUtils.colorize("&7Place your crops in the empty slots"));
        lore.add(MessageUtils.colorize("&7to see their selling price."));
        lore.add(MessageUtils.colorize("&7"));
        lore.add(MessageUtils.colorize("&7Click on a placed crop to sell it."));
        lore.add(MessageUtils.colorize("&7"));
        lore.add(MessageUtils.colorize("&7Your Balance: &6" + 
                plugin.getEconomyManager().format(plugin.getEconomyManager().getBalance(player))));
        
        // If there's an active event, add that information to the lore
        if (plugin.getEventManager().isEventActive()) {
            int multiplier = plugin.getEventManager().getCurrentPriceMultiplier();
            int remainingSeconds = plugin.getEventManager().getRemainingTime();
            int minutes = remainingSeconds / 60;
            int seconds = remainingSeconds % 60;
            
            lore.add("");
            lore.add(MessageUtils.colorize("&6&l✨ EVENT SPESIAL AKTIF!"));
            lore.add(MessageUtils.colorize("&e" + multiplier + "x &7multiplier harga penjualan!"));
            lore.add(MessageUtils.colorize("&7Sisa waktu: &e" + minutes + "m " + seconds + "s"));
        }
        
        meta.setLore(lore);
        infoItem.setItemMeta(meta);
        
        return infoItem;
    }
    
    /**
     * Updates the info item with the player's current balance
     * @param player The player
     * @param inventory The shop inventory
     */
    private void updateInfoItem(Player player, Inventory inventory) {
        ItemStack infoItem = createInfoItem(player);
        inventory.setItem(INFO_SLOT, infoItem);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();
        
        if (!openShops.containsKey(player) || openShops.get(player) != inventory) {
            return;
        }
        
        int slot = event.getRawSlot();
        
        // Prevent clicking on dividers and info
        if (slot < 9 || (slot >= 45 && slot < 54 && slot != SELL_ALL_SLOT)) {
            event.setCancelled(true);
            return;
        }
        
        // Handle sell all button
        if (slot == SELL_ALL_SLOT) {
            event.setCancelled(true);
            sellAllCropsInShop(player, inventory);
            return;
        }
        
        // If clicking in shop inventory but not on dividers
        if (slot < SHOP_SIZE && slot >= 9 && slot < 45) {
            ItemStack clickedItem = inventory.getItem(slot);
            
            if (clickedItem != null && !clickedItem.getType().isAir()) {
                // Check if it's a sellable crop
                double price = CropPriceUtils.getItemPrice(clickedItem);
                
                if (price > 0) {
                    // Sell the item
                    double totalPrice = price * clickedItem.getAmount();
                    plugin.getEconomyManager().depositMoney(player, totalPrice);
                    
                    player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                            "&aSold &f" + clickedItem.getAmount() + "x " + clickedItem.getItemMeta().getDisplayName() + 
                            " &afor &6" + plugin.getEconomyManager().format(totalPrice)));
                    
                    // Remove the item
                    inventory.setItem(slot, null);
                    
                    // Update info item
                    updateInfoItem(player, inventory);
                }
            }
        }
    }
    
    /**
     * Sells all crops in the shop inventory
     * @param player The player
     * @param inventory The shop inventory
     */
    private void sellAllCropsInShop(Player player, Inventory inventory) {
        double totalEarnings = 0;
        int totalItems = 0;
        
        // Get the event multiplier
        int priceMultiplier = plugin.getEventManager().getCurrentPriceMultiplier();
        boolean isEventActive = priceMultiplier > 1;
        
        // Check all items in shop inventory (excluding dividers and buttons)
        for (int i = 9; i < 45; i++) {
            ItemStack item = inventory.getItem(i);
            
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
            inventory.setItem(i, null);
        }
        
        if (totalItems == 0) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                    "&cTidak ada hasil panen yang bisa dijual di toko!"));
            return;
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
        
        // Update info
        updateInfoItem(player, inventory);
    }
    
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();
        
        if (!openShops.containsKey(player) || openShops.get(player) != inventory) {
            return;
        }
        
        // If any of the drag slots are in the divider area, cancel the event
        for (int slot : event.getRawSlots()) {
            if (slot < 9 || (slot >= 45 && slot < 54 && slot != SELL_ALL_SLOT)) {
                event.setCancelled(true);
                return;
            }
        }
        
        // Update info after dragging
        Bukkit.getScheduler().runTaskLater(plugin, () -> updateInfoItem(player, inventory), 1L);
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        Inventory inventory = event.getInventory();
        
        if (!openShops.containsKey(player) || openShops.get(player) != inventory) {
            return;
        }
        
        // Return any items in the shop inventory to the player
        boolean itemsReturned = false;
        for (int i = 9; i < 45; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && !item.getType().isAir()) {
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                
                // Drop any items that didn't fit
                for (ItemStack leftoverItem : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftoverItem);
                }
                
                itemsReturned = true;
            }
        }
        
        if (itemsReturned) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + 
                    "&7Items returned to your inventory."));
        }
        
        openShops.remove(player);
    }
}