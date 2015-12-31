package multitallented.plugins.townships.effects;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import multitallented.redcastlemedia.bukkit.townships.Townships;
import multitallented.redcastlemedia.bukkit.townships.Util;
import multitallented.redcastlemedia.bukkit.townships.effect.Effect;
import multitallented.redcastlemedia.bukkit.townships.events.ToRegionCreatedEvent;
import multitallented.redcastlemedia.bukkit.townships.events.ToTwoSecondEffectEvent;
import multitallented.redcastlemedia.bukkit.townships.region.Region;
import multitallented.redcastlemedia.bukkit.townships.region.RegionManager;
import multitallented.redcastlemedia.bukkit.townships.region.RegionType;
import multitallented.redcastlemedia.bukkit.townships.region.SuperRegion;
import multitallented.redcastlemedia.bukkit.townships.region.TOItem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author Multitallented
 */
public class EffectWarehouse extends Effect {
    public EffectWarehouse(Townships plugin) {
        super(plugin);
        registerEvent(new UpkeepListener(this));
    }

    @Override
    public void init(Townships plugin) {
        super.init(plugin);

    }

    public class UpkeepListener implements Listener {
        private final EffectWarehouse effect;
        public HashMap<Region, List<Location>> invs = new HashMap<Region, List<Location>>();
        public UpkeepListener(EffectWarehouse effect) {
            this.effect = effect;
        }

        @EventHandler
        public void onChestPlace(BlockPlaceEvent event) {
            if (event.isCancelled() || event.getBlock().getType() != Material.CHEST) {
                return;
            }
            List<Region> regions = getPlugin().getRegionManager().getContainingBuildRegions(event.getBlock().getLocation());
            if (regions.isEmpty()) {
                return;
            }
            Region r = regions.get(0);
            RegionType rt = getPlugin().getRegionManager().getRegionType(r.getType());

            boolean hasEffect = false;
            for (String s : rt.getEffects()) {
                if (s.startsWith("warehouse")) {
                    hasEffect = true;
                }
            }
            if (!hasEffect) {
                return;
            }

            Location l = event.getBlock().getLocation();

            File dataFolder = new File(getPlugin().getDataFolder(), "data");
            if (!dataFolder.exists()) {
                return;
            }
            File dataFile = new File(dataFolder, r.getID() + ".yml");
            if (!dataFile.exists()) {
                return;
            }
            FileConfiguration config = new YamlConfiguration();
            try {
                config.load(dataFile);
                List<String> locationList = config.getStringList("chests");
                locationList.add(l.getWorld().getName() + ":" + l.getX() + ":" + l.getY() + ":" + l.getZ());
                config.set("chests", locationList);
                config.save(dataFile);
            } catch (Exception e) {
                getPlugin().warning("[Townships] Unable to save new chest for " + r.getID() + ".yml");
                return;
            }

            if (!invs.containsKey(r)) {
                invs.put(r, new ArrayList<Location>());
            }
            invs.get(r).add(l);
        }

        @EventHandler
        public void onRegionCreated(ToRegionCreatedEvent event) {
            Region r = event.getRegion();
            RegionType rt = getPlugin().getRegionManager().getRegionType(r.getType());
            if (rt == null) {
                return;
            }

            boolean hasEffect = false;
            for (String s : rt.getEffects()) {
                if (s.startsWith("warehouse")) {
                    hasEffect = true;
                }
            }
            if (!hasEffect) {
                return;
            }

            List<Location> chests = new ArrayList<Location>();
            try {
                chests.add(r.getLocation());
            } catch (Exception e) {
                return;
            }

            double lx = Math.floor(r.getLocation().getX()) + 0.4;
            double ly = Math.floor(r.getLocation().getY()) + 0.4;
            double lz = Math.floor(r.getLocation().getZ()) + 0.4;
            double buildRadius = rt.getRawBuildRadius();

            int x = (int) Math.round(lx - buildRadius);
            int y = (int) Math.round(ly - buildRadius);
            y = y < 0 ? 0 : y;
            int z = (int) Math.round(lz - buildRadius);
            int xMax = (int) Math.round(lx + buildRadius);
            int yMax = (int) Math.round(ly + buildRadius);
            World world = r.getLocation().getWorld();
            yMax = yMax > world.getMaxHeight() - 1 ? world.getMaxHeight() - 1 : yMax;
            int zMax = (int) Math.round(lz + buildRadius);

            for (int i = x; i < xMax; i++) {
                for (int j = y; j < yMax; j++) {
                    for (int k = z; k < zMax; k++) {
                        Block block = world.getBlockAt(i,j,k);

                        if (block.getType() == Material.CHEST) {
                            chests.add(block.getLocation());
                        }
                    }
                }
            }


            File dataFolder = new File(getPlugin().getDataFolder(), "data");
            if (!dataFolder.exists()) {
                return;
            }
            File dataFile = new File(dataFolder, r.getID() + ".yml");
            if (!dataFile.exists()) {
                return;
            }
            FileConfiguration config = new YamlConfiguration();
            try {
                config.load(dataFile);
                List<String> locationList = new ArrayList<String>();
                for (Location l : chests) {
                    locationList.add(l.getWorld().getName() + ":" + l.getX() + ":" + l.getY() + ":" + l.getZ());
                }
                config.set("chests", locationList);
                config.save(dataFile);
            } catch (Exception e) {
                getPlugin().warning("[Townships] Unable to save new chest for " + r.getID() + ".yml");
                return;
            }
            invs.put(r, chests);
        }

        @EventHandler
        public void onCustomEvent(ToTwoSecondEffectEvent event) {
            if (!event.getEffect()[0].startsWith("warehouse")) {
                return;
            }
            //declarations
            Region r            = event.getRegion();
            Location l          = r.getLocation();
            RegionManager rm    = getPlugin().getRegionManager();
            RegionType rt       = rm.getRegionType(r.getType());
            Chest rChest        = null;
            List<Chest> availableItems = new ArrayList<Chest>();


            if (rt == null) {
                return;
            }

            //Is there a center chest?
            if (r.getLocation().getBlock().getType() != Material.CHEST) {
                return;
            } else {
                rChest = (Chest) r.getLocation().getBlock().getState();
            }


            //Check to see if the region has enough reagents
            if (!effect.hasReagents(l)) {
                return;
            }


            //Check for excess chests
            if (!invs.containsKey(r)) {
                File dataFolder = new File(getPlugin().getDataFolder(), "data");
                if (!dataFolder.exists()) {
                    return;
                }
                File dataFile = new File(dataFolder, r.getID() + ".yml");
                if (!dataFile.exists()) {
                    System.out.println("[Townships] data file not found " + r.getID() + ".yml");
                    return;
                }
                FileConfiguration config = new YamlConfiguration();
                try {
                    config.load(dataFile);
                    List<Location> tempLocations = processLocationList(config.getStringList("chests"), event.getLocation().getWorld());
                    for (Location lo : tempLocations) {
                        Block block = lo.getBlock();
                        if (block.getType() != Material.CHEST) {
                            continue;
                        }
                        Chest chest = (Chest) block.getState();
                        availableItems.add(chest);
                    }
                    invs.put(r, tempLocations);
                } catch (Exception e) {
                    getPlugin().warning("[Townships] Unable to load chests from " + r.getID() + ".yml");
                    e.printStackTrace();
                    return;
                }
            } else {
                List<Location> removeMe = new ArrayList<Location>();
                for (Location lo : invs.get(r)) {
                    if (lo.getBlock().getType() != Material.CHEST) {
                        removeMe.add(lo);
                        continue;
                    }
                    availableItems.add((Chest) lo.getBlock().getState());
                }

                //Remove excess chests
                deletefromfile: if (!removeMe.isEmpty()) {
                    for (Location lo : removeMe) {
                        invs.get(r).remove(lo);
                    }
                    File dataFolder = new File(getPlugin().getDataFolder(), "data");
                    if (!dataFolder.exists()) {
                        break deletefromfile;
                    }
                    File dataFile = new File(dataFolder, r.getID() + ".yml");
                    if (!dataFile.exists()) {
                        break deletefromfile;
                    }
                    FileConfiguration config = new YamlConfiguration();
                    try {
                        config.load(dataFile);
                        List<String> locationList = new ArrayList<String>();
                        for (Location loc : invs.get(r)) {
                            locationList.add(loc.getWorld().getName() + ":" + loc.getX() + ":" + loc.getY() + ":" + loc.getZ());
                        }
                        config.set("chests", locationList);
                        config.save(dataFile);
                    } catch (Exception e) {
                        getPlugin().warning("[Townships] Unable to save new chest for " + r.getID() + ".yml");
                        break deletefromfile;
                    }
                }
            }

            //tidy central chest
            tidy: {
                int firstEmpty = rChest.getBlockInventory().firstEmpty();
                if (firstEmpty < 9) {
                    break tidy;
                }

                for (Chest currentChest : availableItems) {
                    if (currentChest.equals(rChest)) {
                        continue;
                    }
                    int currentChestFirstEmpty = currentChest.getBlockInventory().firstEmpty();
                    int i = 0;
                    while (currentChestFirstEmpty > -1 && i < 40) {
                        //Are we done moving things out of the chest?
                        ItemStack[] contents = rChest.getBlockInventory().getContents();
                        ItemStack is = null;
                        for (int k = contents.length; k > 0; k--) {
                            if (contents[k-1] != null && contents[k-1].getType() != Material.AIR) {
                                is = contents[k-1];
                                break;
                            }
                        }
                        if (is == null) {
                            break tidy;
                        }
                        
                        //Move the items
                        TOItem item = TOItem.createFromItemStack(is);
                        List<TOItem> tempList = new ArrayList<TOItem>();
                        tempList.add(item);
                        List<List<TOItem>> temptemp = new ArrayList<List<TOItem>>();
                        temptemp.add(tempList);
                        Util.removeItems(temptemp, rChest.getBlockInventory());
                        List<ItemStack> remainingItems = Util.addItems(temptemp, currentChest.getBlockInventory());
                        for (ItemStack iss : remainingItems) {
                            rChest.getBlockInventory().addItem(iss);
                        }
                        //currentChest.getBlockInventory().addItem(is);
                        currentChestFirstEmpty = currentChest.getBlockInventory().firstEmpty();
                        i++;
                    }
                }
            }

            List<Region> deliverTo = new ArrayList<Region>();
            //Check if any regions nearby need items
            for (SuperRegion sr : rm.getContainingSuperRegions(r.getLocation())) {
                outer: for (Region re : rm.getContainedRegions(sr)) {
                    if (effect.hasReagents(re.getLocation())) {
                        continue;
                    }
                    for (OfflinePlayer s : r.getOwners()) {
                        if (re.getOwners().isEmpty() || !re.getOwners().contains(s)) {
                            continue;
                        }
                        deliverTo.add(re);
                        continue outer;
                    }
                    for (OfflinePlayer s : r.getMembers()) {
                        if (re.getOwners().isEmpty() || !re.getOwners().contains(s)) {
                            continue;
                        }
                        deliverTo.add(re);
                        continue outer;
                    }
                }
            }
            for (Region re : deliverTo) {
                if (re.getLocation().getBlock().getType() != Material.CHEST) {
                    continue;
                }
                Chest chest = (Chest) re.getLocation().getBlock().getState();
                RegionType ret = rm.getRegionType(re.getType());
                if (ret == null) {
                    continue;
                }
                if (chest.getBlockInventory() == null) {
                    continue;
                }
                List<List<TOItem>> missingItems = getMissingItems(ret, chest);

                if (missingItems.isEmpty()) {
                    continue;
                }
                moveNeededItems(re, availableItems, missingItems);
            }
        }

        private List<Location> processLocationList(List<String> input, World world) {
            List<Location> tempList = new ArrayList<Location>();
            for (String s : input) {
                String[] splitString = s.split(":");
                if (s.length() < 4) {
                    continue;
                }
                tempList.add(new Location(world, Double.parseDouble(splitString[1]),
                        Double.parseDouble(splitString[2]),
                        Double.parseDouble(splitString[3])));
            }
            return tempList;
        }

        private HashSet<HashSet<TOItem>> convertToHashSet(List<List<TOItem>> input) {
            HashSet<HashSet<TOItem>> returnMe = new HashSet<HashSet<TOItem>>();
            for (List<TOItem> reqs : input) {
                HashSet<TOItem> tempSet = new HashSet<TOItem>();
                for (TOItem item : reqs) {
                    tempSet.add(item.clone());
                }
                returnMe.add(tempSet);
            }
            return returnMe;
        }
        
        private void moveNeededItems(Region destination, List<Chest> availableItems, List<List<TOItem>> neededItems) {
            Chest destinationChest = null;

            try {
                destinationChest = (Chest) destination.getLocation().getBlock().getState();
            } catch (Exception e) {
                return;
            }
            if (destinationChest == null || destinationChest.getBlockInventory().firstEmpty() < 0) {
                return;
            }

            HashMap<Chest, HashMap<Integer, ItemStack>> itemsToMove = new HashMap<Chest, HashMap<Integer, ItemStack>>();


            HashSet<HashSet<TOItem>> req = convertToHashSet(neededItems);
            outer2: for (Iterator<HashSet<TOItem>> it = req.iterator(); it.hasNext();) {
                HashSet<TOItem> orReqs = it.next();
                outer1: for (Iterator<TOItem> its = orReqs.iterator(); its.hasNext();) {
                    TOItem orReq = its.next();
                    outer: for (Chest chest : availableItems) {
                        try {
                            Inventory inv = chest.getBlockInventory();

                            int i = 0;
                            for (ItemStack is : inv.getContents()) {
                                if (is != null && is.getType() != Material.AIR && orReq.equivalentItem(is, true)) {

                                    if (!itemsToMove.containsKey(chest)) {
                                        itemsToMove.put(chest, new HashMap<Integer, ItemStack>());
                                    }

                                    ItemStack nIS = new ItemStack(is);
                                    if (orReq.getQty() > is.getAmount()) {
                                        orReq.setQty(orReq.getQty() - is.getAmount());
                                        itemsToMove.get(chest).put(i, nIS);
                                    } else {
                                        if (orReq.getQty() < is.getAmount()) {
                                            nIS.setAmount(is.getAmount() - orReq.getQty());
                                        }
                                        itemsToMove.get(chest).put(i, nIS);

                                        its.remove();
                                        if (orReqs.isEmpty()) {
                                            it.remove();
                                            continue outer2;
                                        }

                                        continue outer1;
                                    }
                                }
                                i++;
                            }
                        } catch (Exception e) {
                            getPlugin().warning("[Townships] error moving items from warehouse");
                        }
                    }
                }
            }

            Inventory destinationInventory = destinationChest.getBlockInventory();

            //move items from warehouse to needed region
            outerNew: for (Chest chest : itemsToMove.keySet()) {
                for (Integer i : itemsToMove.get(chest).keySet()) {
                    ItemStack moveMe = itemsToMove.get(chest).get(i);
                    TOItem item = TOItem.createFromItemStack(moveMe);
                    List<TOItem> tempList = new ArrayList<TOItem>();
                    tempList.add(item);
                    List<List<TOItem>> temptemp = new ArrayList<List<TOItem>>();
                    temptemp.add(tempList);
                    Util.removeItems(temptemp, chest.getBlockInventory());
                    //chest.getBlockInventory().removeItem(moveMe);
                    Util.addItems(temptemp, destinationInventory);
                    //destinationInventory.addItem(moveMe);

                    if (destinationInventory.firstEmpty() < 0) {
                        chest.update();
                        break outerNew;
                    }
                }
            }
            destinationChest.update();
        }

        private List<List<TOItem>> getMissingItems(RegionType rt, Chest chest) {
            List<List<TOItem>> req = new ArrayList<List<TOItem>>();
            for (List<TOItem> list : rt.getReagents()) {
                List<TOItem> tempList = new ArrayList<TOItem>();
                for (TOItem item : list) {
                    tempList.add(item.clone());
                }
                req.add(tempList);
            }
            Inventory inv = chest.getBlockInventory();
            if (inv == null) {
                return req;
            }

            
            HashMap<Integer, List<TOItem>> removeMe = new HashMap<Integer, List<TOItem>>();
            int k = 0;
            outer: for (List<TOItem> orReqs : req) {
                
                for (TOItem orReq : orReqs) {

                    for (ItemStack iss : inv.getContents()) {
                        if (iss == null) {
                            continue;
                        }

                        if (iss.getType() == orReq.getMat() &&
                                (orReq.isWildDamage() || orReq.getDamage() == (int) (iss.getDurability()))) {
                            if (orReq.getQty() - iss.getAmount() > 0) {
                                orReq.setQty(orReq.getQty() - iss.getAmount());
                            } else {
                                
                                if (!removeMe.containsKey(k)) {
                                    removeMe.put(k, new ArrayList<TOItem>());
                                }
                                removeMe.get(k).add(orReq);
                                break;
                            }
                        }
                    }
                }
                k++;
            }
            List<List<TOItem>> removeLists = new ArrayList<List<TOItem>>();
            for (Integer i : removeMe.keySet()) {
                for (TOItem item : removeMe.get(i)) {
                    req.get(i).remove(item);
                }
                if (removeMe.get(i).isEmpty()) {
                    removeLists.add(removeMe.get(i));
                }
            }
            
            for (List<TOItem> orReqs : removeLists) {
                req.remove(orReqs);
            }
            return req;
        }
    }

}
