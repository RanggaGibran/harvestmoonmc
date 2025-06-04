package id.rnggagib.commands;

import id.rnggagib.HarvestMoonMC;
import id.rnggagib.models.FarmingRegion;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DragFarmTabCompleter implements TabCompleter {
    private final HarvestMoonMC plugin;
    private final List<String> MAIN_COMMANDS = Arrays.asList(
            "wand", "create", "list", "delete", "info", "shop", "sell", "stats", "event", "reload", "hoe", "hoeshop");
    private final List<String> EVENT_COMMANDS = Arrays.asList("start", "stop", "reload");
    private final List<String> SELL_COMMANDS = Arrays.asList("hand", "all");
    private final List<String> ADMIN_COMMANDS = Arrays.asList("wand", "create", "delete", "reload");
    private final List<String> PLAYER_COMMANDS = Arrays.asList("shop", "sell", "stats", "event");
    private final List<String> HOE_COMMANDS = Arrays.asList("get", "list", "upgrade", "reload", "shop", "upgradegui", "give");

    public DragFarmTabCompleter(HarvestMoonMC plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Filter command suggestions based on permissions
            List<String> suggestions = new ArrayList<>();
            
            if (sender.hasPermission("dragfarm.admin.wand")) {
                suggestions.addAll(ADMIN_COMMANDS);
            }
            
            suggestions.addAll(PLAYER_COMMANDS);
            suggestions.add("hoe");
            
            // Return commands that start with the argument
            StringUtil.copyPartialMatches(args[0], suggestions, completions);
        } else if (args.length == 2) {
            // Handle subcommand completions
            switch (args[0].toLowerCase()) {
                case "create":
                    // Suggest "region" as name placeholder
                    completions.add("region_name");
                    break;
                case "delete":
                case "info":
                    // Suggest existing region names
                    if (sender.hasPermission("dragfarm.admin.list")) {
                        completions.addAll(getRegionNames());
                    }
                    break;
                case "event":
                    // Suggest event subcommands if admin
                    if (sender.hasPermission("dragfarm.admin.event")) {
                        StringUtil.copyPartialMatches(args[1], EVENT_COMMANDS, completions);
                    }
                    break;
                case "sell":
                    // Suggest sell options
                    StringUtil.copyPartialMatches(args[1], SELL_COMMANDS, completions);
                    break;
                case "hoe":
                    // Suggest hoe subcommands
                    List<String> hoeCommands = new ArrayList<>();
                    hoeCommands.add("upgrade");
                    
                    if (sender.hasPermission("dragfarm.admin.hoe")) {
                        hoeCommands.add("get");
                        hoeCommands.add("list");
                        hoeCommands.add("reload");
                    }
                    
                    StringUtil.copyPartialMatches(args[1], hoeCommands, completions);
                    break;
            }
        } else if (args.length == 3) {
            // Handle third argument completions
            if (args[0].equalsIgnoreCase("event") && args[1].equalsIgnoreCase("start")) {
                // Suggest multiplier values for event start
                if (sender.hasPermission("dragfarm.admin.event")) {
                    completions.addAll(Arrays.asList("2", "3", "4"));
                }
            } else if (args[0].equalsIgnoreCase("hoe") && args[1].equalsIgnoreCase("get")) {
                // Suggest hoe IDs
                if (sender.hasPermission("dragfarm.admin.hoe")) {
                    StringUtil.copyPartialMatches(args[2], plugin.getHoeManager().getAllHoeIds(), completions);
                }
            } else if (args[0].equalsIgnoreCase("hoe") && args[1].equalsIgnoreCase("give")) {
                if (args.length == 3) {
                    // Suggest online players
                    if (sender.hasPermission("dragfarm.admin.hoe.give")) {
                        return null; // Returns all online players
                    }
                } else if (args.length == 4) {
                    // Suggest hoe IDs
                    if (sender.hasPermission("dragfarm.admin.hoe.give")) {
                        StringUtil.copyPartialMatches(args[3], plugin.getHoeManager().getAllHoeIds(), completions);
                    }
                }
            }
        } else if (args.length == 4) {
            // Handle fourth argument completions
            if (args[0].equalsIgnoreCase("event") && args[1].equalsIgnoreCase("start")) {
                // Suggest duration values for event start
                if (sender.hasPermission("dragfarm.admin.event")) {
                    completions.addAll(Arrays.asList("5", "10", "15", "30"));
                }
            }
        }
        
        // Sort completions alphabetically
        Collections.sort(completions);
        return completions;
    }
    
    /**
     * Gets all farming region names
     * @return List of region names
     */
    private List<String> getRegionNames() {
        // Replace getRegions() with getAllRegions().values()
        return plugin.getRegionManager().getAllRegions().values().stream()
                .map(FarmingRegion::getName)
                .collect(Collectors.toList());
    }
}