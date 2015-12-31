package multitallented.plugins.townships.effects;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import multitallented.redcastlemedia.bukkit.townships.ConfigManager;
import multitallented.redcastlemedia.bukkit.townships.Townships;
import multitallented.redcastlemedia.bukkit.townships.effect.Effect;
import multitallented.redcastlemedia.bukkit.townships.events.ToUpkeepSuccessEvent;
import multitallented.redcastlemedia.bukkit.townships.region.Region;
import multitallented.redcastlemedia.bukkit.townships.region.RegionManager;
import multitallented.redcastlemedia.bukkit.townships.region.RegionType;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;

/**
 *
 * @author Multitallented
 */
public class EffectEvolve extends Effect {
    private final RegionManager rm;
    private final ConfigManager cm;
    //private final HashMap<String, String> evolutions = new HashMap<String, String>();
    private final HashMap<Region, Integer> upkeeps = new HashMap<Region, Integer>();
    private final HashMap<Region, Integer> lastSave = new HashMap<Region, Integer>();
    private Townships plugin;
    public EffectEvolve(Townships plugin) {
        super(plugin);
        this.rm = plugin.getRegionManager();
        this.cm = Townships.getConfigManager();
        registerEvent(new IntruderListener(this));
    }
    
    /**
     * 
     * @param plugin 
     */
    @Override
    public void init(Townships plugin) {
        super.init(plugin);
        this.plugin = plugin;
    }
    
    public class IntruderListener implements Listener {
        private final EffectEvolve effect;
        public IntruderListener(EffectEvolve effect) {
            this.effect = effect;
        }
        
        /**
         * If the region has the "evolve" effect, it will track the number
         * of successful upkeeps until the max upkeeps is reached. When the max
         * upkeeps are reached, it will destroy the region, and create a new one.
         * Region evolutions are defined in evolutions.yml and upkeeps are saved
         * once every 10 successful upkeeps to the region file in the data folder.
         * @param event 
         */
        @EventHandler
        public void onCustomEvent(ToUpkeepSuccessEvent event) {
            Location l = event.getRegionLocation();
            Region r = rm.getRegion(l);
            RegionType rt = rm.getRegionType(r.getType());

            
            int evolve = 0;
            String evolveTarget = null;
            for (String currentEffect : rt.getEffects()) {
                if (!currentEffect.startsWith("evolve.")) {
                    continue;
                }
                String[] evolveParts = currentEffect.split("\\.");
                
                if (evolveParts.length < 2) {
                    continue;
                }
                evolveTarget = evolveParts[1];
                
                if (rm.getRegionType(evolveTarget) == null) {
                    return;
                }
                
                try {
                    evolve = Integer.parseInt(evolveParts[2]);
                } catch (Exception e) {
                    return;
                }
            }
            if (evolveTarget == null) {
                return;
            }
            
            //Get number of successful upkeeps and increase by one
            if (upkeeps.containsKey(r)) {
                upkeeps.put(r, upkeeps.get(r) + 1);
            } else {
                File regionFolder = new File(plugin.getDataFolder(), "data");
                File regionFile = new File(regionFolder, r.getID() + ".yml");
                if (!regionFile.exists()) {
                    return;
                }
                int successes = -1;
                FileConfiguration rConfig = new YamlConfiguration();
                try {
                    rConfig.load(regionFile);
                    successes = rConfig.getInt("successful-upkeeps", -1);
                } catch (Exception e) {
                    return;
                }
                upkeeps.put(r, successes + 1);
            }
            
            //Check if upkeeps limit reached
            //If reached, evolve the region
            //If not, check if needs to be saved
            if (upkeeps.get(r) >= evolve) {
                for (OfflinePlayer s : r.getOwners()) {
                    Player player = s.getPlayer();
                    if (player != null) {
                        player.sendMessage(ChatColor.GRAY + "[Townships] " + ChatColor.WHITE + "Your " + 
                                ChatColor.RED + WordUtils.capitalize(r.getType()) + 
                                ChatColor.WHITE + " at " + 
                                ChatColor.RED + Math.floor(r.getLocation().getX()) + "x, " + 
                                Math.floor(r.getLocation().getY()) + "y, " + 
                                Math.floor(r.getLocation().getZ()) + "z "+ 
                                ChatColor.WHITE + "is evolving.");
                    }
                }
                
                
                //List<Location> regions = event.getEvent().getRegionsToDestroy();
                //regions.add(l);
                //event.getEvent().setRegionsToDestroy(regions);
                //List<Region> cRegions = event.getEvent().getRegionsToCreate();
                //List<String> owners = r.getOwners();
                //List<String> members = r.getMembers();
                //cRegions.add(new Region(r.getID(), l, evolveTarget, owners, members));
                //event.getEvent().setRegionsToCreate(cRegions);
                
                File regionFolder = new File(getPlugin().getDataFolder(), "data");
                File regionFile = new File(regionFolder, r.getID() + ".yml");
                FileConfiguration rConfig = new YamlConfiguration();       
                try {
                    rConfig.load(regionFile);
                    rConfig.set("type", evolveTarget);
                    rConfig.set("successful-upkeeps", 0);
                    rConfig.save(regionFile);
                } catch (IOException | InvalidConfigurationException e) {
                    getPlugin().warning("[Townships] unable to save evolve in " + r.getID() + ".yml");
                    return;
                }
                r.setType(evolveTarget);
                
                upkeeps.remove(r);
                lastSave.remove(r);
                for (OfflinePlayer s : r.getOwners()) {
                    Player player = s.getPlayer();
                    if (player != null) {
                        player.sendMessage(ChatColor.GRAY + "[Townships] " + ChatColor.WHITE + "Your " + 
                                ChatColor.RED + WordUtils.capitalize(r.getType()) + 
                                " " + r.getID() + ChatColor.WHITE + " has evolved.");
                    }
                }
               
            } else {
                if (lastSave.containsKey(r)) {
                    lastSave.put(r, lastSave.get(r) + 1);
                    if (lastSave.get(r) > 9) {
                        File regionFolder = new File(plugin.getDataFolder(), "data");
                        File regionFile = new File(regionFolder, r.getID() + ".yml");
                        if (!regionFile.exists()) {
                            return;
                        }
                        FileConfiguration rConfig = new YamlConfiguration();
                        try {
                            rConfig.load(regionFile);
                            rConfig.set("successful-upkeeps", upkeeps.get(r));
                            rConfig.save(regionFile);
                            lastSave.put(r, 0);
                        } catch (Exception e) {
                            return;
                        }
                    }
                } else {
                    lastSave.put(r, 1);
                }
            }
        }
        
        /**
         * This method attempts to save all unsaved successful upkeeps to their
         * region files when the plugin is disabled.
         * @param event 
         */
        @EventHandler
        public void onPluginDisable(PluginDisableEvent event) {
            if (!event.getPlugin().getDescription().getName().equalsIgnoreCase("Townships")) {
                return;
            }
            System.out.println("[Townships] Saving all region evolutions...");
            File regionFolder = new File(plugin.getDataFolder(), "data");
            for (Region r : lastSave.keySet()) {
                if (lastSave.get(r) > 0) {
                    //Save the upkeeps
                    File regionFile = new File(regionFolder, r.getID() + ".yml");
                    if (!regionFile.exists()) {
                        continue;
                    }
                    FileConfiguration rConfig = new YamlConfiguration();
                    try {
                        rConfig.load(regionFile);
                        rConfig.set("successful-upkeeps", upkeeps.get(r));
                        rConfig.save(regionFile);
                    } catch (Exception e) {
                        continue;
                    }
                }
            }
            System.out.println("[Townships] All region evolutions saved.");
        }
    }
    
}
