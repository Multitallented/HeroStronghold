package multitallented.plugins.townships.effects;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import multitallented.redcastlemedia.bukkit.townships.Townships;
import multitallented.redcastlemedia.bukkit.townships.effect.Effect;
import multitallented.redcastlemedia.bukkit.townships.events.ToCommandEffectEvent;
import multitallented.redcastlemedia.bukkit.townships.region.Region;
import multitallented.redcastlemedia.bukkit.townships.region.RegionType;
import multitallented.redcastlemedia.bukkit.townships.region.SuperRegion;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 *
 * @author Multitallented
 */
public class EffectPort extends Effect {
    private int mana;
    private int warmup;
    private int cooldown;
    private int money;
    private int damage;
    private HashMap<Player, Long> cooldowns = new HashMap<Player, Long>();
    private int stamina;
    private HashSet<ItemStack> reagents = new HashSet<ItemStack>();
    
    public EffectPort(Townships plugin) {
        super(plugin);
        //registerEvent(Type.CUSTOM_EVENT, new TeleportListener(this), Priority.Highest);
    }
    
    @Override
    public void init(Townships plugin) {
        super.init(plugin);
        TeleportListener tpListener = new TeleportListener(this, plugin);
        
        plugin.addCommand("port");
        plugin.addCommand("spawn");
        plugin.addCommand("home");

        File config = new File(plugin.getDataFolder(), "config.yml");
        if (!config.exists()) {
            plugin.warning("config.yml non-existent.");
            return;
        }
        FileConfiguration conf = new YamlConfiguration();
        try {
            conf.load(config);
            ConfigurationSection cs = conf.getConfigurationSection("port");
            if (cs == null) {
                conf.createSection("port");
                conf.set("port.mana", 0);
                conf.set("port.warmup", 0);
                conf.set("port.cooldown", 0);
                conf.set("port.money", 0);
                conf.set("port.damage", 0);
                conf.set("port.stamina", 0);
                List<String> tempSet = new ArrayList<String>();
                tempSet.add("BOAT.1");
                conf.set("port.reagents", tempSet);
                this.mana=0;
                this.warmup=0;
                this.cooldown=0;
                this.money=0;
                this.damage=0;
                this.stamina=0;
            } else {
                this.mana=cs.getInt("mana",0);
                this.warmup=cs.getInt("warmup",0);
                this.cooldown=cs.getInt("cooldown",0);
                this.money=cs.getInt("money",0);
                this.damage=cs.getInt("damage",0);
                this.stamina=cs.getInt("stamina",0);
                this.reagents=processStringList(cs.getStringList("reagents"));
            }
            conf.save(config);
        } catch (Exception e) {
            plugin.warning("Could not load settings for EffectPort.jar");
        }
        
        
    }
    
    private HashSet<ItemStack> processStringList(List<String> list) {
        HashSet<ItemStack> tempSet = new HashSet<ItemStack>();
        if (list == null) {
            return tempSet;
        }
        for (String s : list) {
            String[] parts = s.split("\\.");
            ItemStack is;
            try {
                is = new ItemStack(Material.getMaterial(parts[0]), Integer.parseInt(parts[1]));
                tempSet.add(is);
            } catch (Exception e) {
                System.out.println("[Townships] Invalid port config " + s);
            }
            
        }
        
        return tempSet;
    }
    
    
    public class TeleportListener implements Listener {
        private final EffectPort effect;
        private final Townships plugin;
        public TeleportListener(EffectPort effect, Townships plugin) {
            Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
            this.effect = effect;
            this.plugin = plugin;
        }

        @EventHandler
        public void onCommandEffectEvent(ToCommandEffectEvent event) {
            if (!event.getArgs()[0].equalsIgnoreCase("port") && !event.getArgs()[0].equalsIgnoreCase("spawn") && !event.getArgs()[0].equalsIgnoreCase("home")) {
                return;
            }
            Player player = event.getPlayer();
            Economy econ = Townships.econ;

            //Check if on cooldown
            if (cooldowns.containsKey(player) && cooldowns.get(player) + cooldown > System.currentTimeMillis()) {
                player.sendMessage(ChatColor.GRAY + "[Townships] You can't port for another " + (cooldowns.get(player) + cooldown - System.currentTimeMillis()) + "s");
                return;
            }

            //Check if has enough hp
            if (damage > 0 && player.getHealth() <= damage) {
                player.sendMessage(ChatColor.GRAY + "[Townships] You need " + damage + " health to port");
                return;
            }

            //Check if player has enough money
            if (econ != null && econ.getBalance(player) < money) {
                player.sendMessage(ChatColor.GRAY + "[Townships] You need " + money + " to port");
                return;
            }

            //Check if player has enough stamina
            if (stamina > 0 && player.getFoodLevel() < stamina) {
                player.sendMessage(ChatColor.GRAY + "[Townships] You need " + (stamina - player.getFoodLevel()) + " stamina to port");
                return;
            }

            //Check if player has reagents
            for (ItemStack is : reagents) {
                if (!player.getInventory().contains(is.getType(), is.getAmount())) {
                    player.sendMessage(ChatColor.GRAY + "[Townships] You need " + is.getAmount() + " " + is.getType().name().replace("_", " "));
                    return;
                }
            }

            int j = -1;
            Region r = null;
            Location destination = null;
            if (event.getArgs()[0].equalsIgnoreCase("port") && event.getArgs().length > 1) {
                //Check if region is a port
                try {
                    j = Integer.parseInt(event.getArgs()[1]);
                    r = plugin.getRegionManager().getRegionByID(j);
                    if (r == null) {
                        player.sendMessage(ChatColor.GRAY + "[Townships] There is no port at that number");
                        return;
                    }
                    if (!plugin.getRegionManager().getRegionType(r.getType()).getEffects().contains("port.1")) {
                        player.sendMessage(ChatColor.GRAY + "[Townships] There is no port at that number");
                        return;
                    }
                } catch (Exception e) {
                    player.sendMessage(ChatColor.GRAY + "[Townships] Port names are numbers");
                    return;
                }
            } else if (event.getArgs().length > 1) {
                String townName = event.getArgs()[1];
                SuperRegion sr = plugin.getRegionManager().getSuperRegion(townName);
                if (sr == null) {
                    player.sendMessage(ChatColor.GRAY + "[Townships] There is no town named " + townName);
                    return;
                }
                outer: for (Region region : plugin.getRegionManager().getContainedRegions(sr)) {
                    RegionType rt = plugin.getRegionManager().getRegionType(region.getType());
                    for (String effectName : rt.getEffects()) {
                        if (effectName.contains("port.")) {
                            if (!(effect.isMemberOfRegion(player, region.getLocation()) || effect.isOwnerOfRegion(player, region.getLocation()))) {
                                continue;
                            }
                            r = region;
                            break outer;
                        }
                    }
                }
                if (r == null) {
                    player.sendMessage(ChatColor.GRAY + "[Townships] There are no ports in " + townName);
                    return;
                }
            } else {
                return;
            }
            destination = r.getLocation().getBlock().getRelative(BlockFace.UP).getLocation();
            
            //Check if player is owner or member of that port
            if (!(effect.isMemberOfRegion(player, r.getLocation()) || effect.isOwnerOfRegion(player, r.getLocation()))) {
                player.sendMessage(ChatColor.GRAY + "[Townships] You aren't a member of this port");
                return;
            }
            
            //Check to see if the Townships has enough reagents
            if (!effect.hasReagents(r.getLocation())) {
                return;
            }
            
            //Run upkeep but don't need to know if upkeep occured
            effect.forceUpkeep(r.getLocation());
            
            final Player p = player;
            final Location l = destination;
            
            long delay = 1L;
            if (warmup / 50 > 0) {
                delay = warmup / 50;
            }
            player.sendMessage(ChatColor.GOLD + "[Townships] You will be teleported in " + (warmup / 1000) + "s");
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 2));
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
                public void run() {
                    if (!p.isOnline() || p.isDead()) {
                        return;
                    }
                    //TODO combat check
//                    if (h != null && h.isInCombat()) {
//                        p.sendMessage(ChatColor.RED + "[Townships] You cant use that while in combat");
//                        return;
//                    }
//                    if (mana > h.getMana()) {
//                        p.sendMessage(ChatColor.RED + "[Townships] You dont have enough mana to port");
//                        return;
//                    }
                    if (money > 0 && Townships.econ != null) {
                        if (money > Townships.econ.getBalance(p.getName())) {
                            p.sendMessage(ChatColor.RED + "[Townships] You dont have enough money to port");
                            return;
                        }
                        Townships.econ.withdrawPlayer(p.getName(), money);
                    }
//                    h.setMana(h.getMana() - mana);
                    if (damage > 0) {
                        Bukkit.getPluginManager().callEvent(new EntityDamageEvent(p, DamageCause.CUSTOM, damage));
                    }
                    if (stamina > 0) {
                        p.setFoodLevel(p.getFoodLevel() - stamina);
                    }
                    for (ItemStack is : reagents) {
                        p.getInventory().removeItem(is);
                    }
                    p.teleport(new Location(l.getWorld(), l.getX(), l.getY() + 1, l.getZ()));
                    p.sendMessage(ChatColor.GOLD + "[Townships] You have been teleported!");
                }
            }, delay);
        }
    }
    
}
