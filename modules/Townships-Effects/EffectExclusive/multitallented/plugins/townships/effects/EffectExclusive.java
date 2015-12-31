package multitallented.plugins.townships.effects;

import java.util.ArrayList;
import java.util.List;
import multitallented.redcastlemedia.bukkit.townships.Townships;
import multitallented.redcastlemedia.bukkit.townships.effect.Effect;
import multitallented.redcastlemedia.bukkit.townships.events.ToPreRegionCreatedEvent;
import multitallented.redcastlemedia.bukkit.townships.region.Region;
import multitallented.redcastlemedia.bukkit.townships.region.RegionManager;
import multitallented.redcastlemedia.bukkit.townships.region.RegionType;
import multitallented.redcastlemedia.bukkit.townships.region.SuperRegion;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 *
 * @author Multitallented
 */
public class EffectExclusive extends Effect {
    public EffectExclusive(Townships plugin) {
        super(plugin);
        registerEvent(new EffectExclusive.UpkeepListener(this));
    }
    
    @Override
    public void init(Townships plugin) {
        super.init(plugin);
    }
    
    public class UpkeepListener implements Listener {
        private final EffectExclusive effect;
        public UpkeepListener(EffectExclusive effect) {
            this.effect = effect;
        }
        
        @EventHandler
        public void onRegionCreated(ToPreRegionCreatedEvent event) {
            Location l = event.getLocation();
            RegionManager rm = getPlugin().getRegionManager();
            RegionType rt = event.getRegionType();
            Player player = event.getPlayer();
            String[] parts;
            List<String> exclusions = new ArrayList<String>();
            for (String s : rt.getEffects()) {
                parts = s.split("\\.");
                if (parts.length > 1 && parts[0].equals("exclusive")) {
                    exclusions.add(parts[1]);
                }
            }
            if (exclusions.isEmpty()) {
                return;
            }
            for (SuperRegion sr : rm.getContainingSuperRegions(l)) {
                for (Region r : rm.getContainedRegions(sr)) {
                    if (exclusions.contains(r.getType())) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.RED + "[Townships] You can't make this in the same super region as a " + r.getType());
                        return;
                    }
                }
            }
        }
    }
}