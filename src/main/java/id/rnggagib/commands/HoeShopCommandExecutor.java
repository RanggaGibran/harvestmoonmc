package id.rnggagib.commands;

import id.rnggagib.HarvestMoonMC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HoeShopCommandExecutor implements CommandExecutor {
    private final HarvestMoonMC plugin;
    
    public HoeShopCommandExecutor(HarvestMoonMC plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be executed by a player.");
            return true;
        }
        
        Player player = (Player) sender;
        plugin.getHoeShopGUI().openBuyShop(player);
        return true;
    }
}