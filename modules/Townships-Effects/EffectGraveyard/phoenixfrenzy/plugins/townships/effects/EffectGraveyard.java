package phoenixfrenzy.plugins.townships.effects;

import java.io.File;
import java.io.IOException;
import java.util.*;

import multitallented.redcastlemedia.bukkit.townships.Townships;
import multitallented.redcastlemedia.bukkit.townships.effect.Effect;
import multitallented.redcastlemedia.bukkit.townships.events.ToCommandEffectEvent;
import multitallented.redcastlemedia.bukkit.townships.events.ToTwoSecondEffectEvent;
import multitallented.redcastlemedia.bukkit.townships.region.Region;
import multitallented.redcastlemedia.bukkit.townships.region.RegionManager;
import multitallented.redcastlemedia.bukkit.townships.region.RegionType;
import multitallented.redcastlemedia.bukkit.townships.region.SuperRegion;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import static org.bukkit.event.EventPriority.HIGH;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 *
 * @author Phoenix Frenzy
 * @author Multitallented
 */
public class EffectGraveyard extends Effect {
    public EffectGraveyard(Townships plugin) {
        super(plugin);
        registerEvent(new EffectGraveyard.UpkeepListener(this));
    }
    
    @Override
    public void init(Townships plugin) {
        super.init(plugin);
        plugin.addCommand("wild");
    }
    
    public class UpkeepListener implements Listener {
        private final EffectGraveyard effect;
        private RegionManager rm;
        private World defaultWorld = null;
        
        public HashMap<String, Region> respawnLocations = new HashMap<String, Region>();
        
        public UpkeepListener(EffectGraveyard effect) {
            this.effect = effect;
            rm = effect.getPlugin().getRegionManager();

            //Get the default world
            getDefaultWorld: {
                File folder = getPlugin().getDataFolder();
                if (!folder.exists()) {
                    if (!folder.mkdir()) {
                        break getDefaultWorld;
                    }
                }
                File configFile = new File(folder, "config.yml");
                if (!configFile.exists()) {
                    try {
                        if (!configFile.createNewFile()) {
                            break getDefaultWorld;
                        }
                    } catch (Exception e) {
                        break getDefaultWorld;
                    }
                }
                FileConfiguration config = new YamlConfiguration();
                try {
                    config.load(configFile);
                    String worldName = config.getString("graveyard.world", "world");
                    defaultWorld = Bukkit.getWorld(worldName);
                } catch (Exception e) {
                    break getDefaultWorld;
                }
            }

            for (Region graveyard : rm.getSortedRegions()) {
                RegionType rt = rm.getRegionType(graveyard.getType());
                if (rt == null) {
                    continue;
                }
                boolean hasEffect = false;
                List<String> effects = rt.getEffects();
                for (String s : effects) {
                    if (!s.startsWith("graveyard")) {
                        continue;
                    }
                    hasEffect = true;
                }
                if (!hasEffect) {
                    continue;
                }
                
                File regionFolder = new File(getPlugin().getDataFolder(), "data");
                File regionFile = new File(regionFolder, graveyard.getID() + ".yml");
                if (!regionFile.exists()) {
                    return;
                }
                
                FileConfiguration rConfig = new YamlConfiguration();
                try {
                    rConfig.load(regionFile);
                    
                    List<String> respawningPlayers = rConfig.getStringList("respawning-players");
                    for (String s : respawningPlayers) {
                        respawnLocations.put(s, graveyard);
                    }
                    
                } catch (Exception e) {
                    getPlugin().warning("[Townships] unable to read death locations in " + graveyard.getID() + ".yml");
                    return;
                }
            }
        }

        @EventHandler
        public void onCommandEffectEvent(ToCommandEffectEvent event) {
            if (!event.getArgs()[0].equalsIgnoreCase("wild")) {
                return;
            }
            final Player player = event.getPlayer();
            if (Townships.perms != null && !Townships.perms.has(player, "townships.wild")) {
                player.sendMessage(ChatColor.RED + "[Townships] You don't have permission for /to wild");
                return;
            }
            List<Region> graveyards = new ArrayList<Region>();
            HashSet<String> regionTypes = new HashSet<String>();
            for (String st : getPlugin().getRegionManager().getRegionTypes()) {
                RegionType rt = getPlugin().getRegionManager().getRegionType(st);
                boolean hasEffect = false;
                for (String s : rt.getEffects()) {
                    if (s.startsWith("graveyard_public")) {
                        hasEffect = true;
                        break;
                    }
                }
                if (hasEffect) {
                    regionTypes.add(st);
                }
            }

            for (Region re : getPlugin().getRegionManager().getSortedRegions()) {
                if (regionTypes.contains(re.getType())) {
                    graveyards.add(re);
                }
            }
            Random rand = new Random();
            int randomInt = rand.nextInt(graveyards.size());
            System.out.println("Wild: " + randomInt + "/" + graveyards.size());
            Location location = graveyards.get(randomInt).getLocation().getBlock().getRelative(BlockFace.UP, 2).getLocation();
            player.sendMessage(ChatColor.GRAY + "[Townships] You have been teleported");
            player.teleport(location);
        }

            @EventHandler
        public void onJailTick(ToTwoSecondEffectEvent event) {
            
        }
        
        @EventHandler(priority=HIGH)
        public void onPlayerDeath(PlayerDeathEvent event) {
            if (event.getEntity() == null) {
                return;
            }
            Player player = (Player) event.getEntity();
            
            Location deathLocation = player.getLocation();

            if (defaultWorld != null && !defaultWorld.equals(player.getWorld())) {
                deathLocation = new Location(defaultWorld, deathLocation.getX(), deathLocation.getY(), deathLocation.getZ());
            }

            //If you die in jail, then bypass jail
            Region jail = null;
            boolean bypassJail = false;
            for (Region r : rm.getContainingBuildRegions(deathLocation)) {
                RegionType rt = rm.getRegionType(r.getType());
                if (rt == null) {
                    continue;
                }
                if (rt.getEffects().contains("jail")) {
                    bypassJail = true;
                    break;
                }
            }

            //If you didn't die in a jail, check if you died in a town with a jail
            if (!bypassJail) {
                outer: for (SuperRegion sr : rm.getContainingSuperRegions(deathLocation)) {
                    if (sr.hasMember(player) || sr.hasOwner(player)) {
                        continue;
                    }

                    for (Region r : rm.getContainedRegions(sr)) {
                        RegionType rt = rm.getRegionType(r.getType());
                        if (rt == null) {
                            continue;
                        }

                        if (rt.getEffects().contains("jail")) {
                            jail = r;
                            break outer;
                        }
                    }
                }
            }

            //If you died in a town with a jail, then put their respawn point in the jail
            if (jail != null) {
                File regionFolder = new File(getPlugin().getDataFolder(), "data");
                File regionFile = new File(regionFolder, jail.getID() + ".yml");
                if (!regionFile.exists()) {
                    System.out.println("[Townships] graveyard data file " + jail.getID() + ".yml does not exist");
                    return;
                }

                FileConfiguration rConfig = new YamlConfiguration();
                try {
                    rConfig.load(regionFile);

                    List<String> respawningPlayers = rConfig.getStringList("respawning-players");
                    respawningPlayers.add(player.getName());
                    rConfig.set("respawning-players", respawningPlayers);
                    rConfig.save(regionFile);

                } catch (Exception e) {
                    getPlugin().warning("[Townships] unable to save death location in " + jail.getID() + ".yml");
                    return;
                }
                respawnLocations.put(player.getName(), jail);
                return;
            }
            
            Region graveyard = rm.getClosestRegionWithEffectAndTownMember(deathLocation, "graveyard", player);
            Region publicGraveyard = rm.getClosestRegionWithEffect(deathLocation, "graveyard_public");
            Region respawnRegion = null;
            if (graveyard == null && publicGraveyard != null) {
                respawnRegion = publicGraveyard;
            } else if (graveyard != null && publicGraveyard == null) {
                respawnRegion = graveyard;
            } else if (graveyard != null) {
                if (deathLocation.distanceSquared(graveyard.getLocation()) > deathLocation.distanceSquared(publicGraveyard.getLocation())) {
                    respawnRegion = publicGraveyard;
                } else {
                    respawnRegion = graveyard;
                }
            } else {
                System.out.println("[Townships] unable to find graveyard for " + player.getName());
                return;
            }
            
                
            File regionFolder = new File(getPlugin().getDataFolder(), "data");
            File regionFile = new File(regionFolder, respawnRegion.getID() + ".yml");
            if (!regionFile.exists()) {
                System.out.println("[Townships] graveyard data file " + respawnRegion.getID() + ".yml does not exist");
                return;
            }

            FileConfiguration rConfig = new YamlConfiguration();
            try {
                rConfig.load(regionFile);

                List<String> respawningPlayers = rConfig.getStringList("respawning-players");
                respawningPlayers.add(player.getName());
                rConfig.set("respawning-players", respawningPlayers);
                rConfig.save(regionFile);

            } catch (Exception e) {
                getPlugin().warning("[Townships] unable to save death location in " + respawnRegion.getID() + ".yml");
                return;
            }
            respawnLocations.put(player.getName(), respawnRegion);
        }
        
        @EventHandler(priority=HIGH)
        public void onplayerespawn(PlayerRespawnEvent event) {
            Player player = event.getPlayer();
            Region graveyard = respawnLocations.get(player.getName());
            if (graveyard == null) {
                System.out.println("[Townships] No graveyard found to respawn " + player.getName());
                return;
            }
            Location respawnHere = graveyard.getLocation().getBlock().getRelative(BlockFace.UP).getLocation();
            respawnHere.setX(Math.floor(respawnHere.getX()) + 0.5);
            respawnHere.setY(Math.floor(respawnHere.getY()) + 0.5);
            respawnHere.setZ(Math.floor(respawnHere.getZ()) + 0.5);
            
            event.setRespawnLocation(respawnHere);
            respawnLocations.remove(player.getName());
            
            
            File regionFolder = new File(getPlugin().getDataFolder(), "data");
            File regionFile = new File(regionFolder, graveyard.getID() + ".yml");
            if (!regionFile.exists()) {
                getPlugin().warning("[Townships] Unable to find " + graveyard.getID() + ".yml to remove death location");
                return;
            }

            FileConfiguration rConfig = new YamlConfiguration();
            try {
                rConfig.load(regionFile);

                List<String> respawningPlayers = rConfig.getStringList("respawning-players");
                respawningPlayers.remove(player.getName());
                rConfig.set("respawning-players", respawningPlayers);
                rConfig.save(regionFile);

            } catch (IOException | InvalidConfigurationException e) {
                getPlugin().warning("[Townships] unable to save removed death location in " + graveyard.getID() + ".yml");
            }
        }
    }
}