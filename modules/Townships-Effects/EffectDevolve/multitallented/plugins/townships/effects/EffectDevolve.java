package multitallented.plugins.townships.effects;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.HashMap;
import multitallented.redcastlemedia.bukkit.townships.ConfigManager;
import multitallented.redcastlemedia.bukkit.townships.Townships;
import multitallented.redcastlemedia.bukkit.townships.effect.Effect;
import multitallented.redcastlemedia.bukkit.townships.events.ToPowerChangeEvent;
import multitallented.redcastlemedia.bukkit.townships.events.ToSuperRegionDestroyedEvent;
import multitallented.redcastlemedia.bukkit.townships.region.Region;
import multitallented.redcastlemedia.bukkit.townships.region.RegionManager;
import multitallented.redcastlemedia.bukkit.townships.region.SuperRegion;
import multitallented.redcastlemedia.bukkit.townships.region.SuperRegionType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 *
 * @author Multitallented
 */
public class EffectDevolve extends Effect {
    private final RegionManager rm;
    private final ConfigManager cm;
    private final HashMap<Region, Integer> upkeeps = new HashMap<Region, Integer>();
    private final HashMap<Region, Integer> lastSave = new HashMap<Region, Integer>();
    private Townships plugin;
    public EffectDevolve(Townships plugin) {
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
        private final EffectDevolve effect;
        public IntruderListener(EffectDevolve effect) {
            this.effect = effect;
        }
        
        
        @EventHandler
        public void onCustomEvent(ToPowerChangeEvent event) {
            if (event.isCancelled() || event.getNewPower() > 1) {
                return;
            }
            
            SuperRegion sr = event.getSuperRegion();
            SuperRegionType srt = plugin.getRegionManager().getSuperRegionType(sr.getType());
            if (srt == null) {
                plugin.warning("[Townships] Corrupt super region " + sr.getName() + ".yml");
                return;
            }
            String[] effectString = null;
            for (String s : srt.getEffects()) {
                if (s.split("\\.")[0].equals("devolve")) {
                    effectString = s.split("\\.");
                }
            }
            if (effectString == null || effectString.length < 2) {
                return;
            }
            SuperRegionType devolveTo = plugin.getRegionManager().getSuperRegionType(effectString[1]);
            if (devolveTo == null) {
                plugin.warning("[Townships] Invalid devolve target for " + srt.getName() + ".yml");
                return;
            }
            
            try {
                File regionFolder = new File(getPlugin().getDataFolder(), "superregions");
                File regionFile = new File(regionFolder, sr.getName() + ".yml");
                FileConfiguration rConfig = new YamlConfiguration();
                rConfig.load(regionFile);
                rConfig.set("type", devolveTo.getName());
                rConfig.save(regionFile);
            } catch (IOException | InvalidConfigurationException e) {
                getPlugin().warning("[Townships] unable to save devolved super region in " + sr.getName() + ".yml");
                return;
            }
            
            if (effectString.length > 2) {
                ToSuperRegionDestroyedEvent destroyedEvent = new ToSuperRegionDestroyedEvent(sr, false, true);
                Bukkit.getPluginManager().callEvent(destroyedEvent);
            }
            
            List<Location> childLocations = sr.getChildLocations();
            if (childLocations != null && !childLocations.isEmpty()) {
                Location newLocation = sr.getChildLocations().get(sr.getChildLocations().size() - 1);
                plugin.getRegionManager().setNewSRLocation(sr, newLocation);
            }
            
            plugin.getRegionManager().removeLastChildLocation(sr);
            
            plugin.getRegionManager().setSRType(sr, devolveTo.getName());
            plugin.getRegionManager().setPower(sr, devolveTo.getMaxPower() / 2);
            event.setCancelled(true);
            
            Bukkit.broadcastMessage(ChatColor.WHITE + "[Townships] " + 
                    ChatColor.RED + sr.getName() + 
                    ChatColor.WHITE + " has been damaged and has been reduced to a " + 
                    ChatColor.RED + devolveTo.getName() + ChatColor.WHITE + "!");
        }
    }
}
