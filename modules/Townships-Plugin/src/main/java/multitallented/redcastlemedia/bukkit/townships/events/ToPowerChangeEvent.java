package multitallented.redcastlemedia.bukkit.townships.events;

import java.util.ArrayList;
import java.util.List;

import multitallented.redcastlemedia.bukkit.townships.region.Region;
import multitallented.redcastlemedia.bukkit.townships.region.SuperRegion;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 *
 * @author Multitallented
 */
public class ToPowerChangeEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private List<Location> destroyRegions = new ArrayList<Location>();
    private List<Region> createRegions = new ArrayList<Region>();
    private boolean cancelled;
    private final SuperRegion sr;
    private final int oldPower;
    private int newPower;
    public ToPowerChangeEvent(SuperRegion sr, int oldPower, int newPower) {
        this.sr = sr;
        this.oldPower = oldPower;
        this.newPower = newPower;
    }
    
    public int getNewPower() {
        return newPower;
    }
    public void setNewPower(int newPower) {
        this.newPower = newPower;
    }
    
    public int getOldPower() {
        return oldPower;
    }
    
    public SuperRegion getSuperRegion() {
        return sr;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean bln) {
        cancelled = bln;
    }
}
