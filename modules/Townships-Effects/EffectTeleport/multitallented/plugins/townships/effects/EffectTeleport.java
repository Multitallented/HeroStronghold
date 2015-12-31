package multitallented.plugins.townships.effects;

import multitallented.redcastlemedia.bukkit.townships.Townships;
import multitallented.redcastlemedia.bukkit.townships.effect.Effect;
import multitallented.redcastlemedia.bukkit.townships.events.ToPlayerInRegionEvent;
import multitallented.redcastlemedia.bukkit.townships.region.Region;
import multitallented.redcastlemedia.bukkit.townships.region.RegionManager;
import multitallented.redcastlemedia.bukkit.townships.region.RegionType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 *
 * @author Multitallented
 */
public class EffectTeleport extends Effect {
    
    public EffectTeleport(Townships plugin) {
        super(plugin);
        registerEvent(new TeleportListener(this));
    }
    
    @Override
    public void init(Townships plugin) {
        super.init(plugin);
    }
    
    public class TeleportListener implements Listener {
        private final EffectTeleport effect;
        public TeleportListener(EffectTeleport effect) {
            this.effect = effect;
        }
        //TODO add sign data so that I can switch exit teleporters with a sign
        
        @EventHandler
        public void onCustomEvent(ToPlayerInRegionEvent event) {
            if (!event.getLocation().getBlock().getRelative(BlockFace.UP).equals(event.getPlayer().getLocation().getBlock()) &&
                    !event.getLocation().getBlock().equals(event.getPlayer().getLocation().getBlock())) {
                return;
            }
            Location l = event.getLocation();
            RegionManager rm = getPlugin().getRegionManager();
            Region r = rm.getRegion(l);
            RegionType rt = rm.getRegionType(r.getType());
            
            //Check if the region is a teleporter
            if (effect.regionHasEffect(rt.getEffects(), "teleport") == 0) {
                return;
            }
            
            Block block = l.getBlock().getRelative(BlockFace.UP);
            if (!(block.getState() instanceof Sign)) {
                return;
            }

            Sign sign = (Sign) block.getState();
            String destinationRegionId = sign.getLine(0);
            if (destinationRegionId.equalsIgnoreCase("[Teleport]")) {
                destinationRegionId = sign.getLine(1);
            }
            Region currentRegion = null;
            try {
                currentRegion = rm.getRegionByID(Integer.parseInt(destinationRegionId));
            } catch (Exception e) {
                return;
            }
            if (currentRegion == null) {
                return;
            }
            
            if (r.getOwners().isEmpty() || currentRegion.getOwners().isEmpty()) {
                return;
            }
            //TODO add more error messages
            boolean ownerCheck = false;
            for (OfflinePlayer s : r.getOwners()) {
                if (currentRegion.isOwner(s)) {
                    ownerCheck = true;
                    break;
                }
            }
            if (!ownerCheck) {
                return;
            }
            
            if (effect.regionHasEffect(rm.getRegionType(currentRegion.getType()).getEffects(), "teleport") == 0) {
                return;
            }
            
            Location targetLoc = currentRegion.getLocation();
            
            
            //Check to see if the Townships has enough reagents
            if (!effect.hasReagents(l)) {
                return;
            }
            
            //Run upkeep but don't need to know if upkeep occured
            effect.forceUpkeep(event);
            event.getPlayer().teleport(targetLoc.getBlock().getRelative(BlockFace.NORTH, 2).getRelative(BlockFace.UP).getLocation());
            event.getPlayer().sendMessage(ChatColor.GOLD + "[Townships] You have been teleported!");
        }
    }
    
}
