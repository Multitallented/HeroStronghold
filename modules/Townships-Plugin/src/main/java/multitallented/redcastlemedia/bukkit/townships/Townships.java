package multitallented.redcastlemedia.bukkit.townships;
/**
 *
 * @author Multitallented
 */
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import multitallented.redcastlemedia.bukkit.townships.checkregiontask.CheckRegionTask;
import multitallented.redcastlemedia.bukkit.townships.effect.EffectManager;
import multitallented.redcastlemedia.bukkit.townships.events.ToCommandEffectEvent;
import multitallented.redcastlemedia.bukkit.townships.events.ToPreRegionCreatedEvent;
import multitallented.redcastlemedia.bukkit.townships.events.ToRenameEvent;
import multitallented.redcastlemedia.bukkit.townships.listeners.CustomListener;
import multitallented.redcastlemedia.bukkit.townships.listeners.RegionBlockListener;
import multitallented.redcastlemedia.bukkit.townships.listeners.RegionEntityListener;
import multitallented.redcastlemedia.bukkit.townships.listeners.RegionPlayerInteractListener;
import multitallented.redcastlemedia.bukkit.townships.listeners.guis.GUIListener;
import multitallented.redcastlemedia.bukkit.townships.listeners.guis.GUIManager;
import multitallented.redcastlemedia.bukkit.townships.listeners.guis.InfoGUIListener;
import multitallented.redcastlemedia.bukkit.townships.listeners.guis.RequirementsGUIListener;
import multitallented.redcastlemedia.bukkit.townships.listeners.guis.ShopGUIListener;
import multitallented.redcastlemedia.bukkit.townships.region.Region;
import multitallented.redcastlemedia.bukkit.townships.region.RegionManager;
import multitallented.redcastlemedia.bukkit.townships.region.RegionType;
import multitallented.redcastlemedia.bukkit.townships.region.SuperRegion;
import multitallented.redcastlemedia.bukkit.townships.region.SuperRegionType;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

public class Townships extends JavaPlugin {
    protected FileConfiguration config;
    private RegionManager regionManager;
    private RegionBlockListener blockListener;
    public static Economy econ;
    public static Permission perms;
    public static Chat chat;
    private RegionEntityListener regionEntityListener;
    private RegionPlayerInteractListener dpeListener;
    private Map<String, String> pendingInvites = new HashMap<String, String>();
    private static ConfigManager configManager;
    private Map<String, Charter> pendingCharters = new HashMap<String, Charter>();
    private HashSet<String> effectCommands = new HashSet<String>();
    private GUIManager guiManager;
    private static EffectManager effectManager;
    private CheckRegionTask theSender;
    
    @Override
    public void onDisable() {
        GUIManager.closeAllMenus();
        getLogger().info("is now disabled!");
    }

    @Override
    public void onEnable() {
      
        //setup configs
        config = getConfig();
        config.options().copyDefaults(true);
        saveConfig();
        configManager = new ConfigManager(config, this);
        
        //Setup RegionManager
        regionManager = new RegionManager(this, config);
        
        setupPermissions();
        setupEconomy();
        setupChat();
        
        //Register Listeners Here
        blockListener = new RegionBlockListener(this);
        dpeListener = new RegionPlayerInteractListener(this);
        regionEntityListener = new RegionEntityListener(this);
        guiManager = new GUIManager(this);
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(blockListener, this);
        
        pm.registerEvents(regionEntityListener, this);
        
        pm.registerEvents(dpeListener, this);
        
        pm.registerEvents(new CustomListener(this), this);
        
        pm.registerEvents(guiManager, this);
        
        pm.registerEvents(new GUIListener(regionManager), this);
        pm.registerEvents(new InfoGUIListener(regionManager), this);
        pm.registerEvents(new RequirementsGUIListener(this), this);
        pm.registerEvents(new ShopGUIListener(this), this);
        
        effectManager = new EffectManager(this);
        
        //Setup repeating sync task for checking regions
        getLogger().info("[Townships] starting synchronous effect task");
        theSender = new CheckRegionTask(getServer(), this);
        getServer().getScheduler().scheduleSyncRepeatingTask(this, theSender, 10L, 10L);
        //theSender.run();
        
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        long timeUntilDay = (86400000 + cal.getTimeInMillis() - System.currentTimeMillis()) / 50;
        System.out.println("[Townships] " + timeUntilDay + " ticks until 00:00");
        DailyTimerTask dtt = new DailyTimerTask(this);
        getServer().getScheduler().scheduleSyncRepeatingTask(this, dtt, timeUntilDay, 1728000);

        Permissions.assignPermissions(this);
        getLogger().info("[Townships] is now enabled!");
    }
    
    public static ConfigManager getConfigManager() {
        return configManager;
    }

    public static EffectManager getEffectManager() {
        return effectManager;
    }
    
    public Map<Player, String> getChannels() {
        return dpeListener.getChannels();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = null;
        try {
            player = (Player) sender;
        } catch (Exception e) {
            warning("Only players can use Township commands");
            return true;
        }

        //Are they in a blacklisted world
        if ((Townships.perms == null || !Townships.perms.has(sender, "townships.admin")) && getConfigManager().getBlackListWorlds().contains(player.getWorld().getName())) {
            sender.sendMessage(ChatColor.RED + "[Townships] is disabled on this world");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (player != null && !(Townships.perms == null || Townships.perms.has(player, "townships.admin"))) {
                return true;
            }
            config = getConfig();
            regionManager.reload();
            configManager = new ConfigManager(config, this);
            sender.sendMessage("[Townships] reloaded");
            return true;
        }
        if (player == null) {
            sender.sendMessage("[Townships] doesn't recognize non-player commands.");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("shop")) {
            if (!perms.has(player, "townships.unlock")) {
                player.sendMessage(ChatColor.RED + "[Townships] You don't have permission to unlock regions");
                return true;
            }
            String category = "";

            if (args.length == 1 && regionManager.getRegionCategories().size() > 1) {
                ShopGUIListener.openCategoryShop(player);
                return true;
            }
            if (args.length != 1) {
                category = args[1].toLowerCase();
                if (category.equals("other")) {
                    category = "";
                }
            }
            if (!regionManager.getRegionCategories().containsKey(category) && (category.equals("") && 
                    !regionManager.getRegionCategories().containsKey("other"))
                    && !category.equals("towns")) {
                player.sendMessage(ChatColor.GRAY + "[Townships] that category is not recognized");
                return true;
            }

            boolean permNull = perms == null;
            List<RegionType> regions = new ArrayList<RegionType>();

            List<SuperRegionType> superRegions = new ArrayList<SuperRegionType>();

            boolean createAll = permNull || perms.has(player, "townships.create.all");
            if (createAll) {
                player.sendMessage(ChatColor.GOLD + "[Townships] You've already unlocked all regions");
                return true;
            }
            if (!category.equals("towns")) {
                for (String s : regionManager.getRegionCategories().get(category)) {
                    RegionType rt = regionManager.getRegionType(s);
                    if (rt.getUnlockCost() > 0 && !perms.has(player, "townships.create." + s)) {
                        
                        regions.add(regionManager.getRegionType(s));
                    }
                }
            }
            if (category.equals("") && regionManager.getRegionCategories().containsKey("other")) {
                for (String s : regionManager.getRegionCategories().get("other")) {
                    RegionType rt = regionManager.getRegionType(s);
                    if (rt.getUnlockCost() > 0 && !perms.has(player, "townships.create." + s)) {
                        
                        regions.add(regionManager.getRegionType(s));
                    }
                }
            }
            if (regions.size() > 1) {
                Collections.sort(regions, new Comparator<RegionType>() {

                    @Override
                    public int compare(RegionType o1, RegionType o2) {
                        return GUIManager.compareRegions(o1, o2);
                    }
                });
            }
            if (category.equals("towns")) {
                for (String s : regionManager.getSuperRegionTypes()) {
                    SuperRegionType srt = regionManager.getSuperRegionType(s);
                    if (srt.getUnlockCost() > 0 && !perms.has(player, "townships.create." + s)) {
                        superRegions.add(regionManager.getSuperRegionType(s));
                    }
                }
            }
            if (superRegions.size() > 1) {
                Collections.sort(superRegions, new Comparator<SuperRegionType>() {

                    @Override
                    public int compare(SuperRegionType o1, SuperRegionType o2) {
                        return GUIManager.compareSRegions(o1, o2);
                    }
                });
            }
            ShopGUIListener.openListShop(regions, superRegions, player, category);
            
            return true;
        } else if (args.length > 1 && args[0].equalsIgnoreCase("confirm")) {

            ShopGUIListener.openConfirmation(player, args[1]);
            return true;
        } else if (args.length > 1 && args[0].equalsIgnoreCase("unlock")) {
            if (!perms.has(player, "townships.unlock")) {
                player.sendMessage(ChatColor.RED + "[Townships] You don't have permission to unlock regions");
                return true;
            }
            RegionType rt = regionManager.getRegionType(args[1]);
            NumberFormat formatter = NumberFormat.getCurrencyInstance();
            if (rt != null) {
                if (!econ.has(player, rt.getUnlockCost())) {
                   player.sendMessage(ChatColor.RED + "[Townships] You don't have " + formatter.format(rt.getUnlockCost()) + " to buy a " + rt.getName()); 
                   return true;
                }
                econ.withdrawPlayer(player, rt.getUnlockCost());
                perms.playerAdd(player, "townships.create." + rt.getName());
                player.sendMessage(ChatColor.GREEN + "[Townships] You have unlocked " + rt.getName());
                
                return true;
            }
            SuperRegionType srt = regionManager.getSuperRegionType(args[1]);
            if (srt != null) {
                if (!econ.has(player, srt.getUnlockCost())) {
                   player.sendMessage(ChatColor.RED + "[Townships] You don't have " + formatter.format(srt.getUnlockCost()) + " to buy a " + srt.getName()); 
                   return true;
                }
                econ.withdrawPlayer(player, srt.getUnlockCost());
                perms.playerAdd(player, "townships.create." + srt.getName());
                player.sendMessage(ChatColor.GREEN + "[Townships] You have unlocked " + srt.getName());
                
                return true;
            }
            
            return true;
        } else if (args.length > 2 && args[0].equalsIgnoreCase("war")) {
            //hs war mySR urSR

            //Check for valid super-regions
            SuperRegion myTown = regionManager.getSuperRegion(args[2]);
            SuperRegion enemyTown = regionManager.getSuperRegion(args[1]);
            if (myTown == null || enemyTown == null) {
                player.sendMessage(ChatColor.GRAY + "[Townships] That isn't a valid super-region.");
                return true;
            }

            //Check if already at war
            if (regionManager.hasWar(myTown, enemyTown)) {
                player.sendMessage(ChatColor.GRAY + "[Townships] " + myTown.getName() + " is already at war!");
                return true;
            }

            //Check owner
            if (!myTown.hasOwner(player)) {
                player.sendMessage(ChatColor.GRAY + "[Townships] You are not an owner of " + myTown.getName());
                return true;
            }

            //Calculate Cost
            ConfigManager cm = getConfigManager();
            if (!cm.getUseWar()) {
                player.sendMessage(ChatColor.GRAY + "[Townships] This command is disabled in config.yml");
                return true;
            }
            double cost = cm.getDeclareWarBase() + cm.getDeclareWarPer() * (myTown.getOwners().size() + myTown.getMembers().size() +
                    enemyTown.getOwners().size() + enemyTown.getMembers().size());

            //Check money
            if (Townships.econ != null) {
                if (myTown.getBalance() < cost) {
                    player.sendMessage(ChatColor.GRAY + "[Townships] " + myTown.getName() + " doesn't have enough money to war with " + enemyTown.getName());
                    return true;
                } else {
                    regionManager.addBalance(myTown, -1 * cost);
                }
            }

            regionManager.setWar(myTown, enemyTown);
            final SuperRegion sr1a = myTown;
            final SuperRegion sr2a = enemyTown;
            new Runnable() {
                  @Override
                  public void run()
                  {
                    getServer().broadcastMessage(ChatColor.RED + "[Townships] " + sr1a.getName() + " has declared war on " + sr2a.getName() + "!");
                  }
            }.run();
            return true;
        } else if (args.length > 2 && args[0].equalsIgnoreCase("peace")) {
            //hs peace mySR urSR

            //Check for valid super-regions
            SuperRegion myTown = regionManager.getSuperRegion(args[2]);
            SuperRegion enemyTown = regionManager.getSuperRegion(args[1]);
            if (myTown == null || enemyTown == null) {
                player.sendMessage(ChatColor.GRAY + "[Townships] That isn't a valid super-region.");
                return true;
            }

            //Check if already at war
            if (!regionManager.hasWar(myTown, enemyTown)) {
                player.sendMessage(ChatColor.GRAY + "[Townships] " + myTown.getName() + " isn't at war.");
                return true;
            }

            //Check owner
            if (!myTown.hasOwner(player)) {
                player.sendMessage(ChatColor.GRAY + "[Townships] You are not an owner of " + myTown.getName());
                return true;
            }

            //Calculate Cost
            ConfigManager cm = getConfigManager();
            if (!cm.getUseWar()) {
                player.sendMessage(ChatColor.GRAY + "[Townships] This command is disabled in config.yml");
                return true;
            }
            double cost = cm.getMakePeaceBase() + cm.getMakePeacePer() * (myTown.getOwners().size() + myTown.getMembers().size() +
                    enemyTown.getOwners().size() + enemyTown.getMembers().size());

            //Check money
            if (Townships.econ != null) {
                if (myTown.getBalance() < cost) {
                    player.sendMessage(ChatColor.GRAY + "[Townships] " + myTown.getName() + " doesn't have enough money to make peace with " + enemyTown.getName());
                    return true;
                } else {
                    regionManager.addBalance(myTown, -1 * cost);
                }
            }

            regionManager.setWar(myTown, enemyTown);
            final SuperRegion sr1a = myTown;
            final SuperRegion sr2a = enemyTown;
            new Runnable() {
                  @Override
                  public void run()
                  {
                    getServer().broadcastMessage(ChatColor.RED + "[Townships] " + sr1a.getName() + " has made peace with " + sr2a.getName() + "!");
                  }
            }.run();
            return true;
        } else if (args.length > 0 && args[0].equalsIgnoreCase("shop")) {
            //TODO find what to show them and open the GUI
            return true;
        } else if (args.length > 2 && args[0].equalsIgnoreCase("charter")) {

            //Check if valid super region
            SuperRegionType currentRegionType = regionManager.getSuperRegionType(args[1]);
            if (currentRegionType == null) {
                player.sendMessage(ChatColor.GRAY + "[Townships] " + args[1] + " isnt a valid region type");
                int j=0;
                String message = ChatColor.GOLD + "";
                for (String s : regionManager.getSuperRegionTypes()) {
                    if (perms == null || (perms.has(player, "townships.create.all") ||
                            perms.has(player, "townships.create." + s))) {
                        if (message.length() + s.length() + 2 > 55) {
                            player.sendMessage(message + ", ");
                            message = ChatColor.GOLD + "";
                            j++;
                        }
                        if (j > 14) {
                            break;
                        } else {
                            message += ", " + s;
                        }
                    }
                }
                if (!message.equals(ChatColor.GOLD + "")) {
                    player.sendMessage(message.substring(0, message.length() - 2));
                }
                return true;
            }

            String regionTypeName = args[1].toLowerCase();
            //Permission Check
            if (perms != null && !perms.has(player, "townships.create.all") &&
                    !perms.has(player, "townships.create." + regionTypeName)) {
                player.sendMessage(ChatColor.GRAY + "[Townships] you dont have permission to create a " + regionTypeName);
                return true;
            }

            //Make sure the super-region requires a Charter
            if (currentRegionType.getCharter() <= 0) {
                player.sendMessage(ChatColor.GRAY + "[Townships] " + args[1] + " doesnt require a charter. /to create " + args[1]);
                return true;
            }

            //Make sure the name isn't too long
            if (args[2].length() > 15) {
                player.sendMessage(ChatColor.GRAY + "[Townships] Sorry but that name is too long. (16 max)");
                return true;
            }
            //Check if valid filename
            if (!Util.validateFileName(args[2])) {
                player.sendMessage(ChatColor.GRAY + "[Townships] Sorry but that is an invalid filename.");
                return true;
            }

            //Check if valid name
            if (pendingCharters.containsKey(args[2].toLowerCase())) {
                player.sendMessage(ChatColor.GRAY + "[Townships] There is already a charter or region with that name.");
                return true;
            }
            if (getServer().getPlayerExact(args[2]) != null) {
                player.sendMessage(ChatColor.GRAY + "[Townships] Dont name a super-region after a player");
                return true;
            }

            //Check if allowed super-region
            if (regionManager.getSuperRegion(args[2]) != null && (!regionManager.getSuperRegion(args[2]).hasOwner(player)
                    || regionManager.getSuperRegion(args[2]).getType().equalsIgnoreCase(args[1]))) {
                player.sendMessage(ChatColor.GRAY + "[Townships] That exact super-region already exists.");
                return true;
            }

            //Add the charter
            Charter tempList = new Charter(regionManager.getSuperRegionType(args[1]), player);
            pendingCharters.put(args[2].toLowerCase(), tempList);
            configManager.writeToCharter(args[2].toLowerCase(), tempList);
            player.sendMessage(ChatColor.GOLD + "[Townships] Youve successfully created a charter for " + args[2]);
            player.sendMessage(ChatColor.GOLD + "[Townships] Get other people to type /to signcharter " + args[2] + " to get started.");
            return true;
        } else if (args.length > 1 && args[0].equalsIgnoreCase("charterstats")) {
            //Check if valid charter
            if (!pendingCharters.containsKey(args[1].toLowerCase())) {
                player.sendMessage(ChatColor.GRAY + "[Townships] " + args[1] + " isn't a valid charter type.");
                return true;
            }

            player.sendMessage(ChatColor.GRAY + "[Townships] " + args[1] + " signatures: ");
            int j=0;
            String message = ChatColor.GOLD + "";
            Charter charter = pendingCharters.get(args[1]);
            if (charter != null) {
            	message += charter.getSuperRegionType().getName() + ", ";
                for (OfflinePlayer s : charter.getMembers()) {
                    if (message.length() + s.getName().length() + 2 > 55) {
                        player.sendMessage(message);
                        message = ChatColor.GOLD + "";
                        j++;
                    }
                    if (j > 14) {
                        break;
                    } else {
                        message += s.getName() + ", ";
                    }
                }
                if (!charter.getMembers().isEmpty()) {
                    player.sendMessage(message.substring(0, message.length() - 2));
                }
            } else {
                player.sendMessage(ChatColor.RED + "[Townships] There was an error loading that charter");
                warning("Failed to load charter " + args[1] + ".yml");
            }

            return true;
        } else if (args.length > 1 && args[0].equalsIgnoreCase("signcharter")) {
            //Check if valid name
            if (!pendingCharters.containsKey(args[1].toLowerCase())) {
                player.sendMessage(ChatColor.GRAY + "[townships] There is no charter for " + args[1]);
                return true;
            }

            //Check permission
            if (perms != null && !perms.has(player, "townships.join")) {
                player.sendMessage(ChatColor.GRAY + "[Townships] You dont have permission to sign a charter.");
                return true;
            }

            //Sign Charter
            Charter charter = pendingCharters.get(args[1].toLowerCase());

            //Check if the player has already signed the charter once
            if (charter.getMembers().contains(player.getName())) {
                player.sendMessage(ChatColor.GRAY + "[Townships] You've already signed this charter.");
                return true;
            }

            charter.addMember(player);
            configManager.writeToCharter(args[1], charter);
            pendingCharters.put(args[1], charter);
            player.sendMessage(ChatColor.GOLD + "[Townships] You just signed the charter for " + args[1]);
            int remaining = 0;
            SuperRegionType srt = charter.getSuperRegionType();
            if (srt != null) {
                remaining = srt.getCharter() - charter.getMembers().size();
            }
            if (remaining > 0) {
                player.sendMessage(ChatColor.GOLD + "" + remaining + " signatures to go!");
            }
            Player owner = charter.getMembers().get(0).getPlayer();
            if (owner != null && owner.isOnline()) {
                owner.sendMessage(ChatColor.GOLD + "[Townships] " + player.getDisplayName() + " just signed your charter for " + args[1]);
                if (remaining > 0) {
                    owner.sendMessage(ChatColor.GOLD + "" + remaining + " signatures to go!");
                }
            }
            return true;
        } else if (args.length > 1 && args[0].equals("cancelcharter")) {
            if (!pendingCharters.containsKey(args[1].toLowerCase())) {
                player.sendMessage(ChatColor.GRAY + "[Townships] There is no charter for " + args[1]);
                return true;
            }

            if (pendingCharters.get(args[1]).getMembers().size() < 1 || !pendingCharters.get(args[1]).getMembers().get(0).equals(player)) {
                player.sendMessage(ChatColor.GRAY + "[Townships] You are the not owner of this charter.");
                return true;
            }

            configManager.removeCharter(args[1]);
            pendingCharters.remove(args[1]);
            player.sendMessage(ChatColor.GOLD + "[Townships] You have canceled the charter for " + args[1]);
            return true;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
            String regionName = args[1];

            //Permission Check
            boolean nullPerms = perms == null;
            boolean createAll = nullPerms || perms.has(player, "townships.create.all");
            if (!(nullPerms || createAll || perms.has(player, "townships.create." + regionName))) {

                if (perms.has(player, "townships.rebuild." + regionName)) {
                    player.performCommand("to rebuild " + regionName);
                    return true;
                }

                player.sendMessage(ChatColor.GRAY + "[Townships] you dont have permission to create a " + regionName);
                return true;
            }

            Location currentLocation = player.getLocation();
            currentLocation.setX(Math.floor(currentLocation.getX()) + 0.4);
            currentLocation.setY(Math.floor(currentLocation.getY()) + 0.4);
            currentLocation.setZ(Math.floor(currentLocation.getZ()) + 0.4);

            //Check if player is standing someplace where a chest can be placed.
            Block currentBlock = currentLocation.getBlock();
            if (!currentBlock.isEmpty()) {
                player.sendMessage(ChatColor.GRAY + "[Townships] please stand someplace where a chest can be placed.");
                return true;
            }
            RegionType currentRegionType = regionManager.getRegionType(regionName);
            if (currentRegionType == null) {
                player.sendMessage(ChatColor.GRAY + "[Townships] " + regionName + " isnt a valid region type");
                player.sendMessage(ChatColor.GRAY + "[Townships] Try /to create " + regionName + " <insert_name_here>");
                return true;
            }

            //Check if player can afford to create this region
            double costCheck = 0;
            if (econ != null) {
                double cost = currentRegionType.getMoneyRequirement();
                if (econ.getBalance(player) < cost) {
                    player.sendMessage(ChatColor.GRAY + "[Townships] You need $" + cost + " to make this type of structure.");
                    return true;
                } else {
                    costCheck = cost;
                }

            }

            //Check if over max number of regions of that type
            if (regionManager.isAtMaxRegions(player, currentRegionType)) {
                player.sendMessage(ChatColor.GRAY + "[Townships] You dont have permission to build more " + currentRegionType.getName());
                return true;
            }

            //Check if above min y
            if (currentRegionType.getMinY() != -1 && Math.floor(currentRegionType.getMinY()) > Math.floor(currentLocation.getY())) {
                    player.sendMessage(ChatColor.GRAY + "[Townships] You region is at " + Math.floor(currentLocation.getY()) + "y and must be built above " + currentRegionType.getMinY() + "y");
                    return true;
            }

            //Check if above max y
            if (currentRegionType.getMaxY() != -1 && Math.floor(currentRegionType.getMaxY()) < Math.floor(currentLocation.getY())) {
                    player.sendMessage(ChatColor.GRAY + "[Townships] You region is at " + Math.floor(currentLocation.getY()) + "y and must be built below " + currentRegionType.getMaxY() + "y");
                    return true;
            }

            //Check biome
            if (!currentRegionType.getBiome().isEmpty()
                    && !currentRegionType.getBiome().contains(player.getLocation().getBlock().getBiome().name())) {
                String mes = "";
                for (String me : currentRegionType.getBiome()) {
                    if (mes.length() == 0) {
                        mes += me;
                    } else {
                        mes += ", " + me;
                    }
                }
                player.sendMessage(ChatColor.GRAY + "[Townships] You must build this in a " + mes + " biome");
                player.sendMessage(ChatColor.GRAY + "[Townships] You are currently in a " + player.getLocation().getBlock().getBiome().name() + " biome");
                return true;
            }

            //Check if too close to other region
            List<Region> containingRegions = regionManager.getContainingBuildRegions(currentLocation, currentRegionType.getRawBuildRadius());
            if (!containingRegions.isEmpty()) {

                //If the player is an owner of the region, then try to rebuild instead
                if (!containingRegions.get(0).getOwners().isEmpty() &&
                        containingRegions.get(0).getOwners().contains(player.getName()) &&
                        perms.has(player, "townships.rebuild." + containingRegions.get(0).getType().toLowerCase())) {
                    player.performCommand("to rebuild " + currentRegionType.getName());
                    return true;
                }
                player.sendMessage (ChatColor.GRAY + "[Townships] You are too close to another region");
                return true;
            }

            player.getName();
            List<String> reqSuperRegion = currentRegionType.getSuperRegions();

            boolean meetsReqs = false;
            String limitMessage = null;

            if (reqSuperRegion != null && !reqSuperRegion.isEmpty()) {
                for (SuperRegion sr : regionManager.getContainingSuperRegions(currentLocation)) {
                    if (reqSuperRegion.contains(sr.getType())) {
                        meetsReqs = true;
                        if (!regionManager.isInsideSuperRegion(sr, currentLocation, currentRegionType.getRawBuildRadius())) {
                            player.sendMessage(ChatColor.RED + "[Townships] Not all of the " + regionName + " would be inside the " + sr.getType());
                            return true;
                        }
                        SuperRegionType srt = regionManager.getSuperRegionType(sr.getType());
                        HashMap<String, Integer> limits = srt.getRegionLimits();
                        boolean containsName = limits.containsKey(currentRegionType.getName());
                        boolean containsGroup = !containsName && limits.containsKey(currentRegionType.getGroup());

                        if (containsName || containsGroup) {
                            int limit = containsName ? limits.get(currentRegionType.getName()) : limits.get(currentRegionType.getGroup());

                            if (limit > 0) {
                                int regionCount = 0;
                                for (Region r : regionManager.getContainedRegions(sr)) {
                                    if ((containsName && r.getType().equals(currentRegionType.getName())) || 
                                            (containsGroup && regionManager.getRegionType(r.getType()).getGroup().equals(currentRegionType.getGroup()))) {
                                        regionCount++;
                                        if (limit <= regionCount) {
                                            limitMessage = ChatColor.RED + "[Townships] You can't build more than " + limit + " in this " + sr.getType();
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (!sr.hasOwner(player)) {
                        if (!sr.hasMember(player) || !sr.getMember(player).contains(regionName)) {
                            player.sendMessage(ChatColor.GRAY + "[Townships] You dont have permission from an owner of " + sr.getName()
                                    + " to create a " + regionName + " here");
                            return true;
                        }
                    }
                }
            } else {
                for (SuperRegion sr : regionManager.getContainingSuperRegions(currentLocation)) {
                    if (!sr.hasOwner(player)) {
                        if (!sr.hasMember(player) || !sr.getMember(player).contains(regionName)) {
                            player.sendMessage(ChatColor.GRAY + "[Townships] You dont have permission from an owner of " + sr.getName()
                                    + " to create a " + regionName + " here");
                            return true;
                        }
                    }
                }
                meetsReqs = true;
            }

            if (!meetsReqs) {
                player.sendMessage(ChatColor.GRAY + "[Townships] You are required to build this " + regionName + " in a:");
                String message = ChatColor.GOLD + "";
                int j=0;
                for (String s : reqSuperRegion) {
                    if (message.length() + s.length() + 2 > 55) {
                        player.sendMessage(message);
                        message = ChatColor.GOLD + "";
                        j++;
                    }
                    if (j > 14) {
                        break;
                    } else {
                        message += s + ", ";
                    }
                }
                if (!reqSuperRegion.isEmpty()) {
                    player.sendMessage(message.substring(0, message.length() - 2));
                }
                return true;
            }

            if (limitMessage != null) {
                player.sendMessage(limitMessage);
                return true;
            }

            //Check if it has required blocks
            if (!currentRegionType.getRequirements().isEmpty()) {
                List<String> message = Util.hasCreationRequirements(currentLocation, currentRegionType, regionManager);
                if (!message.isEmpty()) {
                    player.sendMessage(ChatColor.GRAY + "[Townships] you don't have all of the required blocks in this structure.");
                    for (String s : message) {
                        player.sendMessage(ChatColor.GOLD + s);
                    }
                    return true;
                }
            }
            
            ToPreRegionCreatedEvent preEvent = new ToPreRegionCreatedEvent(currentLocation, currentRegionType, player);
            getServer().getPluginManager().callEvent(preEvent);
            if (preEvent.isCancelled()) {
                return true;
            }

            //Create chest at players feet for tracking reagents and removing upkeep items
            currentBlock.setType(Material.CHEST);

            List<OfflinePlayer> owners = new ArrayList<OfflinePlayer>();
            owners.add(player);
            if (econ != null && costCheck > 0) {
                econ.withdrawPlayer(player, costCheck);
            }

            regionManager.addRegion(currentLocation, regionName, owners);
            player.sendMessage(ChatColor.GRAY + "[Townships] " + ChatColor.WHITE + "You successfully created a " + ChatColor.RED + regionName);

            return true;
        } else if (args.length > 2 && args[0].equalsIgnoreCase("create")) {
            //Check if valid name (further name checking later)
            if (args[2].length() > 16 || !Util.validateFileName(args[2])) {
                player.sendMessage(ChatColor.GRAY + "[Townships] That name is invalid.");
                return true;
            }
            if (getServer().getPlayerExact(args[2]) != null) {
                player.sendMessage(ChatColor.GRAY + "[Townships] Dont name a super-region after a player.");
                return true;
            }

            String regionTypeName = args[1];
            //Permission Check
            if (perms != null && !perms.has(player, "townships.create.all") &&
                    !perms.has(player, "townships.create." + regionTypeName)) {
                player.sendMessage(ChatColor.GRAY + "[Townships] you dont have permission to create a " + regionTypeName);
                return true;
            }

            //Check if valid super region
            Location currentLocation = player.getLocation();
            
            currentLocation.setX(Math.floor(currentLocation.getX()) + 0.4);
            currentLocation.setY(Math.floor(currentLocation.getY()) + 0.4);
            currentLocation.setZ(Math.floor(currentLocation.getZ()) + 0.4);

            SuperRegionType currentRegionType = regionManager.getSuperRegionType(regionTypeName);
            if (currentRegionType == null) {
                player.sendMessage(ChatColor.GRAY + "[Townships] " + regionTypeName + " isnt a valid region type");
                int j=0;
                String message = ChatColor.GOLD + "";
                for (String s : regionManager.getSuperRegionTypes()) {
                    if (perms == null || (perms.has(player, "townships.create.all") ||
                            perms.has(player, "townships.create." + s))) {
                        if (message.length() + s.length() + 2 > 55) {
                            player.sendMessage(message);
                            message = ChatColor.GOLD + "";
                            j++;
                        }
                        if (j > 14) {
                            break;
                        } else {
                            message += s + ", ";
                        }
                    }
                }
                if (!regionManager.getSuperRegionTypes().isEmpty()) {
                    player.sendMessage(message.substring(0, message.length() - 2));
                }
                return true;
            }

            //Check if player can afford to create this townships
            double costCheck = 0;
            if (econ != null) {
                double cost = currentRegionType.getMoneyRequirement();
                if (econ.getBalance(player) < cost) {
                    player.sendMessage(ChatColor.GRAY + "[Townships] You need $" + cost + " to make this type of region.");
                    return true;
                } else {
                    costCheck = cost;
                }

            }

            Map<OfflinePlayer, List<String>> members = new HashMap<OfflinePlayer, List<String>>();
            int currentCharter = currentRegionType.getCharter();
            //Make sure the super-region has a valid charter
            if (!Townships.perms.has(player, "townships.admin")) {
                if (currentCharter > 0) {
                    try {
                        if (!pendingCharters.containsKey(args[2])) {
                            player.sendMessage(ChatColor.GRAY + "[Townships] You need to start a charter first. /to charter " + args[1] + " " + args[2]);
                            return true;
                        } else if (pendingCharters.get(args[2]).getMembers().size() <= currentCharter) {
                            player.sendMessage(ChatColor.GRAY + "[Townships] You need " + currentCharter + " signature(s). /to signcharter " + args[2]);
                            return true;
                        } else if (!pendingCharters.get(args[2]).getSuperRegionType().equals(args[1]) ||
                                !pendingCharters.get(args[2]).getMembers().get(0).equals(player)) {
                            player.sendMessage(ChatColor.GRAY + "[Townships] The charter for this name is for a different region type or owner.");
                            player.sendMessage(ChatColor.GRAY + "Owner: " + pendingCharters.get(args[2]).getMembers().get(0).getName() + ", Type: " + pendingCharters.get(args[2]).getSuperRegionType().getName());
                            return true;
                        } else {
                            int i =0;
                            for (OfflinePlayer s : pendingCharters.get(args[2]).getMembers()) {
                                List<String> tempArray = new ArrayList<String>();
                                tempArray.add("member");
                                if (i > 0) {
                                    members.put(s, tempArray);
                                } else {
                                    i++;
                                }
                            }
                        }
                    } catch (Exception e) {
                        warning("Possible failure to find correct charter for " + args[2]);
                    }
                }
            } else if (pendingCharters.containsKey(args[2])) {
                if (currentCharter > 0) {
                    try {
                        if (pendingCharters.get(args[2]).getMembers().size() <= currentCharter) {
                            player.sendMessage(ChatColor.GRAY + "[Townships] You need " + currentCharter + " signature(s). /to signcharter " + args[2]);
                            return true;
                        } else if (!pendingCharters.get(args[2]).getSuperRegionType().getName().equalsIgnoreCase(args[1]) ||
                                !pendingCharters.get(args[2]).getMembers().get(0).equals(player)) {
                            player.sendMessage(ChatColor.GRAY + "[Townships] The charter for this name is for a different region type or owner.");
                            player.sendMessage(ChatColor.GRAY + "Owner: " + pendingCharters.get(args[2]).getMembers().get(0).getName() + ", Type: " + pendingCharters.get(args[2]).getSuperRegionType().getName());
                            return true;
                        } else {
                            int i =0;
                            for (OfflinePlayer s : pendingCharters.get(args[2]).getMembers()) {
                                List<String> tempArray = new ArrayList<String>();
                                tempArray.add("member");
                                if (i > 0) {
                                    members.put(s, tempArray);
                                } else {
                                    i++;
                                }
                            }
                        }
                    } catch (Exception e) {
                        warning("Possible failure to find correct charter for " + args[2]);
                    }
                }
            }

            Map<String, Integer> requirements = currentRegionType.getRequirements();
            HashMap<String, Integer> req = new HashMap<String, Integer>();
            for (String s : currentRegionType.getRequirements().keySet()) {
                req.put(new String(s), new Integer(requirements.get(s)));
            }

            //Check for required regions
            List<String> children = currentRegionType.getChildren();
            if (children != null) {
                for (String s : children) {
                    if (!req.containsKey(s)) {
                        req.put(new String(s), 1);
                    }
                }
            }

            //Check if there already is a super-region by that name, but not if it's one of the child regions
            if (regionManager.getSuperRegion(args[2]) != null && (children == null || !children.contains(regionManager.getSuperRegion(args[2]).getType()))) {
                player.sendMessage(ChatColor.GRAY + "[Townships] There is already a super-region by that name.");
                return true;
            }



            List<String> quietDestroy = new ArrayList<String>();
            double radius = currentRegionType.getRawRadius();


            //Check if there is an overlapping super-region of the same type
            for (SuperRegion sr : regionManager.getSortedSuperRegions()) {
                try {
                    if (sr.getLocation().distance(currentLocation) < radius + regionManager.getSuperRegionType(sr.getType()).getRawRadius() &&
                            (sr.getType().equalsIgnoreCase(regionTypeName) || !sr.hasOwner(player))) {
                        player.sendMessage(ChatColor.GRAY + "[Townships] " + sr.getName() + " is already here.");
                        return true;
                    }
                } catch (IllegalArgumentException iae) {

                }
            }
            
            SuperRegion originalChild = null;
            if (!req.isEmpty()) {
                for (SuperRegion sr : regionManager.getContainingSuperRegions(currentLocation)) {
                    if (children != null && children.contains(sr.getType()) && sr.hasOwner(player)) {
                        if (children.get(0).equals(sr.getType())) {
                            originalChild = sr;
                        }
                        quietDestroy.add(sr.getName());
                    }

                    String rType = sr.getType();
                    if (!sr.hasOwner(player) && (!sr.hasMember(player) || !sr.getMember(player).contains(regionTypeName))) {
                        player.sendMessage(ChatColor.GRAY + "[Townships] You are not permitted to build a " + regionTypeName + " inside " + sr.getName());
                        return true;
                    } 
                    if (req.containsKey(rType)) {
                        int amount = req.get(rType);
                        if (amount < 2) {
                            req.remove(rType);
                            if (req.isEmpty()) {
                                break;
                            }
                        } else {
                            req.put(rType, amount - 1);
                        }
                    }
                }

                Location loc = player.getLocation();
                double x = loc.getX();
                double y = loc.getY();
                double z = loc.getZ();
                double radius1 = currentRegionType.getRawRadius();
                for (Region r : regionManager.getSortedRegions()) {
                    Location l = r.getLocation();
                    if (l.getX() + radius1 < x) {
                        break;
                    }

                    if (l.getX() - radius1 < x && l.getY() + radius1 > y && l.getY() - radius1 < y && 
                            l.getZ() + radius1 > z && l.getZ() - radius1 < z && l.getWorld().equals(loc.getWorld())) {

                        if (req.containsKey(r.getType())) {
                            if (req.get(r.getType()) < 2) {
                                req.remove(r.getType());
                            } else {
                                req.put(r.getType(), req.get(r.getType()) - 1);
                            }
                        } else if (req.containsKey(regionManager.getRegionType(r.getType()).getGroup())) {
                            String group = regionManager.getRegionType(r.getType()).getGroup();
                            if (req.get(group) < 2) {
                                req.remove(group);
                            } else {
                                req.put(group, req.get(group) - 1);
                            }
                        }
                    }
                }
                //Check to see if the new region completely contains the original child region
                if (originalChild != null) {
                    SuperRegionType srt = regionManager.getSuperRegionType(originalChild.getType());
                    if (srt != null && originalChild.getLocation().distance(currentLocation) > srt.getRadius()) {
                        player.sendMessage(ChatColor.RED + "[Townships] You must build this " + currentRegionType + " so that it completely covers your " + originalChild.getType());
                        return true;
                    }
                }

                if (!req.isEmpty()) {
                    for (Region r : regionManager.getContainingRegions(currentLocation)) {
                        String rType = regionManager.getRegion(r.getLocation()).getType();
                        if (req.containsKey(rType)) {
                            int amount = req.get(rType);
                            if (amount <= 1) {
                                req.remove(rType);
                            } else {
                                req.put(rType, amount - 1);
                            }
                        }
                    }

                }
            }
            if (!req.isEmpty()) {
                player.sendMessage(ChatColor.GRAY + "[Townships] This area doesnt have all of the required regions.");
                int j=0;
                String message = ChatColor.GOLD + "";
                for (String s : req.keySet()) {
                    if (message.length() + s.length() + 3 + req.get(s).toString().length() > 55) {
                        player.sendMessage(message);
                        message = ChatColor.GOLD + "";
                        j++;
                    }
                    if (j >14) {
                        break;
                    } else {
                        message += req.get(s) + " " + s + ", ";
                    }
                }
                if (!req.isEmpty()) {
                    player.sendMessage(message.substring(0, message.length() - 2));
                }
                return true;
            }

            //Assimulate any child super regions
            List<OfflinePlayer> owners = new ArrayList<OfflinePlayer>();
            double balance = 0.0;
            int power = 0;
            for (String s : quietDestroy) {
                SuperRegion sr = regionManager.getSuperRegion(s);
                for (OfflinePlayer so : sr.getOwners()) {
                    if (!owners.contains(so))
                        owners.add(so);
                }
                for (OfflinePlayer sm : sr.getMembers().keySet()) {
                    if (!members.containsKey(sm) && sr.getMember(sm).contains("member")) {
                        members.put(sm, sr.getMember(sm));
                    }
                }
                balance += sr.getBalance();
                power += sr.getPower();
            }
            power += currentRegionType.getDailyPower();
            if (power > currentRegionType.getMaxPower()) {
                power = currentRegionType.getMaxPower();
            }

            //Check if more members needed to create the super-region
            if (owners.size() + members.size() < currentRegionType.getPopulation()) {
                player.sendMessage(ChatColor.GRAY + "[Townships] You need " + (currentRegionType.getPopulation() - owners.size() - members.size()) + " more members.");
                return true;
            }
            
            List<Location> childLocations = null;
            if (originalChild != null) {
                childLocations = originalChild.getChildLocations();
//                System.out.println("[Townships] " + originalChild.getLocation().getWorld().getName() + ":" +
//                        originalChild.getLocation().getX() + ":" + originalChild.getLocation().getY() + ":" + originalChild.getLocation().getZ());
                childLocations.add(originalChild.getLocation());
            }

            for (String s : quietDestroy) {
                regionManager.destroySuperRegion(s, false, true);
            }
            if (currentCharter > 0 && pendingCharters.containsKey(args[2])) {
                configManager.removeCharter(args[2]);
                pendingCharters.remove(args[2]);
            }
            String playername = player.getName();
            if (!owners.contains(playername)) {
                owners.add(player);
            }
            if (costCheck > 0) {
                econ.withdrawPlayer(player, costCheck);
            }

            if (quietDestroy.isEmpty()) {
                balance += getConfigManager().getAutoDeposit();
            }
            
            regionManager.addSuperRegion(args[2], currentLocation, regionTypeName, owners, members, power, balance, childLocations);
            player.sendMessage(ChatColor.GOLD + "[Townships] You've created a new " + args[1] + " called " + args[2]);
            return true;
        } else if (args.length > 1 && args[0].equalsIgnoreCase("disable")) {
            SuperRegion sr = regionManager.getSuperRegion(args[1]);
            if (sr == null) {
                player.sendMessage("[Townships] Unable to find super region " + args[1]);
                return true;
            }
            if (perms != null && !perms.has(player, "townships.admin")) {
                player.sendMessage("[Townships] You do not have permission to use this command");
                return true;
            }
            
            regionManager.setPower(sr, 1);
            
            regionManager.reduceRegion(sr);
            return true;
        } else if (args.length > 0 && args[0].equalsIgnoreCase("listall")) {
            if (args.length > 1) {
                SuperRegionType srt = regionManager.getSuperRegionType(args[1]);
                if (srt == null) {
                    player.sendMessage(ChatColor.GRAY + "[Townships] There is no super-region type named " + args[1]);
                    return true;
                }
                String message = ChatColor.GOLD + "";
                int j =0;
                for (SuperRegion sr : regionManager.getSortedSuperRegions()) {
                    if (message.length() + sr.getName().length() + 2 > 55) {
                        player.sendMessage(message);
                        message = ChatColor.GOLD + "";
                        j++;
                    }
                    if (j > 14) {
                        break;
                    } else {
                        message += sr.getName() + ", ";
                    }
                }
                if (!message.equals(ChatColor.GOLD + "")) {
                    player.sendMessage(message);
                }
            } else {
                String message = ChatColor.GOLD + "";
                int j =0;
                for (SuperRegion sr : regionManager.getSortedSuperRegions()) {
                    if (message.length() + sr.getName().length() + 2 > 55) {
                        player.sendMessage(message);
                        message = ChatColor.GOLD + "";
                        j++;
                    }
                    if (j > 14) {
                        break;
                    } else {
                        message += sr.getName() + ", ";
                    }
                }
                if (!message.equals(ChatColor.GOLD + "")) {
                    player.sendMessage(message);
                }
            }
            return true;
        } else if (args.length > 2 && args[0].equalsIgnoreCase("withdraw")) {
            if (econ == null) {
                player.sendMessage(ChatColor.GRAY + "[Townships] No econ plugin recognized");
                return true;
            }
            double amount = 0;
            try {
                amount = Double.parseDouble(args[1]);
                if (amount < 0) {
                    player.sendMessage(ChatColor.GRAY + "[Townships] Withdraw a positive amount only.");
                    return true;
                }
            } catch (Exception e) {
                player.sendMessage(ChatColor.GRAY + "[Townships] Invalid amount /to withdraw <amount> <superregionname>");
                return true;
            }

            //Check if valid super-region
            SuperRegion sr = regionManager.getSuperRegion(args[2]);
            if (sr == null) {
                player.sendMessage(ChatColor.GRAY + "[Townships] " + args[2] + " is not a super-region");
                return true;
            }

            //Check if owner or permitted member
            if ((!sr.hasMember(player) || !sr.getMember(player).contains("withdraw")) && !sr.hasOwner(player)) {
                player.sendMessage(ChatColor.GRAY + "[Townships] You are not a member or dont have permission to withdraw");
                return true;
            }

            //Check if bank has that money
            double output = regionManager.getSuperRegionType(sr.getType()).getOutput();
            if (output < 0 && sr.getBalance() - amount < -output) {
                player.sendMessage(ChatColor.GRAY + "[Townships] You cant withdraw below the the minimum required.");
                return true;
            } else if (output >= 0 && sr.getBalance() - amount < 0) {
                player.sendMessage(ChatColor.GRAY + "[Townships] " + sr.getName() + " doesnt have that much money.");
                return true;
            }

            //Withdraw the money
            econ.depositPlayer(player, amount);
            regionManager.addBalance(sr, -amount);
            player.sendMessage(ChatColor.GOLD + "[Townships] You withdrew " + amount + " in the bank of " + args[2]);
            return true;
        } else if (args.length > 2 && args[0].equalsIgnoreCase("deposit")) {
            if (econ == null) {
                player.sendMessage(ChatColor.GRAY + "[Townships] No econ plugin recognized");
                return true;
            }
            double amount = 0;
            try {
                amount = Double.parseDouble(args[1]);
                if (amount < 0) {
                    player.sendMessage(ChatColor.GRAY + "[Townships] Nice try. Deposit a positive amount.");
                    return true;
                }
            } catch (Exception e) {
                player.sendMessage(ChatColor.GRAY + "[Townships] Invalid amount /to deposit <amount> <superregionname>");
                return true;
            }

            //Check if player has that money
            if (!econ.has(player, amount)) {
                player.sendMessage(ChatColor.GRAY + "[Townships] You dont have that much money.");
                return true;
            }

            //Check if valid super-region
            SuperRegion sr = regionManager.getSuperRegion(args[2]);
            if (sr == null) {
                player.sendMessage(ChatColor.GRAY + "[Townships] " + args[2] + " is not a super-region");
                return true;
            }

            //Check if owner or member
            if (!sr.hasMember(player) && !sr.hasOwner(player)) {
                player.sendMessage(ChatColor.GRAY + "[Townships] You are not a member of " + args[2]);
                return true;
            }

            //Deposit the money
            econ.withdrawPlayer(player, amount);
            regionManager.addBalance(sr, amount);
            player.sendMessage(ChatColor.GOLD + "[Townships] You deposited " + amount + " in the bank of " + args[2]);
            return true;
        } else if (args.length > 2 && args[0].equalsIgnoreCase("settaxes")) {
            player.getName();
            //Check if the player is a owner or member of the super region
            SuperRegion sr = regionManager.getSuperRegion(args[2]);
            if (sr == null) {
                player.sendMessage(ChatColor.GRAY + "[Townships] There is no region called " + args[2]);
                return true;
            }
            if (!sr.hasOwner(player) && !sr.hasMember(player)) {
                player.sendMessage(ChatColor.GRAY + "[Townships] You dont have permission to set taxes for " + args[2] + ".");
                return true;
            }

            //Check if member has permission
            if (sr.hasMember(player) && !sr.getMember(player).contains("settaxes")) {
                player.sendMessage(ChatColor.GRAY + "[Townships] You dont have permission to set taxes for " + args[2] + ".");
                return true;
            }

            //Check if valid amount
            double taxes = 0;
            try {
                taxes = Double.parseDouble(args[1]);
                double maxTax = configManager.getMaxTax();
                if (taxes < 0 || taxes > maxTax) {
                    player.sendMessage(ChatColor.GRAY + "[Townships] You cant set taxes that high/low.");
                    return true;
                }
            } catch (Exception e) {
                player.sendMessage(ChatColor.GRAY + "[Townships] Use /to settaxes <amount> <superregionname>.");
                return true;
            }



            //Set the taxes
            regionManager.setTaxes(sr, taxes);
            player.sendMessage(ChatColor.GOLD + "[Townships] You've set " + args[2] + "'s taxes to " + args[1]);
            return true;
        } else if (args.length > 1 && args[0].equalsIgnoreCase("listperms")) {
            //Get target player
            String playername;
            if (args.length > 2) {
                Player currentPlayer = getServer().getPlayer(args[1]);
                if (currentPlayer == null) {
                    player.sendMessage(ChatColor.GOLD + "[Townships] Could not find " + args[1]);
                    return true;
                } else {
                    playername = currentPlayer.getName();
                }
            } else {
                playername = player.getName();
            }

            String message = ChatColor.GRAY + "[Townships] " + playername + " perms for " + args[2] + ":";
            String message2 = ChatColor.GOLD + "";
            //Check if the player is a owner or member of the super region
            SuperRegion sr = regionManager.getSuperRegion(args[2]);
            if (sr == null) {
                player.sendMessage(ChatColor.GRAY + "[Townships] There is no region called " + args[2]);
                return true;
            }
            if (sr.hasOwner(player)) {
                player.sendMessage(message);
                player.sendMessage(message2 + "All Permissions");
                return true;
            } else if (sr.hasMember(player)) {
                player.sendMessage(message);
                int j=0;
                for (String s : sr.getMember(Bukkit.getOfflinePlayer(label))) {
                    if (message2.length() + s.length() + 2 > 57) {
                        player.sendMessage(message2);
                        message2 = ChatColor.GOLD + "";
                        j++;
                    }
                    if (j > 14) {
                        break;
                    } else {
                        message2 += s + ", ";
                    }
                }
                if (!sr.getMember(Bukkit.getOfflinePlayer(label)).isEmpty()) {
                    player.sendMessage(message2.substring(0, message2.length() - 2));
                }
                return true;
            }
            player.sendMessage(ChatColor.GRAY + "[Townships] " + playername + " doesn't belong to that region.");
            return true;
        } else if (args.length > 0 && args[0].equalsIgnoreCase("listallperms")) {
            player.sendMessage(ChatColor.GRAY + "[Townships] List of all member permissions:");
            player.sendMessage(ChatColor.GRAY + "member = is a member of the super-region.");
            player.sendMessage(ChatColor.GRAY + "title:<title> = player's title in channel");
            player.sendMessage(ChatColor.GRAY + "addmember = lets the player use /to addmember");
            player.sendMessage(ChatColor.GRAY + "<regiontype> = lets the player build that region type");
            player.sendMessage(ChatColor.GRAY + "withdraw = lets the player withdraw from the bank");
            return true;
        } else if (args.length > 0 && args[0].equalsIgnoreCase("ch")) {
            //Check if wanting to be set to any other channel
            if (args.length == 1 || args[1].equalsIgnoreCase("o") || args[1].equalsIgnoreCase("all") || args[1].equalsIgnoreCase("none")) {
                dpeListener.setPlayerChannel(player, "");
                return true;
            }

            if (args.length < 2) {
                player.sendMessage(ChatColor.GRAY + "[Townships] /to ch channelname.  /to ch (to go to all chat)");
                return true;
            }

            //Check if valid super region
            SuperRegion sr = regionManager.getSuperRegion(args[1]);
            if (sr == null) {
                player.sendMessage(ChatColor.GRAY + "[Townships] There is no super-region by that name (" + args[1] + ").");
                player.sendMessage(ChatColor.GRAY + "Try /to ch to go to all chat.");
                return true;
            }

            player.getName();
            if (!sr.hasMember(player) && !sr.hasOwner(player)) {
                player.sendMessage(ChatColor.GRAY + "[Townships] You must be a member of " + args[1] + " before joining thier channel");
                return true;
            }

            //Set the player as being in that channel
            dpeListener.setPlayerChannel(player, args[1]);
            return true;
        } else if (args.length > 2 && (args[0].equalsIgnoreCase("addmember") || args[0].equalsIgnoreCase("add"))) {
            //Check if valid super region
            SuperRegion sr = regionManager.getSuperRegion(args[2]);
            if (sr == null) {
                player.sendMessage(ChatColor.GRAY + "[Townships] There is no super-region by that name (" + args[2] + ").");
                return true;
            }

            player.getName();
            boolean isOwner = sr.hasOwner(player);
            boolean isMember = sr.hasMember(player);
            boolean isAdmin = Townships.perms.has(player, "townships.admin");
            if (!isMember && !isOwner && !isAdmin) {
                player.sendMessage(ChatColor.GRAY + "[Townships] You arent a member of " + args[2]);
                return true;
            }

            //Check if player has permission to invite players
            if (!isAdmin && isMember && !sr.getMember(player).contains("addmember")) {
                player.sendMessage(ChatColor.GRAY + "[Townships] You need permission addmember from an owner of " + args[2]);
                return true;
            }

            //Check if valid player
            Player invitee = getServer().getPlayer(args[1]);
            regionManager.getSuperRegion(args[1]);
            if (invitee == null /* && town == null */) { // TODO: revive this code
                player.sendMessage(ChatColor.GRAY + "[Townships] " + args[1] + " is not online.");
                return true;
            }

            //Check permission townships.join
            if (invitee != null && !perms.has(invitee, "townships.join") && !perms.has(invitee, "townships.join." + sr.getName())) {
                player.sendMessage(ChatColor.GRAY + "[Townships] " + args[1] + " doesnt have permission to join a super-region.");
                return true;
            }

            //Check if already a town member of a blacklisted town
            if (invitee != null && !configManager.getMultipleTownMembership()) {
                for (SuperRegion sr1 : regionManager.getSortedSuperRegions()) {
                    if ((sr1.hasOwner(invitee) || sr1.hasMember(invitee)) &&
                            !configManager.containsWhiteListTownMembership(sr1.getType())) {
                        player.sendMessage(ChatColor.GRAY + "[Townships] That player is already a member of another super-region.");
                        return true;
                    }
                }
            }

            //Check if has housing effect and if has enough housing
            if (!(Townships.perms != null && Townships.perms.has(player, "townships.admin")) && (regionManager.getSuperRegionType(sr.getType()).hasEffect("housing") && !regionManager.hasAvailableHousing(sr))) {
                player.sendMessage(ChatColor.GRAY + "[Townships] You cant addmember people to " + sr.getName() + " until you build more housing");
                return true;
            }

            //Send an invite
            if (invitee != null) {
                pendingInvites.put(invitee.getName(), args[2].toLowerCase());
                player.sendMessage(ChatColor.GRAY + "[Townships] You have invited " + ChatColor.GOLD + invitee.getDisplayName() + ChatColor.GRAY + " to join " + ChatColor.GOLD + args[2]);
                invitee.sendMessage(ChatColor.GOLD + "[Townships] You have been invited to join " + args[2] + ". /to accept " + args[2]);
            } else {
            	// TODO: revive this code
            	/* //Add the town to the super region
                List<String> perm = new ArrayList<String>();
                perm.add("member");
                regionManager.setMember(sr, "sr:" + town.getName(), perm);
                for (OfflinePlayer s : sr.getMembers().keySet()) {
                    Player p = getServer().getPlayer(s);
                    if (p != null) {
                        p.sendMessage(ChatColor.GOLD + town.getName() + " has joined " + args[1]);
                    }
                }
                for (OfflinePlayer s : sr.getOwners()) {
                    Player p = getServer().getPlayer(s);
                    if (p != null) {
                        p.sendMessage(ChatColor.GOLD + town.getName() + " has joined " + args[1]);
                    }
                } */
            }
            return true;
        } else if (args.length > 1 && args[0].equalsIgnoreCase("accept")) {
            //Check if player has a pending invite to that super-region
            if (!pendingInvites.containsKey(player.getName()) || !pendingInvites.get(player.getName()).equals(args[1].toLowerCase())) {
                player.sendMessage(ChatColor.GRAY + "[Townships] You don't have an invite to " + args[1]);
                return true;
            }

            //Check if valid super region
            SuperRegion sr = regionManager.getSuperRegion(args[1]);
            if (sr == null) {
                player.sendMessage(ChatColor.GRAY + "[Townships] There is no super-region by that name (" + args[1] + ").");
                return true;
            }

            //Check if player is a member or owner of that super-region
            String playername = player.getName();
            if (sr.hasMember(player) || sr.hasOwner(player)) {
                player.sendMessage(ChatColor.GRAY + "[Townships] You are already a member of " + args[1]);
                return true;
            }

            //Add the player to the super region
            List<String> perm = new ArrayList<String>();
            perm.add("member");
            regionManager.setMember(sr, player, perm);
            pendingInvites.remove(player.getName());
            player.sendMessage(ChatColor.GOLD + "[Townships] Welcome to " + args[1]);
            for (OfflinePlayer s : sr.getMembers().keySet()) {
                Player p = s.getPlayer();
                if (p != null) {
                    p.sendMessage(ChatColor.GOLD + playername + " has joined " + args[1]);
                }
            }
            for (OfflinePlayer s : sr.getOwners()) {
                Player p = s.getPlayer();
                if (p != null) {
                    p.sendMessage(ChatColor.GOLD + playername + " has joined " + args[1]);
                }
            }
            return true;
        } else if (args.length > 2 && args[0].equalsIgnoreCase("addowner")) {
            Player p = getServer().getPlayer(args[1]);
            String playername = args[1];

            //Check valid super-region
            SuperRegion sr = regionManager.getSuperRegion(args[2]);
            if (sr == null) {
                player.sendMessage(ChatColor.GRAY + "[Townships] There is no super-region named " + args[2]);
                return true;
            }

            //Check valid player
            if (p == null || !sr.hasMember(player)) {
                player.sendMessage(ChatColor.GRAY + "[Townships] There is no player online named: " + args[1]);
                return true;
            } else {
                playername = p.getName();
            }


            //Check if player is an owner of that region
            if (!sr.hasOwner(player) && !Townships.perms.has(player, "townships.admin")) {
                player.sendMessage(ChatColor.GRAY + "[Townships] You arent an owner of " + args[2]);
                return true;
            }

            //Check if playername is already an owner
            if (sr.hasOwner(player)) {
                player.sendMessage(ChatColor.GRAY + "[Townships] " + args[1] + " is already an owner of " + args[2]);
                return true;
            }

            //Check if player is member of super-region
            if (!sr.hasMember(player)) {
                player.sendMessage(ChatColor.GRAY + "[Townships] " + args[1] + " is not a member of " + args[2]);
                return true;
            }

            regionManager.removeMember(sr, player);
            if (p != null)
                p.sendMessage(ChatColor.GOLD + "[Townships] You are now an owner of " + args[2]);
            for (OfflinePlayer s : sr.getMembers().keySet()) {
                Player pl = s.getPlayer();
                if (pl != null) {
                    pl.sendMessage(ChatColor.GOLD + playername + " is now an owner of " + args[2]);
                }
            }
            for (OfflinePlayer s : sr.getOwners()) {
                Player pl = s.getPlayer();
                if (pl != null) {
                    pl.sendMessage(ChatColor.GOLD + playername + " is now an owner of " + args[2]);
                }
            }
            regionManager.setOwner(sr, player);
            return true;
        } else if (args.length > 1 && args[0].equalsIgnoreCase("leave")) {
            player.performCommand("to remove " + player.getName() + " " + args[1]);
            return true;
        } else if (args.length > 2 && (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("kick"))) {
            Player p = getServer().getPlayer(args[1]);
            String playername = args[1];


            //Check valid super-region
            SuperRegion sr = regionManager.getSuperRegion(args[2]);
            if (sr == null) {
                player.sendMessage(ChatColor.GRAY + "[Townships] There is no super-region named " + args[2]);
                return true;
            }

            //Check valid player
            if (p != null) {
                playername = p.getName();
            }

            boolean isMember = sr.hasMember(player); 
            boolean isOwner = sr.hasOwner(player); 
            boolean isAdmin = Townships.perms.has(player, "townships.admin");

            //Check if player is member or owner of super-region
            if (!isMember && !isOwner) {
                player.sendMessage(ChatColor.GRAY + "[Townships] " + args[1] + " is not a member of " + args[2]);
                return true;
            }
            //Check if player is removing self
            if (playername.equalsIgnoreCase(player.getName())) {
                if (isMember) {
                    regionManager.removeMember(sr, player);
                } else if (isOwner) {
                    regionManager.setOwner(sr, player);
                }
                player.sendMessage(ChatColor.GRAY + "[Townships] You have left " + args[2]);
                for (OfflinePlayer s : sr.getMembers().keySet()) {
                    Player pl = s.getPlayer();
                    if (pl != null) {
                        pl.sendMessage(ChatColor.GOLD + playername + " left " + args[2]);
                    }
                }
                for (OfflinePlayer s : sr.getOwners()) {
                    Player pl = s.getPlayer();
                    if (pl != null) {
                        pl.sendMessage(ChatColor.GOLD + playername + " left " + args[2]);
                    }
                }
                return true;
            }

            //Check if player has remove permission
            if (!sr.hasOwner(player) &&  !(!sr.hasMember(player) || !sr.getMember(player).contains("remove"))
                    && !isAdmin) {
                player.sendMessage(ChatColor.GRAY + "[Townships] You don't have permission to remove that member.");
                return true;
            }


            if (isMember) {
                regionManager.removeMember(sr, player);
            } else if (isOwner) {
                regionManager.setOwner(sr, player);
            } else {
                return true;
            }
            if (p != null)
                p.sendMessage(ChatColor.GRAY + "[Townships] You are no longer a member of " + args[2]);

            for (OfflinePlayer s : sr.getMembers().keySet()) {
                Player pl = s.getPlayer();
                if (pl != null) {
                    pl.sendMessage(ChatColor.GOLD + playername + " was removed from " + args[2]);
                }
            }
            for (OfflinePlayer s : sr.getOwners()) {
                Player pl = s.getPlayer();
                if (pl != null) {
                    pl.sendMessage(ChatColor.GOLD + playername + " was removed from " + args[2]);
                }
            }
            return true;
        } else if (args.length > 3 && (args[0].equalsIgnoreCase("toggleperm") || args[0].equalsIgnoreCase("perm"))) {
            Player p = getServer().getPlayer(args[1]);
            //Check valid super-region
            SuperRegion sr = regionManager.getSuperRegion(args[3]);
            if (sr == null) {
                player.sendMessage(ChatColor.GRAY + "[Townships] There is no super-region named " + args[3]);
                return true;
            }

            //Check if player is an owner of the super region
            if (!sr.hasOwner(player)) {
                player.sendMessage(ChatColor.GRAY + "[Townships] You aren't an owner of " + args[3]);
                return true;
            }

            //Check valid player
            if (p == null && !sr.hasMember(Bukkit.getOfflinePlayer(args[1]))) {
                player.sendMessage(ChatColor.GRAY + "[Townships] There is no player named: " + args[1]);
                return true;
            } else if (p != null) {
                p.getName();
            }

            //Check if player is member and not owner of super-region
            if (!sr.hasMember(player)) {
                player.sendMessage(ChatColor.GRAY + "[Townships] " + args[1] + " either owns, or is not a member of " + args[3]);
                return true;
            }

            List<String> perm = sr.getMember(player);
            if (perm.contains(args[2])) {
                perm.remove(args[2]);
                regionManager.setMember(sr, player, perm);
                player.sendMessage(ChatColor.GRAY + "[Townships] Removed perm " + args[2] + " for " + args[1] + " in " + args[3]);
                if (p != null)
                    p.sendMessage(ChatColor.GRAY + "[Townships] Your perm " + args[2] + " was revoked in " + args[3]);
                return true;
            } else {
                perm.add(args[2]);
                regionManager.setMember(sr, player, perm);
                player.sendMessage(ChatColor.GRAY + "[Townships] Added perm " + args[2] + " for " + args[1] + " in " + args[3]);
                if (p != null)
                    p.sendMessage(ChatColor.GRAY + "[Townships] You were granted permission " + args[2] + " in " + args[3]);
                return true;
            }
        } else if (args.length > 0 && args[0].equalsIgnoreCase("whatshere")) {
            Location loc = player.getLocation();
            boolean foundRegion = false;
            for (Region r : regionManager.getContainingRegions(loc)) {
                foundRegion = true;
                player.sendMessage(ChatColor.GRAY + "[Townships] Found Region ID: " + ChatColor.GOLD + r.getID());
                String message = ChatColor.GRAY + "Type: " + r.getType();
                if (!r.getOwners().isEmpty()) {
                    message += ", Owned by: " + r.getOwners().get(0);
                }
                player.sendMessage(message);
            }

            for (SuperRegion sr : regionManager.getContainingSuperRegions(loc)) {
                player.sendMessage(ChatColor.GRAY + "[Townships] Found Super-Region named: " + ChatColor.GOLD + sr.getName());
                String message = ChatColor.GRAY + "Type: " + sr.getType();
                if (!sr.getOwners().isEmpty()) {
                    message += ", Owned by: " + sr.getOwners().get(0);
                }
                player.sendMessage(message);
            }
            if (!foundRegion) {
                player.sendMessage(ChatColor.GRAY + "[Townships] There are no regions here.");
            }
            return true;
        } else if (args.length > 1 && args[0].equalsIgnoreCase("info")) {
            //Check if valid regiontype or super-regiontype
            RegionType rt = regionManager.getRegionType(args[1]);
            SuperRegionType srt = regionManager.getSuperRegionType(args[1]);
            if (rt == null && srt == null) {
                player.sendMessage(ChatColor.GRAY + "[Townships] There is no region type called " + args[1]);
                return true;
            }
            if (rt != null) {
                InfoGUIListener.openInfoInventory(rt, player, null);
            } else if (srt != null) {
                InfoGUIListener.openInfoInventory(srt, player, null);
            }

                /*player.sendMessage(ChatColor.GRAY + "[Townships] Info for region type " + ChatColor.GOLD + args[1] + ":");

                String message = "";
                if (rt.getMoneyRequirement() != 0) {
                    message += ChatColor.GRAY + "Cost: " + ChatColor.GOLD + rt.getMoneyRequirement();
                }
                if (rt.getMoneyOutput() != 0) {
                    message += ChatColor.GRAY + ", Payout: " + ChatColor.GOLD + rt.getMoneyOutput();
                }
                message += ChatColor.GRAY + ", Radius: " + ChatColor.GOLD + (int) Math.sqrt(rt.getRadius());
                player.sendMessage(message);

                String description = rt.getDescription();
                int j=0;
                if (description != null) {
                    message = ChatColor.GRAY + "Description: " + ChatColor.GOLD;
                    if (description.length() + message.length() <= 55) {
                        player.sendMessage(message + description);
                        description = null;
                    }
                    while (description != null && j<12) {
                        if (description.length() > 53) {
                            message += description.substring(0, 53);
                            player.sendMessage(message);
                            description = description.substring(53);
                            message = ChatColor.GOLD + "";
                            j++;
                        } else {
                            player.sendMessage(message + description);
                            description = null;
                            j++;
                        }
                    }
                }

                message = ChatColor.GRAY + "Effects: " + ChatColor.GOLD;
                if (rt.getEffects() != null) {
                    for (String is : rt.getEffects()) {
                        String addLine = is.split("\\.")[0] + ", ";
                        if (message.length() + addLine.length() > 55) {
                            player.sendMessage(message.substring(0, message.length() - 2));
                            message = ChatColor.GOLD + "";
                            j++;
                        }
                        if (j < 12) {
                            message += addLine;
                        } else {
                            break;
                        }
                    }
                }
                if (rt.getEffects() == null || rt.getEffects().isEmpty()) {
                    message += "None";
                    player.sendMessage(message);
                } else {
                    player.sendMessage(message.substring(0, message.length()-2));
                }
                message = ChatColor.GRAY + "Required Blocks: " + ChatColor.GOLD;
                if (rt.getRequirements() != null) {
                    for (List<HSItem> is : rt.getRequirements()) {
                        String addLine = "";
                        for (HSItem iss : is) {
                            String itemName = "";
                            if (iss.isWildDamage()) {
                                itemName = iss.getMat().name();
                            } else {
                                ItemStack ist = new ItemStack(iss.getMat(), 1, (short) iss.getDamage());
                                itemName = Items.itemByStack(ist).getName();
                            }

                            if (addLine.equals("")) {
                                addLine = iss.getQty() + ":" + itemName + ", ";
                            } else {
                                addLine = " or " + iss.getQty() + ":" + itemName + ", ";
                            }
                        }
                        if (message.length() + addLine.length() > 55) {
                            player.sendMessage(message.substring(0, message.length() - 2));
                            message = ChatColor.GOLD + "";
                            j++;
                        }
                        if (j < 12) {
                            message += addLine;
                        } else {
                            break;
                        }
                    }
                }
                if (rt.getRequirements() == null || rt.getRequirements().isEmpty()) {
                    message += "None";
                    player.sendMessage(message);
                } else {
                    player.sendMessage(message.substring(0, message.length()-2));
                }
                message = ChatColor.GRAY + "Required Items: " + ChatColor.GOLD;
                if (rt.getReagents() != null) {
                    for (List<HSItem> is : rt.getReagents()) {
                        String addLine = "";
                        for (HSItem iss : is) {

                            String itemName = "";
                            if (iss.isWildDamage()) {
                                itemName = iss.getMat().name();
                            } else {
                                ItemStack ist = new ItemStack(iss.getMat(), 1, (short) iss.getDamage());
                                itemName = Items.itemByStack(ist).getName();
                            }
                            if (addLine.equals("")) {
                                addLine = iss.getQty() + ":" + itemName + ", ";
                            } else {
                                addLine = " or " + iss.getQty() + ":" + itemName + ", ";
                            }
                        }
                        if (message.length() + addLine.length() > 55) {
                            player.sendMessage(message.substring(0, message.length() - 2));
                            message = ChatColor.GOLD + "";
                            j++;
                        }
                        if (j < 12) {
                            message += addLine;
                        } else {
                            break;
                        }
                    }
                }
                if (rt.getReagents() == null || rt.getReagents().isEmpty()) {
                    message += "None";
                    player.sendMessage(message);
                } else {
                    player.sendMessage(message.substring(0, message.length()-2));
                }
                if (rt.getUpkeep() != null && !rt.getUpkeep().isEmpty()) {
                    message = ChatColor.GRAY + "Upkeep Cost: " + ChatColor.GOLD;
                    for (List<HSItem> is : rt.getUpkeep()) {
                        String addLine = "";
                        for (HSItem iss : is) {
                            String itemName = "";
                            if (iss.isWildDamage()) {
                                itemName = iss.getMat().name();
                            } else {
                                ItemStack ist = new ItemStack(iss.getMat(), 1, (short) iss.getDamage());
                                itemName = Items.itemByStack(ist).getName();
                            }

                            if (addLine.equals("")) {
                                addLine = iss.getQty() + ":" + itemName + ", ";
                            } else {
                                addLine = " or " + iss.getQty() + ":" + itemName + ", ";
                            }
                        }
                        if (message.length() + addLine.length() > 55) {
                            player.sendMessage(message.substring(0, message.length() - 2));
                            message = ChatColor.GOLD + "";
                            j++;
                        }
                        if (j < 12) {
                            message += addLine;
                        } else {
                            break;
                        }
                    }
                    player.sendMessage(message.substring(0, message.length()-2));
                }

                if (rt.getOutput() != null && !rt.getOutput().isEmpty()) {
                    message = ChatColor.GRAY + "Output: " + ChatColor.GOLD;
                    for (List<HSItem> is : rt.getOutput()) {
                        String addLine = "";
                        for (HSItem iss : is) {
                            String itemName = "";
                            if (iss.isWildDamage()) {
                                itemName = iss.getMat().name();
                            } else {
                                ItemStack ist = new ItemStack(iss.getMat(), 1, (short) iss.getDamage());
                                itemName = Items.itemByStack(ist).getName();
                            }

                            if (addLine.equals("")) {
                                addLine = iss.getQty() + ":" + itemName + ", ";
                            } else {
                                addLine = " or " + iss.getQty() + ":" + itemName + ", ";
                            }
                        }
                        if (message.length() + addLine.length() > 55) {
                            player.sendMessage(message.substring(0, message.length() - 2));
                            message = ChatColor.GOLD + "";
                            j++;
                        }
                        if (j < 12) {
                            message += addLine;
                        } else {
                            break;
                        }
                    }
                    player.sendMessage(message.substring(0, message.length()-2));
                }
            } else if (srt != null) {
                player.sendMessage(ChatColor.GRAY + "[Townships] Info for super-region type " + ChatColor.GOLD + args[1] + ":");

                String message = "";
                if (srt.getMoneyRequirement() != 0) {
                    message += ChatColor.GRAY + "Cost: " + ChatColor.GOLD + srt.getMoneyRequirement();
                }
                if (srt.getOutput() != 0) {
                    message += ChatColor.GRAY + ", Payout: " + ChatColor.GOLD + srt.getOutput();
                }

                if (!message.equals("")) {
                    player.sendMessage(message);
                }

                message = ChatColor.GRAY + "Power: " + ChatColor.GOLD + srt.getMaxPower() + " (+" + srt.getDailyPower() + "), ";
                if (srt.getCharter() != 0) {
                    message += ChatColor.GRAY + "Charter: " + ChatColor.GOLD + srt.getCharter() + ChatColor.GRAY + ", ";
                }
                message += "Radius: " + ChatColor.GOLD + (int) Math.sqrt(srt.getRadius());

                player.sendMessage(message);

                int j=0;
                if (srt.getDescription() != null) {
                    message = ChatColor.GRAY + "Description: " + ChatColor.GOLD;
                    String tempMess = srt.getDescription();
                    if (tempMess.length() + message.length() <= 55) {
                        player.sendMessage(message + tempMess);
                        tempMess = null;
                    }
                    while (tempMess != null && j<12) {
                        if (tempMess.length() > 53) {
                            message += tempMess.substring(0, 53);
                            player.sendMessage(message);
                            tempMess = tempMess.substring(53);
                            message = ChatColor.GOLD + "";
                            j++;
                        } else {
                            player.sendMessage(message + tempMess);
                            tempMess = null;
                            j++;
                        }
                    }
                }
                message = ChatColor.GRAY + "Effects: " + ChatColor.GOLD;
                List<String> effects = srt.getEffects();
                if (effects != null) {
                    for (String is : effects) {
                        String addLine = is + ", ";
                        if (message.length() + addLine.length() > 55) {
                            player.sendMessage(message.substring(0, message.length() - 2));
                            message = ChatColor.GOLD + "";
                            j++;
                        }
                        if (j < 11) {
                            message += addLine;
                        } else {
                            break;
                        }
                    }
                }
                if (effects != null && !effects.isEmpty()) {
                    player.sendMessage(message.substring(0, message.length()-2));
                } else {
                    message += "None";
                    player.sendMessage(message);
                }
                message = ChatColor.GRAY + "Required Regions: " + ChatColor.GOLD;
                if (srt.getRequirements() != null) {
                    for (String is : srt.getRequirements().keySet()) {
                        String addLine = is + ":" + srt.getRequirement(is) + ", ";
                        if (message.length() + addLine.length() > 55) {
                            player.sendMessage(message.substring(0, message.length() - 2));
                            message = ChatColor.GOLD + "";
                            j++;
                        }
                        if (j < 12) {
                            message += addLine;
                        } else {
                            break;
                        }
                    }
                }
                if (srt.getRequirements() == null || srt.getRequirements().isEmpty()) {
                    message += "None";
                    player.sendMessage(message);
                } else {
                    player.sendMessage(message.substring(0, message.length()-2));
                }
                if (srt.getChildren() != null) {
                    message = ChatColor.GRAY + "Evolves from: " + ChatColor.GOLD;
                    for (String is : srt.getChildren()) {
                        String addLine = is + ", ";
                        if (message.length() + addLine.length() > 55) {
                            player.sendMessage(message.substring(0, message.length() - 2));
                            message = ChatColor.GOLD + "";
                            j++;
                        }
                        if (j < 12) {
                            message += addLine;
                        } else {
                            break;
                        }
                    }
                    player.sendMessage(message.substring(0, message.length()-2));
                }
            }*/
            return true;
        } else if (args.length > 1 && args[0].equalsIgnoreCase("addowner")) {
            String playername = args[1];
            Player aPlayer = getServer().getPlayer(playername);
            if (aPlayer != null) {
                playername = aPlayer.getName();
            }

            Location loc = player.getLocation();
            for (Region r : regionManager.getContainingBuildRegions(loc)) {
                if (r.isOwner(player) || (perms != null && perms.has(player, "townships.admin"))) {
                    if (r.isOwner(player)) {
                        player.sendMessage(ChatColor.GRAY + "[Townships] " + playername + " is already an owner of this region.");
                        return true;
                    }
                    if (r.isMember(player)) {
                        regionManager.setMember(r, player);
                    }
                    regionManager.setOwner(r, player);
                    player.sendMessage(ChatColor.GRAY + "[Townships] " + ChatColor.WHITE + "Added " + playername + " as an owner.");
                    if (aPlayer != null) {
                        aPlayer.sendMessage(ChatColor.GRAY + "[Townships] " + ChatColor.WHITE + "You're now a co-owner of " + player.getDisplayName() + "'s " + r.getType());
                    }
                    return true;
                } else {
                    boolean takeover = false;
                    for (SuperRegion sr : regionManager.getContainingSuperRegions(loc)) {
                        if (!sr.hasOwner(player)) {
                            takeover = false;
                            break;
                        }
                        if (regionManager.getSuperRegionType(sr.getType()).hasEffect("control")) {
                            takeover = true;
                        }
                    }
                    if (takeover) {
                        if (r.isOwner(player)) {
                            player.sendMessage(ChatColor.GRAY + "[Townships] " + playername + " is already an owner of this region.");
                            return true;
                        }
                        if (r.isMember(player)) {
                            regionManager.setMember(r, player);
                        }
                        regionManager.setOwner(r, player);
                        player.sendMessage(ChatColor.GRAY + "[Townships] " + ChatColor.WHITE + "Added " + playername + " as an owner.");
                        if (aPlayer != null) {
                            aPlayer.sendMessage(ChatColor.GRAY + "[Townships] " + ChatColor.WHITE + "You're now a co-owner of " + player.getDisplayName() + "'s " + r.getType());
                        }
                        return true;
                    } else {
                        player.sendMessage(ChatColor.GRAY + "[Townships] You don't own this region.");
                        return true;
                    }
                }
            }


            player.sendMessage(ChatColor.GRAY + "[Townships] You're not standing in a region.");
            return true;
        } else if (args.length > 1 && (args[0].equalsIgnoreCase("addmember") || args[0].equalsIgnoreCase("add"))) {
            String playername = args[1];
            Player aPlayer = getServer().getPlayer(playername);
            if (aPlayer == null) {
                SuperRegion sr = regionManager.getSuperRegion(args[1]);
                if (sr == null) {
                    playername = args[1];
                } else {
                    playername = "sr:" + sr.getName();
                }
            } else {
                playername = aPlayer.getName();
            }
            Location loc = player.getLocation();
            for (Region r : regionManager.getContainingBuildRegions(loc)) {
                if (r.isOwner(player) || (perms != null && perms.has(player, "townships.admin"))) {
                    if (r.isMember(player)) {
                        player.sendMessage(ChatColor.GRAY + "[Townships] " + playername + " is already a member of this region.");
                        return true;
                    }
                    if (r.isOwner(player) && !(playername.equals(player.getName()) && r.getOwners().get(0).equals(player.getName()))) {
                        regionManager.setOwner(r, player);
                    }
                    regionManager.setMember(r, player);
                    player.sendMessage(ChatColor.GRAY + "[Townships] " + ChatColor.WHITE + "Added " + playername + " to the region.");
                    return true;
                } else {
                    player.sendMessage(ChatColor.GRAY + "[Townships] You don't own this region.");
                    return true;
                }
            }


            player.sendMessage(ChatColor.GRAY + "[Townships] You're not standing in a region.");
            return true;
        } else if (args.length > 2 && args[0].equals("addmemberid")) {
            String playername = args[1];
            Player aPlayer = getServer().getPlayer(playername);
            if (aPlayer == null) {
                SuperRegion sr = regionManager.getSuperRegion(args[1]);
                if (sr == null) {
                    playername = args[1];
                } else {
                    playername = "sr:" + sr.getName();
                }
            } else {
                playername = aPlayer.getName();
            }
            Region r = null;
            try {
                r = regionManager.getRegionByID(Integer.parseInt(args[2]));
                r.getType();
            } catch (Exception e) {
                player.sendMessage(ChatColor.GRAY + "[Townships] " + args[1] + " is not a valid id");
                return true;
            }
            if (r.isOwner(player) || (perms != null && perms.has(player, "townships.admin"))) {
                if (r.isMember(player)) {
                    player.sendMessage(ChatColor.GRAY + "[Townships] " + playername + " is already a member of this region.");
                    return true;
                }
                if (r.isOwner(player) && !(playername.equals(player.getName()) && r.getOwners().get(0).equals(player.getName()))) {
                    regionManager.setOwner(r, player);
                }
                regionManager.setMember(r, player);
                player.sendMessage(ChatColor.GRAY + "[Townships] " + ChatColor.WHITE + "Added " + playername + " to the region.");
                return true;
            } else {
                player.sendMessage(ChatColor.GRAY + "[Townships] You don't own this region.");
                return true;
            }
        } else if (args.length > 1 && args[0].equalsIgnoreCase("whereis")) {
            RegionType rt = regionManager.getRegionType(args[1]);
            if (rt == null) {
                player.sendMessage(ChatColor.GRAY + "[Townships] There is no region type " + args[1]);
                return true;
            }
            boolean found = false;
            for (Region r : regionManager.getSortedRegions()) {
                if (r.isOwner(player) && r.getType().equals(args[1])) {
                    player.sendMessage(ChatColor.GOLD + "[Townships] " + args[1] + " at " + ((int) r.getLocation().getX())
                            + ", " + ((int) r.getLocation().getY()) + ", " + ((int) r.getLocation().getZ()));
                    found = true;
                }
            }
            if (!found) {
                player.sendMessage(ChatColor.GOLD + "[Townships] " + args[1] + " not found.");
            }
            return true;
        } else if (args.length > 1 && args[0].equalsIgnoreCase("setowner")) {
            String playername = args[1];
            Player aPlayer = getServer().getPlayer(playername);
            if (aPlayer != null) {
                playername = aPlayer.getName();
            } else {
                player.sendMessage(ChatColor.GRAY + "[Townships] " + playername + " must be online to setowner");
                return true;
            }


            Location loc = player.getLocation();
            List<Region> containedRegions = regionManager.getContainingBuildRegions(loc);
            for (Region r : regionManager.getContainingBuildRegions(aPlayer.getLocation())) {
                if (regionManager.isAtMaxRegions(aPlayer, regionManager.getRegionType(r.getType()))) {
                    player.sendMessage(ChatColor.GRAY + "[Townships] " + ChatColor.RED + playername + " cannot own more " + r.getType());
                    return true;
                }
                if (r.isOwner(player) || (perms != null && perms.has(player, "townships.admin"))) {
                    //Check if too far away
                    if (!containedRegions.contains(r)) {
                        continue;
                    }

                    if (r.isMember(player)) {
                        regionManager.setMember(r, player);
                    }
                    regionManager.setMember(r, player);
                    regionManager.setOwner(r, player);
                    regionManager.setPrimaryOwner(r, player);
                    player.sendMessage(ChatColor.GRAY + "[Townships] " + ChatColor.WHITE + "Set " + playername + " as the owner.");

                    aPlayer.sendMessage(ChatColor.GRAY + "[Townships] " + ChatColor.WHITE + "You're now the owner of " + player.getDisplayName() + "'s " + r.getType());
                    return true;
                } else {
                    player.sendMessage(ChatColor.GRAY + "[Townships] You don't own this region.");
                    return true;
                }
            }

            if (containedRegions.isEmpty()) {
                player.sendMessage(ChatColor.GRAY + "[Townships] You're not standing in a region.");
                return true;
            }

            player.sendMessage(ChatColor.GRAY + "[Townships] " + playername + " must be close by also.");
            return true;
        } else if (args.length > 1 && args[0].equalsIgnoreCase("remove")) {
            String playername = args[1];
            Player aPlayer = getServer().getPlayer(playername);
            if (aPlayer != null) {
                playername = aPlayer.getName();
            }
            Location loc = player.getLocation();
            for (Region r : regionManager.getContainingBuildRegions(loc)) {
                if (r.isOwner(player) || (perms != null && perms.has(player, "townships.admin"))) {
                    if (r.isPrimaryOwner(player)) {
                        player.sendMessage(ChatColor.GRAY + "[Townships] You must use /to setowner to change the original owner.");
                        return true;
                    }
                    if (!r.isMember(player) && !r.isOwner(player)) {
                        player.sendMessage(ChatColor.GRAY + "[Townships] " + playername + " doesn't belong to this region");
                        return true;
                    }
                    if (r.isMember(player)) {
                        regionManager.setMember(r, player);
                    } else if (r.isOwner(player)) {
                        regionManager.setOwner(r, player);
                    }
                    player.sendMessage(ChatColor.GRAY + "[Townships] " + ChatColor.WHITE + "Removed " + playername + " from the region.");
                    return true;
                } else {
                    player.sendMessage(ChatColor.GRAY + "[Townships] You don't own this region.");
                    return true;
                }
            }

            player.sendMessage(ChatColor.GRAY + "[Townships] You're not standing in a region.");
            return true;
        } else if (args.length > 1 && args[0].equalsIgnoreCase("destroy")) {
            //Check if valid region
            SuperRegion sr = regionManager.getSuperRegion(args[1]);
            Region r = null;
            if (sr == null) {
                try {
                    r = regionManager.getRegionByID(Integer.parseInt(args[1]));
                } catch (Exception e) {
                    player.sendMessage(ChatColor.GRAY + "[Townships] There is no region named " + args[1]);
                    return true;
                }
                if (r == null) {
                    player.sendMessage(ChatColor.GRAY + "[Townships] There is no region by that ID: " + args[1]);
                    return true;
                }
                if ((perms == null || !perms.has(player, "townships.admin")) && (r.getOwners().isEmpty() || !r.getOwners().contains(player))) {
                    player.sendMessage(ChatColor.GRAY + "[Townships] You are not the owner of that region.");
                    return true;
                }
                RegionType rt = regionManager.getRegionType(r.getType());
                if (rt != null && (getConfigManager().getSalvage() > 0 || rt.getSalvage() != 0) && r.isPrimaryOwner(player)) {
                    NumberFormat formatter = NumberFormat.getCurrencyInstance();
                    double salvageValue = getConfigManager().getSalvage() * rt.getMoneyRequirement();
                    salvageValue = rt.getSalvage() != 0 ? rt.getSalvage() : salvageValue;
                    player.sendMessage(ChatColor.GREEN + "[Townships] You salvaged region " + r.getID() + " for " + formatter.format(salvageValue));
                    econ.depositPlayer(player, salvageValue);
                }
                regionManager.destroyRegion(r.getLocation());
                regionManager.removeRegion(r.getLocation());
                return true;
            }

            //Check if owner or admin of that region
            if ((perms == null || !perms.has(player, "townships.admin")) && (sr.getOwners().isEmpty() || !sr.getOwners().contains(player.getName()))) {
                player.sendMessage(ChatColor.GRAY + "[Townships] You are not the owner of that region.");
                return true;
            }

            regionManager.destroySuperRegion(args[1], true);
            return true;
        } else if (args.length == 1 && args[0].equalsIgnoreCase("destroy")) {
            Location loc = player.getLocation();
            List<Location> locationsToDestroy = new ArrayList<Location>();
            for (Region r : regionManager.getContainingBuildRegions(loc)) {
                if (r.isOwner(player) || (perms != null && perms.has(player, "townships.admin"))) {
                    regionManager.destroyRegion(r.getLocation());
                    locationsToDestroy.add(r.getLocation());
                    break;
                } else {
                    player.sendMessage(ChatColor.GRAY + "[Townships] You don't own this region.");
                    return true;
                }
            }

            if (locationsToDestroy.isEmpty()) {
                player.sendMessage(ChatColor.GRAY + "[Townships] You're not standing in a region.");
            }
            
            for (Location l : locationsToDestroy) {
                regionManager.removeRegion(l);
                player.sendMessage(ChatColor.GRAY + "[Townships] Region destroyed.");
            }
            return true;
        } else if (args.length > 0 && args[0].equalsIgnoreCase("list")) {
            String category = "";

            if (args.length == 1 && regionManager.getRegionCategories().size() > 1) {
                GUIListener.openCategoryInventory(player);
                return true;
            }
            if (args.length != 1) {
                category = args[1].toLowerCase();
                if (category.equals("other")) {
                    category = "";
                }
            }
            
            if (!regionManager.getRegionCategories().containsKey(category) && (category.equals("") && 
                    !regionManager.getRegionCategories().containsKey("other"))
                    && !category.equals("towns")) {
                player.sendMessage(ChatColor.GRAY + "[Townships] that category is not recognized");
                return true;
            }

            /*player.sendMessage(ChatColor.GRAY + "[Townships] list of Region Types");
            String message = ChatColor.GOLD + "";*/
            boolean permNull = perms == null;
            List<RegionType> regions = new ArrayList<RegionType>();

            List<SuperRegionType> superRegions = new ArrayList<SuperRegionType>();

            boolean createAll = permNull || perms.has(player, "townships.create.all");
            if (!category.equals("towns") && category.contains(category)) {
                for (String s : regionManager.getRegionCategories().get(category)) {
                    if (createAll || permNull || perms.has(player, "townships.create." + s)) {
                        /*if (message.length() + s.length() + 2 > 55) {
                            player.sendMessage(message);
                            message = ChatColor.GOLD + "";
                            j++;
                        }
                        if (j > 14) {
                            break;
                        } else {
                            message += s + ", ";
                        }*/
                        regions.add(regionManager.getRegionType(s));
                    }
                }
            }
            if (category.equals("") && regionManager.getRegionCategories().containsKey("other")) {
                for (String s : regionManager.getRegionCategories().get("other")) {
                    if (createAll || permNull || perms.has(player, "townships.create." + s)) {
                        /*if (message.length() + s.length() + 2 > 55) {
                            player.sendMessage(message);
                            message = ChatColor.GOLD + "";
                            j++;
                        }
                        if (j > 14) {
                            break;
                        } else {
                            message += s + ", ";
                        }*/
                        regions.add(regionManager.getRegionType(s));
                    }
                }
            }
            if (regions.size() > 1) {
                Collections.sort(regions, new Comparator<RegionType>() {

                    @Override
                    public int compare(RegionType o1, RegionType o2) {
                        return GUIManager.compareRegions(o1, o2);
                    }
                });
            }
            if (category.equals("towns")) {
                for (String s : regionManager.getSuperRegionTypes()) {
                    if (createAll || permNull || perms.has(player, "townships.create." + s)) {
                        /*if (message.length() + s.length() + 2 > 55) {
                            player.sendMessage(message);
                            message = ChatColor.GOLD + "";
                            j++;
                        }
                        if (j > 14) {
                            break;
                        } else {
                            message += s + ", ";
                        }*/
                        superRegions.add(regionManager.getSuperRegionType(s));
                    }
                }
            }
            if (superRegions.size() > 1) {
                Collections.sort(superRegions, new Comparator<SuperRegionType>() {

                    @Override
                    public int compare(SuperRegionType o1, SuperRegionType o2) {
                        return GUIManager.compareSRegions(o1,o2);
                    }
                });
            }
            GUIListener.openListInventory(regions, superRegions, player, category);
            /*if (!message.equals(ChatColor.GOLD + "")) {
                player.sendMessage(message.substring(0, message.length() - 2));
            }*/
            return true;
        } else if (args.length > 2 && args[0].equalsIgnoreCase("rename")) {
            //Check if valid super-region
            SuperRegion sr = regionManager.getSuperRegion(args[1]);
            if (sr == null) {
                player.sendMessage(ChatColor.GRAY + "[Townships] There is no super-region by that name");
                return true;
            }

            //Check if valid name
            if (args[2].length() > 16 && Util.validateFileName(args[2])) {
                player.sendMessage(ChatColor.GRAY + "[Townships] That name is too long. Use 15 characters or less");
                return true;
            }

            //Check if player can rename the super-region
            if (!sr.hasOwner(player) && !Townships.perms.has(player, "townships.admin")) {
                player.sendMessage(ChatColor.GRAY + "[Townships] You don't have permission to rename that super-region.");
                return true;
            }

            double cost = configManager.getRenameCost();
            if (Townships.econ != null && cost > 0) {
                if (!Townships.econ.has(player, cost)) {
                    player.sendMessage(ChatColor.GRAY + "[Townships] It costs " + ChatColor.RED + cost + " to rename that.");
                    return true;
                } else {
                    Townships.econ.withdrawPlayer(player, cost);
                }
            }
            ToRenameEvent toRenameEvent = new ToRenameEvent(sr, args[1], args[2]);
            Bukkit.getPluginManager().callEvent(toRenameEvent);
            List<Location> childLocations = sr.getChildLocations();
            regionManager.destroySuperRegion(args[1], false, true);
            regionManager.addSuperRegion(args[2], sr.getLocation(), sr.getType(), sr.getOwners(), sr.getMembers(), sr.getPower(), sr.getBalance(), childLocations);
            player.sendMessage(ChatColor.GOLD + "[Townships] " + args[1] + " is now " + args[2]);
            return true;
        } else if (args.length > 0 && (args[0].equalsIgnoreCase("show"))) {

            return true;
        } else if (args.length > 0 && (args[0].equalsIgnoreCase("stats") || args[0].equalsIgnoreCase("who"))) {
            if (args.length == 1) {
                Location loc = player.getLocation();

                if (who(loc, player)) {
                    return true;
                }

                //player.sendMessage(ChatColor.GRAY + "[Townships] There are no regions here.");
                player.performCommand("to whatshere");
                return true;
            }

            SuperRegion sr = regionManager.getSuperRegion(args[1]);

            if (sr != null) {

                NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);

                SuperRegionType srt = regionManager.getSuperRegionType(sr.getType());
                int population = sr.getOwners().size() + sr.getMembers().size();
                double revenue = sr.getTaxes() * sr.getMembers().size() + srt.getOutput();
                boolean reqs = regionManager.hasAllRequiredRegions(sr);
                boolean hasMoney = sr.getBalance() > 0;
                boolean notDisabled = reqs && hasMoney && sr.getPower() > 0;
                regionManager.refreshGracePeriod(sr, hasMoney && reqs);
                long gracePeriod = regionManager.getRemainingGracePeriod(sr);
                String housing = "NA";
                if (srt.hasEffect("housing")) {
                    int housin = 0;
                    for (Region r : regionManager.getContainedRegions(sr)) {
                        housin += regionManager.getRegionType(r.getType()).getHousing();
                    }
                    housing = housin + "";
                }

                player.sendMessage(ChatColor.GRAY + "[Townships] ==:|" + ChatColor.GOLD + sr.getName() + " (" + sr.getType() + ") " + ChatColor.GRAY + "|:==");
                player.sendMessage(ChatColor.GRAY + "Population: " + ChatColor.GOLD + population + "/" + housing + ChatColor.GRAY +
                        " Bank: " + (sr.getBalance() < srt.getOutput() ? ChatColor.RED : ChatColor.GOLD) + formatter.format(sr.getBalance()) + ChatColor.GRAY +
                        " Power: " + (sr.getPower() < srt.getDailyPower() ? ChatColor.RED : ChatColor.GOLD) + sr.getPower() + 
                        " (+" + srt.getDailyPower() + ") / " + sr.getMaxPower());
                player.sendMessage(ChatColor.GRAY + "Taxes: " + ChatColor.GOLD + formatter.format(sr.getTaxes())
                        + ChatColor.GRAY + " Total Revenue: " + (revenue < 0 ? ChatColor.RED : ChatColor.GOLD) + formatter.format(revenue) +
                        ChatColor.GRAY + " Disabled: " + (notDisabled ? (ChatColor.GOLD + "false") : (ChatColor.RED + "true")));
                
                if (!notDisabled) {
                    long hours = (gracePeriod / (1000 * 60 * 60)) % 24;
                    long minutes = (gracePeriod / (1000 * 60)) % 60;
                    long seconds = (gracePeriod / 1000) % 60;
                    player.sendMessage(ChatColor.GOLD + "Grace Period: " + ChatColor.RED + hours + "h " + minutes + "m " + seconds + "s");
                }
                
                if (sr.hasMember(player) || sr.hasOwner(player)) {
                    player.sendMessage(ChatColor.GRAY + "Location: " + ChatColor.GOLD + (int) sr.getLocation().getX() + ", " + (int) sr.getLocation().getY() + ", " + (int) sr.getLocation().getZ());
                }
                if (sr.getTaxes() != 0) {
                    String message = ChatColor.GRAY + "Tax Revenue History: " + ChatColor.GOLD;
                    for (double d : sr.getTaxRevenue()) {
                        message += formatter.format(d) + ", ";
                    }
                    if (!sr.getTaxRevenue().isEmpty()) {
                        player.sendMessage(message.substring(0, message.length() - 2));
                    } else {
                        player.sendMessage(message);
                    }
                }
                String missingRegions = regionManager.hasAllRequiredRegions(sr, null);
                if (missingRegions != null) {
                    player.sendMessage(missingRegions);
                }
                
                String message = ChatColor.GRAY + "Owners: " + ChatColor.GOLD;
                int j = 0;
                for (OfflinePlayer s : sr.getOwners()) {
                    if (message.length() + s.getName().length() + 2 > 55) {
                        player.sendMessage(message);
                        message = ChatColor.GOLD + "";
                        j++;
                    }
                    if (j > 14) {
                      break;  
                    } else {
                        message += s.getName() + ", ";
                    }
                }
                if (!sr.getOwners().isEmpty()) {
                    player.sendMessage(message.substring(0, message.length() - 2));
                } else {
                    player.sendMessage(message);
                }
                message = ChatColor.GRAY + "Members: " + ChatColor.GOLD;
                for (OfflinePlayer s : sr.getMembers().keySet()) {
                    if (message.length() + 2 + s.getName().length() > 55) {
                        player.sendMessage(message);
                        message = ChatColor.GOLD + "";
                        j++;
                    }
                    if (j > 14) {
                        break;
                    } else {
                        message += s.getName() + ", ";
                    }
                }
                if (!sr.getMembers().isEmpty()) {
                    player.sendMessage(message.substring(0, message.length() - 2));
                } else {
                    player.sendMessage(message);
                }
                message = ChatColor.GRAY + "Wars: " + ChatColor.GOLD;
                for (SuperRegion srr : regionManager.getWars(sr)) {
                    String s = srr.getName();
                    if (message.length() + s.length() + 2 > 55) {
                        player.sendMessage(message);
                        message = ChatColor.GOLD + "";
                        j++;
                    }
                    if (j > 14) {
                      break;  
                    } else {
                        message += s + ", ";
                    }
                }
                if (!sr.getOwners().isEmpty()) {
                    player.sendMessage(message.substring(0, message.length() - 2));
                } else {
                    player.sendMessage(message);
                }
                return true;
            }

            Player p = getServer().getPlayer(args[1]);
            if (p != null) {
                p.getName();
                player.sendMessage(ChatColor.GRAY + "[Townships] " + p.getDisplayName() + " is a member of:");
                String message = ChatColor.GOLD + "";
                int j = 0;
                for (SuperRegion sr1 : regionManager.getSortedSuperRegions()) {
                    if (sr1.hasOwner(player) || sr1.hasMember(player)) {
                        if (message.length() + sr1.getName().length() + 2 > 55) {
                            player.sendMessage(message);
                            message = ChatColor.GOLD + "";
                            j++;
                        }
                        if (j > 14) {
                            break;
                        } else {
                            message += sr1.getName() + ", ";
                        }
                    }
                }
                if (!regionManager.getSortedRegions().isEmpty()) {
                    player.sendMessage(message.substring(0, message.length() - 2));
                }
                return true;
            }

            player.sendMessage(ChatColor.GRAY + "[Townships] Could not find player or super-region by that name");
            player.performCommand("to info " + args[1]);
            return true;
        } else if (args.length > 0 && effectCommands.contains(args[0])) {
            Bukkit.getServer().getPluginManager().callEvent(new ToCommandEffectEvent(args, player));
            return true;
        } else {
            //TODO add a page 3 to help for more instruction?
            if (args.length > 0 && args[args.length - 1].equals("2")) {
                sender.sendMessage(ChatColor.GRAY + "[Townships] by " + ChatColor.GOLD + "Multitallented" + ChatColor.GRAY + ": <> = required, () = optional" +
                        ChatColor.GOLD + " Page 2");
                sender.sendMessage(ChatColor.GRAY + "/to charter <towntype> <townname>");
                sender.sendMessage(ChatColor.GRAY + "/to charterstats <townname>");
                sender.sendMessage(ChatColor.GRAY + "/to signcharter <townname>");
                sender.sendMessage(ChatColor.GRAY + "/to cancelcharter <townname>");
                sender.sendMessage(ChatColor.GRAY + "/to rename <name> <newname>");
                sender.sendMessage(ChatColor.GRAY + "/to settaxes <amount> <name>");
                sender.sendMessage(ChatColor.GRAY + "/to withdraw|deposit <amount> <name>");
                sender.sendMessage(ChatColor.GRAY + "/to listperms <playername> <name>");
                sender.sendMessage(ChatColor.GRAY + "/to listallperms");
                sender.sendMessage(ChatColor.GRAY + "/to perm <playername> <perm> <name>");
                sender.sendMessage(ChatColor.GRAY + "/to ch (channel) -- Use /to ch to leave a channel");
                sender.sendMessage(ChatColor.GRAY + "See " + getConfigManager().getHelpPage() + " for more info | " + ChatColor.GOLD + "Page 2/3");
            } else if (args.length > 0 && args[args.length - 1].equals("3")) {
                sender.sendMessage(ChatColor.GRAY + "[Townships] by " + ChatColor.GOLD + "Multitallented" + ChatColor.GRAY + ": <> = required, () = optional" +
                        ChatColor.GOLD + " Page 3");
                sender.sendMessage(ChatColor.GRAY + "/to war <enemytown> <mytown>");
                sender.sendMessage(ChatColor.GRAY + "/to peace <enemytown> <mytown>");
                sender.sendMessage(ChatColor.GRAY + "See " + getConfigManager().getHelpPage() + " for more info | " + ChatColor.GOLD + "Page 3/3");
            } else {
                sender.sendMessage(ChatColor.GRAY + "[Townships] by " + ChatColor.GOLD + "Multitallented" + ChatColor.GRAY + ": () = optional" +
                        ChatColor.GOLD + " Page 1");
                sender.sendMessage(ChatColor.GRAY + "/to list");
                sender.sendMessage(ChatColor.GRAY + "/to info <regiontype>");
                sender.sendMessage(ChatColor.GRAY + "/to create <regiontype> (townname)");
                sender.sendMessage(ChatColor.GRAY + "/to destroy (name)");
                sender.sendMessage(ChatColor.GRAY + "/to add|addowner|remove <playername> (townname)");
                sender.sendMessage(ChatColor.GRAY + "/to leave <townname>");
                sender.sendMessage(ChatColor.GRAY + "/to whatshere");
                sender.sendMessage(ChatColor.GRAY + "/to who (playername|townname)");
                sender.sendMessage(ChatColor.GRAY + "See " + getConfigManager().getHelpPage() + " for more info |" + ChatColor.GOLD + " Page 1/3");
            }

            return true;
        }
    }

    public boolean who(Location loc, Player player) {
        for (Region r : regionManager.getContainingBuildRegions(loc)) {
            player.sendMessage(ChatColor.GRAY + "[Townships] ==:|" + ChatColor.GOLD + r.getID() + " (" + r.getType() + ") " + ChatColor.GRAY + "|:==");
            String message = ChatColor.GRAY + "Owners: " + ChatColor.GOLD;
            int j = 0;
            for (OfflinePlayer s : r.getOwners()) {
                if (message.length() + s.getName().length() + 2 > 55) {
                    player.sendMessage(message);
                    message = ChatColor.GOLD + "";
                    j++;
                }
                if (j > 14) {
                    break;
                } else {
                    message += s.getName() + ", ";
                }
            }
            if (!r.getOwners().isEmpty()) {
                player.sendMessage(message.substring(0, message.length() - 2));
            } else {
                player.sendMessage(message);
            }
            message = ChatColor.GRAY + "Members: " + ChatColor.GOLD;
            for (OfflinePlayer s : r.getMembers()) {
                if (message.length() + 2 + s.getName().length() > 55) {
                    player.sendMessage(message);
                    message = ChatColor.GOLD + "";
                    j++;
                }
                if (j > 14) {
                    break;
                } else {
                    message += s.getName() + ", ";
                }
            }
            if (!r.getMembers().isEmpty()) {
                player.sendMessage(message.substring(0, message.length() - 2));
            } else {
                player.sendMessage(message);
            }
            return true;
        }
        return false;
    }

    public void addCommand(String command) {
        effectCommands.add(command);
    }
    
    public boolean setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            econ = rsp.getProvider();
            if (econ != null)
                System.out.println("[Townships] Hooked into " + econ.getName());
        }
        return econ != null;
    }
    private boolean setupPermissions()
    {
        RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) {
            perms = permissionProvider.getProvider();
            if (perms != null)
                System.out.println("[Townships] Hooked into " + perms.getName());
        }
        return (perms != null);
    }
    private boolean setupChat()
    {
        RegisteredServiceProvider<Chat> chatProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.chat.Chat.class);
        if (chatProvider != null) {
            chat = chatProvider.getProvider();
        }
        return (chat != null);
    }
    public RegionManager getRegionManager() {
        return regionManager;
    }

    public CheckRegionTask getCheckRegionTask() {
        return theSender;
    }
    
    public void warning(String s) {
        String warning = "[Townships] " + s;
        getLogger().warning(warning);
    }
    
    public void setConfigManager(ConfigManager cm) {
        configManager = cm;
    }
    
    public void setCharters(Map<String, Charter> input) {
        this.pendingCharters = input;
    }
    
}
