package multitallented.redcastlemedia.bukkit.townships.effect;

import java.util.List;

import multitallented.redcastlemedia.bukkit.townships.Townships;
import multitallented.redcastlemedia.bukkit.townships.Util;
import multitallented.redcastlemedia.bukkit.townships.events.ToEvent;
import multitallented.redcastlemedia.bukkit.townships.events.ToReagentCheckEvent;
import multitallented.redcastlemedia.bukkit.townships.events.ToUpkeepEvent;
import multitallented.redcastlemedia.bukkit.townships.events.ToUpkeepSuccessEvent;
import multitallented.redcastlemedia.bukkit.townships.region.Region;
import multitallented.redcastlemedia.bukkit.townships.region.RegionManager;
import multitallented.redcastlemedia.bukkit.townships.region.RegionType;
import multitallented.redcastlemedia.bukkit.townships.region.SuperRegion;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 *
 * @author Multitallented
 */
public class Effect {
    private Townships plugin;
    
    public Effect(Townships plugin) {
        this.plugin = plugin;
    }
    
    public Townships getPlugin() {
        return plugin;
    }
    
    protected void registerEvent(Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, plugin);
    }

    /**
     * The icon used to represent the effect in a GUI
     *
     * @return The item stack to display as the effect icon
     */
    public ItemStack getIcon() {
            ItemStack is = new ItemStack(Material.POTION);
            ItemMeta im = is.getItemMeta();
            im.setDisplayName("Unknown Effect");
            is.setItemMeta(im);
            return is;
    }

    /**
     * This is called when the user clicks on an effect in a who GUI
     *
     * @param effect The Region Type effect split string
     * @return The inventory for the GUI
     */
    public Inventory getGUI(Region r, RegionType rt, String[] effect) {
            return null;
    }

    /**
     * This is called when the user clicks on an effect in an info GUI
     *
     * @param effect The Region Type effect split string
     * @return The inventory for the GUI
     */
    public Inventory getGUI(RegionType rt, String[] effect) {
            return null;
    }
    
    /**
     * Returns the first region that contains that location.
     * 
     * @deprecated use regionManager.getContainingRegions(Location)
     * @param currentLocation the location to check
     * @return 
     */
    public Region getContainingRegion(Location currentLocation) {
        double x1 = currentLocation.getX();
        Location loc = null;
        RegionManager rm = plugin.getRegionManager();
        for (Region r : rm.getSortedRegions()) {
            double radius = rm.getRegionType(r.getType()).getRadius();
            Location l = r.getLocation();
            if (l.getX() + radius < x1) {
                return null;
            }
            try {
                if (!(l.getX() - radius > x1) && l.distanceSquared(currentLocation) < radius) {
                    loc = l;
                    break;
                }
            } catch (IllegalArgumentException iae) {

            }
        }
        if (loc == null) {
            return null;
        }
        return rm.getRegion(loc);
    }
    
    /**
     * Use this to find if a region (not super-region) has an effect and what 
     * number is associated with that effect. The number has no internal meaning
     * to Townships, only you decide what that number means. Example:
     * arrowturrets use the number to determine arrow velocity. Conveyors use the
     * number as the item id that needs to be moved.
     * 
     * @param list regionManager.getRegionType(region.getType()).getEffects()
     * @param name example: "denyblockbreak"
     * @return the number following the effect (returns 0 if doesn't have that effect)
     */
    public int regionHasEffect(List<String> list, String name) {
        int data = 0;
        if (list == null || list.isEmpty()) {
            return 0;
        }
        
        for (String effect : list) {
            String[] params = effect.split("\\.");
            if (params.length > 1 && params[0].equalsIgnoreCase(name)) {
                data = Integer.parseInt(params[1]);
            } else if (params[0].equalsIgnoreCase(name)) {
                data = 1;
            }
        }
        if (data < 1) {
            return 0;
        }
        return data;
    }
    
    /**
     * Use this to find if a region (not super-region) has an effect and what 
     * number is associated with that effect. The number has no internal meaning
     * to Townships, only you decide what that number means. Example:
     * arrowturrets use the number to determine arrow velocity. Conveyors use the
     * number as the item id that needs to be moved.
     * 
     * @param region the region to check
     * @param name example: "denyblockbreak"
     * @return the number following the effect (returns 0 if doesn't have that effect)
     */
    public int regionHasEffect(Region region, String name) {
        int data = 0;
        List<String> effects = plugin.getRegionManager().getRegionType(region.getType()).getEffects();
        if (effects == null || effects.isEmpty())
            return 0;
        
        for (String effect : effects) {
            String[] params = effect.split("\\.");
            if (params.length > 1 && params[0].equalsIgnoreCase(name)) {
                data = Integer.parseInt(params[1]);
            }
        }
        if (data < 1)
            return 0;
        return data;
    }
    
    /**
     * Checks if the player is an owner of the region. Equivalent to
     * region.isOwner(player.getName());
     * 
     * @param player
     * @param region
     * @return 
     */
    public boolean isOwnerOfRegion(Player player, Region region) {
        if (region == null) {
            plugin.warning("[Townships] null region isOwnerCheck for " + player.getName() + " at " + 
                    Math.floor(player.getLocation().getX()) + ":" + 
                    Math.floor(player.getLocation().getY()) + ":" + 
                    Math.floor(player.getLocation().getZ()));
            return false;
        }
        return region.isOwner(player);
    }
    
    /**
     * Checks if the player is is a member of the region at the location
     * specified. This will throw a NullPointerException if there is no region
     * at that location.
     * 
     * @param player
     * @param location the location of the region center region.getLocation()
     * @return 
     */
    public boolean isOwnerOfRegion(Player player, Location location) throws NullPointerException {
        return getPlugin().getRegionManager().getRegion(location).getOwners().contains(player.getName());
    }
    
    /**
     * This function does more than just region.isMember(player.getName()). It
     * checks if the region has added a super-region that the player is a member
     * of. Also checks if the region has added "all" players.
     * 
     * @throws java.lang.NullPointerException if no region found at the given location
     * @param player
     * @param location the location of the region center region.getLocation()
     * @return 
     */
    public boolean isMemberOfRegion(Player player, Location location) throws NullPointerException {
        RegionManager rm = getPlugin().getRegionManager();
        Region r = rm.getRegion(location);
        if (r.isMember(player)) {
            return true;
        } // TODO: revive this code
        /* else if (r.isMember("all")) {
            return true;
        } else {
            for (String s : r.getMembers()) {
                if (s.contains("sr:")) {
                    String superRegionName = s.replace("sr:", "");
                    SuperRegion sr = rm.getSuperRegion(superRegionName);
                    if (sr != null && (sr.hasMember(player) || sr.hasOwner(player))) {
                        return true;
                    }
                }
            }
        } */
        return false;
    }

    public static boolean isMemberRegion(Player player, Location location, RegionManager rm) {
        Region r = rm.getRegion(location);
        if (r.isMember(player)) {
            return true;
        } // TODO: revive this code
        /* else if (r.isMember("all")) {
            return true;
        } else {
            for (String s : r.getMembers()) {
                if (s.contains("sr:")) {
                    String superRegionName = s.replace("sr:", "");
                    SuperRegion sr = rm.getSuperRegion(superRegionName);
                    if (sr != null && (sr.hasMember(player) || sr.hasOwner(player))) {
                        return true;
                    }
                }
            }
        } */
        return false;
    }
    
    /**
     * Checks if the region has output and the chest is not full first. Then it
     * checks if the chest has the necessary reagents.
     * 
     * @param location the location of the center of the region region.getLocation()
     * @return 
     */
    public boolean hasReagents(Location location) {
        if (location == null || location.getBlock() == null || location.getBlock().getState() == null) {
            return false;
        }
        RegionManager rm = getPlugin().getRegionManager();
        Region r = null;
        RegionType rt = null;
        r = rm.getRegion(location);
        if (r == null) {
            return false;
        }
        rt = rm.getRegionType(r.getType());
        if (r == null || rt == null) {
            return false;
        }

        BlockState bs = location.getBlock().getState();
        if (!(bs instanceof Chest)) {
            return false;
        }

        ToReagentCheckEvent rce = new ToReagentCheckEvent(location);
        Bukkit.getPluginManager().callEvent(rce);
        if (rce.isCancelled()) {
                return false;
        }


        if (rt.getPowerDrain() != 0) {
            List<SuperRegion> srs = rm.getContainingSuperRegions(r.getLocation());
            if (srs.isEmpty()) {
                return false;
            }
            boolean hasPower = false;
            for (SuperRegion sr : srs) {
                if (sr.getPower() + rt.getPowerDrain() > 0) {
                    hasPower = true;
                    break;
                }
            }
            if (!hasPower) {
                return false;
            }
        }

        //Check if the player has enough money
        if (Townships.econ != null) {
            double balance = Townships.econ.getBalance(r.getOwners().get(0));
            if (balance + rt.getMoneyOutput() < 0) {
                return false;
            }
        }

        //Check if chest is full and region has output
        Chest chest = ((Chest) bs);
        return Util.containsItems(rt.getReagents(), chest.getInventory());
    }
    
    /**
     * Forces the region to consume upkeep items and output money and items
     * regardless of the upkeep-chance.
     * 
     * @see #forceUpkeep( multitallented.redcastlemedia.bukkit.townships.events.ToEvent ) Use this instead if possible
     * @param location the location of the center of the region region.getLocation()
     */
    public void forceUpkeep(Location location) {
        ToUpkeepEvent upEvent = new ToUpkeepEvent(location);
        Bukkit.getPluginManager().callEvent(upEvent);
        if (upEvent.isCancelled()) {
            return;
        }
        RegionManager rm = getPlugin().getRegionManager();
        Region r = rm.getRegion(location);
        RegionType rt = rm.getRegionType(r.getType());
        
        
        BlockState bs = location.getBlock().getState();
        if (!(bs instanceof Chest)) {
            return;
        }
        Chest chest = (Chest) bs;
        
        
        //Remove the upkeep items from the region chest and add items from output
        if (chest.getInventory().firstEmpty() < 0 && !rt.getOutput().isEmpty()) {
            return;
        }
        if (!Util.containsItems(rt.getUpkeep(), chest.getInventory())) {
            return;
        }
        /*boolean hasUpkeep = true;
        for (ItemStack is : rt.getUpkeep()) {
            if (!chest.getInventory().contains(is.getType(), is.getAmount())) {
                hasUpkeep = false;
            }
        }
        if (!hasUpkeep) {
            return;
        }*/
        
        //Check and remove money from the player
        OfflinePlayer playername;
        try {
            playername = r.getOwners().get(0);
        } catch (IndexOutOfBoundsException ioobe) {
            return;
        }
        double output = rt.getMoneyOutput();
        if (rt.getMoneyOutput() != 0 && Townships.econ != null) {
            if (output < 0  && Townships.econ.getBalance(playername) < Math.abs(output)) {
                return;
            }
        }
        
        if (rt.getPowerDrain() != 0) {
            List<SuperRegion> srs = rm.getContainingSuperRegions(location);
            if (srs.isEmpty()) {
                return;
            }
            boolean hasPower = false;
            for (SuperRegion sr : srs) {
                if (sr.getPower() > rt.getPowerDrain() && sr.getMaxPower() > sr.getPower() - rt.getPowerDrain()) {
                    hasPower = true;
                    rm.setPower(sr, sr.getPower() - rt.getPowerDrain());
                    break;
                }
            }
            if (!hasPower) {
                return;
            }
        }
        
        Util.removeItems(rt.getUpkeep(), chest.getInventory());
        /*for (ItemStack is : rt.getUpkeep()) {
            chest.getInventory().removeItem(is);
        }*/
        
        if (Townships.econ != null) {
            if (output < 0) {
                Townships.econ.withdrawPlayer(playername, Math.abs(output));
            } else {
                Townships.econ.depositPlayer(playername, output);
            }
        }
        Util.addItems(rt.getOutput(), chest.getInventory());
        /*for (ItemStack is : rt.getOutput()) {
            chest.getInventory().addItem(is);
        }*/
        chest.update(true);
        ToUpkeepSuccessEvent tsue = new ToUpkeepSuccessEvent(upEvent);
        Bukkit.getPluginManager().callEvent(tsue);
    }
    /**
     * Forces the region to consume upkeep items and output money and items
     * regardless of the upkeep-chance.
     * 
     * @param event
     */
    public void forceUpkeep(ToEvent event) {
        Location l = event.getLocation();
        ToUpkeepEvent upEvent = new ToUpkeepEvent(l);
        Bukkit.getPluginManager().callEvent(upEvent);
        if (upEvent.isCancelled()) {
            return;
        }
        RegionManager rm = getPlugin().getRegionManager();
        Region r = rm.getRegion(l);
        RegionType rt = rm.getRegionType(r.getType());
        
        
        BlockState bs = l.getBlock().getState();
        if (!(bs instanceof Chest)) {
            return;
        }
        Chest chest = (Chest) bs;
        
        
        //Remove the upkeep items from the region chest and add items from output
        
        if (chest.getInventory().firstEmpty() < 0 && !rt.getOutput().isEmpty()) {
            return;
        }
        if (!Util.containsItems(rt.getUpkeep(), chest.getInventory())) {
            return;
        }
        /*boolean hasUpkeep = true;
        for (ItemStack is : rt.getUpkeep()) {
            if (!chest.getInventory().contains(is.getType(), is.getAmount())) {
                hasUpkeep = false;
            }
        }
        if (!hasUpkeep) {
            return;
        }*/
        
        //Check and remove money from the player
        OfflinePlayer playername;
        try {
            playername = r.getOwners().get(0);
        } catch (IndexOutOfBoundsException ioobe) {
            return;
        }
        double output = rt.getMoneyOutput();
        if (rt.getMoneyOutput() != 0 && Townships.econ != null) {
            if (output < 0  && Townships.econ.getBalance(playername) < Math.abs(output)) {
                return;
            }
        }
        
        if (rt.getPowerDrain() != 0) {
            List<SuperRegion> srs = rm.getContainingSuperRegions(l);
            if (srs.isEmpty()) {
                return;
            }
            boolean hasPower = false;
            for (SuperRegion sr : srs) {
                if (sr.getPower() > rt.getPowerDrain() && sr.getMaxPower() > sr.getPower() - rt.getPowerDrain()) {
                    hasPower = true;
                    rm.setPower(sr, sr.getPower() - rt.getPowerDrain());
                    break;
                }
            }
            if (!hasPower) {
                return;
            }
        }
        
        
        /*for (ItemStack is : rt.getUpkeep()) {
            chest.getInventory().removeItem(is);
        }*/
        Util.removeItems(rt.getUpkeep(), chest.getInventory());
        
        if (Townships.econ != null) {
            if (output < 0) {
                Townships.econ.withdrawPlayer(playername, Math.abs(output));
            } else {
                Townships.econ.depositPlayer(playername, output);
            }
        }
        Util.addItems(rt.getOutput(), chest.getInventory());
        /*for (ItemStack is : rt.getOutput()) {
            chest.getInventory().addItem(is);
        }*/
        Bukkit.getPluginManager().callEvent(new ToUpkeepSuccessEvent(event));
        chest.update();
    }
    
    /**
     * Forces the region to consume upkeep items and output money and items
     * if a random number is lower than the upkeep-chance.
     * 
     * @see #upkeep( multitallented.redcastlemedia.bukkit.townships.events.ToEvent ) Use this instead if possible
     * @param location the location of the center of the region region.getLocation()
     */
    public boolean upkeep(Location location) {
        ToUpkeepEvent upEvent = new ToUpkeepEvent(location);
        Bukkit.getPluginManager().callEvent(upEvent);
        if (upEvent.isCancelled()) {
            return false;
        }
        RegionManager rm = getPlugin().getRegionManager();
        Region r = rm.getRegion(location);
        RegionType rt = rm.getRegionType(r.getType());
        
        BlockState bs = location.getBlock().getState();
        if (!(bs instanceof Chest))
            return false;
        Chest chest = (Chest) bs;
        
        //Remove the upkeep items from the region chest and add items from output
        if (chest.getInventory().firstEmpty() < 0 && !rt.getOutput().isEmpty()) {
            return false;
        }
        if (!Util.containsItems(rt.getUpkeep(), chest.getInventory())) {
            return false;
        }
        /*boolean hasUpkeep = true;
        for (ItemStack is : rt.getUpkeep()) {
            if (!chest.getInventory().contains(is.getType(), is.getAmount())) {
                hasUpkeep = false;
            }
        }
        if (!hasUpkeep) {
            return false;
        }*/
        
        
        //Check and remove money from the player
        OfflinePlayer playername;
        try {
            playername = r.getOwners().get(0);
        } catch (IndexOutOfBoundsException ioobe) {
            return false;
        }
        double output = rt.getMoneyOutput();
        if (output != 0 && Townships.econ != null) {
            Economy econ = Townships.econ;
            if (r.getOwners().isEmpty()) {
                return false;
            }
            if (output < 0  && econ.getBalance(playername) < Math.abs(output)) {
                return false;
            }
        }
        
        if (rt.getPowerDrain() != 0) {
            List<SuperRegion> srs = rm.getContainingSuperRegions(location);
            if (srs.isEmpty()) {
                return false;
            }
            boolean hasPower = false;
            for (SuperRegion sr : srs) {
                if (sr.getPower() > rt.getPowerDrain() && sr.getMaxPower() > sr.getPower() - rt.getPowerDrain()) {
                    hasPower = true;
                    rm.setPower(sr, sr.getPower() - rt.getPowerDrain());
                    break;
                }
            }
            if (!hasPower) {
                return false;
            }
        }
        
        Util.removeItems(rt.getUpkeep(), chest.getInventory());
        /*for (ItemStack is : rt.getUpkeep()) {
            chest.getInventory().removeItem(is);
        }*/
        
        if (Townships.econ != null) {
            if (output < 0) {
                Townships.econ.withdrawPlayer(playername, Math.abs(output));
            } else {
                Townships.econ.depositPlayer(playername, output);
            }
        }
        /*for (ItemStack is : rt.getOutput()) {
            chest.getInventory().addItem(is);
        }*/
        Util.addItems(rt.getOutput(), chest.getInventory());
        
        chest.update(true);
        ToUpkeepSuccessEvent tsue = new ToUpkeepSuccessEvent(upEvent);
        Bukkit.getPluginManager().callEvent(tsue);
        return true;
    }
    
    /**
     * Forces the region to consume upkeep items and output money and items
     * if a random number is lower than the upkeep-chance.
     * 
     * @param event
     */
    public boolean upkeep(ToEvent event) {
        Location l = event.getLocation();
        ToUpkeepEvent upEvent = new ToUpkeepEvent(l);
        Bukkit.getPluginManager().callEvent(upEvent);
        if (upEvent.isCancelled()) {
            return false;
        }
        RegionManager rm = getPlugin().getRegionManager();
        Region r = rm.getRegion(l);
        RegionType rt = rm.getRegionType(r.getType());
        
        BlockState bs = l.getBlock().getState();
        if (!(bs instanceof Chest))
            return false;
        Chest chest = (Chest) bs;
        
        //Remove the upkeep items from the region chest and add items from output
        if (chest.getInventory().firstEmpty() < 0 && !rt.getOutput().isEmpty()) {
            return false;
        }
        if (!Util.containsItems(rt.getUpkeep(), chest.getInventory())) {
            return false;
        }
        /*boolean hasUpkeep = true;
        for (ItemStack is : rt.getUpkeep()) {
            if (!chest.getInventory().contains(is.getType(), is.getAmount())) {
                hasUpkeep = false;
            }
        }
        if (!hasUpkeep) {
            return false;
        }*/
        
        
        //Check and remove money from the player
        OfflinePlayer playername;
        try {
            playername = r.getOwners().get(0);
        } catch (IndexOutOfBoundsException ioobe) {
            return false;
        }
        double output = rt.getMoneyOutput();
        if (output != 0 && Townships.econ != null) {
            Economy econ = Townships.econ;
            if (r.getOwners().isEmpty()) {
                return false;
            }
            if (output < 0  && econ.getBalance(playername) < Math.abs(output)) {
                return false;
            }
        }
        
        if (rt.getPowerDrain() != 0) {
            List<SuperRegion> srs = rm.getContainingSuperRegions(l);
            if (srs.isEmpty()) {
                return false;
            }
            boolean hasPower = false;
            for (SuperRegion sr : srs) {
                if (sr.getPower() > rt.getPowerDrain() && sr.getMaxPower() > sr.getPower() - rt.getPowerDrain()) {
                    hasPower = true;
                    rm.setPower(sr, sr.getPower() - rt.getPowerDrain());
                    break;
                }
            }
            if (!hasPower) {
                return false;
            }
        }
        
        Util.removeItems(rt.getUpkeep(), chest.getInventory());
        /*for (ItemStack is : rt.getUpkeep()) {
            chest.getInventory().removeItem(is);
        }*/
        
        if (Townships.econ != null) {
            if (output < 0) {
                Townships.econ.withdrawPlayer(playername, Math.abs(output));
            } else {
                Townships.econ.depositPlayer(playername, output);
            }
        }
        Util.addItems(rt.getOutput(), chest.getInventory());
        /*for (ItemStack is : rt.getOutput()) {
            chest.getInventory().addItem(is);
        }*/
        Bukkit.getPluginManager().callEvent(new ToUpkeepSuccessEvent(event));
        chest.update(true);
        return true;
    }
    
    public void init(Townships plugin) {
        this.plugin = plugin;
    }
}
