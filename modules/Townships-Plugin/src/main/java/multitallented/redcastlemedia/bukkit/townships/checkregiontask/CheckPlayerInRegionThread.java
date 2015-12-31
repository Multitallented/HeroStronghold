package multitallented.redcastlemedia.bukkit.townships.checkregiontask;

import java.util.ArrayList;
import java.util.List;

import multitallented.redcastlemedia.bukkit.townships.events.ToEvent;
import multitallented.redcastlemedia.bukkit.townships.events.ToPlayerEnterRegionEvent;
import multitallented.redcastlemedia.bukkit.townships.events.ToPlayerExitRegionEvent;
import multitallented.redcastlemedia.bukkit.townships.events.ToPlayerInRegionEvent;
import multitallented.redcastlemedia.bukkit.townships.region.Region;
import multitallented.redcastlemedia.bukkit.townships.region.RegionManager;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;

/**
 *
 * @author Multitallented
 */
public class CheckPlayerInRegionThread {
    private final RegionManager rm;
    private final Location loc;
    private final Player p;
    private final PluginManager pm;
    private final CheckRegionTask crt;
    public CheckPlayerInRegionThread(CheckRegionTask crt, PluginManager pm, RegionManager rm, Player p) {
        this.crt = crt;
        this.pm = pm;
        this.rm = rm;
        this.p = p;
        this.loc = p.getLocation();
    }

    public void go() {
        List<Region> containedRegions = rm.getContainingRegions(loc);

        for (Region re : containedRegions) {
            ToPlayerInRegionEvent pIREvent = new ToPlayerInRegionEvent(re.getLocation(), p);
            callEvent(pIREvent);
        }

        List<Region> previousRegions = crt.lastRegion.get(p.getName());
        if (previousRegions == null) {
            previousRegions = new ArrayList<Region>();
        }

        for (Region r : containedRegions) {
            if (!previousRegions.contains(r)) {
                ToPlayerEnterRegionEvent event = new ToPlayerEnterRegionEvent(r.getLocation(), p);
                callEvent(event);
            }
        }

        for (Region r : previousRegions) {
            if (!containedRegions.contains(r)) {
                ToPlayerExitRegionEvent event = new ToPlayerExitRegionEvent(r.getLocation(), p);
                callEvent(event);
            }
        }

        if (!containedRegions.isEmpty()) {
            crt.lastRegion.put(p.getName(), containedRegions);
        } else {
            crt.lastRegion.remove(p.getName());
        }
    }

    private void callEvent(Event eve) {
        pm.callEvent(eve);
        ToEvent event;
        if (eve instanceof ToEvent) {
            event = (ToEvent) eve;
        } else {
            return;
        }

        try {
            for (Location dl : event.getRegionsToDestroy()) {
                if (!crt.containsRegionToDestory(dl)) {
                    crt.addOrDestroyRegionToDestroy(dl);
                }
            }
        } catch (NullPointerException npe) {

        }
        try {
            for (Region reg : event.getRegionsToCreate()) {
                crt.addRegionToCreate(reg);
            }
        } catch (Exception e) {

        }
    }


}
