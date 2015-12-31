package multitallented.plugins.townships.effects;

import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import multitallented.redcastlemedia.bukkit.townships.Townships;
import multitallented.redcastlemedia.bukkit.townships.effect.Effect;
import multitallented.redcastlemedia.bukkit.townships.events.ToTwoSecondEffectEvent;
import multitallented.redcastlemedia.bukkit.townships.events.ToUpkeepSuccessEvent;
import multitallented.redcastlemedia.bukkit.townships.region.Region;
import multitallented.redcastlemedia.bukkit.townships.region.RegionManager;
import multitallented.redcastlemedia.bukkit.townships.region.RegionType;
import multitallented.redcastlemedia.bukkit.townships.region.SuperRegion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 *
 * @author Multitallented
 */
public class EffectRepulse extends Effect {
    private final Townships plugin;
    public EffectRepulse(Townships plugin) {
        super(plugin);
        this.plugin = plugin;
        registerEvent(new IntruderListener(this));
    }
    
    @Override
    public void init(Townships plugin) {
        super.init(plugin);
    }
    
    public class IntruderListener implements Listener {
        private final EffectRepulse effect;
        private final HashMap<Location, Long> hasRepulse = new HashMap<Location, Long>();
        private final HashMap<String, HashSet<String>> poisonedPlayers = new HashMap<String, HashSet<String>>();
        public IntruderListener(EffectRepulse effect) {
            this.effect = effect;
        }

        @EventHandler
        public void onUpkeepSuccessEvent(ToUpkeepSuccessEvent event) {
            RegionManager rm = getPlugin().getRegionManager();
            Region r = rm.getRegion(event.getRegionLocation());
            if (r == null) {
                return;
            }
            RegionType rt = rm.getRegionType(r.getType());
            if (rt == null) {
                return;
            }
            boolean hasEffect = false;
            long period = 10000;
            for (String s : rt.getEffects()) {
                String[] parts = s.split("\\.");
                if (parts[0].equals("repulse")) {
                    hasEffect = true;

                    if (parts.length > 2) {
                        try {
                            period = Long.parseLong(parts[2]);
                        } catch (Exception e) {

                        }
                    }
                    break;
                }
            }
            if (!hasEffect) {
                return;
            }

            hasRepulse.put(event.getRegionLocation(), System.currentTimeMillis() + period);

            for (SuperRegion sr : rm.getContainingSuperRegions(event.getRegionLocation())) {
                if (poisonedPlayers.containsKey(sr.getName())) {
                    poisonedPlayers.remove(sr.getName());
                }
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (rm.getContainingSuperRegions(p.getLocation()).contains(sr)) {
                        p.sendMessage(ChatColor.RED + "[Townships] Poison has been activated for " + ChatColor.WHITE + sr.getName());
                    }
                }
            }
        }
        
        @EventHandler
        public void onCustomEvent(ToTwoSecondEffectEvent event) {
            RegionManager rm = plugin.getRegionManager();
            
            if (!event.getEffect()[0].equals("repulse")) {
                return;
            }
            
            int damage = 1;
            if (event.getEffect().length > 1) {
                try {
                    damage = Integer.parseInt(event.getEffect()[1]);
                } catch (Exception e) {
                    
                }
            }
            
            if (!hasRepulse.containsKey(event.getRegion().getLocation()) ||
                    hasRepulse.get(event.getRegion().getLocation()) < System.currentTimeMillis()) {
                hasRepulse.remove(event.getRegion().getLocation());
                return;
            }
            
            List<SuperRegion> containingSR = rm.getContainingSuperRegions(event.getRegion().getLocation());
            
            for (Player p : Bukkit.getOnlinePlayers()) {
                for (SuperRegion sr : rm.getContainingSuperRegions(p.getLocation())) {
                    if (!containingSR.contains(sr)) {
                        break;
                    }
                    
                    if (sr.hasMember(p) || sr.hasOwner(p)) {
                        break;
                    }

                    //check combat
//                    if (Townships.heroes != null) {
//                        Hero hero = Townships.heroes.getCharacterManager().getHero(p);
//                        if (!hero.isInCombat() &&
//                                (!poisonedPlayers.containsKey(sr.getName()) ||
//                                        !poisonedPlayers.get(sr.getName()).contains(p.getName()))) {
//                            break;
//                        } else {
//                            hero.refreshCombat();
//
//                            if (!poisonedPlayers.containsKey(sr.getName())) {
//                                poisonedPlayers.put(p.getName(), new HashSet<String>());
//                            }
//                            if (!poisonedPlayers.get(sr.getName()).contains(p.getName())) {
//                                poisonedPlayers.get(sr.getName()).add(p.getName());
//                            }
//                        }
//                    }
                    
                    p.damage(damage);
                }
            }
        }
    }
}
