package multitallented.plugins.townships.effects;

import java.util.List;
import multitallented.redcastlemedia.bukkit.townships.Townships;
import multitallented.redcastlemedia.bukkit.townships.effect.Effect;
import multitallented.redcastlemedia.bukkit.townships.events.ToPlayerInRegionEvent;
import multitallented.redcastlemedia.bukkit.townships.region.RegionManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
/**
 *
 * @author Multitallented
 */
public class EffectGainStamina extends Effect {
    private final RegionManager rm;
    public EffectGainStamina(Townships plugin) {
        super(plugin);
        this.rm = plugin.getRegionManager();
        registerEvent(new IntruderListener(this));
    }
    
    @Override
    public void init(Townships plugin) {
        super.init(plugin);
    }
    
    public class IntruderListener implements Listener {
        private final EffectGainStamina effect;
        public IntruderListener(EffectGainStamina effect) {
            this.effect = effect;
        }
        
        @EventHandler
        public void onCustomEvent(ToPlayerInRegionEvent event) {
            Player player = event.getPlayer();
            if (player.getFoodLevel() == 20)
                return;
            
            Location l = event.getLocation();
            List<String> effects = effect.rm.getRegionType(effect.rm.getRegion(l).getType()).getEffects();
            //Check if the region has the shoot arrow effect and return arrow velocity
            int food = effect.regionHasEffect(effects, "gain_stamina");
            if (food == 0)
                return;
            
            
            //Check if the player owns or is a member of the region
            if (!effect.isOwnerOfRegion(player, l) && !effect.isMemberOfRegion(player, l)) {
                return;
            }
            
            //Check to see if the Townships has enough reagents
            if (!effect.hasReagents(l))
                return;
            
            //Run upkeep but don't need to know if upkeep occured
            effect.forceUpkeep(event);
            
            //grant the player food
            if (player.getFoodLevel() + food <= 20) {
                player.setFoodLevel(player.getFoodLevel() + food);
            } else {
                player.setFoodLevel(20);
            }
        }
    }
    
}
