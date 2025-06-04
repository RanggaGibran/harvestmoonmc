package id.rnggagib.commands;

import id.rnggagib.HarvestMoonMC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FarmCommandExecutor implements CommandExecutor {
    // private final HarvestMoonMC plugin; // Not strictly needed if only performing a command

    public FarmCommandExecutor(HarvestMoonMC plugin) {
        // this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }
        Player player = (Player) sender;
        
        // Ensure the player has permission for /warp farm if your warp plugin requires it
        // For example, if using Essentials: if (!player.hasPermission("essentials.warp.farm")) { ... }
        
        player.performCommand("warp farm"); 
        return true;
    }
}