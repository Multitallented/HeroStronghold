package multitallented.plugins.townships.effects;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.HashMap;
import java.util.List;
import multitallented.redcastlemedia.bukkit.townships.Townships;
import multitallented.redcastlemedia.bukkit.townships.Util;
import multitallented.redcastlemedia.bukkit.townships.effect.Effect;
import multitallented.redcastlemedia.bukkit.townships.events.ToCommandEffectEvent;
import multitallented.redcastlemedia.bukkit.townships.events.ToRegionCreatedEvent;
import multitallented.redcastlemedia.bukkit.townships.events.ToRegionDestroyedEvent;
import multitallented.redcastlemedia.bukkit.townships.region.Region;
import multitallented.redcastlemedia.bukkit.townships.region.RegionManager;
import multitallented.redcastlemedia.bukkit.townships.region.RegionType;
import multitallented.redcastlemedia.bukkit.townships.region.SuperRegion;
import multitallented.redcastlemedia.bukkit.townships.region.SuperRegionType;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 *
 * @author Multitallented
 */
public class EffectRebuild extends Effect {
    private final RegionManager rm;
    public EffectRebuild(Townships plugin) {
        super(plugin);
        this.rm = plugin.getRegionManager();
        registerEvent(new IntruderListener(this));
    }
    
    @Override
    public void init(Townships plugin) {
        super.init(plugin);
        
        plugin.addCommand("rebuild");
    }
    
    public class IntruderListener implements Listener {
        private final EffectRebuild effect;

        public IntruderListener(EffectRebuild effect) {
            this.effect = effect;
            getPlugin().addCommand("rebuild");
        }
        
        @EventHandler
        public void onCommandEffectEvent(ToCommandEffectEvent event) {
            if (event.getArgs().length < 1) {
                return;
            }
            
            if (!event.getArgs()[0].equalsIgnoreCase("rebuild")) {
                return;
            }

            Player player = event.getPlayer();
            Economy econ = Townships.econ;
            Permission perms = Townships.perms;

            RegionManager regionManager = getPlugin().getRegionManager();
            
            String regionName = null;
            if (event.getArgs().length > 1) {
                regionName = event.getArgs()[1];
            }
            
            List<Region> containedBuildRegions = regionManager.getContainingBuildRegions(player.getLocation());
            if (containedBuildRegions.isEmpty()) {
                return;
            }
            Region childRegion = containedBuildRegions.get(0);
            RegionType childRegionType = regionManager.getRegionType(childRegion.getType());
            Location currentLocation = childRegion.getLocation();
            
            //Check if region can be rebuilt
            boolean hasEffect = false;
            for (String s : childRegionType.getEffects()) {
                String[] parts = s.split("\\.");
                if (parts.length < 2) {
                    continue;
                }
                if (parts[0].equals("rebuild")) {
                    if (regionName == null) {
                        regionName = parts[1];
                        hasEffect = true;
                    } else if (parts[1].equals(regionName)) {
                        hasEffect = true;
                    }
                }
            }
            if (!hasEffect) {
                if (regionName == null) {
                    player.sendMessage(ChatColor.GRAY + "[Townships] This region can't be rebuilt.");
                } else {
                    player.sendMessage(ChatColor.GRAY + "[Townships] This region can't be rebuilt into a " + regionName + ".");
                }
                return;
            }

            //Owner Check
            if (childRegion.getOwners().isEmpty() || !childRegion.getOwners().get(0).equals(player.getName())) {
                player.sendMessage(ChatColor.GRAY + "[Townships] You are not an owner of this " + childRegionType.getName() + ".");
                return;
            }
            
            //Permission Check
            boolean nullPerms = perms == null;
            boolean createAll = nullPerms || perms.has(player, "townships.create.all");
            if (!(nullPerms || createAll || perms.has(player, "townships.rebuild." + regionName))) {
                
                player.sendMessage(ChatColor.GRAY + "[Townships] you dont have permission to create a " + regionName);
                return;
            }

            RegionType currentRegionType = regionManager.getRegionType(regionName);
            if (currentRegionType == null) {
                player.sendMessage(ChatColor.GRAY + "[Townships] " + regionName + " isnt a valid region type");
                player.sendMessage(ChatColor.GRAY + "[Townships] Try /hs create " + regionName + " <insert_name_here>");
                return;
            }
            
            //Check if player can afford to create this townships
            double costCheck = 0;
            if (econ != null) {
                double cost = currentRegionType.getMoneyRequirement();
                if (econ.getBalance(player.getName()) < cost) {
                    player.sendMessage(ChatColor.GRAY + "[Townships] You need $" + cost + " to make this type of structure.");
                    return;
                } else {
                    costCheck = cost;
                }
                
            }
            
            //Check if over max number of regions of that type
            if (regionManager.isAtMaxRegions(player, currentRegionType, -1)) {
                player.sendMessage(ChatColor.GRAY + "[Townships] You dont have permission to build more " + currentRegionType.getName());
                return;
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
                return;
            }
            
            //Check if too close to other Townships
            if (!regionManager.getContainingBuildRegionsExcept(currentLocation, childRegion).isEmpty()) {
                player.sendMessage (ChatColor.GRAY + "[Townships] You are too close to another region");
                return;
            }
            
            //Check if in a super region and if has permission to make that region
            String playername = player.getName();
            List<String> reqSuperRegion = currentRegionType.getSuperRegions();
            
            boolean meetsReqs = false;
            String limitMessage = null;
            
            if (reqSuperRegion != null && !reqSuperRegion.isEmpty()) {
                for (SuperRegion sr : regionManager.getContainingSuperRegions(currentLocation)) {
                    if (reqSuperRegion.contains(sr.getType())) {
                        meetsReqs = true;
                        if (!regionManager.isInsideSuperRegion(sr, currentLocation, currentRegionType.getRawBuildRadius())) {
                            player.sendMessage(ChatColor.RED + "[Townships] Not all of the " + regionName + " would be inside the " + sr.getType());
                            return;
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
                            return;
                        }
                    }
                }
            } else {
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
                if (reqSuperRegion == null || !reqSuperRegion.isEmpty()) {
                    player.sendMessage(message.substring(0, message.length() - 2));
                }
                return;
            }
            
            if (limitMessage != null) {
                player.sendMessage(limitMessage);
                return;
            }
            
            //Check if it has required blocks
            if (!currentRegionType.getRequirements().isEmpty()) {
                List<String> message = Util.hasCreationRequirements(currentLocation, currentRegionType, regionManager);
                if (!message.isEmpty()) {
                    player.sendMessage(ChatColor.GRAY + "[Townships] you don't have all of the required blocks in this structure.");
                    for (String s : message) {
                        player.sendMessage(ChatColor.GOLD + s);
                    }
                    return;
                }
            }
            
            //Create chest at players feet for tracking reagents and removing upkeep items
            
            if (econ != null && costCheck > 0) {
                econ.withdrawPlayer(player, costCheck);
            }
            
            player.sendMessage(ChatColor.GRAY + "[Townships] " + ChatColor.WHITE + 
                    "You successfully rebuilt a " + ChatColor.RED + childRegion.getType() + 
                    ChatColor.WHITE + " into a " + ChatColor.RED + regionName);
            
            childRegion.setType(regionName);
            File regionFolder = new File(getPlugin().getDataFolder(), "data");
            File regionFile = new File(regionFolder, childRegion.getID() + ".yml");
            FileConfiguration rConfig = new YamlConfiguration();       
            try {
                rConfig.load(regionFile);
                rConfig.set("type", regionName);
                rConfig.save(regionFile);
            } catch (IOException | InvalidConfigurationException e) {
                getPlugin().warning("[Townships] unable to save rebuild in " + childRegion.getID() + ".yml");
            }
            ToRegionDestroyedEvent destroyedEvent = new ToRegionDestroyedEvent(childRegion, false);
            Bukkit.getPluginManager().callEvent(destroyedEvent);
            ToRegionCreatedEvent createdEvent = new ToRegionCreatedEvent(childRegion);
            Bukkit.getPluginManager().callEvent(createdEvent);
        }
    }
}