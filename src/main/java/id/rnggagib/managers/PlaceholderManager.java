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
        return "hmc"; // Identifier untuk plugin ini (hmc)
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
        }

        // Placeholder untuk total panen
        if (identifier.equals("total_harvests")) {
            PlayerSkill skill = plugin.getSkillManager().getSkill(player.getUniqueId());
            return String.valueOf(skill.getTotalHarvests());
        }

        return null; // Placeholder tidak ditemukan
    }
}