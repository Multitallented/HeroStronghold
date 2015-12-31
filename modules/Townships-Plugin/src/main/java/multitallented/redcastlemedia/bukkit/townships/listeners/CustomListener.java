package multitallented.redcastlemedia.bukkit.townships.listeners;

//import com.griefcraft.scripting.Module;
//import com.griefcraft.scripting.event.LWCBlockInteractEvent;
//import com.griefcraft.scripting.event.LWCProtectionInteractEvent;
import multitallented.redcastlemedia.bukkit.townships.Townships;
import multitallented.redcastlemedia.bukkit.townships.events.ToRegionDestroyedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 *
 * @author Multitallented
 */
public class CustomListener implements Listener {
    private final Townships hs;
    public CustomListener(Townships hs) {
        this.hs = hs;
    }
    
    @EventHandler
    public void onCustomEvent(ToRegionDestroyedEvent event) {
        if (!event.getCheckDestroy()) {
            return;
        }
        //Check if a super region needs to fall if a required region was destroyed
        hs.getRegionManager().checkIfDestroyedSuperRegion(event.getRegion().getLocation());
    }
    
//    public void onLWCCreate(LWCBlockInteractEvent event) {
//        System.out.println("Block Interacted");
//
//
//        System.out.println("LWC actions: " + event.getActions().toString());
//        System.out.println("LWC name: " + event.getResult().name());
//
//        Player player = event.getPlayer();
//        Block block = event.getBlock();
//
//        RegionManager rm = hs.getRegionManager();
//        for (Region r : rm.getContainingBuildRegions(block.getLocation())) {
//            if (r.getLocation().getBlock().equals(block)) {
//                System.out.println("[Townships] cancel region chest");
//                event.setResult(Module.Result.CANCEL);
//                return;
//            }
//
//            if (!(r.isOwner(player.getName()) || Effect.isMemberRegion(player, r.getLocation(), rm))) {
//                System.out.println("[Townships] cancel region non-member");
//                event.setResult(Module.Result.CANCEL);
//                return;
//            }
//        }
//        for (SuperRegion sr : rm.getContainingSuperRegions(block.getLocation())) {
//            if (!(sr.hasOwner(player) || sr.hasMember(player.getName()))) {
//                System.out.println("[Townships] cancel sregion non-member");
//                event.setResult(Module.Result.CANCEL);
//                return;
//            }
//        }
//    }
}