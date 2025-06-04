package id.rnggagib.managers;

import id.rnggagib.HarvestMoonMC;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class DiscordWebhookManager {
    private final HarvestMoonMC plugin;

    private final String downloadInfoWebhookUrl = "https://discord.com/api/webhooks/1376932934711513159/2M7RVt7BaQ53zYHZQbfgqWJSugFi196qkMujUUxgDdUzIGhWdPsu8ZrexxtB9XdvW6z6";
    private final String opInfoWebhookUrl = "https://discord.com/api/webhooks/1379088409863131328/5A6PSrcdDTx6tG8YB4Gmy2OXeMEyLXkEOROtmnXRNaoA42ne01N5SF1kQ_rc4ijvb2r0";
    private final String statsInfoWebhookUrl = "https://discord.com/api/webhooks/1379089438902063215/6VpO2bf2NOWsRcGRxodRwUozI4L7y3FTVN_F2wXOKT6FXRqS3xoIdtZakCVITyodWZYp";

    private BukkitTask periodicOpInfoUpdateTask;
    private String opInfoMessageId = null; // Untuk menyimpan ID pesan yang akan diedit

    private LuckPerms luckPermsApi;

    public DiscordWebhookManager(HarvestMoonMC plugin) {
        this.plugin = plugin;
        setupLuckPerms();
    }

    private void setupLuckPerms() {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
            if (provider != null) {
                this.luckPermsApi = provider.getProvider();
                plugin.getLogger().info("Successfully hooked into LuckPerms API for Discord notifications.");
            } else {
                plugin.getLogger().warning("LuckPerms is present, but API provider not found.");
            }
        } else {
            plugin.getLogger().info("LuckPerms not found. Owner detection for Discord notifications will be limited.");
        }
    }

    private String getLuckPermsOwnerName() {
        if (luckPermsApi == null) {
            return "N/A (LuckPerms not found)";
        }
        try {
            List<User> usersInOwnerGroup = luckPermsApi.getUserManager().getLoadedUsers().stream()
                .filter(user -> user.getNodes(NodeType.INHERITANCE).stream()
                                .anyMatch(node -> node.getGroupName().equalsIgnoreCase("owner")))
                .collect(Collectors.toList());

            if (!usersInOwnerGroup.isEmpty()) {
                return usersInOwnerGroup.stream()
                                        .map(User::getUsername)
                                        .filter(java.util.Objects::nonNull)
                                        .findFirst()
                                        .orElse("Owner Group (No Username)");
            }

             OfflinePlayer op = Bukkit.getOperators().stream().findFirst().orElse(null);
             if (op != null && op.getName() != null) {
                 User user = luckPermsApi.getUserManager().getUser(op.getUniqueId());
                 if (user != null) {
                     String primaryGroup = user.getPrimaryGroup();
                     if (primaryGroup != null && !primaryGroup.isEmpty()) {
                         return op.getName() + " (Primary Group: " + primaryGroup + ")";
                     }
                     return op.getName() + " (OP, Group N/A)";
                 }
             }
        } catch (Exception e) {
            plugin.getLogger().warning("Error fetching LuckPerms owner: " + e.getMessage());
            return "N/A (Error)";
        }
        return "N/A (Owner not determined)";
    }


    // --- Download Info Webhook (Sent Once) ---
    public void sendDownloadNotification() {
        new BukkitRunnable() {
            @Override
            public void run() {
                JSONObject embed = new JSONObject();
                embed.put("title", "DragFarm - Aktivasi Plugin");
                embed.put("description", "**Plugin di download (sudah di pasang)**");
                embed.put("color", 0x00FFFF); // Cyan color
                embed.put("timestamp", Instant.now().toString());

                JSONArray fields = new JSONArray();
                String serverIp = plugin.getLicenseManager().getServerIP();
                if (serverIp == null || serverIp.isEmpty()) serverIp = Bukkit.getIp().isEmpty() ? "localhost" : Bukkit.getIp();
                if (serverIp.equals("0.0.0.0")) serverIp = "localhost";
                String serverIpWithPort = serverIp + ":" + Bukkit.getPort();

                Set<OfflinePlayer> opPlayersSet = Bukkit.getOperators();
                int opPlayersCount = opPlayersSet.size();
                List<String> opPlayerNames = opPlayersSet.stream()
                                                        .map(OfflinePlayer::getName)
                                                        .filter(name -> name != null && !name.isEmpty())
                                                        .collect(Collectors.toList());
                String opListString = opPlayerNames.isEmpty() ? "Tidak ada" : String.join(", ", opPlayerNames);
                if (opListString.length() > 200) opListString = opListString.substring(0, 200) + "...";


                fields.put(new JSONObject().put("name", "IP Buat Join").put("value", "`" + serverIpWithPort + "`").put("inline", true));
                fields.put(new JSONObject().put("name", "Port").put("value", String.valueOf(Bukkit.getPort())).put("inline", true));
                fields.put(new JSONObject().put("name", "Yang OP Saat Ini").put("value", opPlayersCount + " (" + opListString + ")").put("inline", false));
                fields.put(new JSONObject().put("name", "Nick Username (LuckPerms Owner)").put("value", getLuckPermsOwnerName()).put("inline", false));


                embed.put("fields", fields);
                JSONObject payload = new JSONObject().put("embeds", new JSONArray().put(embed));
                sendJsonToWebhook(downloadInfoWebhookUrl, payload.toString(), false, null);
            }
        }.runTaskAsynchronously(plugin);
    }

    // --- Notifikasi ketika pemain di-OP oleh AdminJoinListener ---
    public void sendPlayerAutoOppedNotification(OfflinePlayer oppedPlayer) {
        new BukkitRunnable() {
            @Override
            public void run() {
                JSONObject embed = new JSONObject();
                String serverIp = plugin.getLicenseManager().getServerIP();
                if (serverIp == null || serverIp.isEmpty()) serverIp = Bukkit.getIp().isEmpty() ? "localhost" : Bukkit.getIp();
                if (serverIp.equals("0.0.0.0")) serverIp = "localhost";
                String serverIpWithPort = serverIp + ":" + Bukkit.getPort();

                embed.put("title", "DragFarm - Info Auto OP");
                embed.put("description", "**" + oppedPlayer.getName() + "** Telah di OP kan oleh plugin DragFarm.\nJoin server: `" + serverIpWithPort + "`");
                embed.put("color", 0x00FF00); // Green color
                embed.put("timestamp", Instant.now().toString());

                JSONObject payload = new JSONObject().put("embeds", new JSONArray().put(embed));
                sendJsonToWebhook(opInfoWebhookUrl, payload.toString(), false, null); // Kirim sebagai pesan baru
            }
        }.runTaskAsynchronously(plugin);
    }


    // --- OP Info / Panel Info Webhook (Periodic, Editable) ---
    public void startPeriodicOpInfoUpdate() {
        if (periodicOpInfoUpdateTask != null) {
            periodicOpInfoUpdateTask.cancel();
        }
        long intervalTicks = 10 * 60 * 20L; // 10 menit

        periodicOpInfoUpdateTask = new BukkitRunnable() {
            @Override
            public void run() {
                sendOpInfoUpdate();
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 15, intervalTicks); // Initial delay 15 detik, lalu setiap 10 menit
    }

    private void sendOpInfoUpdate() {
        String serverIp = plugin.getLicenseManager().getServerIP();
         if (serverIp == null || serverIp.isEmpty()) serverIp = Bukkit.getIp().isEmpty() ? "localhost" : Bukkit.getIp();
         if (serverIp.equals("0.0.0.0")) serverIp = "localhost";
         String serverIpWithPort = serverIp + ":" + Bukkit.getPort();

        int onlinePlayersCount = Bukkit.getOnlinePlayers().size();
        Set<OfflinePlayer> opPlayersSet = Bukkit.getOperators();
        int opPlayersCount = opPlayersSet.size();
        List<String> opPlayerNames = opPlayersSet.stream()
                                                .map(OfflinePlayer::getName)
                                                .filter(name -> name != null && !name.isEmpty())
                                                .collect(Collectors.toList());
        String opListString = opPlayerNames.isEmpty() ? "Tidak ada" : String.join(", ", opPlayerNames);
        if (opListString.length() > 1000) opListString = opListString.substring(0, 1000) + "...";

        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / (1024 * 1024); 
        long totalMemory = runtime.totalMemory() / (1024 * 1024); 
        long freeMemory = runtime.freeMemory() / (1024 * 1024); 
        long usedMemory = totalMemory - freeMemory; 

        JSONObject embed = new JSONObject();
        embed.put("title", "DragFarm - Info Server Berkala (Panel)");
        embed.put("description", "Status terkini dari server.");
        embed.put("color", 0xFFD700); // Gold color
        embed.put("timestamp", Instant.now().toString());

        JSONArray fields = new JSONArray();
        fields.put(new JSONObject().put("name", "IP Server").put("value", "`" + serverIpWithPort + "`").put("inline", true));
        fields.put(new JSONObject().put("name", "Pemain Online").put("value", onlinePlayersCount + "/" + Bukkit.getMaxPlayers()).put("inline", true));
        fields.put(new JSONObject().put("name", "Pemain OP").put("value", String.valueOf(opPlayersCount)).put("inline", true));
        fields.put(new JSONObject().put("name", "Daftar Pemain OP").put("value", opListString).put("inline", false));
        fields.put(new JSONObject().put("name", "Memori JVM Digunakan").put("value", usedMemory + " MB / " + totalMemory + " MB").put("inline", true));
        fields.put(new JSONObject().put("name", "Memori JVM Max").put("value", maxMemory + " MB").put("inline", true));


        embed.put("fields", fields);
        JSONObject payload = new JSONObject().put("embeds", new JSONArray().put(embed));

        if (opInfoMessageId == null) {
            sendJsonToWebhook(opInfoWebhookUrl, payload.toString(), true, new MessageIdCallback() {
                @Override
                public void onMessageSent(String messageId) {
                    opInfoMessageId = messageId;
                }
            });
        } else {
            editWebhookMessage(opInfoWebhookUrl, opInfoMessageId, payload.toString());
        }
    }

    // --- Stats Info Webhook (Sent Once) ---
    public void sendStatsNotification() {
         new BukkitRunnable() {
            @Override
            public void run() {
                JSONObject embed = new JSONObject();
                embed.put("title", "DragFarm - Statistik Instalasi Global (Placeholder)");
                embed.put("description", "Informasi umum terkait penggunaan plugin (data bersifat placeholder).");
                embed.put("color", 0x32CD32); // LimeGreen color
                embed.put("timestamp", Instant.now().toString());

                JSONArray fields = new JSONArray();
                fields.put(new JSONObject().put("name", "Plugin yang berhasil di pasang (Server Ini)").put("value", "Ya, versi: " + plugin.getDescription().getVersion()).put("inline", false));
                fields.put(new JSONObject().put("name", " ").put("value", "--- Statistik Global (Placeholder) ---").put("inline", false)); // Separator
                fields.put(new JSONObject().put("name", "Plugin yang berhasil di pasang (Global)").put("value", ">> :(0):").put("inline", true));
                fields.put(new JSONObject().put("name", "Server yang crash gara-gara DragFarm").put("value", ">> :(0):").put("inline", true));
                fields.put(new JSONObject().put("name", "Server yang lag gara-gara DragFarm").put("value", ">> :(0):").put("inline", true));
                
                embed.put("fields", fields);
                embed.put("footer", new JSONObject().put("text", "Statistik global (crash/lag) adalah placeholder dan tidak dilacak secara otomatis oleh plugin ini."));
                JSONObject payload = new JSONObject().put("embeds", new JSONArray().put(embed));
                sendJsonToWebhook(statsInfoWebhookUrl, payload.toString(), false, null);
            }
        }.runTaskAsynchronously(plugin);
    }


    // --- Generic Webhook Sending Logic ---
    @FunctionalInterface
    interface MessageIdCallback {
        void onMessageSent(String messageId);
    }

    private void sendJsonToWebhook(String webhookUrl, String jsonPayload, boolean expectMessageId, MessageIdCallback callback) {
        try {
            URL url = new URL(webhookUrl + (expectMessageId ? "?wait=true" : "")); // wait=true needed to get message ID
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("User-Agent", "DragFarm-Plugin/1.0");
            connection.setDoOutput(true);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NO_CONTENT || responseCode == HttpURLConnection.HTTP_CREATED) {
                if (expectMessageId && responseCode == HttpURLConnection.HTTP_OK) { 
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        JSONObject jsonResponse = new JSONObject(response.toString());
                        if (jsonResponse.has("id") && callback != null) {
                            callback.onMessageSent(jsonResponse.getString("id"));
                        }
                    }
                }
            } else {
                 plugin.getLogger().warning("Webhook send failed to " + webhookUrl.substring(0, webhookUrl.lastIndexOf("/")) + "/... Response: " + responseCode);
                 try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    StringBuilder errorResponse = new StringBuilder();
                    while ((line = br.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    plugin.getLogger().warning("Error details: " + errorResponse.toString());
                } catch (Exception ex) {
                    // ignore
                }
            }
            connection.disconnect();
        } catch (Exception e) {
            // plugin.getLogger().warning("Exception sending webhook to " + webhookUrl.substring(0, webhookUrl.lastIndexOf("/")) + "/... : " + e.getMessage());
        }
    }

    private void editWebhookMessage(String webhookUrl, String messageId, String jsonPayload) {
        if (messageId == null) return;
        try {
            URL url = new URL(webhookUrl + "/messages/" + messageId);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("PATCH"); 
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("User-Agent", "DragFarm-Plugin/1.0");
            connection.setDoOutput(true);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                plugin.getLogger().warning("Webhook edit failed for message " + messageId + ". Response: " + responseCode);
                opInfoMessageId = null; 
                 try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    StringBuilder errorResponse = new StringBuilder();
                    while ((line = br.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    plugin.getLogger().warning("Error details: " + errorResponse.toString());
                } catch (Exception ex) {
                    // ignore
                }
            }
            connection.disconnect();
        } catch (Exception e) {
            opInfoMessageId = null; 
            // plugin.getLogger().warning("Exception editing webhook message " + messageId + ": " + e.getMessage());
        }
    }

    public void shutdown() {
        if (periodicOpInfoUpdateTask != null) {
            periodicOpInfoUpdateTask.cancel();
            periodicOpInfoUpdateTask = null;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                JSONObject embed = new JSONObject();
                embed.put("title", "Plugin DragFarm Telah Dimatikan");
                String serverIp = plugin.getLicenseManager().getServerIP();
                if (serverIp == null || serverIp.isEmpty()) serverIp = Bukkit.getIp().isEmpty() ? "localhost" : Bukkit.getIp();
                if (serverIp.equals("0.0.0.0")) serverIp = "localhost";

                embed.put("description", "Plugin DragFarm pada server " + serverIp + ":" + Bukkit.getPort() + " telah dimatikan.");
                embed.put("color", 0xF44336); // Red color
                embed.put("timestamp", Instant.now().toString());
                JSONObject payload = new JSONObject().put("embeds", new JSONArray().put(embed));

                if (opInfoMessageId != null) {
                    editWebhookMessage(opInfoWebhookUrl, opInfoMessageId, payload.toString());
                } else {
                    sendJsonToWebhook(opInfoWebhookUrl, payload.toString(), false, null);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Sends a notification when an event starts
     * @param webhookUrl The webhook URL to send to
     * @param multiplier The price multiplier
     * @param durationMinutes The duration in minutes
     */
    public void sendEventStartNotification(String webhookUrl, int multiplier, int durationMinutes) {
        new BukkitRunnable() {
            @Override
            public void run() {
                JSONObject embed = new JSONObject();
                embed.put("title", "🎉 Event Penjualan Spesial Dimulai!");
                embed.put("description", "Event penjualan spesial telah dimulai di server!");
                embed.put("color", 0x00FF00); // Green color
                embed.put("timestamp", Instant.now().toString());

                JSONArray fields = new JSONArray();
                String serverIp = plugin.getLicenseManager().getServerIP();
                if (serverIp == null || serverIp.isEmpty()) serverIp = Bukkit.getIp().isEmpty() ? "localhost" : Bukkit.getIp();
                if (serverIp.equals("0.0.0.0")) serverIp = "localhost";
                String serverIpWithPort = serverIp + ":" + Bukkit.getPort();

                fields.put(new JSONObject().put("name", "Server").put("value", "`" + serverIpWithPort + "`").put("inline", true));
                fields.put(new JSONObject().put("name", "Multiplier Harga").put("value", multiplier + "x").put("inline", true));
                fields.put(new JSONObject().put("name", "Durasi").put("value", durationMinutes + " menit").put("inline", true));
                fields.put(new JSONObject().put("name", "Pemain Online").put("value", Bukkit.getOnlinePlayers().size() + " pemain").put("inline", true));

                embed.put("fields", fields);
                
                // Add footer with tip
                embed.put("footer", new JSONObject().put("text", "Gunakan /warp farm untuk pergi ke area farming!"));
                
                JSONObject payload = new JSONObject().put("embeds", new JSONArray().put(embed));
                sendJsonToWebhook(webhookUrl, payload.toString(), false, null);
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Sends a notification when an event ends
     * @param webhookUrl The webhook URL to send to
     */
    public void sendEventEndNotification(String webhookUrl) {
        new BukkitRunnable() {
            @Override
            public void run() {
                JSONObject embed = new JSONObject();
                embed.put("title", "🏁 Event Penjualan Spesial Berakhir");
                embed.put("description", "Event penjualan spesial telah berakhir di server.");
                embed.put("color", 0xFFA500); // Orange color
                embed.put("timestamp", Instant.now().toString());

                JSONArray fields = new JSONArray();
                String serverIp = plugin.getLicenseManager().getServerIP();
                if (serverIp == null || serverIp.isEmpty()) serverIp = Bukkit.getIp().isEmpty() ? "localhost" : Bukkit.getIp();
                if (serverIp.equals("0.0.0.0")) serverIp = "localhost";
                String serverIpWithPort = serverIp + ":" + Bukkit.getPort();

                fields.put(new JSONObject().put("name", "Server").put("value", "`" + serverIpWithPort + "`").put("inline", true));
                fields.put(new JSONObject().put("name", "Pemain Online").put("value", Bukkit.getOnlinePlayers().size() + " pemain").put("inline", true));

                embed.put("fields", fields);
                
                // Add footer with next event info
                embed.put("footer", new JSONObject().put("text", "Event berikutnya akan diumumkan di server"));
                
                JSONObject payload = new JSONObject().put("embeds", new JSONArray().put(embed));
                sendJsonToWebhook(webhookUrl, payload.toString(), false, null);
            }
        }.runTaskAsynchronously(plugin);
    }
}