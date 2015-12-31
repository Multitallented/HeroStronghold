package multitallented.redcastlemedia.bukkit.townships.region;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import multitallented.redcastlemedia.bukkit.townships.ConfigManager;
import multitallented.redcastlemedia.bukkit.townships.PermSet;
import multitallented.redcastlemedia.bukkit.townships.Townships;
import multitallented.redcastlemedia.bukkit.townships.Util;
import multitallented.redcastlemedia.bukkit.townships.effect.Effect;
import multitallented.redcastlemedia.bukkit.townships.events.*;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author Multitallented
 */
public class RegionManager {
    private final Map<Location, Region> liveRegions = new HashMap<Location, Region>();
    private final Map<Integer, Region> idRegions = new HashMap<Integer, Region>();
    private final List<Region> sortedRegions = new ArrayList<Region>();
    private final Map<String, SuperRegion> liveSuperRegions = new HashMap<String, SuperRegion>();
    private final List<SuperRegion> sortedSuperRegions = new ArrayList<SuperRegion>();
    private final Map<String, RegionType> regionTypes = new HashMap<String, RegionType>();
    private final Map<String, SuperRegionType> superRegionTypes = new HashMap<String, SuperRegionType>();
    private final Townships plugin;
    private FileConfiguration dataConfig;
    private final ConfigManager configManager;
    private final HashMap<SuperRegion, HashSet<SuperRegion>> wars = new HashMap<SuperRegion, HashSet<SuperRegion>>();
    private HashMap<String, PermSet> permSets = new HashMap<String, PermSet>();
    private final HashSet<String> possiblePermSets = new HashSet<String>();
    private final List<Region> sortedBuildRegions = new ArrayList<Region>();
    private final HashMap<String, List<String>> regionCategories = new HashMap<String, List<String>>();
    
    public RegionManager(Townships plugin, FileConfiguration config) {
        this.plugin = plugin;
        configManager = new ConfigManager(config, plugin);
        plugin.setConfigManager(configManager);
        load();
    }
    
    public void reload() {
        liveRegions.clear();
        idRegions.clear();
        sortedRegions.clear();
        liveSuperRegions.clear();
        regionTypes.clear();
        superRegionTypes.clear();
        wars.clear();
        permSets = new HashMap<String, PermSet>();
        possiblePermSets.clear();
        sortedBuildRegions.clear();
        regionCategories.clear();

        load();
    }
    private void load() {
        permSets = PermSet.loadPermSets(plugin);
        for (String s : permSets.keySet()) {
            possiblePermSets.add(s);
        }
        
        File regionFolder = new File(plugin.getDataFolder(), "RegionConfig");
        if (!regionFolder.exists()) {
            File regionFile = new File(plugin.getDataFolder(), "regions.yml");
            if (!regionFile.exists()) {
                DefaultRegions.createDefaultRegionFiles(plugin);
            } else {
                DefaultRegions.migrateRegions(regionFile, plugin);
            }
        }
        for (File currentRegionFile : regionFolder.listFiles()) {
            if (currentRegionFile.isFile()) {
                try {
                    FileConfiguration rConfig = new YamlConfiguration();
                    rConfig.load(currentRegionFile);
                    String regionName = currentRegionFile.getName().replace(".yml", "");
                    HashMap<String, List<String>> namedItems = processNamedItems(rConfig.getConfigurationSection("named-items"));
                    regionTypes.put(regionName, new RegionType(regionName,
                            rConfig.getString("group", regionName),
                            rConfig.getStringList("effects"),
                            Math.pow(rConfig.getInt("radius") + 0.4, 2),
                            Math.pow(rConfig.getInt("build-radius", rConfig.getInt("radius", 2)) + 0.4, 2),
                            processItemStackList(rConfig.getStringList("requirements"), currentRegionFile.getName()),
                            rConfig.getStringList("super-regions"),
                            processItemStackList(rConfig.getStringList("reagents"), currentRegionFile.getName(), namedItems),
                            processItemStackList(rConfig.getStringList("input"), currentRegionFile.getName(), namedItems),
                            processItemStackList(rConfig.getStringList("output"), currentRegionFile.getName(), namedItems),
                            rConfig.getDouble("cost"),
                            rConfig.getDouble("payout"),
                            rConfig.getDouble("exp"),
                            rConfig.getString("description"),
                            rConfig.getInt("power-drain", 0),
                            rConfig.getInt("housing", 0),
                            rConfig.getStringList("biome"),
                            Util.stringToItemStack(rConfig.getString("icon","1")),
                            rConfig.getInt("min-y", -1),
                            rConfig.getInt("max-y", -1),
                            rConfig.getDouble("unlock", 0),
                            rConfig.getDouble("salvage", 0),
                            namedItems
                    ));
                    if (!regionCategories.containsKey("")) {
                        List<String> tempList = new ArrayList<String>();
                        tempList.add(regionName);
                        regionCategories.put("", tempList);
                    } else {
                        regionCategories.get("").add(regionName);
                    }
                } catch (Exception e) {
                    plugin.warning("[Townships] failed to load " + currentRegionFile.getName());
                    e.printStackTrace();
                }
            } else if (currentRegionFile.isDirectory()) {
                for (File cRegionFile : currentRegionFile.listFiles()) {
                    try {
                        FileConfiguration rConfig = new YamlConfiguration();
                        rConfig.load(cRegionFile);
                        String regionName = cRegionFile.getName().replace(".yml", "");
                        HashMap<String, List<String>> namedItems = processNamedItems(rConfig.getConfigurationSection("named-items"));
                        regionTypes.put(regionName, new RegionType(regionName,
                                rConfig.getString("group", regionName),
                                rConfig.getStringList("effects"),
                                Math.pow(rConfig.getInt("radius") + 0.4, 2),
                                Math.pow(rConfig.getInt("build-radius", rConfig.getInt("radius", 2)) + 0.4, 2),
                                processItemStackList(rConfig.getStringList("requirements"), cRegionFile.getName()),
                                rConfig.getStringList("super-regions"),
                                processItemStackList(rConfig.getStringList("reagents"), cRegionFile.getName(), namedItems),
                                processItemStackList(rConfig.getStringList("input"), cRegionFile.getName(), namedItems),
                                processItemStackList(rConfig.getStringList("output"), cRegionFile.getName(), namedItems),
                                rConfig.getDouble("cost"),
                                rConfig.getDouble("payout"),
                                rConfig.getDouble("exp"),
                                rConfig.getString("description"),
                                rConfig.getInt("power-drain", 0),
                                rConfig.getInt("housing", 0),
                                rConfig.getStringList("biome"),
                                Util.stringToItemStack(rConfig.getString("icon","1")),
                                rConfig.getInt("min-y", -1),
                                rConfig.getInt("max-y", -1),
                                rConfig.getDouble("unlock", 0),
                                rConfig.getDouble("salvage", 0),
                                namedItems
                        ));
                        if (!regionCategories.containsKey(currentRegionFile.getName().toLowerCase())) {
                            List<String> tempList = new ArrayList<String>();
                            tempList.add(regionName);
                            regionCategories.put(currentRegionFile.getName().toLowerCase(), tempList);
                        } else {
                            regionCategories.get(currentRegionFile.getName().toLowerCase()).add(regionName);
                        }
                    } catch (Exception e) {
                        plugin.warning("[Townships] failed to load " + cRegionFile.getName());
                        e.printStackTrace();
                    }
                }
            }
        }
        
        File suRegionFolder = new File(plugin.getDataFolder(), "SuperRegionConfig");
        if (!suRegionFolder.exists()) {
            File sRegionFile = new File(plugin.getDataFolder(), "super-regions.yml");
            if (!sRegionFile.exists()) {
                DefaultRegions.createDefaultSuperRegionFiles(plugin);
            } else {
                DefaultRegions.migrateSuperRegions(sRegionFile, plugin);
            }
        }
        for (File currentRegionFile : suRegionFolder.listFiles()) {
            try {
                FileConfiguration rConfig = new YamlConfiguration();
                rConfig.load(currentRegionFile);
                String regionName = currentRegionFile.getName().replace(".yml", "");
                superRegionTypes.put(regionName, new SuperRegionType(regionName,
                        rConfig.getStringList("effects"),
                        Math.pow(rConfig.getInt("radius") + 0.4, 2),
                        //processRegionTypeMap(rConfig.getStringList("requirements")),
                        rConfig.getStringList("requirements"),
                        rConfig.getDouble("cost", 0),
                        rConfig.getDouble("payout", 0),
                        rConfig.getStringList("children"),
                        rConfig.getInt("max-power", 100),
                        rConfig.getInt("daily-power-increase", 10),
                        rConfig.getInt("charter", 0),
                        rConfig.getDouble("exp", 0),
                        rConfig.getString("central-structure"),
                        rConfig.getString("description"),
                        rConfig.getInt("population", 0),
                        Util.stringToItemStack(rConfig.getString("icon", "1")),
                        rConfig.getStringList("limits"),
                        rConfig.getDouble("unlock", 0)
                ));
            } catch (Exception e) {
                plugin.warning("[Townships] failed to load " + currentRegionFile.getName());
            }
        }

        File playerFolder = new File(plugin.getDataFolder(), "data"); // Setup the Data Folder if it doesn't already exist
        playerFolder.mkdirs();
        for (File regionFile : playerFolder.listFiles()) {
            try {
                //Load saved region data
                dataConfig = new YamlConfiguration();
                dataConfig.load(regionFile);
                String locationString = dataConfig.getString("location");
                if (locationString != null) {
                    Location location = null;
                    if (locationString != null) {
                        String[] params = locationString.split(":");
                        World world  = plugin.getServer().getWorld(params[0]);
                        location = new Location(world, Double.parseDouble(params[1]),Double.parseDouble(params[2]),Double.parseDouble(params[3]));
                    }
                    String type = dataConfig.getString("type");
                    List<OfflinePlayer> owners = dataConfig.getStringList("owners").stream().map(UUID::fromString).map(Bukkit::getOfflinePlayer).collect(Collectors.toList());
                    List<OfflinePlayer> members = dataConfig.getStringList("members").stream().map(UUID::fromString).map(Bukkit::getOfflinePlayer).collect(Collectors.toList());
                    if (owners == null) {
                        owners = new ArrayList<OfflinePlayer>();
                    }
                    if (members == null) {
                        members = new ArrayList<OfflinePlayer>();
                    }
                    if (location != null && type != null) {
                        try {
                            liveRegions.put(location, new Region(Integer.parseInt(regionFile.getName().replace(".yml", "")), location, type, owners, members));

                            sortedRegions.add(liveRegions.get(location));
                            sortedBuildRegions.add(liveRegions.get(location));
                            idRegions.put(liveRegions.get(location).getID(), liveRegions.get(location));
                        } catch (NullPointerException npe) {
                            System.out.println("[Townships] failed to load data from " + regionFile.getName());
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("[Townships] failed to load data from " + regionFile.getName());
                System.out.println(e.getStackTrace());
            }
        }
        if (sortedRegions.size() > 1) {
            Collections.sort(sortedRegions, new Comparator<Region>() {

                @Override
                public int compare(Region o1, Region o2) {
                    return (int) (-1 *(o1.getLocation().getX() + getRegionType(o1.getType()).getRawRadius() - (o2.getLocation().getX() + getRegionType(o2.getType()).getRawRadius())));
                }
            });
        }
        if (sortedBuildRegions.size() > 1) {
            Collections.sort(sortedBuildRegions, new Comparator<Region>() {

                @Override
                public int compare(Region o1, Region o2) {
                    return (int) (-1 *(o1.getLocation().getX() + getRegionType(o1.getType()).getRawBuildRadius() - (o2.getLocation().getX() + getRegionType(o2.getType()).getRawBuildRadius())));
                }
            });
        }
        
        //Load super regions
        File sRegionFolder = new File(plugin.getDataFolder(), "superregions"); // Setup the Data Folder if it doesn't already exist
        sRegionFolder.mkdirs();
        
        for (File sRegionFile : sRegionFolder.listFiles()) {
            try {
                //Load saved region data
                FileConfiguration sRegionDataConfig = new YamlConfiguration();
                sRegionDataConfig.load(sRegionFile);
                String name = sRegionFile.getName().replace(".yml", "");
                String locationString = sRegionDataConfig.getString("location", "0:64:0");
                if (locationString != null) {
                    Location location = null;
                    if (locationString != null) {
                        String[] params = locationString.split(":");
                        World world  = plugin.getServer().getWorld(params[0]);
                        location = new Location(world, Double.parseDouble(params[1]),Double.parseDouble(params[2]),Double.parseDouble(params[3]));
                    }
                    String type = sRegionDataConfig.getString("type", "shack");
                    List<OfflinePlayer> owners = sRegionDataConfig.getStringList("owners").stream().map(UUID::fromString).map(Bukkit::getOfflinePlayer).collect(Collectors.toList());
                    ConfigurationSection configMembers = sRegionDataConfig.getConfigurationSection("members");
                    Map<OfflinePlayer, List<String>> members = new HashMap<OfflinePlayer, List<String>>();
                    for (String s : configMembers.getKeys(false)) {
                        List<String> perm = configMembers.getStringList(s);
                        if (perm.contains("member")) {
                            members.put(Bukkit.getOfflinePlayer(UUID.fromString(s)), configMembers.getStringList(s));
                        }
                    }
                    int power = sRegionDataConfig.getInt("power", 10);
                    double taxes = sRegionDataConfig.getDouble("taxes", 0.0);
                    double balance = sRegionDataConfig.getDouble("balance", 0.0);
                    List<Double> taxRevenue1 = sRegionDataConfig.getDoubleList("tax-revenue");
                    LinkedList<Double> taxRevenue = new LinkedList<Double>();
                    int maxPower = sRegionDataConfig.getInt("max-power", this.getSuperRegionType(type).getMaxPower());
                    if (taxRevenue1 != null) {
                        for (double d : taxRevenue1) {
                            taxRevenue.add(d);
                        }
                    }
                    List<String> preProcessedLocationList = sRegionDataConfig.getStringList("child-locations");
                    List<Location> childLocations = processLocationList(preProcessedLocationList);
                    long lastDisable = sRegionDataConfig.getLong("last-disable",0);
                    
                    if (location != null && type != null) {
                        liveSuperRegions.put(name.toLowerCase(), new SuperRegion(name, location, type, owners, members, power, taxes, balance, taxRevenue, maxPower, childLocations, lastDisable));
                        
                        sortedSuperRegions.add(liveSuperRegions.get(name.toLowerCase()));
                    }
                }
            } catch (Exception e) {
                System.out.println("[Townships] failed to load superregions from " + sRegionFile.getName());
                e.printStackTrace();
            }
        }
        if (sortedSuperRegions.size() > 1) {
            Collections.sort(sortedSuperRegions, new Comparator<SuperRegion>() {

                @Override
                public int compare(SuperRegion o1, SuperRegion o2) {
                    return (int) (-1 *(o1.getLocation().getX() + getSuperRegionType(o1.getType()).getRawRadius() - (o2.getLocation().getX() + getSuperRegionType(o2.getType()).getRawRadius())));
                }
            });
        }
        
        FileConfiguration warConfig = new YamlConfiguration();
        try {
            File warFile = new File(plugin.getDataFolder(), "war.yml");
            if (!warFile.exists()) {
                warFile.createNewFile();
            }
            warConfig.load(warFile);
            for (String key : warConfig.getKeys(false)) {
                if (!liveSuperRegions.containsKey(key.toLowerCase())) {
                    continue;
                }
                SuperRegion sr = liveSuperRegions.get(key.toLowerCase());
                HashSet<SuperRegion> tempSet = new HashSet<SuperRegion>();
                for (String s : warConfig.getStringList(key)) {
                    if (liveSuperRegions.containsKey(s.toLowerCase())) {
                        tempSet.add(liveSuperRegions.get(s.toLowerCase()));
                    }
                }
                wars.put(sr, tempSet);
            }
        } catch (Exception ioe) {
            Logger log = plugin.getLogger();
            log.warning("[Townships] failed to load war.yml");
        }
    }
    
    public void setNewSRLocation(SuperRegion sr, Location loc) {
        String name = sr.getName();
        File dataFile = new File(plugin.getDataFolder() + "/superregions", name + ".yml");
        if (!dataFile.exists()) {
            plugin.warning("[Townships] unable to find " + name + ".yml");
            return;
        }
        try {
            dataFile.createNewFile();
            dataConfig = new YamlConfiguration();
            dataConfig.load(dataFile);
            System.out.println("[Townships] saving new superregion to " + name + ".yml");
            
            dataConfig.set("location", loc.getWorld().getName() + ":" + loc.getX()
                    + ":" + loc.getBlockY() + ":" + loc.getZ());
            
            dataConfig.save(dataFile);
            
        } catch (Exception ioe) {
            System.out.println("[Townships] unable to write super region to file " + name + ".yml");
            ioe.printStackTrace();
            return;
        }
        
        sr.setLocation(loc);
        
        if (sortedSuperRegions.size() > 1) {
            Collections.sort(sortedSuperRegions, new Comparator<SuperRegion>() {

                @Override
                public int compare(SuperRegion o1, SuperRegion o2) {
                    return (int) (-1 *(o1.getLocation().getX() + getSuperRegionType(o1.getType()).getRawRadius() - (o2.getLocation().getX() + getSuperRegionType(o2.getType()).getRawRadius())));
                }
            });
        }
    }
    
    private List<Location> processLocationList(List<String> input) {
        List<Location> tempList = new ArrayList<Location>();
        for (String s : input) {
            try {
                String[] params = s.split(":");
                World world  = plugin.getServer().getWorld(params[0]);
                tempList.add(new Location(world, Double.parseDouble(params[1]),Double.parseDouble(params[2]),Double.parseDouble(params[3])));
            } catch (Exception e) {
                plugin.warning("[Townships] Failed to parse location list.");
            }
        }
        return tempList;
    }
    
    private HashMap<String, List<String>> processNamedItems(ConfigurationSection namedItemConfig) {
        HashMap<String, List<String>> namedItems = new HashMap<String, List<String>>();
        if (namedItemConfig == null) {
            return namedItems;
        }
        for (String s : namedItemConfig.getKeys(false)) {
            List<String> tempList = new ArrayList<String>();
            String key = Util.parseColors(namedItemConfig.getConfigurationSection(s).getString("name"));
            tempList.add(key);
            List<String> lore = namedItemConfig.getConfigurationSection(s).getStringList("lore");
            if (lore != null) {
                for (String loreLine : lore) {
                    tempList.add(Util.parseColors(loreLine));
                }
            }
            namedItems.put(ChatColor.stripColor(key), tempList);
        }
        return namedItems;
    }
    
    private List<List<TOItem>> processItemStackList(List<String> input, String filename) {
        return processItemStackList(input, filename, null);
    }
    
    private List<List<TOItem>> processItemStackList(List<String> input, String filename, HashMap<String, List<String>> namedItems) {
        List<List<TOItem>> returnList = new ArrayList<List<TOItem>>();
        for (String current : input) {
            List<TOItem> cList = new ArrayList<TOItem>();

            if (current.startsWith("g:")) {
                current = processItemGroup(current);
            }

            for (String subItem : current.split(",")) {
                
                String[] params = subItem.split("\\.");
                if (params.length < 2) {
                    plugin.warning("[Townships] could not find item " + params[0] + " in " + filename);
                    continue;
                }
                ItemStack is = null;
                Material cMat = null;
                cMat = Material.getMaterial(params[0]);
                
                if (cMat != null) {
                    is = new ItemStack(cMat);
                    
                } else {
                    int itemID = -1;
                    try {
                        itemID = Integer.parseInt(params[0]);
                    } catch (Exception e) {
                        plugin.warning("[Townships] could not find item " + params[0] + " in " + filename);
                        continue;
                    }
                    
                    itemID = itemID < 0 ? -1 : itemID;
                    if (itemID != -1) {
                        is = new ItemStack(itemID);
                    } else {
                        plugin.warning("[Townships] could not find item " + params[0] + " in " + filename);
                        continue;
                    }
                }
                
                //LOG.1.64.50,LOG.2.64.50
                TOItem hsItem = null;
                try {
                    if (params.length > 4 && namedItems != null && namedItems.get(params[4]) != null) {
                        List<String> metaParts = new ArrayList<String>(namedItems.get(params[4]));
                        String displayName = metaParts.get(0);
                        metaParts.remove(displayName);
                        
                        hsItem = new TOItem(is.getType(), Integer.parseInt(params[2]), Integer.parseInt(params[1]), Integer.parseInt(params[3]), displayName, metaParts);
                    } else if (params.length > 3) {
                        hsItem = new TOItem(is.getType(), Integer.parseInt(params[2]), Integer.parseInt(params[1]), Integer.parseInt(params[3]));
                    } else if (params.length > 2) {
                        hsItem = new TOItem(is.getType(), Integer.parseInt(params[2]), Integer.parseInt(params[1]));
                    } else {
                        hsItem = new TOItem(is.getType(), Integer.parseInt(params[1]));
                    }
                } catch (Exception e) {
                    plugin.warning("[Townships] error reading item " + params[0] + " in " + filename);
                    continue;
                }
                cList.add(hsItem);
            }
            returnList.add(cList);
        }
        return returnList;
    }

    private String processItemGroup(String input) {
        String[] parts = input.split(":");
        if (parts.length < 3) {
            return "1.1";
        }
        String groupName = parts[1];
        int quantity;
        try {
            quantity = Integer.parseInt(parts[2]);
        } catch (Exception e) {
            quantity = 1;
        }
        String unprocessedGroup = new String(configManager.getItemGroups().get(groupName));
        String returnGroup = "";
        String[] unprocessedItems = unprocessedGroup.split(",");
        int chance = (int) (100 / unprocessedItems.length);
        for (String s : unprocessedItems) {
            if (s.split("\\.").length < 2) {
                s += ".-1";
            }
            if (!returnGroup.equals("")) {
                returnGroup += ",";
            }
            returnGroup += s + "." + quantity + "." + chance;
        }
        return returnGroup;
    }
    
    public void addRegion(Location loc, String type, List<OfflinePlayer> owners) {
        int i = 0;
        File dataFile = new File(plugin.getDataFolder() + "/data", i + ".yml");
        while (dataFile.exists()) {
            i++;
            dataFile = new File(plugin.getDataFolder() + "/data", i + ".yml");
        }
        try {
            dataFile.createNewFile();
            dataConfig = new YamlConfiguration();
            System.out.println("[Townships] saving new region to " + i + ".yml");
            //dataConfig.load(dataFile);
            
            
            dataConfig.set("location", loc.getWorld().getName() + ":" + loc.getX()
                    + ":" + loc.getBlockY() + ":" + loc.getZ());
            dataConfig.set("type", type);
            dataConfig.set("owners", owners.stream().map(OfflinePlayer::getUniqueId).map(UUID::toString).collect(Collectors.toList()));
            dataConfig.set("members", new ArrayList<String>());
            dataConfig.save(dataFile);
            liveRegions.put(loc, new Region(i, loc, type, owners, new ArrayList<OfflinePlayer>()));
            idRegions.put(i, liveRegions.get(loc));
            sortedBuildRegions.add(liveRegions.get(loc));
            if (sortedBuildRegions.size() > 1) {
                Collections.sort(sortedBuildRegions, new Comparator<Region>() {

                    @Override
                    public int compare(Region o1, Region o2) {
                        return (int) (-1 *(o1.getLocation().getX() + getRegionType(o1.getType()).getRawBuildRadius() - (o2.getLocation().getX() + getRegionType(o2.getType()).getRawBuildRadius())));
                    }
                });
            }
            sortedRegions.add(liveRegions.get(loc));
            if (sortedRegions.size() > 1) {
                Collections.sort(sortedRegions, new Comparator<Region>() {

                    @Override
                    public int compare(Region o1, Region o2) {
                        return (int) (-1 *(o1.getLocation().getX() + getRegionType(o1.getType()).getRawRadius() - (o2.getLocation().getX() + getRegionType(o2.getType()).getRawRadius())));
                    }
                });
            }
            plugin.getServer().getPluginManager().callEvent(new ToRegionCreatedEvent(liveRegions.get(loc)));
        } catch (Exception ioe) {
            System.out.println("[Townships] unable to write new region to file " + i + ".yml");
            ioe.printStackTrace();
        }
    }
    
    public void addRegion(Location loc, String type, List<OfflinePlayer> owners, List<OfflinePlayer> members) {
        int i = 0;
        File dataFile = new File(plugin.getDataFolder() + "/data", i + ".yml");
        while (dataFile.exists()) {
            i++;
            dataFile = new File(plugin.getDataFolder() + "/data", i + ".yml");
        }
        try {
            dataFile.createNewFile();
            dataConfig = new YamlConfiguration();
            System.out.println("[Townships] saving new region to " + i + ".yml");
            //dataConfig.load(dataFile);
            
            
            dataConfig.set("location", loc.getWorld().getName() + ":" + loc.getX()
                    + ":" + loc.getBlockY() + ":" + loc.getZ());
            dataConfig.set("type", type);
            dataConfig.set("owners", owners.stream().map(OfflinePlayer::getUniqueId).map(UUID::toString).collect(Collectors.toList()));
            dataConfig.set("members", members.stream().map(OfflinePlayer::getUniqueId).map(UUID::toString).collect(Collectors.toList()));
            dataConfig.save(dataFile);
            liveRegions.put(loc, new Region(i, loc, type, owners, members));
            idRegions.put(i, liveRegions.get(loc));
            sortedRegions.add(liveRegions.get(loc));
            sortedBuildRegions.add(liveRegions.get(loc));
            if (sortedBuildRegions.size() > 1) {
                Collections.sort(sortedBuildRegions, new Comparator<Region>() {

                    @Override
                    public int compare(Region o1, Region o2) {
                        return (int) (-1 *(o1.getLocation().getX() + getRegionType(o1.getType()).getRawBuildRadius() - (o2.getLocation().getX() + getRegionType(o2.getType()).getRawBuildRadius())));
                    }
                });
            }
            if (sortedRegions.size() > 1) {
                Collections.sort(sortedRegions, new Comparator<Region>() {

                    @Override
                    public int compare(Region o1, Region o2) {
                        return (int) (-1 *(o1.getLocation().getX() + getRegionType(o1.getType()).getRawRadius() - (o2.getLocation().getX() + getRegionType(o2.getType()).getRawRadius())));
                    }
                });
            }
            plugin.getServer().getPluginManager().callEvent(new ToRegionCreatedEvent(liveRegions.get(loc)));
        } catch (Exception ioe) {
            System.out.println("[Townships] unable to write new region to file " + i + ".yml");
            ioe.printStackTrace();
        }
    }
    
    public boolean addSuperRegion(String name, Location loc, String type, List<OfflinePlayer> owners, Map<OfflinePlayer, List<String>> members, int power, double balance, List<Location> childLocations) {
        File dataFile = new File(plugin.getDataFolder() + "/superregions", name + ".yml");
        if (dataFile.exists()) {
            return false;
        }
        try {
            dataFile.createNewFile();
            dataConfig = new YamlConfiguration();
            System.out.println("[Townships] saving new superregion to " + name + ".yml");
            
            dataConfig.set("location", loc.getWorld().getName() + ":" + loc.getX()
                    + ":" + loc.getBlockY() + ":" + loc.getZ());
            dataConfig.set("type", type);
            dataConfig.set("owners", owners.stream().map(OfflinePlayer::getUniqueId).map(UUID::toString).collect(Collectors.toList()));
            dataConfig.createSection("members");
            for (OfflinePlayer s : members.keySet()) {
                dataConfig.set("members." + s.getUniqueId().toString(), members.get(s));
            }
            dataConfig.set("power", power);
            dataConfig.set("balance", balance);
            dataConfig.save(dataFile);
            int maxPower = this.getSuperRegionType(type).getMaxPower();
            dataConfig.set("max-power", maxPower);
            List<String> childLocationTemp = new ArrayList<String>();
            if (childLocations != null ) {
                for (Location l : childLocations) {
                    childLocationTemp.add(l.getWorld().getName() + ":" + l.getX()
                        + ":" + l.getBlockY() + ":" + l.getZ());
                }
            } else {
                childLocations = new ArrayList<Location>();
            }
            dataConfig.set("child-locations", childLocationTemp);
            dataConfig.save(dataFile);
            liveSuperRegions.put(name.toLowerCase(), new SuperRegion(name, loc, type, owners, members, power, 0.0, balance, new LinkedList<Double>(), maxPower, childLocations, 0));
            
            sortedSuperRegions.add(liveSuperRegions.get(name.toLowerCase()));
            
            if (sortedSuperRegions.size() > 1) {
                if (sortedSuperRegions.size() > 1) {
                    Collections.sort(sortedSuperRegions, new Comparator<SuperRegion>() {

                        @Override
                        public int compare(SuperRegion o1, SuperRegion o2) {
                            return (int) (-1 *(o1.getLocation().getX() + getSuperRegionType(o1.getType()).getRawRadius() - (o2.getLocation().getX() + getSuperRegionType(o2.getType()).getRawRadius())));
                        }
                    });
                }
            }
            plugin.getServer().getPluginManager().callEvent(new ToSuperRegionCreatedEvent(name));
            return true;
        } catch (Exception ioe) {
            System.out.println("[Townships] unable to write new superregion to file " + name + ".yml");
            ioe.printStackTrace();
            return false;
        }
    }
    
    public void destroyRegion(Location l) {
        ////////////////////////////////////////////
        //Note: this method does not remove the region due to Concurrent Modification Exception
        //You have to do that separately with removeRegion(Location l);
        Region currentRegion = liveRegions.get(l);
        RegionType rt = getRegionType(currentRegion.getType());
        srDisabledCheck: {
            if (rt == null) {
                break srDisabledCheck;
            }
            for (SuperRegion sr : getContainingSuperRegions(l)) {
                String message = hasAllRequiredRegions(sr, rt);
                if (message != null) {
                    
                    for (OfflinePlayer playername : sr.getOwners()) {
                        Player currentPlayer = playername.getPlayer();
                        if (currentPlayer != null) {
                            currentPlayer.sendMessage(ChatColor.RED + "[Townships] " + sr.getName() + " is disabled!");
                            currentPlayer.sendMessage("[Townships] " + message);
                        }
                    }
                    
                    for (OfflinePlayer playername : sr.getMembers().keySet()) {
                        Player currentPlayer = playername.getPlayer();                 
                        if (currentPlayer != null) {
                            currentPlayer.sendMessage(ChatColor.RED + "[Townships] " + sr.getName() + " is disabled!");
                            currentPlayer.sendMessage("[Townships] " + message);
                        }
                    }
                }
            }
        }
        
        File dataFile = new File(plugin.getDataFolder() + "/data", currentRegion.getID() + ".yml");
        if (!dataFile.exists()) {
            System.out.println("[Townships] Unable to destroy non-existent region " + currentRegion.getID() + ".yml");
            return;
        }
        if (!dataFile.delete()) {
            System.out.println("[Townships] Unable to destroy non-existent region " + currentRegion.getID() + ".yml");
            return;
        } else {
            System.out.println("[Townships] Successfully destroyed region " + currentRegion.getID() + ".yml");
        }
        final String regionTypeName = currentRegion.getType();
        final Location threadL = l;
        new Runnable()
        {
          @Override
          public void run()
          {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                try {
                    if (p.getLocation().distanceSquared(threadL) < 400) {
                        p.sendMessage(ChatColor.GRAY + "[Townships] " + ChatColor.WHITE + regionTypeName + " was disabled!");
                        if (configManager.getExplode()) {
                            p.sendMessage(ChatColor.GRAY + "[Townships] " + ChatColor.RED + "look out it's going to explode!");
                        }
                    }
                } catch (IllegalArgumentException e) {

                }
            }
          }
        }.run();
        
        plugin.getServer().getPluginManager().callEvent(new ToRegionDestroyedEvent(currentRegion));
        if (configManager.getExplode()) {
            l.getBlock().setType(Material.AIR);
            TNTPrimed tnt = l.getWorld().spawn(l, TNTPrimed.class); 
            tnt.setFuseTicks(1);
            
        }
        l.getBlock().setType(Material.AIR);
    }
    
    public void destroySuperRegion(String name, boolean sendMessage) {
        destroySuperRegion(name, sendMessage, false);
    }
    
    public void destroySuperRegion(String name, boolean sendMessage, boolean evolving) {
        SuperRegion currentRegion = liveSuperRegions.get(name.toLowerCase());

        Bukkit.getPluginManager().callEvent(new ToSuperRegionDestroyedEvent(currentRegion, evolving));
        removeWars(name);
        liveSuperRegions.remove(name.toLowerCase());
        sortedSuperRegions.remove(currentRegion);

        File superRegionFolder = new File(plugin.getDataFolder(), "/superregions");
        for (File superRegionFile : superRegionFolder.listFiles()) {
            if (superRegionFile.getName().replace(".yml", "").equalsIgnoreCase(name)) {
                if (!superRegionFile.delete()) {
                    System.out.println("[Townships] Unable to destroy non-existent superregion " + name + ".yml");

                    if (sendMessage) {
                        final String regionName = name;
                        new Runnable() {
                            @Override
                            public void run()
                            {
                                plugin.getServer().broadcastMessage(ChatColor.GRAY + "[Townships] " + ChatColor.RED + regionName + " was destroyed!");
                            }
                        }.run();
                    }

                    return;
                } else {
                    System.out.println("[Townships] Successfully destroyed superregion " + name + ".yml");

                    if (sendMessage) {
                        final String regionName = name;
                        new Runnable() {
                            @Override
                            public void run()
                            {
                                plugin.getServer().broadcastMessage(ChatColor.GRAY + "[Townships] " + ChatColor.RED + regionName + " was destroyed!");
                            }
                        }.run();
                    }
                    return;
                }
            }
        }

        System.out.println("[Townships] Unable to destroy non-existent superregion " + name + ".yml");
    }
    
    public void addChildLocations(SuperRegion sr, List<Location> childLocations) {
        if (childLocations.isEmpty()) {
            return;
        }
        String name = sr.getName();
        File dataFile = new File(plugin.getDataFolder() + "/superregions", name + ".yml");
        if (!dataFile.exists()) {
            return;
        }
        try {
            dataFile.createNewFile();
            dataConfig = new YamlConfiguration();
            dataConfig.load(dataFile);
            System.out.println("[Townships] saving new superregion to " + name + ".yml");
            List<String> childLocationTemp = new ArrayList<String>();
            for (Location l : sr.getChildLocations()) {
                childLocationTemp.add(l.getWorld().getName() + ":" + l.getX()
                    + ":" + l.getBlockY() + ":" + l.getZ());
            }
            for (Location l : childLocations) {
                childLocationTemp.add(l.getWorld().getName() + ":" + l.getX()
                    + ":" + l.getBlockY() + ":" + l.getZ());
            }
            dataConfig.set("child-locations", childLocationTemp);
            dataConfig.save(dataFile);
        } catch (Exception e) {
            plugin.warning("[Townships] failed to save child location for " + name + ".yml");
            return;
        }
        sr.getChildLocations().addAll(childLocations);
    }
    public void setSRType(SuperRegion sr, String type) {
        String name = sr.getName();
        if (getSuperRegionType(type) == null) {
            plugin.warning("[Townships] null super region type save attempted");
            return;
        }
        File dataFile = new File(plugin.getDataFolder() + "/superregions", name + ".yml");
        if (!dataFile.exists()) {
            return;
        }
        try {
            dataFile.createNewFile();
            dataConfig = new YamlConfiguration();
            dataConfig.load(dataFile);
            System.out.println("[Townships] saving superregion type to " + name + ".yml");
            dataConfig.set("type", type);
            dataConfig.save(dataFile);
        } catch (Exception e) {
            plugin.warning("[Townships] failed to save child location for " + name + ".yml");
            return;
        }
        sr.setType(type);
    }
    public void removeLastChildLocation(SuperRegion sr) {
        if (sr.getChildLocations().isEmpty()) {
            System.out.println("[Townships] child locations is empty");
            return;
        }
        String name = sr.getName();
        File dataFile = new File(plugin.getDataFolder() + "/superregions", name + ".yml");
        if (!dataFile.exists()) {
            System.out.println("[Townships] no file found");
            return;
        }
        try {
            dataFile.createNewFile();
            dataConfig = new YamlConfiguration();
            dataConfig.load(dataFile);
            System.out.println("[Townships] saving superregion to " + name + ".yml");
            List<String> childLocationTemp = new ArrayList<String>();
            for (Location l : sr.getChildLocations()) {
                childLocationTemp.add(l.getWorld().getName() + ":" + l.getX()
                    + ":" + l.getBlockY() + ":" + l.getZ());
            }
            childLocationTemp.remove(childLocationTemp.size() - 1);
            dataConfig.set("child-locations", childLocationTemp);
            dataConfig.save(dataFile);
        } catch (Exception e) {
            plugin.warning("[Townships] failed to save child locations for " + name + ".yml");
            return;
        }
        sr.getChildLocations().remove(sr.getChildLocations().size() - 1);
    }
    
    public void checkIfDestroyedSuperRegion(Location loc) {
        Set<String> regionsToDestroy = new HashSet<String>();
        Region or = getRegion(loc);
        for (SuperRegion sr : this.getContainingSuperRegions(loc)) {
            if (or.getType().equals(getSuperRegionType(sr.getType()).getCentralStructure()) ||
                    getRegionType(or.getType()).getGroup().equals(getSuperRegionType(sr.getType()).getCentralStructure())) {
                regionsToDestroy.add(sr.getName());
            }
        }
        
        for (String s : regionsToDestroy) {
            destroySuperRegion(s, true);
        }
    }
    
    public List<Region> getContainedRegions(SuperRegion sr) {
        List<Region> tempRegions = new ArrayList<Region>();
        Location loc = sr.getLocation();
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        double radius = getSuperRegionType(sr.getType()).getRawRadius();
        for (Region r : getSortedBuildRegions()) {	  	
            Location l = r.getLocation();  	
            if (l.getX() + radius < x) { 	
                break;	  	
            }
            
            if (l.getX() - radius < x && l.getY() + radius > y && l.getY() - radius < y && 
                    l.getZ() + radius > z && l.getZ() - radius < z &&
                    l.getWorld().equals(loc.getWorld())) {
                tempRegions.add(r);
            }
        }
        
        return tempRegions;
    }
    
    public String hasAllRequiredRegions(SuperRegion sr, RegionType missingRegionType) {
        String missingRegions = "";
        Location loc = sr.getLocation();
        SuperRegionType srt = getSuperRegionType(sr.getType());
        Map<String, Integer> reqs = new HashMap<String, Integer>();
        for (String s : srt.getRequirements().keySet()) {
            int amount = srt.getRequirement(s);
            if (missingRegionType != null && (s.equals(missingRegionType.getName()) || s.equals(missingRegionType.getGroup()))) {
                amount += 1;
            }
            reqs.put(new String(s), amount);
            
        }
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        double radius = getSuperRegionType(sr.getType()).getRawRadius();
        for (Region r : getSortedRegions()) {	  	
            Location l = r.getLocation();  	
            if (l.getX() + radius < x) { 	
                break;	  	
            }
            
            if (l.getX() - radius < x && l.getY() + radius > y && l.getY() - radius < y && 
                    l.getZ() + radius > z && l.getZ() - radius < z && l.getWorld().equals(loc.getWorld())) {
                String group = "";
                if (reqs.containsKey(getRegionType(r.getType()).getGroup())) {
                    group = getRegionType(r.getType()).getGroup();
                } else if (reqs.containsKey(getRegionType(r.getType()).getName())) {
                    group = getRegionType(r.getType()).getName();
                }
                if (!group.equals("")) {
                    if (reqs.get(group) < 2) {
                        reqs.remove(group);
                    } else {
                        reqs.put(group, reqs.get(group) - 1);
                    }
                }
            }
        }
        if (reqs.isEmpty()) {
            return null;
        }
        for (String s : reqs.keySet()) {
            if (missingRegions.equals("")) {
                missingRegions = ChatColor.GOLD + "Missing Regions: ";
            } else {
                missingRegions += ", ";
            }
            missingRegions += reqs.get(s) + " " + s;
        }
        return missingRegions;
    }
    
    public boolean hasAllRequiredRegions(SuperRegion sr) {
        Location loc = sr.getLocation();
        SuperRegionType srt = getSuperRegionType(sr.getType());
        Map<String, Integer> reqs = new HashMap<String, Integer>();
        for (String s : srt.getRequirements().keySet()) {
            reqs.put(new String(s), new Integer(srt.getRequirement(s)));
        }
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        double radius = getSuperRegionType(sr.getType()).getRawRadius();
        for (Region r : getSortedRegions()) {	  	
            Location l = r.getLocation();  	
            if (l.getX() + radius < x) { 	
                break;	  	
            }
            
            if (l.getX() - radius < x && l.getY() + radius > y && l.getY() - radius < y && 
                    l.getZ() + radius > z && l.getZ() - radius < z && l.getWorld().equals(loc.getWorld())) {
                String group = "";
                if (reqs.containsKey(getRegionType(r.getType()).getGroup())) {
                    group = getRegionType(r.getType()).getGroup();
                } else if (reqs.containsKey(getRegionType(r.getType()).getName())) {
                    group = getRegionType(r.getType()).getName();
                }
                if (!group.equals("")) {
                    if (reqs.get(group) < 2) {
                        reqs.remove(group);
                    } else {
                        reqs.put(group, reqs.get(group) - 1);
                    }
                }
            }
        }
        if (reqs.isEmpty()) {
            return true;
        }
        return false;
    }
    
    public synchronized void reduceRegion(SuperRegion sr) {
        ConfigManager cm = Townships.getConfigManager();
        if (!cm.getUsePower()) {
            return;
        }
        int powerLoss = cm.getPowerPerKill();
        int currentPower = sr.getPower() - powerLoss;
        currentPower = currentPower > 0 ? currentPower : 0;
        final String st = sr.getName();
        if (currentPower < 26 && sr.getPower() > 25) {
            new Runnable() {
                  @Override
                  public void run()
                  {
                    plugin.getServer().broadcastMessage(ChatColor.RED + "[Townships] " + st + " reached 25 power! Destruction is near!");
                  }
            }.run();
        } else if (currentPower < 11 && sr.getPower() > 10) {
            new Runnable() {
                  @Override
                  public void run()
                  {
                    plugin.getServer().broadcastMessage(ChatColor.RED + "[Townships] " + st + " reached 10 power! Destruction is at hand!");
                  }
            }.run();
        } else if (currentPower < 1) {
            new Runnable() {
                  @Override
                  public void run()
                  {
                    plugin.getServer().broadcastMessage(ChatColor.RED + "[Townships] " + st + " reached 0 power!");
                  }
            }.run();
        }
        setPower(sr, currentPower);
    }
    
    public synchronized void reduceRegion(SuperRegion sr, int power) {
        ConfigManager cm = Townships.getConfigManager();
        if (!cm.getUsePower()) {
            return;
        }
        int powerLoss = power;
        int currentPower = sr.getPower() - powerLoss;
        currentPower = currentPower > 0 ? currentPower : 0;
        final String st = sr.getName();
        if (currentPower < 26 && sr.getPower() > 25) {
            new Runnable() {
                  @Override
                  public void run()
                  {
                    plugin.getServer().broadcastMessage(ChatColor.RED + "[Townships] " + st + " reached 25 power! Destruction is near!");
                  }
            }.run();
        } else if (currentPower < 11 && sr.getPower() > 10) {
            new Runnable() {
                  @Override
                  public void run()
                  {
                    plugin.getServer().broadcastMessage(ChatColor.RED + "[Townships] " + st + " reached 10 power! Destruction is at hand!");
                  }
            }.run();
        } else if (currentPower < 1) {
            new Runnable() {
                  @Override
                  public void run()
                  {
                    plugin.getServer().broadcastMessage(ChatColor.RED + "[Townships] " + st + " reached 0 power!");
                  }
            }.run();
        }
        setPower(sr, currentPower);
    }
    
    public synchronized void setPower(SuperRegion sr, int newPower) {
        ConfigManager cm = Townships.getConfigManager();
        if (!cm.getUsePower()) {
            return;
        }
        int maxPower = sr.getMaxPower();
        if (newPower > maxPower) {
            newPower = maxPower;
        }
        ToPowerChangeEvent powerEvent = new ToPowerChangeEvent(sr, sr.getPower(), newPower);
        Bukkit.getPluginManager().callEvent(powerEvent);
        if (powerEvent.isCancelled()) {
            return;
        }
        newPower = powerEvent.getNewPower();
        
        File superRegionFile = new File(plugin.getDataFolder() + "/superregions", sr.getName() + ".yml");
        if (!superRegionFile.exists()) {
            plugin.warning("Failed to find file " + sr.getName() + ".yml");
            return;
        }
        FileConfiguration sRegionConfig = new YamlConfiguration();
        try {
            sRegionConfig.load(superRegionFile);
        } catch (Exception e) {
            plugin.warning("Failed to load " + sr.getName() + ".yml to save new Power");
            return;
        }
        sRegionConfig.set("power", newPower);
        try {
            sRegionConfig.save(superRegionFile);
        } catch (Exception e) {
            plugin.warning("Failed to save " + sr.getName() + ".yml");
            return;
        }
        sr.setPower(newPower);
    }
    
    public synchronized void setMaxPower(SuperRegion sr, int newPower) {
        ConfigManager cm = Townships.getConfigManager();
        if (!cm.getUsePower()) {
            return;
        }
        File superRegionFile = new File(plugin.getDataFolder() + "/superregions", sr.getName() + ".yml");
        if (!superRegionFile.exists()) {
            plugin.warning("Failed to find file " + sr.getName() + ".yml");
            return;
        }
        FileConfiguration sRegionConfig = new YamlConfiguration();
        try {
            sRegionConfig.load(superRegionFile);
        } catch (Exception e) {
            plugin.warning("Failed to load " + sr.getName() + ".yml to save new Power");
            return;
        }
        sRegionConfig.set("max-power", newPower);
        try {
            sRegionConfig.save(superRegionFile);
        } catch (Exception e) {
            plugin.warning("Failed to save " + sr.getName() + ".yml");
            return;
        }
        sr.setMaxPower(newPower);
    }
    
    /**
     * Adds a double to the taxRevenue record for the SuperRegion
     */
    public synchronized void addTaxRevenue(SuperRegion sr, double input) {
        File superRegionFile = new File(plugin.getDataFolder() + "/superregions", sr.getName() + ".yml");
        if (!superRegionFile.exists()) {
            plugin.warning("Failed to find file " + sr.getName() + ".yml");
            return;
        }
        FileConfiguration sRegionConfig = new YamlConfiguration();
        try {
            sRegionConfig.load(superRegionFile);
        } catch (Exception e) {
            plugin.warning("Failed to load " + sr.getName() + ".yml to save new taxRevenue");
            return;
        }
        sr.addTaxRevenue(input);
        LinkedList<Double> taxRevenue = sr.getTaxRevenue();
        sRegionConfig.set("tax-revenue", taxRevenue);
        try {
            sRegionConfig.save(superRegionFile);
        } catch (Exception e) {
            plugin.warning("Failed to save " + sr.getName() + ".yml");
            return;
        }
    }
    
    public void setTaxes(SuperRegion sr, double taxes) {
        File superRegionFile = new File(plugin.getDataFolder() + "/superregions", sr.getName() + ".yml");
        if (!superRegionFile.exists()) {
            plugin.warning("Failed to find file " + sr.getName() + ".yml");
            return;
        }
        FileConfiguration sRegionConfig = new YamlConfiguration();
        try {
            sRegionConfig.load(superRegionFile);
        } catch (Exception e) {
            plugin.warning("Failed to load " + sr.getName() + ".yml to save new taxes");
            return;
        }
        sRegionConfig.set("taxes", taxes);
        try {
            sRegionConfig.save(superRegionFile);
        } catch (Exception e) {
            plugin.warning("Failed to save " + sr.getName() + ".yml");
            return;
        }
        sr.setTaxes(taxes);
    }
    
    public void setMember(SuperRegion sr, OfflinePlayer name, List<String> input) {
        File superRegionFile = new File(plugin.getDataFolder() + "/superregions", sr.getName() + ".yml");
        if (!superRegionFile.exists()) {
            plugin.warning("Failed to find file " + sr.getName() + ".yml");
            return;
        }
        FileConfiguration sRegionConfig = new YamlConfiguration();
        try {
            sRegionConfig.load(superRegionFile);
        } catch (Exception e) {
            plugin.warning("Failed to load " + sr.getName() + ".yml to save member");
            return;
        }
        if (sr.hasMember(name)) {
            sr.remove(name);
        }
        sr.addMember(name, input);
        sRegionConfig.set("members." + name, input);
        try {
            sRegionConfig.save(superRegionFile);
        } catch (Exception e) {
            plugin.warning("Failed to save " + sr.getName() + ".yml");
            return;
        }
    }
    
    public void removeMember(SuperRegion sr, OfflinePlayer name) {
        File superRegionFile = new File(plugin.getDataFolder() + "/superregions", sr.getName() + ".yml");
        if (!superRegionFile.exists()) {
            plugin.warning("Failed to find file " + sr.getName() + ".yml");
            return;
        }
        FileConfiguration sRegionConfig = new YamlConfiguration();
        try {
            sRegionConfig.load(superRegionFile);
        } catch (Exception e) {
            plugin.warning("Failed to load " + sr.getName() + ".yml to remove member " + name);
            return;
        }
        sr.remove(name);
        sRegionConfig.set("members." + name, new ArrayList<String>());
        try {
            sRegionConfig.save(superRegionFile);
        } catch (Exception e) {
            plugin.warning("Failed to save " + sr.getName() + ".yml");
            return;
        }
    }
    
    public void setOwner(SuperRegion sr, OfflinePlayer name) {
        File superRegionFile = new File(plugin.getDataFolder() + "/superregions", sr.getName() + ".yml");
        if (!superRegionFile.exists()) {
            plugin.warning("Failed to find file " + sr.getName() + ".yml");
            return;
        }
        FileConfiguration sRegionConfig = new YamlConfiguration();
        try {
            sRegionConfig.load(superRegionFile);
        } catch (Exception e) {
            plugin.warning("Failed to load " + sr.getName() + ".yml to save owner");
            return;
        }
        List<OfflinePlayer> owners = sr.getOwners();
        if (owners.contains(name)) {
            owners.remove(name);
        } else {
            owners.add(name);
        }
        sRegionConfig.set("owners", owners);
        try {
            sRegionConfig.save(superRegionFile);
        } catch (Exception e) {
            plugin.warning("Failed to save " + sr.getName() + ".yml");
            return;
        }
    }
    
    public void setMember(Region r, OfflinePlayer name) {
        File regionFile = new File(plugin.getDataFolder() + "/data", r.getID() + ".yml");
        if (!regionFile.exists()) {
            plugin.warning("Failed to find file " + r.getID() + ".yml");
            return;
        }
        FileConfiguration regionConfig = new YamlConfiguration();
        try {
            regionConfig.load(regionFile);
        } catch (Exception e) {
            plugin.warning("Failed to load " + r.getID() + ".yml to save member");
            return;
        }
        List<OfflinePlayer> members = r.getMembers();
        if (members.contains(name)) {
            members.remove(name);
        } else {
            members.add(name);
        }
        regionConfig.set("members", members);
        try {
            regionConfig.save(regionFile);
        } catch (Exception e) {
            plugin.warning("Failed to save " + r.getID() + ".yml");
            return;
        }
    }
    
    public void setOwner(Region r, OfflinePlayer name) {
        File regionFile = new File(plugin.getDataFolder() + "/data", r.getID() + ".yml");
        if (!regionFile.exists()) {
            plugin.warning("Failed to find file " + r.getID() + ".yml");
            return;
        }
        FileConfiguration regionConfig = new YamlConfiguration();
        try {
            regionConfig.load(regionFile);
        } catch (Exception e) {
            plugin.warning("Failed to load " + r.getID() + ".yml to save owner");
            return;
        }
        List<OfflinePlayer> owners = r.getOwners();
        if (owners.contains(name)) {
            owners.remove(name);
        } else {
            owners.add(name);
        }
        regionConfig.set("owners", owners);
        try {
            regionConfig.save(regionFile);
        } catch (Exception e) {
            plugin.warning("Failed to save " + r.getID() + ".yml");
            return;
        }
    }
    
    public void setPrimaryOwner(Region r, OfflinePlayer name) {
        File regionFile = new File(plugin.getDataFolder() + "/data", r.getID() + ".yml");
        if (!regionFile.exists()) {
            plugin.warning("Failed to find file " + r.getID() + ".yml");
            return;
        }
        FileConfiguration regionConfig = new YamlConfiguration();
        try {
            regionConfig.load(regionFile);
        } catch (Exception e) {
            plugin.warning("Failed to load " + r.getID() + ".yml to save owner");
            return;
        }
        List<OfflinePlayer> owners = r.getOwners();
        if (owners.contains(name)) {
            owners.remove(name);
            owners.add(0, name);
        } else {
            owners.add(0, name);
        }
        regionConfig.set("owners", owners);
        try {
            regionConfig.save(regionFile);
        } catch (Exception e) {
            plugin.warning("Failed to save " + r.getID() + ".yml");
            return;
        }
    }
    
    /**
     * Adds (or subtracts if negative) the balance from the super-region.
     * It saves that data to sr.getName() + ".yml".
     * If the new balance would be less than 0, it takes the remainder and
     * subtracts that from the owner's balance.
     */
    public synchronized double addBalance(SuperRegion sr, double balance) {
        File superRegionFile = new File(plugin.getDataFolder() + "/superregions", sr.getName() + ".yml");
        if (!superRegionFile.exists()) {
            plugin.warning("Failed to save " + sr.getName() + " new bank balance: " + balance);
            return 0;
        }
        FileConfiguration sRegionConfig = new YamlConfiguration();
        try {
            sRegionConfig.load(superRegionFile);
        } catch (Exception e) {
            plugin.warning("Failed to load " + sr.getName() + ".yml to save new bank balance");
            return 0;
        }
        double newBalance = balance + sr.getBalance();
        if (balance < 0) {
            if (newBalance < 0 && Townships.econ != null) {
                OfflinePlayer ownerName = sr.getOwners().get(0);
                double ownerBalance = Townships.econ.getBalance(ownerName);
                if (newBalance + ownerBalance <= 0 && ownerBalance != 0) {
                    Townships.econ.withdrawPlayer(ownerName, ownerBalance);
                    Player p = ownerName.getPlayer();
                    if (p != null && p.isOnline()) {
                        p.sendMessage(ChatColor.RED + "[Townships] " + sr.getName() + " and you are out of money. Do something fast!");
                    }
                } else {
                    Townships.econ.withdrawPlayer(ownerName, -newBalance);
                }
                
            }
        }
        sRegionConfig.set("balance", newBalance);
        sr.setBalance(newBalance);
        try {
            sRegionConfig.save(superRegionFile);
        } catch (Exception e) {
            plugin.warning("Failed to save " + sr.getName() + ".yml");
            return 0;
        }
        
        return newBalance;
    }
    
    public List<Region> getContainingRegions(Location loc) {
        List<Region> tempList = new ArrayList<Region>();
        double x = Math.floor(loc.getX() + 0.4);
        double y = Math.floor(loc.getY() + 0.4);
        y = y < 0 ? 0 : y;
        y = y > loc.getWorld().getMaxHeight() ? loc.getWorld().getMaxHeight() : y;
        double z = Math.floor(loc.getZ() + 0.4);
        for (Region r : getSortedRegions()) {
            try {
                double radius = getRegionType(r.getType()).getRawRadius();
                Location l = r.getLocation();
                if (Math.round(l.getX() + radius) < x) {
                    break;
                }
                if (Math.round(l.getX() - radius) < x && Math.round(l.getY() + radius) > y && Math.round(l.getY() - radius) < y && 
                        Math.round(l.getZ() + radius) > z && Math.round(l.getZ() - radius) < z && l.getWorld().equals(loc.getWorld())) {
                    tempList.add(r);
                }
            } catch (NullPointerException npe) {
                plugin.warning("Region " + r.getID() + " is corrupted");
            }
        }
        return tempList;
    }

    public boolean isInsideSuperRegion(SuperRegion sr, Location l, double radius) {
        SuperRegionType srt = getSuperRegionType(sr.getType());

        double x = l.getX();
        double y = l.getY();
        double z = l.getZ();

        double sx = sr.getLocation().getX();
        double sy = sr.getLocation().getY();
        double sz = sr.getLocation().getZ();

        double sRadius = srt.getRawRadius();
      
        return Math.round(x + radius) <= Math.round(sx + sRadius) &&
                Math.round(x - radius) >= Math.round(sx - sRadius) &&
                Math.round(y + radius) <= Math.round(sy + sRadius) &&
                Math.round(y - radius) >= Math.round(sy - sRadius) &&
                Math.round(z + radius) <= Math.round(sz + sRadius) &&
                Math.round(z - radius) >= Math.round(sz - sRadius);
    }

    public boolean isInsideSuperRegion(SuperRegion sr, Region r) {
        return isInsideSuperRegion(sr, r.getLocation(), getRegionType(r.getType()).getRawBuildRadius());
    }
    
    public List<Region> getContainingRegions(Location loc, double modifier) {
        List<Region> tempList = new ArrayList<Region>();
        double x = Math.floor(loc.getX()) + 0.4;
        double y = Math.floor(loc.getY()) + 0.4;
        double z = Math.floor(loc.getZ()) + 0.4;
        for (Region r : getSortedRegions()) {
            try {
                double radius = getRegionType(r.getType()).getRawRadius() + modifier;
                Location l = r.getLocation();
                if (Math.round(l.getX() + radius) < x) {
                    break;
                }
                if (Math.round(l.getX() - radius) < x && Math.round(l.getY() + radius + 1) > y && Math.round(l.getY() - radius) < y && 
                        Math.round(l.getZ() + radius) > z && Math.round(l.getZ() - radius) < z && l.getWorld().equals(loc.getWorld())) {
                    tempList.add(r);
                }
            } catch (NullPointerException npe) {
                plugin.warning("Region " + r.getID() + " is corrupted");
            }
        }
        return tempList;
    }
    
    public List<Region> getContainingBuildRegions(Location loc) {
        return getContainingBuildRegions(loc, 0);
    }
    
    public List<Region> getContainingBuildRegionsExcept(Location loc, Region except) {
        return getContainingBuildRegionsExcept(loc, except, 0);
    }
    
    public List<Region> getContainingBuildRegions(Location loc, double modifier) {
        List<Region> tempList = new ArrayList<Region>();
        double x = Math.floor(loc.getX()) + 0.4;
        double y = Math.floor(loc.getY()) + 0.4;
        double z = Math.floor(loc.getZ()) + 0.4;
        for (Region r : sortedBuildRegions) {
            try {
                double radius = getRegionType(r.getType()).getRawBuildRadius() + modifier;
                radius = radius < 1 ? getRegionType(r.getType()).getRawRadius() : radius;
                Location l = r.getLocation();
                if (Math.round(l.getX() + radius) < x) {
                    break;
                }
                if (Math.round(l.getX() - radius) < x && Math.round(l.getY() + radius + 1) > y && Math.round(l.getY() - radius) < y && 
                        Math.round(l.getZ() + radius) > z && Math.round(l.getZ() - radius) < z && l.getWorld().equals(loc.getWorld())) {
                    tempList.add(r);
                }
            } catch (NullPointerException npe) {
                plugin.warning("Region " + r.getID() + " is corrupted");
            }
        }
        return tempList;
    }
    
    public List<Region> getContainingBuildRegionsExcept(Location loc, Region except, double modifier) {
        List<Region> tempList = new ArrayList<Region>();
        double x = Math.floor(loc.getX()) + 0.4;
        double y = Math.floor(loc.getY()) + 0.4;
        double z = Math.floor(loc.getZ()) + 0.4;
        for (Region r : sortedBuildRegions) {
            try {
                double radius = getRegionType(r.getType()).getRawBuildRadius() + modifier;
                radius = radius < 1 ? getRegionType(r.getType()).getRawRadius() : radius;
                Location l = r.getLocation();
                if (Math.round(l.getX() + radius) < x) {
                    break;
                }
                if (except.equals(r)) {
                    continue;
                }
                if (Math.round(l.getX() - radius) < x && Math.round(l.getY() + radius + 1) > y && Math.round(l.getY() - radius) < y && 
                        Math.round(l.getZ() + radius) > z && Math.round(l.getZ() - radius) < z && l.getWorld().equals(loc.getWorld())) {
                    tempList.add(r);
                }
            } catch (NullPointerException npe) {
                plugin.warning("Region " + r.getID() + " is corrupted");
            }
        }
        return tempList;
    }
    
    public Region getClosestRegionType(Location loc, String type) {
        Region re = null;
        double distance = 999999999;
        for (Region r : getSortedRegions()) {
            Location l = r.getLocation();
            if (r.getType().equalsIgnoreCase(type) &&
                (l.getWorld() != null && l.getWorld().equals(loc.getWorld()))) {
                double tempDistance=r.getLocation().distance(loc);
                if (tempDistance < distance) {
                    distance=tempDistance;
                    re=r;
                } else {
                    break;
                }
            }
        }
        return re;
    }
    
    public Region getClosestRegionWithEffectAndMember(Location loc, String effect, Player player) {
        Region re = null;
        double distance = 999999999;
        for (Region r : getSortedRegions()) {
            Location l = r.getLocation();
            RegionType rt = getRegionType(r.getType());
            if (rt == null) {
                continue;
            }
            boolean hasEffect = false;
            for (String s : rt.getEffects()) {
                if (s.startsWith(effect)) {
                    hasEffect = true;
                    break;
                }
            }
            if (hasEffect &&
                (r.isMember(player) || r.isOwner(player)) &&
                (l.getWorld() != null && l.getWorld().equals(loc.getWorld()))) {
                double tempDistance=r.getLocation().distance(loc);
                if (tempDistance < distance) {
                    distance=tempDistance;
                    re=r;
                }
            }
        }
        return re;
    }
    
    public Region getClosestRegionWithEffectAndTownMember(Location loc, String effect, Player player) {
        Region re = null;
        player.getName();
        double distance = 999999999;
        for (Region r : getSortedRegions()) {
            Location l = r.getLocation();
            RegionType rt = getRegionType(r.getType());
            if (rt == null) {
                continue;
            }
            boolean hasEffect = false;
            for (String s : rt.getEffects()) {
                if (s.startsWith(effect)) {
                    hasEffect = true;
                    break;
                }
            }
            if (!hasEffect || !l.getWorld().equals(loc.getWorld())) {
                continue;
            }
            for (SuperRegion sr : getContainingSuperRegions(r.getLocation())) {
                if (sr.hasMember(player) || sr.hasOwner(player)) {
                    double tempDistance=l.distance(loc);
                    if (tempDistance < distance) {
                        distance=tempDistance;
                        re=r;
                    }
                    break;
                }
            }
        }
        return re;
    }
    
    public Region getClosestRegionWithEffect(Location loc, String effect) {
        Region re = null;
        double distance = 999999999;
        for (Region r : getSortedRegions()) {
            Location l = r.getLocation();
            RegionType rt = getRegionType(r.getType());
            if (rt == null) {
                continue;
            }
            boolean hasEffect = false;
            for (String s : rt.getEffects()) {
                if (s.startsWith(effect)) {
                    hasEffect = true;
                    break;
                }
            }
            
            if (hasEffect && (l.getWorld() != null && l.getWorld().equals(loc.getWorld()))) {
                double tempDistance=r.getLocation().distance(loc);
                if (tempDistance < distance) {
                    distance=tempDistance;
                    re=r;
                }
            }
        }
        return re;
    }
    
    public List<SuperRegion> getContainingSuperRegions(Location loc) {
        List<SuperRegion> tempList = new ArrayList<SuperRegion>();
        
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        for (SuperRegion sr : getSortedSuperRegions()) {
            try {
                double radius = getSuperRegionType(sr.getType()).getRawRadius();
                Location l = sr.getLocation();
                if (l.getX() + radius < x) {
                    break;
                }
                if (l.getX() - radius < x && l.getY() + radius > y && l.getY() - radius < y && 
                        l.getZ() + radius > z && l.getZ() - radius < z && l.getWorld().equals(loc.getWorld())) {
                    tempList.add(sr);
                }
            } catch (NullPointerException npe) {
                plugin.warning("SuperRegion " + sr.getName() + " is corrupted");
            }
        }
        return tempList;
    }
    
    @Deprecated
    public boolean shouldTakeAction(Location loc, Player player, int modifier, String effectName, boolean useReagents) {
        return shouldTakeAction(loc, player, new RegionCondition(effectName,useReagents,modifier));
//
//        Effect effect = new Effect(plugin);
//        for (Region r : this.getContainingRegions(loc, modifier)) {
//            boolean nullPlayer = player == null;
//            boolean member = false;
//            if (!nullPlayer) {
//                if ((r.isMember(player.getName()) || r.isOwner(player.getName()))) {
//                    member = true;
//                } else if (r.isMember("all")) {
//                    member = true;
//                } else  {
//                    for (String s : r.getMembers()) {
//                        if (s.contains("sr:")) {
//                            SuperRegion sr = getSuperRegion(s.replace("sr:", ""));
//                            if (sr != null && (sr.hasMember(player.getName()) || sr.hasOwner(player))) {
//                                member = true;
//                            }
//                        }
//                    }
//                }
//            }
//            if (!useReagents && (nullPlayer || !member) && effect.regionHasEffect(getRegionType(r.getType()).getEffects(), effectName) != 0) {
//                return true;
//            }
//            if (useReagents && (nullPlayer || !member) && effect.regionHasEffect(getRegionType(r.getType()).getEffects(), effectName) != 0
//                    && effect.hasReagents(r.getLocation())) {
//                return true;
//            }
//        }
//        for (SuperRegion sr : this.getContainingSuperRegions(loc)) {
//            boolean nullPlayer = player == null;
//            boolean member = false;
//            if (!nullPlayer) {
//                member = (sr.hasOwner(player) || sr.hasMember(player.getName()));
//                if (!member) {
//                    for (SuperRegion playerSR : getSortedSuperRegions()) {
//                        if ((playerSR.hasMember(player.getName()) || playerSR.hasOwner(player)) && sr.hasMember("sr:" + playerSR.getName())) {
//                            member = true;
//                            break;
//                        }
//                    }
//                }
//            }
//            boolean reqs = hasAllRequiredRegions(sr);
//            boolean hasEffect = getSuperRegionType(sr.getType()).hasEffect(effectName);
//            boolean hasPower = sr.getPower() > 0;
//            boolean hasMoney = sr.getBalance() > 0;
//            boolean hasGrace = refreshGracePeriod(sr, reqs && hasMoney);
//            if (useReagents && (nullPlayer || !member) && hasEffect && hasPower && ((reqs && hasMoney) || hasGrace)) {
//                return true;
//            }
//            if (!useReagents && (nullPlayer || !member) && hasEffect) {
//                return true;
//            }
//        }
//        return false;
    }
    
    public boolean shouldTakeAction(Location loc, Player player, RegionCondition condition) {
        int modifier = condition.MODIFIER;
        boolean useReagents = condition.USE_REAGENTS;
        String effectName = condition.NAME;
        Effect effect = new Effect(plugin);
        for (Region r : this.getContainingRegions(loc, modifier)) {
            boolean nullPlayer = player == null;
            boolean member = false;
            if (!nullPlayer) {
                if ((r.isMember(player) || r.isOwner(player))) {
                    member = true;
                } // TODO: revive this code
                /* else if (r.isMember("all")) {
                    member = true;
                } else  {
                    for (OfflinePlayer s : r.getMembers()) {
                        if (s.contains("sr:")) {
                            SuperRegion sr = getSuperRegion(s.replace("sr:", ""));
                            if (sr != null && (sr.hasMember(player) || sr.hasOwner(player))) {
                                member = true;
                            }
                        }
                    }
                } */
            }
            if (!useReagents && (nullPlayer || !member) && effect.regionHasEffect(getRegionType(r.getType()).getEffects(), effectName) != 0) {
                return true;
            }
            if (useReagents && (nullPlayer || !member) && effect.regionHasEffect(getRegionType(r.getType()).getEffects(), effectName) != 0
                    && effect.hasReagents(r.getLocation())) {
                return true;
            }
        }
        for (SuperRegion sr : this.getContainingSuperRegions(loc)) {
            boolean nullPlayer = player == null;
            boolean member = false;
            if (!nullPlayer) {
                member = (sr.hasOwner(player) || sr.hasMember(player));
                // TODO: Revive this code
                /*
                if (!member) {
                    for (SuperRegion playerSR : getSortedSuperRegions()) {
                        if ((playerSR.hasMember(player) || playerSR.hasOwner(player)) && sr.hasMember("sr:" + playerSR.getName())) {
                            member = true;
                            break;
                        }
                    }
                }
                */
            }
            boolean reqs = hasAllRequiredRegions(sr);
            boolean hasEffect = getSuperRegionType(sr.getType()).hasEffect(effectName);
            boolean hasPower = sr.getPower() > 0;
            boolean hasMoney = sr.getBalance() > 0;
            boolean hasGrace = refreshGracePeriod(sr, reqs && hasMoney);
            if (useReagents && (nullPlayer || !member) && hasEffect && hasPower && ((reqs && hasMoney) || hasGrace)) {
                return true;
            }
            if (!useReagents && (nullPlayer || !member) && hasEffect) {
                return true;
            }
        }
        return false;
    }
    
    public boolean shouldTakeAction(Location loc, Player player, List<RegionCondition> conditions) {
        Effect effect = new Effect(plugin);
        HashMap<Integer,List<RegionCondition>> conditionJA = new HashMap<Integer, List<RegionCondition>>();
        for (RegionCondition rc : conditions) {
            if (!conditionJA.containsKey(rc.MODIFIER) || conditionJA.get(rc.MODIFIER) == null) {
                conditionJA.put(rc.MODIFIER, new ArrayList<RegionCondition>());
                conditionJA.get(rc.MODIFIER).add(rc);
            } else {
                conditionJA.get(rc.MODIFIER).add(rc);
            }
        }
        for (Integer i : conditionJA.keySet()) {
            for (Region r : this.getContainingRegions(loc, i)) {
                boolean nullPlayer = player == null;
                boolean member = false;
                if (!nullPlayer) {
                    if ((r.isMember(player) || r.isOwner(player))) {
                        member = true;
                    } // TODO: revive this code
                    /* else if (r.isMember("all")) {
                        member = true;
                    } else  {
                        for (OfflinePlayer s : r.getMembers()) {
                            if (s.contains("sr:")) {
                                SuperRegion sr = getSuperRegion(s.replace("sr:", ""));
                                if (sr != null && (sr.hasMember(player) || sr.hasOwner(player))) {
                                    member = true;
                                }
                            }
                        }
                    } */
                }
                for (RegionCondition rc : conditionJA.get(i)) {
                    boolean useReagents = rc.USE_REAGENTS;
                    String effectName = rc.NAME;
                    if (!useReagents && (nullPlayer || !member) && effect.regionHasEffect(getRegionType(r.getType()).getEffects(), effectName) != 0) {
                        return true;
                    }
                    if (useReagents && (nullPlayer || !member) && effect.regionHasEffect(getRegionType(r.getType()).getEffects(), effectName) != 0
                            && effect.hasReagents(r.getLocation())) {
                        return true;
                    }
                }
            }
            for (SuperRegion sr : this.getContainingSuperRegions(loc)) {
                boolean nullPlayer = player == null;
                boolean member = false;
                if (!nullPlayer) {
                    member = (sr.hasOwner(player) || sr.hasMember(player));
                    // TODO: Revive this code
                    /*
                    if (!member) {
                        for (SuperRegion playerSR : getSortedSuperRegions()) {
                            if ((playerSR.hasMember(player) || playerSR.hasOwner(player)) && sr.hasMember("sr:" + playerSR.getName())) {
                                member = true;
                                break;
                            }
                        }
                    }
                    */
                }
                boolean reqs = hasAllRequiredRegions(sr);
                boolean hasPower = sr.getPower() > 0;
                boolean hasMoney = sr.getBalance() > 0;
                boolean hasGrace = refreshGracePeriod(sr, reqs && hasMoney);

                for (RegionCondition rc : conditionJA.get(i)) {
                    String effectName = rc.NAME;
                    boolean useReagents = rc.USE_REAGENTS;
                    boolean hasEffect = getSuperRegionType(sr.getType()).hasEffect(effectName);
                    if (useReagents && (nullPlayer || !member) && hasEffect && hasPower && ((reqs && hasMoney) || hasGrace)) {
                        return true;
                    }
                    if (!useReagents && (nullPlayer || !member) && hasEffect) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    public boolean refreshGracePeriod(SuperRegion sr, boolean notDisabled) {
        long grace = getRemainingGracePeriod(sr);
        
        if (grace < 0 && !notDisabled) {
            long lastDisable = Townships.getConfigManager().getGracePeriod() + System.currentTimeMillis();
            sr.setLastDisable(lastDisable);
            saveLastDisable(sr, lastDisable);
            return true;
        }
        if (notDisabled) {
            if (grace > -1) {
                sr.setLastDisable(-1);
                saveLastDisable(sr, -1);
            }
            return true;
        }
        return grace != 0;
    }
    
    private void saveLastDisable(SuperRegion sr, long lastDisable) {
        File sRegionFile = new File(plugin.getDataFolder() + "/superregions", sr.getName() + ".yml");
        if (!sRegionFile.exists()) {
            plugin.warning("Failed to find file " + sr.getName() + ".yml");
            return;
        }
        FileConfiguration sRegionConfig = new YamlConfiguration();
        try {
            sRegionConfig.load(sRegionFile);
        } catch (Exception e) {
            plugin.warning("Failed to load " + sr.getName() + ".yml to save owner");
            return;
        }
        
        sRegionConfig.set("last-disable", lastDisable);
        try {
            sRegionConfig.save(sRegionFile);
        } catch (Exception e) {
            plugin.warning("Failed to save " + sr.getName() + ".yml");
            return;
        }
    }
    
    public long getRemainingGracePeriod(SuperRegion sr) {
        if (sr == null) {
            return 0;
        }
        if (sr.getLastDisable() < 1) {
            return -1;
        }
        return Math.max(0, sr.getLastDisable() - System.currentTimeMillis());
    }
    
    public void setWar(SuperRegion sr1, SuperRegion sr2) {
        File warFile = new File(plugin.getDataFolder(), "war.yml");
        try {
            if (!warFile.exists()) {
                System.out.println("[Townships] failed to load war.yml");
                return;
            }
        } catch (Exception e) {
            System.out.println("[Townships] failed to load war.yml");
            return;
        }
        FileConfiguration warConfig = new YamlConfiguration();
        try {
            warConfig.load(warFile);
        } catch (Exception e) {
            System.out.println("[Townships] failed to load war.yml");
            return;
        }
        if (hasWar(sr1, sr2)) {
            try {
                if (wars.containsKey(sr1)) {
                    List<String> tempList = warConfig.getStringList(sr1.getName());
                    tempList.remove(sr2.getName());
                    warConfig.set(sr1.getName(), tempList);
                    warConfig.save(warFile);
                    if (tempList.isEmpty()) {
                        wars.remove(sr1);
                    } else {
                        wars.get(sr1).remove(sr2);
                    }
                } else if (wars.containsKey(sr2)) {
                    List<String> tempList = warConfig.getStringList(sr2.getName());
                    tempList.remove(sr1.getName());
                    warConfig.set(sr2.getName(), tempList);
                    warConfig.save(warFile);
                    if (tempList.isEmpty()) {
                        wars.remove(sr2);
                    } else {
                        wars.get(sr2).remove(sr1);
                    }
                }
            } catch (Exception e) {
                System.out.println("[Townships] failed to remove war from war.yml");
                return;
            }
        } else {
            try {
                if (wars.containsKey(sr1)) {
                    List<String> tempList = warConfig.getStringList(sr1.getName());
                    tempList.add(sr2.getName());
                    warConfig.set(sr1.getName(), tempList);
                    warConfig.save(warFile);
                    wars.get(sr1).add(sr2);
                } else if (wars.containsKey(sr2)) {
                    List<String> tempList = warConfig.getStringList(sr2.getName());
                    tempList.add(sr1.getName());
                    warConfig.set(sr2.getName(), tempList);
                    warConfig.save(warFile);
                    wars.get(sr2).add(sr1);
                } else {
                    List<String> tempSet = new ArrayList<String>();
                    HashSet<SuperRegion> tempSet2 = new HashSet<SuperRegion>();
                    tempSet.add(sr2.getName());
                    tempSet2.add(sr2);
                    warConfig.set(sr1.getName(), tempSet);
                    warConfig.save(warFile);
                    wars.put(sr1, tempSet2);
                }
            } catch (Exception e) {
                System.out.println("[Townships] failed to save new war to war.yml");
                return;
            }
        }
    }

    
    public void removeRegion(Location l) {
        if (liveRegions.containsKey(l)) {
            System.out.println("[Townships] successfully removed region " + liveRegions.get(l).getID());
            idRegions.remove(liveRegions.get(l).getID());
            sortedRegions.remove(liveRegions.get(l));
            sortedBuildRegions.remove(liveRegions.get(l));
            liveRegions.remove(l);
        } else {
            plugin.warning("[Townships] unable to remove region at " + Math.floor(l.getX()) + ":" + Math.floor(l.getY()) + ":" + Math.floor(l.getZ()));
        }
    }
    
    public Region getRegionByID(int id) {
        return idRegions.get(id);
    }
    
    public Set<String> getRegionTypes() {
        return regionTypes.keySet();
    }
    
    public List<Region> getSortedRegions() {
        return sortedRegions;
    }
    
    public List<Region> getSortedBuildRegions() {
        return sortedBuildRegions;
    }
    
    public List<SuperRegion> getSortedSuperRegions() {
        return sortedSuperRegions;
    }
    
    public Set<String> getSuperRegionTypes() {
        return superRegionTypes.keySet();
    }
    
    public RegionType getRegionType(String name) {
        return regionTypes.get(name);
    }
    
    public SuperRegionType getSuperRegionType(String name) {
        return superRegionTypes.get(name);
    }
    
    public Set<Location> getRegionLocations() {
        return liveRegions.keySet();
    }
    
    public Set<String> getSuperRegionNames() {
        return liveSuperRegions.keySet();
    }
    
    public Region getRegion(Location loc) {
        return liveRegions.get(loc);
    }
    
    public SuperRegion getSuperRegion(String name) {
        return liveSuperRegions.get(name.toLowerCase());
    }
    
    public Map<Location, Region> getRegions() {
        return liveRegions;
    }
    
    public boolean hasWar(SuperRegion sr1, SuperRegion sr2) {
        for (SuperRegion sr : wars.keySet()) {
            if (sr1.equals(sr)) {
                for (SuperRegion srt : wars.get(sr)) {
                    if (srt.equals(sr2)) {
                        return true;
                    }
                }
            } else if (sr2.equals(sr)) {
                for (SuperRegion srt : wars.get(sr)) {
                    if (srt.equals(sr1)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    public boolean isAtWar(Player p, Player p1) {
        HashSet<SuperRegion> tempSet = new HashSet<SuperRegion>();
        HashSet<SuperRegion> dTempSet = new HashSet<SuperRegion>();
        for (SuperRegion sr : getSortedSuperRegions()) {
            if (sr.hasMember(p) || sr.hasOwner(p)) {
                tempSet.add(sr);
            } else if (sr.hasMember(p1) || sr.hasOwner(p1)) {
                dTempSet.add(sr);
            }
        }
        for (SuperRegion sr : tempSet) {
            for (SuperRegion srt : dTempSet) {
                if (hasWar(sr, srt)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public HashSet<SuperRegion> getWars(SuperRegion sr) {
        HashSet<SuperRegion> tempSet = new HashSet<SuperRegion>();
        if (wars.containsKey(sr)) {
            tempSet = wars.get(sr);
        }
        for (SuperRegion srt : wars.keySet()) {
            if (!srt.equals(sr)) {
                for (SuperRegion srr : wars.get(srt)) {
                    if (srr.equals(sr)) {
                        tempSet.add(srt);
                        break;
                    }
                }
            }
        }
        return tempSet;
    }

    public void removeWars(String name) {
        FileConfiguration warConfig = new YamlConfiguration();
        File warFile = new File(plugin.getDataFolder(), "war.yml");
        try {
            warConfig.load(warFile);
        } catch (Exception e) {
            System.out.println("[Townships] Failed to load war.yml");
        }
        SuperRegion sr = liveSuperRegions.get(name);
        if (wars.containsKey(sr)) {
            wars.remove(sr);
            warConfig.set(name, new ArrayList<String>());
        }
        
        for (SuperRegion srt : wars.keySet()) {
            if (!srt.equals(sr)) {
                for (SuperRegion srr : wars.get(srt)) {
                    if (srr.equals(sr)) {
                        if (wars.get(srt).size() < 2) {
                            wars.remove(srt);
                            warConfig.set(srt.getName(), new ArrayList<String>());
                        } else {
                            wars.get(srt).remove(srr);
                            List<String> tempList = new ArrayList<String>();
                            for (SuperRegion ts : wars.get(srt)) {
                                tempList.add(ts.getName());
                            }
                            warConfig.set(srt.getName(), tempList);
                        }
                        break;
                    }
                }
            }
        }
        
        try {
            warConfig.save(warFile);
        } catch (Exception e) {
            System.out.println("[Townships] Failed to save war.yml");
        }
    }
    
    public boolean isAtMaxRegions(Player p, RegionType rt) {
        return isAtMaxRegions(p, rt, 0);
    }

    public boolean isAtMaxRegions(Player p, RegionType rt, int modifier) {
        if (rt == null) {
            return false;
        }
        //Find permSet
        List<String> sets = getPermSets(p);

        int maxName = -1;
        int maxGroup = -1;
        int highestNamePriority = -9999999;
        int highestGroupPriority = -9999999;
        for (String s : sets) {
            PermSet set = permSets.get(s);
            if (set != null && set.getPerms() != null) {

                if (set.getPerms().containsKey(rt.getName()) && set.getPriority() > highestNamePriority) {
                    maxName = set.getPerms().get(rt.getName());
                    highestNamePriority = set.getPriority();
                }
                if (set.getPerms().containsKey(rt.getGroup()) && set.getPriority() > highestGroupPriority) {
                    maxGroup = set.getPerms().get(rt.getGroup());
                    highestGroupPriority = set.getPriority();
                }
            }
        }

        /*PermSet ps = null;
        for (String s : sets) {
            if (permSets.containsKey(s) && permSets.get(s).getPriority() > highestPriority) {
                ps = permSets.get(s);
                highestPriority = ps.getPriority();
            }
        }
        
        //Find total regions of that type
        if (ps == null || ps.getPerms() == null) {
            return false;
        }
        int max = 0;
        int max2 = 0;
        if (ps.getPerms().get(rt.getName()) != null) {
            max = ps.getPerms().get(rt.getName());
        }
        if (ps.getPerms().get(rt.getGroup()) != null) {
            max2 = ps.getPerms().get(rt.getGroup());
        }*/
        
        boolean useNames = maxName > -1;
        boolean useGroups = maxGroup > -1;
        if (!useNames && !useGroups) {
            return false;
        }
        
        //Find all regions of that type and count them
        int i = 0;
        int k = 0;
        for (Region r : sortedRegions) {
            if (r.getOwners().isEmpty() || !r.getOwners().get(0).equals(p.getName())) {
                continue;
            }
            if (useNames && r.getType().equals(rt.getName())) {
                i++;
            }
            if (useGroups && getRegionType(r.getType()).getGroup().equals(rt.getGroup())) {
                k++;
            }
        }
        return !((!useNames || maxName > i + modifier) && (!useGroups || maxGroup > k + modifier));
    }
    
    public List<String> getPermSets(Player p) {
        List<String> sets = new ArrayList<String>();
        for (String s : possiblePermSets) {
            if (Townships.perms.has(p, "townships.group." + s)) {
                sets.add(s);
            }
        }
        return sets;
    }
    
    public boolean canBuildHere(Player p, Location l) {
        Effect effect = new Effect(plugin);
        for (Region r : getContainingRegions(l)) {
            if (r.isMember(p) || r.isOwner(p)) {
                continue;
            } else if ((effect.regionHasEffect(r, "denyblockbuild") != 0 && effect.hasReagents(r.getLocation())) ||
                    effect.regionHasEffect(r, "denyblockbuildnoreagent") != 0) {
                return false;
            }
        }
        for (SuperRegion sr : getContainingSuperRegions(l)) {
            SuperRegionType srt = getSuperRegionType(sr.getType());
            if (sr.hasMember(p) || sr.hasOwner(p)) {
                continue;
            } else if (srt.hasEffect("denyblockbuild") || srt.hasEffect("denyblockbuildnoreagent")) {
                return false;
            }
        }
        
        return true;
    }
    
    public boolean canBreakHere(Location l, Player p) {
        Effect effect = new Effect(plugin);
        for (Region r : getContainingRegions(l)) {
            if (r.isMember(p) || r.isOwner(p)) {
                continue;
            } else if ((effect.regionHasEffect(r, "denyblockbreak") != 0 && effect.hasReagents(r.getLocation())) ||
                    effect.regionHasEffect(r, "denyblockbreaknoreagent") != 0) {
                return false;
            }
        }
        for (SuperRegion sr : getContainingSuperRegions(l)) {
            SuperRegionType srt = getSuperRegionType(sr.getType());
            if (sr.hasMember(p) || sr.hasOwner(p)) {
                continue;
            } else if (srt.hasEffect("denyblockbreak") || srt.hasEffect("denyblockbreaknoreagent")) {
                return false;
            }
        }
        
        return true;
    }
    
    public boolean hasAvailableHousing(SuperRegion sr) {
        int housing = 0;
        for (Region r : getContainedRegions(sr)) {
            housing += getRegionType(r.getType()).getHousing();
        }
        return housing > sr.getPopulation();
    }
    
    public HashMap<String, List<String>> getRegionCategories() {
        return regionCategories;
    }
}
