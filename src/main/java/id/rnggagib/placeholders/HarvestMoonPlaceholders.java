package id.rnggagib.placeholders;

import id.rnggagib.HarvestMoonMC;
import id.rnggagib.models.PlayerSkill;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class HarvestMoonPlaceholders extends PlaceholderExpansion {
    
    private final HarvestMoonMC plugin;
    
    public HarvestMoonPlaceholders(HarvestMoonMC plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getIdentifier() {
        return "harvestmoon";
    }
    
    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }
    
    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }
        
        // %harvestmoon_farming_level%
        if (identifier.equals("farming_level")) {
            PlayerSkill skill = plugin.getSkillManager().getSkill(player);
            return String.valueOf(skill.getLevel());
        }
        
        // %harvestmoon_farming_xp%
        if (identifier.equals("farming_xp")) {
            PlayerSkill skill = plugin.getSkillManager().getSkill(player);
            return String.valueOf(skill.getXp());
        }
        
        // %harvestmoon_farming_next_level_xp%
        if (identifier.equals("farming_next_level_xp")) {
            PlayerSkill skill = plugin.getSkillManager().getSkill(player);
            return String.valueOf(skill.getXpForNextLevel());
        }
        
        // %harvestmoon_farming_progress%
        if (identifier.equals("farming_progress")) {
            PlayerSkill skill = plugin.getSkillManager().getSkill(player);
            return String.format("%.1f", skill.getProgressToNextLevel() * 100);
        }
        
        return null;
    }
}