package sevaron.plugins.townships.effects;

import java.util.List;
import multitallented.redcastlemedia.bukkit.townships.Townships;
import multitallented.redcastlemedia.bukkit.townships.effect.Effect;
import multitallented.redcastlemedia.bukkit.townships.region.Region;
import multitallented.redcastlemedia.bukkit.townships.region.RegionCondition;
import multitallented.redcastlemedia.bukkit.townships.region.RegionManager;
import multitallented.redcastlemedia.bukkit.townships.region.RegionType;
import multitallented.redcastlemedia.bukkit.townships.region.SuperRegion;
import multitallented.redcastlemedia.bukkit.townships.region.SuperRegionType;
import org.bukkit.Location;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

/**
 *
 * @author Sevaron
 */
public class EffectDenyMobSpawn extends Effect implements Listener {
    public final Townships aPlugin;
  public EffectDenyMobSpawn(Townships plugin)
  {
    super(plugin);
    this.aPlugin = plugin;
    this.registerEvent(this);
  }
  @Override
  public void init(Townships plugin){
      super.init(plugin);
  }
  @EventHandler
  public void onCustomEvent(CreatureSpawnEvent event) {
    if (event.isCancelled() || event.getSpawnReason() == SpawnReason.CUSTOM || !(event.getEntity() instanceof Monster)) {
      return;
    }

    Location l = event.getLocation();
    RegionManager rm = this.getPlugin().getRegionManager();

    List<RegionCondition> conditions = new ArrayList<RegionCondition>();
    conditions.add(new RegionCondition("deny_mob_spawn", true, 0));
    conditions.add(new RegionCondition("deny_mob_spawn_no_reagent", false, 0));
    if (rm.shouldTakeAction(l, null, conditions)) {
        event.setCancelled(true);
    }
    
    /*for (Region r : rm.getContainingRegions(l)) {
        RegionType rt = rm.getRegionType(r.getType());
        if (rt == null) {
            continue;
        }
        boolean hasEffect = false;
        for (String s : rt.getEffects()) {
            if (!s.startsWith("deny_mob_spawn")) {
                continue;
            }
            if (s.equals("deny_mob_spawn") && !hasReagents(r.getLocation())) {
                return;
            }
            hasEffect = true;
        }
        if (!hasEffect) {
            return;
        }
        event.setCancelled(true);
    }

    for (SuperRegion sr : rm.getContainingSuperRegions(l)) {
        SuperRegionType srt = rm.getSuperRegionType(sr.getType());
        if (srt == null) {
            continue;
        }
        boolean hasEffect = false;
        for (String s : srt.getEffects()) {
            if (!s.startsWith("deny_mob_spawn")) {
                continue;
            }
            if (s.equals("deny_mob_spawn") && !hasReagents(sr.getLocation())) {
                return;
            }
            hasEffect = true;
        }
        if (!hasEffect) {
            return;
        }
        event.setCancelled(true);
    }*/
  }

}