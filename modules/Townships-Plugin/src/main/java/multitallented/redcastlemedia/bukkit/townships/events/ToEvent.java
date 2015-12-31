/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package multitallented.redcastlemedia.bukkit.townships.events;

import java.util.List;

import multitallented.redcastlemedia.bukkit.townships.region.Region;

import org.bukkit.Location;

/**
 *
 * @author Multitallented
 */
public interface ToEvent {
    public void setRegionsToCreate(List<Region> regions);
    public List<Region> getRegionsToCreate();
    public void setRegionsToDestroy(List<Location> regions);
    public List<Location> getRegionsToDestroy();
    public Location getLocation();
}
