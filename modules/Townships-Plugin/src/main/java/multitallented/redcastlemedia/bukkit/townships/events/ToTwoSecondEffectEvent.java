package multitallented.redcastlemedia.bukkit.townships.events;

import java.util.ArrayList;
import java.util.List;

import multitallented.redcastlemedia.bukkit.townships.region.Region;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 *
 * @author Multitallented
 */
public class ToTwoSecondEffectEvent extends Event implements ToEvent {

    private static final HandlerList handlers = new HandlerList();
    private final Region re;
    private List<Location> destroyRegions = new ArrayList<Location>();
    private List<Region> createRegions = new ArrayList<Region>();
    private final String[] effect;

    public ToTwoSecondEffectEvent(Region re, String[] effect) {
        this.re = re;
        this.effect = effect;
    }
    
    public String[] getEffect() {
        return effect;
    }
    
    @Override
    public void setRegionsToCreate(List<Region> newRegions) {
        this.createRegions = newRegions;
    }
    
    @Override
    public List<Region> getRegionsToCreate() {
        return createRegions;
    }
    
    @Override
    public Location getLocation() {
        return re.getLocation();
    }
    
    public Region getRegion() {
        return re;
    }
    
    @Override
    public List<Location> getRegionsToDestroy() {
        return destroyRegions;
    }
    
    @Override
    public void setRegionsToDestroy(List<Location> r) {
        this.destroyRegions = r;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
}
