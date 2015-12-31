package multitallented.plugins.townships.effects;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import multitallented.redcastlemedia.bukkit.townships.Townships;
import multitallented.redcastlemedia.bukkit.townships.effect.Effect;
import multitallented.redcastlemedia.bukkit.townships.events.ToTwoSecondEvent;
import multitallented.redcastlemedia.bukkit.townships.region.RegionManager;
import multitallented.redcastlemedia.bukkit.townships.region.SuperRegion;
import multitallented.redcastlemedia.bukkit.townships.region.SuperRegionType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.projectiles.ProjectileSource;

/**
 *
 * @author Multitallented
 */
public class EffectAntiCamp extends Effect {
    private final Townships plugin;
    public EffectAntiCamp(Townships plugin) {
        super(plugin);
        this.plugin = plugin;
        registerEvent(new IntruderListener(this));
    }
    
    @Override
    public void init(Townships plugin) {
        super.init(plugin);
    }
    
    public class IntruderListener implements Listener {
        private final EffectAntiCamp effect;
        private final HashMap<String, Integer> deathCounts = new HashMap<String, Integer>();
        private final HashMap<String, String> lastDamager = new HashMap<String, String>();
        private final HashMap<String, String> lastDeathTown = new HashMap<String, String>();
        private final HashMap<String, Long> lastPoison = new HashMap<String, Long>();

        public IntruderListener(EffectAntiCamp effect) {
            this.effect = effect;
        }

        @EventHandler
        public void onDamage(EntityDamageByEntityEvent event) {
            if (event.isCancelled() || !(event.getEntity() instanceof Player)) {
                return;
            }

            Entity damager = event.getDamager();
            if (event.getCause() == DamageCause.PROJECTILE) {
                ProjectileSource shooter = ((Projectile)damager).getShooter();
                if (!(shooter instanceof Entity)) {
                    return;
                }
                damager = (Entity) shooter;
            }
            if (!(damager instanceof Player)) {
                return;
            }

            Player damagee = (Player) event.getEntity();
            Player damager2 = (Player) damager;

            //record damage for when they die
            lastDamager.put(damager2.getName(), damagee.getName());
        }

        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent event) {
            String playerName = event.getPlayer().getName();

            if (lastDeathTown.containsKey(playerName)) {
                lastDeathTown.remove(playerName);
            }

            if (deathCounts.containsKey(playerName)) {
                deathCounts.remove(playerName);
            }
        }

        @EventHandler
        public void onDeath(PlayerDeathEvent event) {
            Player player = event.getEntity();
            RegionManager rm = getPlugin().getRegionManager();

            //If you die, then remove them you from lastDamager
            if (lastDamager.containsKey(player.getName())) {

                //remove the killer from deathCounts
                if (deathCounts.containsKey(lastDamager.get(player.getName()))) {
                    deathCounts.remove(lastDamager.get(player.getName()));
                }
                lastDamager.remove(player.getName());
            }

            //if the person who's dying has died more than twice, then I don't care
            if (deathCounts.containsKey(player.getName()) && deathCounts.get(player.getName()) > 2) {
                return;
            }

            //If they died outside of a super region then I don't care
            List<SuperRegion> superRegions = rm.getContainingSuperRegions(player.getLocation());
            if (superRegions.isEmpty()) {
                return;
            }

            for (SuperRegion sr : superRegions) {
                SuperRegionType srt = rm.getSuperRegionType(sr.getType());
                if (srt == null) {
                    continue;
                }

                boolean hasEffect = false;
                for (String s : srt.getEffects()) {
                    String[] parts = s.split("\\.");
                    if (parts.length > 1 && parts[0].equalsIgnoreCase("anticamp")) {
                        hasEffect = true;
                    }
                }
                if (!hasEffect) {
                    continue;
                }

                //If the person dying was a member, then increment their deathCount
                if (sr.hasOwner(player) || sr.hasMember(player)) {
                    if (deathCounts.containsKey(player.getName())) {
                        deathCounts.put(player.getName(), deathCounts.get(player.getName()) + 1);
                    } else {
                        deathCounts.put(player.getName(), 1);
                    }
                    lastDeathTown.put(player.getName(), sr.getName());
                    return;
                }
            }
        }

        @EventHandler
        public void onTwoSeconds(ToTwoSecondEvent event) {
            RegionManager rm = plugin.getRegionManager();

            //Activate poison
            List<String> removeMeDeathCounts = new ArrayList<String>();

            //Go through everyone who has died in their own town
            for (String name : deathCounts.keySet()) {

                //Skip people who haven't died enough
                if (deathCounts.get(name) < 3 || !lastDeathTown.containsKey(name)) {
                    continue;
                }

                removeMeDeathCounts.add(name);
                SuperRegion sr = rm.getSuperRegion(lastDeathTown.get(name));
                lastDeathTown.remove(name);
                if (sr == null) {
                    continue;
                }
                SuperRegionType srt = rm.getSuperRegionType(sr.getType());
                if (srt == null) {
                    return;
                }
                long period = 0;
                boolean hasEffect = false;
                for (String s : srt.getEffects()) {
                    String[] parts = s.split("\\.");
                    if (parts.length > 2 && parts[0].equalsIgnoreCase("anticamp")) {
                        try {
                            period = Integer.parseInt(parts[2]);
                        } catch (Exception e) {
                            break;
                        }
                        hasEffect = true;
                        break;
                    }
                }
                if (!hasEffect || period < 1) {
                    continue;
                }

                lastPoison.put(sr.getName(), System.currentTimeMillis() + (period * 1000));
            }
            for (String s : removeMeDeathCounts) {
                deathCounts.remove(s);
            }

            //Deal Poison Damage
            List<String> removeMePoison = new ArrayList<String>();
            for (String srName : lastPoison.keySet()) {

                Long lastPoisonTime = lastPoison.get(srName);

                if (lastPoisonTime == null || System.currentTimeMillis() > lastPoisonTime) {
                    removeMePoison.add(srName);
//                    lastPoison.remove(srName);
                    continue;
                }

                SuperRegion sr = rm.getSuperRegion(srName);
                if (sr == null) {
                    continue;
                }
                SuperRegionType srt = rm.getSuperRegionType(sr.getType());
                if (srt == null) {
                    continue;
                }

                int damage = 1;

                boolean hasEffect = false;
                for (String s : srt.getEffects()) {
                    String[] parts = s.split("\\.");
                    if (parts.length > 1 && parts[0].equalsIgnoreCase("anticamp")) {
                        try {
                            damage = Integer.parseInt(parts[1]);
                        } catch (Exception e) {
                            break;
                        }
                        hasEffect = true;
                        break;
                    }
                }
                if (!hasEffect) {
                    continue;
                }

                for (Player p : Bukkit.getOnlinePlayers()) {
                    List<SuperRegion> containingSR = rm.getContainingSuperRegions(p.getLocation());
                    for (SuperRegion r : rm.getContainingSuperRegions(p.getLocation())) {
                        if (!containingSR.contains(r)) {
                            break;
                        }

                        if (sr.hasMember(p) || sr.hasOwner(p)) {
                            break;
                        }

                        p.damage(damage);
                    }
                }
            }
            for (String s : removeMePoison) {
                lastPoison.remove(s);
            }
        }
    }
}
