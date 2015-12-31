package multitallented.plugins.townships.effects;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import multitallented.redcastlemedia.bukkit.townships.Townships;
import multitallented.redcastlemedia.bukkit.townships.effect.Effect;
import multitallented.redcastlemedia.bukkit.townships.events.ToRegionCreatedEvent;
import multitallented.redcastlemedia.bukkit.townships.events.ToSuperRegionCreatedEvent;
import multitallented.redcastlemedia.bukkit.townships.region.Region;
import multitallented.redcastlemedia.bukkit.townships.region.SuperRegion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 *
 * @author Multitallented
 */
public class EffectLog extends Effect {

    public HashMap<String, HashMap<String, String>> previousLocations = new HashMap<String, HashMap<String, String>>();
    public HashMap<String, List<String>> comebacks = new HashMap<String, List<String>>();
    
    public EffectLog(Townships plugin) {
        super(plugin);
        registerEvent(new UpkeepListener(plugin, this));
    }

    @Override
    public void init(Townships plugin) {
        super.init(plugin);
    }

    public class UpkeepListener implements Listener {
        private final EffectLog effect;
        private final Townships plugin;
        public UpkeepListener(Townships plugin, EffectLog effect) {
            this.effect = effect;
            this.plugin = plugin;
        }

        @EventHandler
        public void onRegionCreated(ToRegionCreatedEvent event) {
            Region r = event.getRegion();
            OfflinePlayer player = r.getOwners().get(0);
            Location l = r.getLocation();
            
            int bonusPoints = getBonusPoints(r.getType(), player.getName(), locationToString(l));
            if (bonusPoints < 0) {
                return;
            }
            
            int pointValue = getPoints(r.getType());
            pointValue += bonusPoints;
            File eventFile = new File(getPlugin().getDataFolder(), "event.yml");
            if (!eventFile.exists()) {
                try {
                    eventFile.createNewFile();
                } catch (IOException ex) {
                    plugin.warning("[Townships] Unable to create new event.yml");
                    return;
                }
            }
            
            FileConfiguration eventConfig = new YamlConfiguration();
            try {
                eventConfig.load(eventFile);
                int score = eventConfig.getInt(player.getName(), 0);
                eventConfig.set(player.getName(), score + pointValue);
                eventConfig.save(eventFile);
                
                player.getPlayer().sendMessage(ChatColor.GREEN + "[Townships] You just earned " + pointValue + " points!");
                player.getPlayer().sendMessage(ChatColor.GREEN + "[Townships] Your new total is " + (score + pointValue) + " points!");
            } catch (Exception e) {
                plugin.warning("[Townships] Unable to save to event.yml");
                return;
            }
            
            
            File eventLogFile = new File(getPlugin().getDataFolder(), "event-log.txt");
            if (!eventLogFile.exists()) {
                try {
                    eventLogFile.createNewFile();
                } catch (IOException ex) {
                    plugin.warning("[Townships] Unable to create new event-log.txt");
                    return;
                }
            }
            
            try {
                FileWriter writer = new FileWriter(eventLogFile, true);
                DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
                Date today = Calendar.getInstance().getTime();
                String reportDate = df.format(today);
                writer.write(player.getName() + ": [" + reportDate + "] built " + r.getType() + " at " + 
                        locationToString(l) +
                        " for " + pointValue + " points\n");
                writer.close();
            } catch (Exception e) {
                plugin.warning("[Townships] Unable to save to event-log.txt");
                return;
            }
        }
        
        @EventHandler
        public void onSRegionCreated(ToSuperRegionCreatedEvent event) {
            String name = event.getName();
            SuperRegion r = plugin.getRegionManager().getSuperRegion(name);
            if (r == null) {
                return;
            }
            String playername = r.getOwners().get(0).getName();
            Location l = r.getLocation();
            
            int bonusPoints = getBonusPoints(r.getType(), playername, locationToString(l));
            if (bonusPoints < 0) {
                return;
            }
            
            int pointValue = getPoints(r.getType());
            pointValue += bonusPoints;
            File eventFile = new File(getPlugin().getDataFolder(), "event.yml");
            if (!eventFile.exists()) {
                try {
                    eventFile.createNewFile();
                } catch (IOException ex) {
                    plugin.warning("[Townships] Unable to create new event.yml");
                    return;
                }
            }
            
            FileConfiguration eventConfig = new YamlConfiguration();
            try {
                eventConfig.load(eventFile);
                int score = eventConfig.getInt(playername, 0);
                eventConfig.set(playername, score + pointValue);
                eventConfig.save(eventFile);
                
                Bukkit.getPlayer(playername).sendMessage(ChatColor.GREEN + "[Townships] You just earned " + pointValue + " points!");
                Bukkit.getPlayer(playername).sendMessage(ChatColor.GREEN + "[Townships] Your new total is " + (score + pointValue) + " points!");
            } catch (Exception e) {
                plugin.warning("[Townships] Unable to save to event.yml");
                return;
            }
            
            
            File eventLogFile = new File(getPlugin().getDataFolder(), "event-log.txt");
            if (!eventLogFile.exists()) {
                try {
                    eventLogFile.createNewFile();
                } catch (IOException ex) {
                    plugin.warning("[Townships] Unable to create new event-log.txt");
                    return;
                }
            }
            
            try {
                FileWriter writer = new FileWriter(eventLogFile, true);
                DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
                Date today = Calendar.getInstance().getTime();
                String reportDate = df.format(today);
                writer.write(playername + ": [" + reportDate + "] built " + r.getType() + " at " + 
                        Math.floor(l.getX()) + "," + Math.floor(l.getY()) + "," + Math.floor(l.getZ()) + 
                        " for " + pointValue + " points\n");
                writer.close();
            } catch (Exception e) {
                plugin.warning("[Townships] Unable to save to event-log.txt");
                return;
            }
        }

        private String locationToString(Location l) {
            return l.getWorld().getName() + ":" + (int) l.getX() + ":" + (int) l.getY() + ":" + (int) l.getZ();
        }

        private int getBonusPoints(String r, String playername, String l) {
            int bonusPoints = 0;
            if (previousLocations.containsKey(playername)) {
                if (previousLocations.get(playername).containsKey(l)) {
                    if (previousLocations.get(playername).get(l).equals(r)) {
                        boolean isComeback = false;
                        if (!comebacks.containsKey(playername) || !comebacks.get(playername).contains(r)) {

                            switch (r) {
                                case "tribe":
                                case "hamlet":
                                case "village":
                                case "town":
                                case "city":
                                    Bukkit.getPlayer(playername).sendMessage(ChatColor.GRAY + "[Township] Town comeback detected! Congrats!");
                                    bonusPoints = 50;
                                    isComeback = true;
                                    break;
                                case "siegecannon":
                                    Bukkit.getPlayer(playername).sendMessage(ChatColor.GRAY + "[Township] SiegeCannon comeback detected! Congrats!");
                                    bonusPoints = 20;
                                    isComeback = true;
                                    break;
                                case "outpost":
                                    Bukkit.getPlayer(playername).sendMessage(ChatColor.GRAY + "[Township] Outpost comeback detected! Congrats!");
                                    bonusPoints = 30;
                                    isComeback = true;
                                    break;
                            }
                            if (bonusPoints > 0) {
                                if (!comebacks.containsKey(playername)) {
                                    comebacks.put(playername, new ArrayList<String>());
                                }
                                comebacks.get(playername).add(r);
                            }
                        }
                        if (!isComeback) {
                            previousLocations.get(playername).put(l, r);
                            return -1;
                        }
                    } else {
                        previousLocations.get(playername).put(l, r);
                    }
                } else {
                    previousLocations.get(playername).put(l, r);
                }
            } else {
                previousLocations.put(playername, new HashMap<String, String>());
                previousLocations.get(playername).put(l, r);
            }
            return bonusPoints;
        }
        
        private int getPoints(String regionType) {
            switch (regionType) {
                case "powerobelisk":
                case "shack":
                case "potatofarm":
                case "wheatfarm":
                case "cactusfarm":
                case "sugarcanefarm":
                case "dirtquarry":
                case "tempturret":
                case "landmine":
                    return 1;
                    
                case "councilroom":
                case "grocer":
                case "melonfarm":
                case "pumpkinfarm":
                case "house":
                case "flintworks":
                case "coalmine":
                case "cobblequarry":
                case "gravelquarry":
                case "sandquarry":
                    return 2;
                    
                case "townhall":
                case "armory":
                case "library":
                case "courthouse":
                case "tavern":
                case "waterworks":
                case "chalet":
                case "carrotfarm":
                case "arrowfactory":
                case "loggerhut":
                case "ironmine":
                case "redstonemine":
                case "cloudcollector":
                case "mossycobblequarry":
                case "defensecannon":
                    return 3;
                    
                case "cityhall":
                case "manor":
                case "oakfarm":
                case "birchfarm":
                case "sprucefarm":
                case "jungletreefarm":
                case "darkoakfarm":
                case "acaciafarm":
                case "fishinghut":
                case "goldmine":
                case "spongeworks":
                    return 4;
                    
                case "capitalbuilding":
                case "port":
                case "warehouse":
                case "mansion":
                case "bakery":
                case "jeweler":
                case "apothecary":
                case "tinkerer":
                case "codifex":
                case "smithy":
                case "mystic":
                case "bazaar":
                case "arrowturret":
                case "ammodump":
                    return 5;
                    
                case "villa":
                    return 6;
                    
                case "estate":
                    return 7;
                    
                case "inn":
                case "barracks":
                case "soulgrinder":
                case "soulgemcutter":
                case "soulchemist":
                case "soulworks":
                case "soulpress":
                case "soulforge":
                case "soulaltar":
                case "souldepot":
                case "hospital":
                case "siegecannon":
                    return 10;
                    
                case "graveyard":
                case "harvester":
                case "gemmine":
                case "philosopher":
                case "redsoulmine":
                case "cryptographer":
                case "ironworks":
                case "cabal":
                case "tradingpost":
                case "outpost":
                    return 15;
                    
                case "tribe":
                    return 25;
                case "hamlet":
                    return 50;
                case "village":
                    return 100;
                case "town":
                    return 200;
                case "city":
                    return 300;
                    
            }
            return 0;
        }
    }
}
