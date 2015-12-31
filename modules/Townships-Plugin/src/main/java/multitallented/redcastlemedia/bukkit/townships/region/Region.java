package multitallented.redcastlemedia.bukkit.townships.region;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

/**
 *
 * @author Multitallented
 * @author Phoenix_Frenzy
 */
public class Region {
    private int id;
    private Location loc;
    private String type;
    private List<OfflinePlayer> owners;
    private List<OfflinePlayer> members;
    
    public Region(int id, Location loc, String type, List<OfflinePlayer> owners, List<OfflinePlayer> members) {
        this.id = id;
        this.loc = loc;
        this.type = type;
        this.owners = owners;
        this.members = members;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public int getID() {
        return id;
    }
    
    public Location getLocation() {
        return loc;
    }
    
    public String getType() {
        return type;
    }
    
    public List<OfflinePlayer> getOwners() {
        return owners;
    }
    
    public List<OfflinePlayer> getMembers() {
        return members;
    }
    
    public void addOwner(OfflinePlayer name) {
        owners.add(name);
    }
    
    public void addMember(OfflinePlayer name) {
        members.add(name);
    }
    
    public boolean remove(OfflinePlayer name) {
        if (owners.contains(name)) {
            owners.remove(name);
            return true;
        } else if (members.contains(name)) {
            members.remove(name);
            return true;
        } else {
            return false;
        }
    }
    
    public boolean isPrimaryOwner(OfflinePlayer name) {
        return owners.get(0).equals(name);
    }
    
    public boolean isOwner(OfflinePlayer name) {
        return owners.contains(name);
    }
    
    public boolean isMember(OfflinePlayer name) {
        return members.contains(name);
    }
}

