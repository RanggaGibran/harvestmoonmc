package id.rnggagib.managers;

import id.rnggagib.HarvestMoonMC;
import id.rnggagib.models.PlayerSkill;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class PlaceholderManager extends PlaceholderExpansion {
    private final HarvestMoonMC plugin;

    public PlaceholderManager(HarvestMoonMC plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "df"; // Ubah dari "hmc" menjadi "df"
    }

    @Override
    public String getAuthor() {
        return "rnggagib";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // Plugin ini akan tetap terdaftar sampai server restart
    }

    @Override
    public String onRequest(OfflinePlayer player, String identifier) {
        if (player == null || !player.isOnline()) {
            return "";
        }

        // Placeholder untuk level farming
        if (identifier.equals("farming_level")) {
            PlayerSkill skill = plugin.getSkillManager().getSkill(player.getUniqueId());
            return String.valueOf(skill.getLevel());
        }

        // Placeholder untuk XP farming
        if (identifier.equals("farming_xp")) {
            PlayerSkill skill = plugin.getSkillManager().getSkill(player.getUniqueId());
            return String.valueOf(skill.getXp());
        }

        // Placeholder untuk XP yang dibutuhkan untuk level berikutnya
        if (identifier.equals("farming_xp_next_level")) {
            PlayerSkill skill = plugin.getSkillManager().getSkill(player.getUniqueId());
            return String.valueOf(plugin.getSkillManager().getXpForNextLevel(skill.getLevel()));
        }

        // Placeholder untuk persentase progress ke level berikutnya
        if (identifier.equals("farming_progress_percent")) {
            PlayerSkill skill = plugin.getSkillManager().getSkill(player.getUniqueId());
            int currentXp = skill.getXp();
            int requiredXp = plugin.getSkillManager().getXpForNextLevel(skill.getLevel());
            double progress = (double) currentXp / requiredXp * 100;
            return String.format("%.1f", progress);
        }

        // Placeholder untuk bonus kualitas
        if (identifier.equals("quality_bonus")) {
            double qualityBonus = plugin.getSkillManager().getQualityMultiplier((Player) player);
            return String.format("%.0f", (qualityBonus - 1) * 100);
        }        // Placeholder untuk total panen
        if (identifier.equals("total_harvests")) {
            PlayerSkill skill = plugin.getSkillManager().getSkill(player.getUniqueId());
            return String.valueOf(skill.getTotalHarvests());
        }

        // Placeholder untuk progress bar level (style 1: ████████░░)
        if (identifier.equals("farming_level_bar")) {
            PlayerSkill skill = plugin.getSkillManager().getSkill(player.getUniqueId());
            return createProgressBar(skill, "█", "░", 10);
        }

        // Placeholder untuk progress bar level (style 2: ▰▰▰▰▰▱▱▱▱▱)
        if (identifier.equals("farming_level_bar_2")) {
            PlayerSkill skill = plugin.getSkillManager().getSkill(player.getUniqueId());
            return createProgressBar(skill, "▰", "▱", 10);
        }

        // Placeholder untuk progress bar level (style 3: |||||||---)
        if (identifier.equals("farming_level_bar_3")) {
            PlayerSkill skill = plugin.getSkillManager().getSkill(player.getUniqueId());
            return createProgressBar(skill, "|", "-", 10);
        }

        // Placeholder untuk progress bar panjang (20 karakter)
        if (identifier.equals("farming_level_bar_long")) {
            PlayerSkill skill = plugin.getSkillManager().getSkill(player.getUniqueId());
            return createProgressBar(skill, "█", "░", 20);
        }

        // Placeholder untuk progress bar dengan warna
        if (identifier.equals("farming_level_bar_colored")) {
            PlayerSkill skill = plugin.getSkillManager().getSkill(player.getUniqueId());
            return createColoredProgressBar(skill, 10);
        }

        // Placeholder untuk XP current/required format
        if (identifier.equals("farming_xp_format")) {
            PlayerSkill skill = plugin.getSkillManager().getSkill(player.getUniqueId());
            int currentXp = skill.getXp();
            int requiredXp = plugin.getSkillManager().getXpForNextLevel(skill.getLevel());
            return currentXp + "/" + requiredXp;
        }        return null; // Placeholder tidak ditemukan
    }

    /**
     * Creates a progress bar for the farming level
     * @param skill The player's skill
     * @param filledChar Character for filled portions
     * @param emptyChar Character for empty portions
     * @param barLength Length of the progress bar
     * @return Formatted progress bar string
     */
    private String createProgressBar(PlayerSkill skill, String filledChar, String emptyChar, int barLength) {
        if (skill.getLevel() >= 30) {
            // Max level - show full bar
            StringBuilder bar = new StringBuilder();
            for (int i = 0; i < barLength; i++) {
                bar.append(filledChar);
            }
            return bar.toString();
        }

        int currentXp = skill.getXp();
        int requiredXp = plugin.getSkillManager().getXpForNextLevel(skill.getLevel());
        double progress = (double) currentXp / requiredXp;
        
        int filledBars = (int) (progress * barLength);
        int emptyBars = barLength - filledBars;
        
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < filledBars; i++) {
            bar.append(filledChar);
        }
        for (int i = 0; i < emptyBars; i++) {
            bar.append(emptyChar);
        }
        
        return bar.toString();
    }

    /**
     * Creates a colored progress bar for the farming level
     * @param skill The player's skill
     * @param barLength Length of the progress bar
     * @return Colored progress bar string
     */
    private String createColoredProgressBar(PlayerSkill skill, int barLength) {
        if (skill.getLevel() >= 30) {
            // Max level - show full golden bar
            StringBuilder bar = new StringBuilder("&6");
            for (int i = 0; i < barLength; i++) {
                bar.append("█");
            }
            return bar.toString();
        }

        int currentXp = skill.getXp();
        int requiredXp = plugin.getSkillManager().getXpForNextLevel(skill.getLevel());
        double progress = (double) currentXp / requiredXp;
        
        int filledBars = (int) (progress * barLength);
        int emptyBars = barLength - filledBars;
        
        StringBuilder bar = new StringBuilder();
        
        // Color based on progress
        if (progress < 0.25) {
            bar.append("&c"); // Red for low progress
        } else if (progress < 0.5) {
            bar.append("&e"); // Yellow for medium progress
        } else if (progress < 0.75) {
            bar.append("&a"); // Green for good progress
        } else {
            bar.append("&b"); // Aqua for high progress
        }
        
        // Add filled bars
        for (int i = 0; i < filledBars; i++) {
            bar.append("█");
        }
        
        // Add empty bars in gray
        bar.append("&7");
        for (int i = 0; i < emptyBars; i++) {
            bar.append("█");
        }
        
        return bar.toString();
    }
}