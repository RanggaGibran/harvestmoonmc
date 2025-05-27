package id.rnggagib.models;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.HashMap;
import java.util.Map;

public class FarmingRegion implements ConfigurationSerializable {
    private String name;
    private String worldName;
    private Location min;
    private Location max;
    
    public FarmingRegion(String name, Location pos1, Location pos2) {
        this.name = name;
        this.worldName = pos1.getWorld().getName();
        
        // Ensure min and max coordinates are correct
        this.min = new Location(
            pos1.getWorld(),
            Math.min(pos1.getX(), pos2.getX()),
            Math.min(pos1.getY(), pos2.getY()),
            Math.min(pos1.getZ(), pos2.getZ())
        );
        
        this.max = new Location(
            pos1.getWorld(),
            Math.max(pos1.getX(), pos2.getX()),
            Math.max(pos1.getY(), pos2.getY()),
            Math.max(pos1.getZ(), pos2.getZ())
        );
    }
    
    // Constructor from serialized data
    public FarmingRegion(Map<String, Object> map) {
        this.name = (String) map.get("name");
        this.worldName = (String) map.get("world");
        
        Map<String, Object> minMap = (Map<String, Object>) map.get("min");
        Map<String, Object> maxMap = (Map<String, Object>) map.get("max");
        
        this.min = new Location(
            org.bukkit.Bukkit.getWorld(worldName),
            (Double) minMap.get("x"),
            (Double) minMap.get("y"),
            (Double) minMap.get("z")
        );
        
        this.max = new Location(
            org.bukkit.Bukkit.getWorld(worldName),
            (Double) maxMap.get("x"),
            (Double) maxMap.get("y"),
            (Double) maxMap.get("z")
        );
    }
    
    public boolean contains(Location location) {
        if (!location.getWorld().getName().equals(worldName)) {
            return false;
        }
        
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        
        return x >= min.getX() && x <= max.getX() &&
               y >= min.getY() && y <= max.getY() &&
               z >= min.getZ() && z <= max.getZ();
    }
    
    public String getName() {
        return name;
    }
    
    public World getWorld() {
        return min.getWorld();
    }
    
    public Location getMin() {
        return min;
    }
    
    public Location getMax() {
        return max;
    }
    
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("world", worldName);
        
        Map<String, Object> minMap = new HashMap<>();
        minMap.put("x", min.getX());
        minMap.put("y", min.getY());
        minMap.put("z", min.getZ());
        map.put("min", minMap);
        
        Map<String, Object> maxMap = new HashMap<>();
        maxMap.put("x", max.getX());
        maxMap.put("y", max.getY());
        maxMap.put("z", max.getZ());
        map.put("max", maxMap);
        
        return map;
    }
}