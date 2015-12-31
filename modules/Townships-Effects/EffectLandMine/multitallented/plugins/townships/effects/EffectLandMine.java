package multitallented.plugins.townships.effects;

import java.util.ArrayList;
import java.util.List;
import multitallented.redcastlemedia.bukkit.townships.Townships;
import multitallented.redcastlemedia.bukkit.townships.effect.Effect;
import multitallented.redcastlemedia.bukkit.townships.events.ToPlayerInRegionEvent;
import multitallented.redcastlemedia.bukkit.townships.region.RegionManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 *
 * @author Multitallented
 */
public class EffectLandMine extends Effect {
    private final Townships plugin;
    public EffectLandMine(Townships plugin) {
        super(plugin);
        this.plugin = plugin;
        registerEvent(new IntruderListener(this));
    }
    
    @Override
    public void init(Townships plugin) {
        super.init(plugin);
    }
    
    public class IntruderListener implements Listener {
        private final EffectLandMine effect;
        public IntruderListener(EffectLandMine effect) {
            this.effect = effect;
        }
        
        @EventHandler
        public void onCustomEvent(ToPlayerInRegionEvent event) {
            RegionManager rm = plugin.getRegionManager();
            Player player = event.getPlayer();
            
            Location l = event.getLocation();
            List<String> effects = rm.getRegionType(rm.getRegion(l).getType()).getEffects();

            //Check if the region has the shoot arrow effect and return arrow velocity
            int explode = effect.regionHasEffect(effects, "landmine");
            if (explode == 0)
                return;
            
            
            //Check if the player owns or is a member of the region
            if (effect.isOwnerOfRegion(player, l) || effect.isMemberOfRegion(player, l)) {
                return;
            }
            
            //Check to see if the Townships has enough reagents
            if (!effect.hasReagents(l))
                return;
            
            //Run upkeep but don't need to know if upkeep occured
            effect.forceUpkeep(event);
            
            //Check to see if exploding regions are enabled
            if (!Townships.getConfigManager().getExplode()) {
                rm.destroyRegion(l);
                l.getBlock().setType(Material.AIR);
                TNTPrimed tnt = l.getWorld().spawn(l, TNTPrimed.class);
                tnt.setFuseTicks(1);
            }
            
            //Set the event to destroy the region
            List<Location> tempArray = new ArrayList<Location>();
            tempArray.add(l);
            event.setRegionsToDestroy(tempArray);
        }
    }
    
}
