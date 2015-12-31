package multitallented.redcastlemedia.bukkit.townships.checkregiontask;

import java.util.List;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import multitallented.redcastlemedia.bukkit.townships.Townships;
import multitallented.redcastlemedia.bukkit.townships.events.ToTwoSecondEvent;
import multitallented.redcastlemedia.bukkit.townships.region.Region;
import multitallented.redcastlemedia.bukkit.townships.region.SuperRegion;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

/**
 *
 * @author Multitallented
 */
public class CheckRegionTask implements Runnable {
    private final transient Server server;
    private final Townships hs;
    private final Set<Location> regionsToDestroy = new HashSet<Location>();
    private final HashSet<Region> regionsToCreate = new HashSet<Region>();
    private int i= 0;

    public final HashMap<String, List<Region>> lastRegion = new HashMap<String, List<Region>>();
    public final HashMap<String, List<SuperRegion>> lastSRegion = new HashMap<String, List<SuperRegion>>();

    public CheckRegionTask(Server server, Townships hs) {
        this.server = server;
        this.hs = hs;
    }
    
    public synchronized void addOrDestroyRegionToDestroy(Location l) {
        if (!regionsToDestroy.remove(l)) {
            regionsToDestroy.add(l);
        }
    }
    
    public synchronized void addRegionToCreate(Region r) {
        regionsToCreate.add(r);
    }
    
    public boolean containsRegionToDestory(Location l) {
        return regionsToDestroy.contains(l);
    }
    
    public HashSet<Region> getRegionsToCreate(Region r) {
        return regionsToCreate;
    }

    @Override
    public void run() {
        PluginManager pm = server.getPluginManager();
        if (Townships.getConfigManager().getPlayerInRegionChecks()) {
            Collection<? extends Player> players = server.getOnlinePlayers();
            int chunk = players.size() / 4;
            for (int j=chunk * i; j<(i==3 ? players.size() : chunk * (i+1)); j++) {
                try {
                    CheckPlayerInRegionThread thread = new CheckPlayerInRegionThread(this, pm, hs.getRegionManager(), (Player) players.toArray()[j]);
                    thread.go();
                    CheckPlayerInSRegionThread srThread = new CheckPlayerInSRegionThread(this, hs.getRegionManager(), (Player) players.toArray()[j]);
                    srThread.go();
                } catch (Exception e) {

                }
                Thread.yield();
            }
        }
        if (i == 3) {
            i=-1;
            pm.callEvent(new ToTwoSecondEvent());
            TriggerEffectTask triggerEffectTask = new TriggerEffectTask(hs.getRegionManager(), server.getPluginManager(), this);
            triggerEffectTask.go();
        } else {
            i++;
        }

        for (Location l : regionsToDestroy) {
            hs.getRegionManager().destroyRegion(l);
            hs.getRegionManager().removeRegion(l);
        }
        regionsToDestroy.clear();

        for (Region r : regionsToCreate) {
            hs.getRegionManager().addRegion(r.getLocation(), r.getType(), r.getOwners(), r.getMembers());
        }
        regionsToCreate.clear();
    }
}
