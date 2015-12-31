package multitallented.plugins.townships.effects;

import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import multitallented.redcastlemedia.bukkit.townships.Townships;
import multitallented.redcastlemedia.bukkit.townships.effect.Effect;
import multitallented.redcastlemedia.bukkit.townships.events.ToTwoSecondEffectEvent;
import multitallented.redcastlemedia.bukkit.townships.region.Region;
import multitallented.redcastlemedia.bukkit.townships.region.RegionManager;
import multitallented.redcastlemedia.bukkit.townships.region.RegionType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.entity.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author Multitallented
 */
public class EffectConveyorBelt extends Effect {
    public EffectConveyorBelt(Townships plugin) {
        super(plugin);
        registerEvent(new UpkeepListener(this, plugin.getRegionManager()));
    }
    
    @Override
    public void init(Townships plugin) {
        super.init(plugin);
    }
    
    public class UpkeepListener implements Listener {
    private final EffectConveyorBelt effect;
    private final RegionManager rm;
    private HashMap<Region, StorageMinecart> carts = new HashMap<Region, StorageMinecart>();
    private HashMap<Region, Location> cachePoints = new HashMap<Region, Location>();
    private HashMap<Region, Region> cacheRegions = new HashMap<Region, Region>();
    public UpkeepListener(EffectConveyorBelt effect, RegionManager rm) {
        this.effect = effect;
        this.rm = rm;
    }

    @EventHandler
    public void onCustomEvent(ToTwoSecondEffectEvent event) {
        if (event.getEffect().length < 2 || !event.getEffect()[0].equals("conveyor")) {
            return;
        }
        Region r = event.getRegion();
        Location l = r.getLocation();

        //Check if has effect conveyor
        RegionType rt = rm.getRegionType(r.getType());
        if (rt == null) {
            return;
        }
        int conveyor = Integer.parseInt(event.getEffect()[1]);
        if (conveyor < 1) {
            return;
        }

        handleExistingCarts(r);

        //Check if has reagents
        if (!effect.hasReagents(l)) {
            return;
        }

        Location loc = null;
        if (cachePoints.containsKey(r)) {
            loc = cachePoints.get(r);
        } else {
            double radius = rt.getRawBuildRadius();
            double x0 = l.getX();
            double y0 = l.getY();
            double z0 = l.getZ();
            outer: for (int x = (int) (x0 - radius); x < x0 + radius; x++) {
                for (int y = (int) (y0 - radius); y < y0 + radius; y++) {
                    for (int z = (int) (z0 - radius); z < z0 + radius; z++) {
                        Block b = l.getWorld().getBlockAt(x, y, z);
                        if (b.getType() == Material.POWERED_RAIL) {
                            cachePoints.put(r, b.getRelative(BlockFace.UP).getLocation());
                            break outer;
                        }
                    }
                }
            }
            if (loc == null) {
                return;
            }
            loc = cachePoints.get(r);
        }

        Chest chest = null;
        try {
            chest = (Chest) l.getBlock().getState();
        } catch (Exception e) {
            return;
        }
        Inventory cInv = chest.getInventory();
        HashSet<ItemStack> iss = new HashSet<ItemStack>();
        if (!cInv.contains(Material.STORAGE_MINECART) || !cInv.contains(conveyor)) {
            return;
        }
        for (ItemStack is : cInv.getContents()) {
            if (is != null && is.getTypeId() == conveyor) {
                iss.add(is);
            }
        }
        if (iss.isEmpty()) {
            return;
        }

        //If chunk not loaded try using region cache to move directly
        if (!loc.getChunk().isLoaded()) {
            if (cacheRegions.containsKey(r)) {
                Chest tempChest = (Chest) cacheRegions.get(r).getLocation().getBlock().getState();
                if (tempChest.getInventory().firstEmpty() < 0) {
                    return;
                }
                for (ItemStack is : iss) {
                    cInv.removeItem(is);
                }
                try {
                    for (ItemStack is : iss) {
                        tempChest.getInventory().addItem(is);
                    }
                } catch (Exception e) {

                }
            }
            return;
        } else {
            for (ItemStack is : iss) {
                cInv.removeItem(is);
            }

            ItemStack tempCart = new ItemStack(Material.STORAGE_MINECART, 1);
            cInv.removeItem(tempCart);

            StorageMinecart cart = loc.getWorld().spawn(loc, StorageMinecart.class);

            for (ItemStack is : iss) {
                cart.getInventory().addItem(is);
            }
            carts.put(r, cart);
        }
    }

    private void handleExistingCarts(Region r) {
            HashSet<Region> removeMe = new HashSet<Region>();

            if (carts.containsKey(r)) {
                StorageMinecart sm = carts.get(r);
                if (sm.isDead()) {
                    removeMe.add(r);
                    return;
                }
                List<Region> regions = rm.getContainingBuildRegions(sm.getLocation());
                if (!regions.isEmpty()) {
                    Chest currentChest = null;
                    Region re;
                    try {
                        if (regions.get(0).equals(r)) {
                            return;
                        }
                        re = regions.get(0);
                        currentChest = (Chest) re.getLocation().getBlock().getState();
                    } catch (Exception e) {
                        return;
                    }
                    HashSet<ItemStack> cartInventory = new HashSet<ItemStack>();

                    Inventory originInv = null;
                    try {
                        originInv = ((Chest) carts.get(sm).getLocation().getBlock().getState()).getInventory();
                        originInv.addItem(new ItemStack(Material.STORAGE_MINECART, 1));
                    } catch (Exception e) {

                    }
                    boolean isFull = false;
                    cartInventory.addAll(Arrays.asList(sm.getInventory().getContents()));
                    for (ItemStack is : cartInventory) {
                        try {
                            if (!isFull) {
                                if (currentChest.getBlockInventory().firstEmpty() < 0) {
                                    isFull = true;
                                    if (originInv == null || originInv.firstEmpty() < 0) {
                                        break;
                                    } else {
                                        originInv.addItem(is);
                                        sm.getInventory().removeItem(is);
                                    }
                                }
                                sm.getInventory().removeItem(is);
                                currentChest.getInventory().addItem(is);
                            } else {
                                sm.getInventory().removeItem(is);
                                originInv.addItem(is);
                            }
                        } catch (NullPointerException npe) {

                        }
                    }
                    try {
                        Chest chest = (Chest) r.getLocation().getBlock().getState();
                        if (chest.getBlockInventory().firstEmpty() > -1) {
                            chest.getBlockInventory().addItem(new ItemStack(Material.STORAGE_MINECART));
                        }
                    } catch (Exception e) {
                        
                    }
                    removeMe.add(r);
                    if (!cacheRegions.containsKey(r)) {
                        cacheRegions.put(r, re);
                    }
                }
            }

            for (Region rr : removeMe) {
                carts.get(rr).remove();
                carts.remove(rr);
            }
        }
    }
}
