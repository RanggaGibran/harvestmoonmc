package id.rnggagib.gui;

import id.rnggagib.HarvestMoonMC;
import id.rnggagib.models.CustomHoe;
import id.rnggagib.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class HoeShopGUI implements Listener {
    private final HarvestMoonMC plugin;
    private final Map<Player, Inventory> openShops = new HashMap<>();
    private final Map<Player, String> viewingHoe = new HashMap<>();
    
    private static final int SHOP_SIZE = 36; // 4 baris
    // Constants for default titles if config is missing
    private static final String DEFAULT_HOE_BUY_SHOP_TITLE = "Toko Cangkul";
    private static final String DEFAULT_UPGRADE_TITLE = "Upgrade Cangkul";
    
    public HoeShopGUI(HarvestMoonMC plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
      public void openBuyShop(Player player) {
        String buyShopTitle = plugin.getConfig().getString("customization.gui.hoe_shop_main_title", DEFAULT_HOE_BUY_SHOP_TITLE);
        Inventory shopInventory = Bukkit.createInventory(null, SHOP_SIZE, buyShopTitle);
        ItemStack divider = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");

        // Baris 1: Pembatas
        for (int i = 0; i < 9; i++) {
            shopInventory.setItem(i, divider);
        }

        // Baris 2: Item-item utama
        shopInventory.setItem(9, divider);
        shopInventory.setItem(10, divider);
        ItemStack basicHoe = createHoeShopItem("basic_hoe", player);
        if (basicHoe != null) shopInventory.setItem(11, basicHoe); 
        shopInventory.setItem(12, divider);
        shopInventory.setItem(13, divider); 
        // Removed upgrade button
        shopInventory.setItem(15, divider); 
        shopInventory.setItem(16, divider);
        shopInventory.setItem(17, divider);

        // Baris 3: Pembatas
        for (int i = 18; i < 27; i++) {
            shopInventory.setItem(i, divider);
        }
        
        // Baris 4: Tombol kembali & Pembatas
        for (int i = 27; i < 36; i++) {
            if (i == 31) { 
                ItemStack backButton = createGuiItem(Material.BARRIER, "§c§lKembali");
                shopInventory.setItem(i, backButton);
            } else {
                shopInventory.setItem(i, divider);
            }
        }
        
        openShops.put(player, shopInventory);
        player.openInventory(shopInventory);
    }
    
    public void openUpgradeShop(Player player, ItemStack currentHoeItem) {
        CustomHoe hoe = plugin.getHoeManager().getHoeFromItemStack(currentHoeItem);
        if (hoe == null || hoe.getNextTier() == null) {
            player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix") + "&cTidak ada upgrade yang tersedia untuk hoe ini atau hoe tidak valid!"));
            return;
        }
        
        viewingHoe.put(player, hoe.getId());
        
        String upgradeShopTitle = plugin.getConfig().getString("customization.gui.upgrade_title", DEFAULT_UPGRADE_TITLE);
        Inventory upgradeInventory = Bukkit.createInventory(null, SHOP_SIZE, upgradeShopTitle);
        ItemStack divider = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");

        // Baris 1: Pembatas
        for (int i = 0; i < 9; i++) {
            upgradeInventory.setItem(i, divider);
        }
        
        // Baris 2: Hoe saat ini -> Panah -> Hoe berikutnya
        upgradeInventory.setItem(9, divider);
        upgradeInventory.setItem(10, divider);
        upgradeInventory.setItem(11, currentHoeItem); 
        upgradeInventory.setItem(12, divider); 
        ItemStack arrow = createGuiItem(Material.ARROW, "§e§l→ Upgrade →");
        upgradeInventory.setItem(13, arrow); 
        upgradeInventory.setItem(14, divider);
        CustomHoe nextTierHoe = plugin.getHoeManager().getHoeById(hoe.getNextTier());
        if (nextTierHoe != null) {
            ItemStack nextTierHoeItem = plugin.getHoeManager().createHoe(nextTierHoe.getId());
            if (nextTierHoeItem != null) {
                ItemMeta meta = nextTierHoeItem.getItemMeta();
                if (meta != null) {
                    List<String> loreList = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                    loreList.add(" ");
                    loreList.add(MessageUtils.colorize("&7Biaya Upgrade: &6" + plugin.getEconomyManager().format(hoe.getUpgradeCost())));
                    Map<Material, Integer> materials = hoe.getUpgradeMaterials();
                    if (!materials.isEmpty()) {
                        loreList.add(MessageUtils.colorize("&7Material Dibutuhkan:"));
                        for (Map.Entry<Material, Integer> entry : materials.entrySet()) {
                            loreList.add(MessageUtils.colorize("&7- " + formatMaterialName(entry.getKey().toString()) + ": &ax" + entry.getValue()));
                        }
                    }
                    meta.setLore(loreList);
                    nextTierHoeItem.setItemMeta(meta);
                }
                upgradeInventory.setItem(15, nextTierHoeItem); 
            } else {
                upgradeInventory.setItem(15, createGuiItem(Material.BARRIER, "§cError: Next tier not found"));
            }
        } else {
            upgradeInventory.setItem(15, createGuiItem(Material.BARRIER, "§cError: Next tier not found"));
        }
        upgradeInventory.setItem(16, divider);
        upgradeInventory.setItem(17, divider);

        // Baris 3: Tombol Upgrade & Pembatas
        for (int i = 18; i < 27; i++) {
            if (i == 22) { 
                String upgradeButtonText = plugin.getConfig().getString("customization.gui.upgrade_button", "§a§lUpgrade Sekarang");
                upgradeInventory.setItem(i, createGuiItem(Material.EMERALD_BLOCK, upgradeButtonText,
                        Collections.singletonList("§7Klik untuk mengupgrade hoe Anda")));
            } else {
                upgradeInventory.setItem(i, divider);
            }
        }
        
        // Baris 4: Tombol Batal & Pembatas
        for (int i = 27; i < 36; i++) {
            if (i == 31) { 
                String cancelButtonText = plugin.getConfig().getString("customization.gui.cancel_button", "§c§lBatal");
                upgradeInventory.setItem(i, createGuiItem(Material.BARRIER, cancelButtonText, 
                        Collections.singletonList("§7Klik untuk kembali")));
            } else {
                upgradeInventory.setItem(i, divider);
            }
        }
        
        openShops.put(player, upgradeInventory);
        player.openInventory(upgradeInventory);
    }
    
    /**
     * Membuat item untuk ditampilkan di GUI
     */
    private ItemStack createGuiItem(Material material, String name) {
        return createGuiItem(material, name, new ArrayList<>());
    }
    
    /**
     * Membuat item untuk ditampilkan di GUI dengan lore
     */
    private ItemStack createGuiItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
      /**
     * Membuat item hoe untuk ditampilkan di shop
     */
    private ItemStack createHoeShopItem(String hoeId, Player player) {
        ItemStack hoeItem = plugin.getHoeManager().createHoe(hoeId);
        CustomHoe hoe = plugin.getHoeManager().getHoeById(hoeId);
        
        if (hoeItem != null && hoe != null) {
            ItemMeta meta = hoeItem.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add("");
                
                // Check if player already has this hoe
                boolean playerHasHoe = false;
                for (ItemStack item : player.getInventory().getContents()) {
                    if (item != null) {
                        CustomHoe playerHoe = plugin.getHoeManager().getHoeFromItemStack(item);
                        if (playerHoe != null && playerHoe.getId().equals(hoeId)) {
                            playerHasHoe = true;
                            break;
                        }
                    }
                }
                
                if (playerHasHoe) {
                    // Player already has this hoe
                    lore.add("§c§lAnda sudah memiliki cangkul ini");
                    // Change the item appearance to show it's unavailable
                    hoeItem.setType(Material.GRAY_DYE);
                } else {
                    // Player doesn't have this hoe yet
                    lore.add("§a§lHarga: §a" + plugin.getEconomyManager().format(5000)); // Harga dasar untuk basic_hoe
                    lore.add("§7Klik untuk membeli");
                }
                
                meta.setLore(lore);
                hoeItem.setItemMeta(meta);
            }
        }
        
        return hoeItem;
    }
    
    /**
     * Format nama material agar lebih mudah dibaca
     */
    private String formatMaterialName(String material) {
        return Arrays.stream(material.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .reduce((a, b) -> a + " " + b)
                .orElse(material);
    }
    
    /**
     * Event handler untuk klik pada inventory
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();
        
        if (!openShops.containsKey(player) || !openShops.get(player).equals(inventory)) {
            return;
        }
        
        event.setCancelled(true);
        
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR || 
            (clickedItem.getType() == Material.GRAY_STAINED_GLASS_PANE && clickedItem.getItemMeta().getDisplayName().equals(" "))) {
            return;
        }
        
        String viewTitle = event.getView().getTitle();
        String buyShopTitle = plugin.getConfig().getString("customization.gui.hoe_shop_main_title", DEFAULT_HOE_BUY_SHOP_TITLE);
        String upgradeShopTitle = plugin.getConfig().getString("customization.gui.upgrade_title", DEFAULT_UPGRADE_TITLE);        if (viewTitle.equals(buyShopTitle)) { 
            if (slot == 11) { 
                if (clickedItem.getType() != Material.GRAY_STAINED_GLASS_PANE && clickedItem.getType() != Material.GRAY_DYE) {
                    buyBasicHoe(player);
                } else if (clickedItem.getType() == Material.GRAY_DYE) {
                    // Player already has this hoe
                    player.sendMessage(MessageUtils.colorize(plugin.getConfig().getString("messages.prefix", "&6[DragFarm] &r") +
                            "&cAnda sudah memiliki cangkul ini!"));
                }
            } else if (slot == 31 && clickedItem.getType() == Material.BARRIER) { 
                player.closeInventory();
            }
        } 
        else if (viewTitle.equals(upgradeShopTitle)) { // Corrected comparison
            if (slot == 22 && clickedItem.getType() == Material.EMERALD_BLOCK) { 
                upgradePlayerHoe(player);
            } else if (slot == 31 && clickedItem.getType() == Material.BARRIER) { 
                openBuyShop(player); 
            }
        }
    }
    
    /**
     * Proses pembelian basic hoe
     */
    private void buyBasicHoe(Player player) {
        double price = 5000; // Harga untuk basic_hoe
        
        if (plugin.getEconomyManager().hasMoney(player, price)) {
            plugin.getEconomyManager().withdrawMoney(player, price);
            ItemStack hoe = plugin.getHoeManager().createHoe("basic_hoe");
            player.getInventory().addItem(hoe);
            player.sendMessage(MessageUtils.colorize("&aBerhasil membeli Basic Farming Hoe!"));
            player.closeInventory();
        } else {
            player.sendMessage(MessageUtils.colorize("&cUang tidak cukup! Anda butuh " + 
                    plugin.getEconomyManager().format(price)));
        }
    }
    
    /**
     * Proses upgrade hoe player
     */
    private void upgradePlayerHoe(Player player) {
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        CustomHoe hoe = plugin.getHoeManager().getHoeFromItemStack(heldItem);
        
        if (hoe == null) {
            player.sendMessage(MessageUtils.colorize("&cSilakan pegang hoe yang ingin di-upgrade di tangan Anda!"));
            player.closeInventory();
            return;
        }
        
        boolean success = plugin.getHoeManager().upgradeHoe(heldItem, player);
        if (success) {
            player.closeInventory();
        }
    }
    
    /**
     * Event handler untuk menutup inventory
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        openShops.remove(player);
        viewingHoe.remove(player);
    }
}