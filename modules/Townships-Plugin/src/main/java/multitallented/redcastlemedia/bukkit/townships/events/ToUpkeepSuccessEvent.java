package multitallented.redcastlemedia.bukkit.townships.events;

import java.util.List;
import multitallented.redcastlemedia.bukkit.townships.region.Region;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 *
 * @author multitallented
 */
public class ToUpkeepSuccessEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Location loc;
    private ToEvent event;
    public ToUpkeepSuccessEvent(ToEvent event) {
        this.loc = event.getLocation();
        this.event = event;
    }
    
    public Location getRegionLocation() {
        return loc;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
    
    public ToEvent getEvent() {
        return this.event;
    }
    
    public void setEvent(ToEvent event) {
        this.event = event;
    }
    
    public void setRegionsToCreate(List<Region> r) {
        event.setRegionsToCreate(r);
    }
    
    public List<Region> getRegionsToCreate() {
        return event.getRegionsToCreate();
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
