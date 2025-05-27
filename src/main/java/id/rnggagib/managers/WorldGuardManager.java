package id.rnggagib.managers;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import id.rnggagib.HarvestMoonMC;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

public class WorldGuardManager {
    private final HarvestMoonMC plugin;
    private boolean worldGuardEnabled = false;
    
    public WorldGuardManager(HarvestMoonMC plugin) {
        this.plugin = plugin;
        this.worldGuardEnabled = WorldGuardPlugin.inst() != null;
        
        if (worldGuardEnabled) {
            plugin.getLogger().info("WorldGuard manager initialized");
        } else {
            plugin.getLogger().warning("WorldGuard not found. Farming regions may not work in protected areas.");
        }
    }
    
    /**
     * Checks if the player can break the specified block, considering WorldGuard protection
     * @param player The player
     * @param block The block
     * @return true if the player can break the block, false otherwise
     */
    public boolean canBreakWheat(Player player, Block block) {
        if (!worldGuardEnabled) return true;
        if (block.getType() != Material.WHEAT) return false;
        
        try {
            Location location = block.getLocation();
            
            // Check if the block is in our farming region
            if (plugin.getRegionManager().getRegionAt(location) != null) {
                // If it's in our farming region AND it's wheat, bypass WorldGuard
                return true;
            }
            
            // Gunakan testBuild langsung untuk pemeriksaan standar
            return WorldGuard.getInstance().getPlatform().getRegionContainer()
                    .createQuery()
                    .testBuild(BukkitAdapter.adapt(location), WorldGuardPlugin.inst().wrapPlayer(player));
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking WorldGuard protection: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Sets the custom flag on a region
     */
    public void setWheatFarmingFlag(String regionId, boolean allow) {
        // Implementasi untuk mengatur flag pada region
        // ...
    }
}