package multitallented.redcastlemedia.bukkit.townships.listeners;

import com.griefcraft.lwc.LWC;
import com.griefcraft.scripting.JavaModule;
import com.griefcraft.scripting.event.LWCProtectionRegisterEvent;
import multitallented.redcastlemedia.bukkit.townships.Townships;
import multitallented.redcastlemedia.bukkit.townships.effect.Effect;
import multitallented.redcastlemedia.bukkit.townships.region.Region;
import multitallented.redcastlemedia.bukkit.townships.region.RegionManager;
import multitallented.redcastlemedia.bukkit.townships.region.SuperRegion;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 *
 * @author Multitallented
 */
public class LWCListener extends JavaModule {
    private Townships hs;
    private LWC lwc;
    
    public LWCListener(Townships hs, LWC lwc) {
        this.hs = hs;
        this.lwc = lwc;
    }
    
    /**
     * Prevent players from making LWCs in places they shouldn't
     * @param event
     */
    @Override
    public void onRegisterProtection(LWCProtectionRegisterEvent event) {
        if (event.isCancelled()) {
            return;
        }
        
        Player player = event.getPlayer();
        Block block = event.getBlock();

        RegionManager rm = hs.getRegionManager();
        for (Region r : rm.getContainingBuildRegions(block.getLocation())) {
            if (r.getLocation().getBlock().equals(block)) {
                player.sendMessage("[Townships] You can't protect a region chest");
                lwc.removeModes(player);
                event.setCancelled(true);
                return;
            }

            if (!(r.isOwner(player) || Effect.isMemberRegion(player, r.getLocation(), rm))) {
                player.sendMessage("[Townships] You can't protect a someone else's region");
                lwc.removeModes(player);
                event.setCancelled(true);
                return;
            }
        }
        for (SuperRegion sr : rm.getContainingSuperRegions(block.getLocation())) {
            if (!(sr.hasOwner(player) || sr.hasMember(player))) {
                player.sendMessage("[Townships] You can't protect a someone else's super region");
                lwc.removeModes(player);
                event.setCancelled(true);
                return;
            }
        }
    }
}
