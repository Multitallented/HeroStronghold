package multitallented.plugins.townships.effects;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import multitallented.redcastlemedia.bukkit.townships.Townships;
import multitallented.redcastlemedia.bukkit.townships.Util;
import multitallented.redcastlemedia.bukkit.townships.effect.Effect;
import multitallented.redcastlemedia.bukkit.townships.events.ToTwoSecondEffectEvent;
import multitallented.redcastlemedia.bukkit.townships.region.Region;
import multitallented.redcastlemedia.bukkit.townships.region.RegionType;
import multitallented.redcastlemedia.bukkit.townships.region.TOItem;
import org.bukkit.Location;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author Multitallented
 */
public class EffectScheduledDuplicate extends Effect {
    private HashMap<Location, Long> lastUpkeep = new HashMap<Location, Long>();
    
    public EffectScheduledDuplicate(Townships plugin) {
        super(plugin);
        registerEvent(new UpkeepListener(this));
    }
    
    @Override
    public void init(Townships plugin) {
        super.init(plugin);
    }
    
    public class UpkeepListener implements Listener {
        private final EffectScheduledDuplicate effect;
        public UpkeepListener(EffectScheduledDuplicate effect) {
            this.effect = effect;
        }
        
        
        @EventHandler
        public void onCustomEvent(ToTwoSecondEffectEvent event) {
            if (!event.getEffect()[0].equals("scheduled_duplicate")) {
                return;
            }
            
            Region r = event.getRegion();
            Location l = r.getLocation();
            if (event.getEffect().length < 3) {
                return;
            }
            long period = Long.parseLong(event.getEffect()[1]) * 1000;

            if (lastUpkeep.get(l) == null) {
                //Check to see if the Townships has enough reagents
                if (!effect.hasReagents(l)) {
                    return;
                }

                if (!duplicate(event)) {
                    return;
                }

                //Run upkeep but don't need to know if upkeep occured
                effect.forceUpkeep(l);
                //effect.forceUpkeep(l);
                lastUpkeep.put(l, new Date().getTime());
                return;
            } else if (period + lastUpkeep.get(l) > new Date().getTime()) {
                return;
            }

            //Check to see if the Townships has enough reagents
            if (!effect.hasReagents(l)) {
                return;
            }

            if (!duplicate(event)) {
                return;
            }

            //Run upkeep but don't need to know if upkeep occured
            effect.forceUpkeep(l);
            lastUpkeep.put(l, new Date().getTime());
        }

        public boolean duplicate(ToTwoSecondEffectEvent event) {

            List<TOItem> duplicates = new ArrayList<TOItem>();
            Location l = event.getRegion().getLocation();
            String[] parts = event.getEffect()[2].split(",");
            for (String s : parts) {
                ItemStack currentStack = Util.stringToItemStack(s);
                duplicates.add(new TOItem(currentStack.getType(), currentStack.getTypeId(), 1));
            }
            Inventory inv = null;
            try {
                inv = ((Chest) l.getBlock().getState()).getBlockInventory();
            } catch (Exception e) {
                return false;
            }
            ItemStack is2 = null;
            outer: for (ItemStack is : inv.getContents()) {
                for (TOItem item : duplicates) {
                    if (item.equivalentItem(is, true)) {
                        is2 = is.clone();
                        is2.setAmount(1);
                        break outer;
                    }
                }
            }
            if (is2 == null) {
                return false;
            }
            inv.addItem(is2);
            return true;
        }
    }
}