package multitallented.redcastlemedia.bukkit.townships.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import multitallented.redcastlemedia.bukkit.townships.ConfigManager;
import multitallented.redcastlemedia.bukkit.townships.Townships;
import multitallented.redcastlemedia.bukkit.townships.Util;
import multitallented.redcastlemedia.bukkit.townships.effect.Effect;
import multitallented.redcastlemedia.bukkit.townships.region.Region;
import multitallented.redcastlemedia.bukkit.townships.region.RegionCondition;
import multitallented.redcastlemedia.bukkit.townships.region.RegionManager;
import multitallented.redcastlemedia.bukkit.townships.region.SuperRegion;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.projectiles.ProjectileSource;

/**
 *
 * @author Multitallented
 */
public class RegionEntityListener implements Listener {
    private final RegionManager rm;
    private final Townships plugin;
    private final HashMap<String, Long> lastDeath = new HashMap<String, Long>();
    public RegionEntityListener(Townships plugin) {
        this.plugin = plugin;
        this.rm = plugin.getRegionManager();
    }
    
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        ConfigManager cm = Townships.getConfigManager();
        if (!cm.getUsePower()) {
            return;
        }
        EntityDamageEvent ede = event.getEntity().getLastDamageCause();
        if (!(ede instanceof EntityDamageByEntityEvent)) {
            return;
        }
        EntityDamageByEntityEvent edby = (EntityDamageByEntityEvent) ede;
        Entity d = edby.getDamager();
        if (edby.getCause() == DamageCause.PROJECTILE) {
            ProjectileSource source = ((Projectile) d).getShooter();
            if (source instanceof Entity) {
                d = (Entity) source;
            }
        }
        if (!(d instanceof Player)) {
            return;
        }        
        Player dPlayer = (Player) d;
        int powerLoss = cm.getPowerPerKill();
        
        Player player = (Player) event.getEntity();

        long spawnKill = Townships.getConfigManager().getSpawnKill();
        long currentTime = new Date().getTime();
        if (powerLoss > 0 && lastDeath.containsKey(player.getName()) && 
                lastDeath.get(player.getName()) + spawnKill > currentTime) {
            System.out.println("[Townships] Repeat kill detected.");
            powerLoss = 0;
        }
        lastDeath.put(player.getName(), currentTime);

        if (cm.getUseWar() && powerLoss > 0) {
            HashSet<SuperRegion> tempSet = new HashSet<SuperRegion>();
            HashSet<SuperRegion> dTempSet = new HashSet<SuperRegion>();
            for (SuperRegion sr : rm.getSortedSuperRegions()) {
                if (sr.hasMember(player) || sr.hasOwner(player)) {
                    tempSet.add(sr);
                } else if (sr.hasMember(dPlayer) || sr.hasOwner(dPlayer)) {
                    dTempSet.add(sr);
                }
            }
            for (SuperRegion sr : tempSet) {
                for (SuperRegion srt : dTempSet) {
                    if (rm.hasWar(sr, srt)) {
                        rm.reduceRegion(sr);
                        SendMessageThread smt = new SendMessageThread(plugin, sr.getName(), plugin.getChannels(), null, player, "lost " + powerLoss + " power (" + sr.getPower() + " remaining)");
                        try {
                            smt.run();
                        } catch(Exception e) {

                        }
                        if (sr.getPower() < powerLoss && cm.getDestroyNoPower()) {
                            rm.destroySuperRegion(sr.getName(), true);
                        }
                    }
                }
            }
        } else if (powerLoss > 0) {
            Set<String> regionsToReduce = new HashSet<String>();
            for (String s : rm.getSuperRegionNames()) {
                SuperRegion sr = rm.getSuperRegion(s);
                if (sr.hasMember(player) || sr.hasOwner(player))
                    regionsToReduce.add(s);
            }
            if (!regionsToReduce.isEmpty()) {
                for (String s : regionsToReduce) {
                    SuperRegion sr = rm.getSuperRegion(s);
                    rm.reduceRegion(sr);
                    SendMessageThread smt = new SendMessageThread(plugin, sr.getName(), plugin.getChannels(), null, player, "lost " + powerLoss + " power (" + sr.getPower() + " remaining)");
                    try {
                        smt.run();
                    } catch(Exception e) {

                    }
                    if (sr.getPower() < powerLoss && cm.getDestroyNoPower()) {
                        rm.destroySuperRegion(s, true);
                    }
                }
            }
        }
    }
    
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) {
            return;
        }
        
        if (event.getEntity() instanceof ItemFrame) {
            List<RegionCondition> conditions = new ArrayList<RegionCondition>();
            conditions.add(new RegionCondition("deny_block_break", true, 0));
            conditions.add(new RegionCondition("deny_block_break_no_reagent", false, 0));
            Player player = null;
            if (event.getDamager() instanceof Player) {
                player = (Player) event.getDamager();
            }
            if (rm.shouldTakeAction(event.getEntity().getLocation(), player, conditions)) {
                if (player != null) {
                    player.sendMessage("[Townships] This region is protected");
                }
                event.setCancelled(true);
                return;
            }
        }

        if (event.getEntity() instanceof Animals) {
            List<RegionCondition> conditions = new ArrayList<RegionCondition>();
            conditions.add(new RegionCondition("deny_animal_damage", true, 0));
            conditions.add(new RegionCondition("deny_animal_damage_no_reagent", false, 0));
            Player player = null;
            if (event.getDamager() instanceof Player) {
                player = (Player) event.getDamager();
            }
            if (rm.shouldTakeAction(event.getEntity().getLocation(), player, conditions)) {
                if (player != null) {
                    player.sendMessage("[Townships] This region is protected");
                }
                event.setCancelled(true);
            }
            return;
        }
        if (event.getEntity() instanceof Villager || event.getEntity() instanceof IronGolem) {
            List<RegionCondition> conditions = new ArrayList<RegionCondition>();
            conditions.add(new RegionCondition("deny_villager_damage", true, 0));
            conditions.add(new RegionCondition("deny_villager_damage_no_reagent", false, 0));
            Player player = null;
            if (event.getDamager() instanceof Player) {
                player = (Player) event.getDamager();
            }
            if (rm.shouldTakeAction(event.getEntity().getLocation(), player, conditions)) {
                if (player != null) {
                    player.sendMessage("[Townships] This region is protected");
                }
                event.setCancelled(true);
            }
            return;
        }


        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        
        if (rm.shouldTakeAction(event.getEntity().getLocation(), (Player) event.getEntity(), 0, "deny_damage", true) ||
                rm.shouldTakeAction(event.getEntity().getLocation(), (Player) event.getEntity(), 0, "deny_damage_no_reagent", false)) {
            player.sendMessage(ChatColor.RED + "[Townships] Damage is disabled here.");
            event.setCancelled(true);
            return;
        }
        Entity damagerEntity = event.getDamager();
        Player dPlayer;

        if (damagerEntity instanceof Player) {
            dPlayer = (Player) event.getDamager();
        } else if (damagerEntity instanceof Projectile) {
            Projectile proj = (Projectile) damagerEntity;
            if (proj.getShooter() != null && proj.getShooter() instanceof Player) {
                dPlayer = (Player) proj.getShooter();
            } else {
                return;
            }
        } else {
            return;
        }
        boolean isInCombat = false;
        //TODO add combat check here

        Location loc = player.getLocation();
        for (SuperRegion sr : rm.getContainingSuperRegions(loc)) {
            boolean notMember = player == null;
            if (!notMember) {
                notMember = !(sr.hasOwner(player) || sr.hasMember(player));
            }
            boolean reqs = rm.hasAllRequiredRegions(sr);
            boolean hasEffect = rm.getSuperRegionType(sr.getType()).hasEffect("deny_pvp");
            boolean hasEffect1 = rm.getSuperRegionType(sr.getType()).hasEffect("deny_pvp_no_reagent");
            boolean hasEffect2 = rm.getSuperRegionType(sr.getType()).hasEffect("deny_friendly_fire");
            boolean hasEffect3 = rm.getSuperRegionType(sr.getType()).hasEffect("deny_friendly_fire_no_reagent");
            boolean hasFortified = rm.getSuperRegionType(sr.getType()).hasEffect("fortified");
            boolean atWar = rm.isAtWar(player, dPlayer);
            boolean hasPower = sr.getPower() > 0;
            boolean hasMoney = sr.getBalance() > 0;
            boolean bothMembers = !notMember && (sr.hasMember(dPlayer) || sr.hasOwner(dPlayer));
            if (!isInCombat && (hasEffect1 || (hasEffect && reqs && hasPower && hasMoney && !atWar))) {
                dPlayer.sendMessage(ChatColor.RED + "[Townships] " + player.getDisplayName() + " is protected in this region.");
                event.setCancelled(true);
                return;
            } else if ((bothMembers && hasEffect3) || (bothMembers && hasEffect2 && reqs && hasPower && hasMoney)) {
                dPlayer.sendMessage(ChatColor.RED + "[Townships] Friendly fire is off in this region.");
                event.setCancelled(true);
                return;
            } else if (hasFortified && notMember && !bothMembers) {
                event.setDamage(event.getDamage() * 1.25);
            }
        }
        for (Region r : rm.getContainingRegions(loc)) {
            Effect effect = new Effect(plugin);
            boolean member = r.isMember(player) || r.isOwner(player);
            boolean bothMembers = member && (r.isMember(dPlayer) || r.isOwner(dPlayer));
            boolean hasEffect = effect.regionHasEffect(rm.getRegionType(r.getType()).getEffects(), "deny_pvp") > 0;
            boolean hasEffect1 = effect.regionHasEffect(rm.getRegionType(r.getType()).getEffects(), "deny_pvp_no_reagent") > 0;
            boolean hasEffect2 = effect.regionHasEffect(rm.getRegionType(r.getType()).getEffects(), "deny_friendly_fire") > 0;
            boolean hasEffect3 = effect.regionHasEffect(rm.getRegionType(r.getType()).getEffects(), "deny_friendly_fire_no_reagent") > 0;
            boolean hasReagents = effect.hasReagents(r.getLocation());
            
            if (!isInCombat && (hasEffect1 || (hasEffect && hasReagents))) {
                dPlayer.sendMessage(ChatColor.RED + "[Townships] " + player.getDisplayName() + " is protected in this region.");
                event.setCancelled(true);
                return;
            } else if ((bothMembers && hasEffect3) || (bothMembers && hasEffect2 && hasReagents)) {
                dPlayer.sendMessage(ChatColor.RED + "[Townships] Friendly fire is off in this region.");
                event.setCancelled(true);
                return;
            }
        }
    }
    
    @EventHandler
    public void onPaintingPlace(HangingPlaceEvent event) {
        if ((event.isCancelled() || !rm.shouldTakeAction(event.getEntity().getLocation(), event.getPlayer(), 0, "deny_block_build", true))  &&
                (event.isCancelled() || !rm.shouldTakeAction(event.getEntity().getLocation(), event.getPlayer(), 0, "deny_block_build_no_reagent", false))) {
            return;
        }

        event.setCancelled(true);
        event.getPlayer().sendMessage(ChatColor.GRAY + "[Townships] This region is protected");
    }

    @EventHandler
    public void onPaintingBreak(HangingBreakEvent event) {
        if (event.isCancelled() || !(event instanceof HangingBreakByEntityEvent))
            return;
        HangingBreakByEntityEvent pEvent = (HangingBreakByEntityEvent) event;
        if (!(pEvent.getRemover() instanceof Player))
            return;
        Player player = (Player) pEvent.getRemover();
        if ((!rm.shouldTakeAction(event.getEntity().getLocation(), player, 0, "deny_block_break", true)) && 
                (!rm.shouldTakeAction(event.getEntity().getLocation(), player, 0, "deny_block_break_no_reagent", false))) {
            return;
        }

        event.setCancelled(true);
        player.sendMessage(ChatColor.GRAY + "[Townships] This region is protected");
    }

    @EventHandler
    public void onHangingBreakEvent(HangingBreakEvent event) {
        Hanging hanging = event.getEntity();
        Player player = null;
        if (event instanceof HangingBreakByEntityEvent) {
            HangingBreakByEntityEvent entityEvent = (HangingBreakByEntityEvent) event;
            Entity removerEntity = entityEvent.getRemover();

            if (removerEntity instanceof Projectile) {
                Projectile projectile = (Projectile) removerEntity;
                ProjectileSource remover = projectile.getShooter();
                removerEntity = (remover instanceof LivingEntity ? (LivingEntity) remover : null);
            }

            if (removerEntity instanceof Player) {
                player = (Player) removerEntity;
            }
        }
        List<RegionCondition> conditions = new ArrayList<RegionCondition>();
        conditions.add(new RegionCondition("deny_block_break", true, 0));
        conditions.add(new RegionCondition("deny_block_break_no_reagent", false, 0));

        if (rm.shouldTakeAction(hanging.getLocation(), player, conditions)) {
            if (player != null) {
                    player.sendMessage("[Townships] This region is protected");
            }
            event.setCancelled(true);
            return;
        }
    }


    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityExplode(EntityExplodeEvent event) {
        /*if (event.isCancelled() || !(event.getEntity() instanceof Creeper || event.getEntity() instanceof EnderDragon
                || event.getEntity() instanceof TNTPrimed || event.getEntity() instanceof Fireball)) {
            return;
        }*/
        if (event.isCancelled() && !Townships.getConfigManager().getExplosionOverride()) {
            return;
        }
        List<RegionCondition> conditions = new ArrayList<RegionCondition>();
        conditions.add(new RegionCondition("deny_explosion", true, 5));
        conditions.add(new RegionCondition("deny_explosion_no_reagent", false, 5));
        if (event.getEntity() == null) {
            
        } else if (event.getEntity().getClass().equals(Creeper.class)) {
            conditions.add(new RegionCondition("deny_creeper_explosion", true, 5));
            conditions.add(new RegionCondition("deny_creeper_explosion_no_reagent", false, 5));
        } else if (event.getEntity().getClass().equals(TNTPrimed.class)) {
            conditions.add(new RegionCondition("deny_tnt_explosion", true, 5));
            conditions.add(new RegionCondition("deny_tnt_explosion_no_reagent", false, 5));
        } else if (event.getEntity().getClass().equals(Fireball.class)) {
            conditions.add(new RegionCondition("deny_ghast_explosion", true, 5));
            conditions.add(new RegionCondition("deny_ghast_explosion_no_reagent", false, 5));
        }
        if (rm.shouldTakeAction(event.getLocation(), null, conditions)) {
            event.setCancelled(true);
            return;
        }
        
        
        
        final Location loc = event.getLocation();
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                List<Location> tempArray = new ArrayList<Location>();
                for (Region r : rm.getContainingBuildRegions(loc, 5)) {
                    if (!Util.hasRequiredBlocks(r, rm)) {
                        tempArray.add(r.getLocation());
                    }
                }
                for (Location l : tempArray) {
                    rm.destroyRegion(l);
                    rm.removeRegion(l);
                }
            }
        }, 1L);
        
        
    }
}