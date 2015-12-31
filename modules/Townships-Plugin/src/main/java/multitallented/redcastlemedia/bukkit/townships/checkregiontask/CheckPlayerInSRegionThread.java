package multitallented.redcastlemedia.bukkit.townships.checkregiontask;

import java.util.ArrayList;
import java.util.List;

import multitallented.redcastlemedia.bukkit.townships.events.ToPlayerEnterSRegionEvent;
import multitallented.redcastlemedia.bukkit.townships.events.ToPlayerExitSRegionEvent;
import multitallented.redcastlemedia.bukkit.townships.events.ToPlayerInSRegionEvent;
import multitallented.redcastlemedia.bukkit.townships.region.RegionManager;
import multitallented.redcastlemedia.bukkit.townships.region.SuperRegion;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;

/**
 *
 * @author Multitallented
 */
public class CheckPlayerInSRegionThread {
    private final RegionManager rm;
    private final Location loc;
    private final Player p;
    private final PluginManager pm;
    private final CheckRegionTask crt;
    public CheckPlayerInSRegionThread(CheckRegionTask crt, RegionManager rm, Player p) {
        this.crt = crt;
        this.pm = Bukkit.getPluginManager();
        this.rm = rm;
        this.p = p;
        this.loc = p.getLocation();
    }

    public void go() {
        List<SuperRegion> containedRegions = rm.getContainingSuperRegions(loc);

        for (SuperRegion sr : containedRegions) {
            ToPlayerInSRegionEvent pIREvent = new ToPlayerInSRegionEvent(sr.getName(), p);
            callEvent(pIREvent);
        }

        List<SuperRegion> previousRegions = crt.lastSRegion.get(p.getName());
        if (previousRegions == null) {
            previousRegions = new ArrayList<SuperRegion>();
        }

        for (SuperRegion sr : containedRegions) {
            if (!previousRegions.contains(sr)) {
                ToPlayerEnterSRegionEvent event = new ToPlayerEnterSRegionEvent(sr.getName(), p);
                callEvent(event);
            }
        }

        for (SuperRegion sr : previousRegions) {
            if (!containedRegions.contains(sr)) {
                ToPlayerExitSRegionEvent event = new ToPlayerExitSRegionEvent(sr.getName(), p);
                callEvent(event);
            }
        }

        if (!containedRegions.isEmpty()) {
            crt.lastSRegion.put(p.getName(), containedRegions);
        } else {
            crt.lastSRegion.remove(p.getName());
        }
    }

    private void callEvent(Event eve) {
        pm.callEvent(eve);
    }


}
