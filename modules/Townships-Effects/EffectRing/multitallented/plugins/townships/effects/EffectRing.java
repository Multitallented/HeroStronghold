package multitallented.plugins.townships.effects;

import java.util.List;
import multitallented.redcastlemedia.bukkit.townships.Townships;
import multitallented.redcastlemedia.bukkit.townships.effect.Effect;
import multitallented.redcastlemedia.bukkit.townships.events.ToSuperRegionCreatedEvent;
import multitallented.redcastlemedia.bukkit.townships.events.ToSuperRegionDestroyedEvent;
import multitallented.redcastlemedia.bukkit.townships.region.RegionManager;
import multitallented.redcastlemedia.bukkit.townships.region.SuperRegion;
import multitallented.redcastlemedia.bukkit.townships.region.SuperRegionType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 *
 * @author Multitallented
 */
public class EffectRing extends Effect {
    private final RegionManager rm;
    private final Townships aPlugin;
    private final int Y_LEVEL = 80;
    public EffectRing(Townships plugin) {
        super(plugin);
        this.rm = plugin.getRegionManager();
        this.aPlugin = plugin;
        registerEvent(new IntruderListener());
    }
    
    @Override
    public void init(Townships plugin) {
        super.init(plugin);
    }
    
    public class IntruderListener implements Listener {
        private int x= 0;
        private int z= 0;
        
//        @EventHandler
//        public void onRingBreak(BlockBreakEvent event) {
//            if (event.isCancelled() ||
//                    event.getBlock().getType() != Material.GLOWSTONE ||
//                    Math.floor(event.getBlock().getLocation().getY()) != Y_LEVEL) {
//                return;
//            }
//            if (Townships.perms != null && Townships.perms.has(event.getPlayer(), "townships.admin")) {
//                return;
//            }
//            event.setCancelled(true);
//        }
        
//        @EventHandler
//        public void onRingBuild(BlockPlaceEvent event) {
//            if (event.isCancelled() ||
//                    event.getBlock().getType() != Material.GLOWSTONE ||
//                    Math.floor(event.getBlock().getLocation().getY()) != 150) {
//                return;
//            }
//            if (Townships.perms != null && Townships.perms.has(event.getPlayer(), "townships.admin")) {
//                return;
//            }
//            event.setCancelled(true);
//        }
        
        @EventHandler
        public void onCustomEvent(ToSuperRegionCreatedEvent event) {
            final SuperRegion sr = rm.getSuperRegion(event.getName());
            
            //Check if super-region has the effect
            SuperRegionType srt = rm.getSuperRegionType(sr.getType());
            if (srt == null) {
                return;
            }
            if (!srt.getEffects().contains("ring")) {
                return;
            }
            
            final Location l = sr.getLocation();
            int baseY = (int) l.getWorld().getHighestBlockAt(l).getY();
            baseY = baseY < 64 ? 64 : baseY;
            baseY = baseY + Y_LEVEL > l.getWorld().getMaxHeight() ? l.getWorld().getMaxHeight() - 1 : baseY + Y_LEVEL;
            final int yL = baseY;
            final int radius = (int) rm.getSuperRegionType(sr.getType()).getRawRadius();
            final World world = l.getWorld();
            x = 0;
            z = 0;
            final int threadID = aPlugin.getServer().getScheduler().scheduleSyncRepeatingTask(aPlugin,
                    new Runnable() {
                        @Override
                        public void run() {
                            
                            if (x <= radius) {
                                int xp = (int) l.getX() + x;
                                int xn = (int) l.getX() - x;
                                int asdf = (int) Math.sqrt(radius*radius - (x * x));
                                int zp = asdf + (int) l.getZ();
                                int zn = (int) l.getZ() - asdf;
                                world.getBlockAt(xp, yL, zp).setType(Material.GLOWSTONE);
                                world.getBlockAt(xn, yL, zp).setType(Material.GLOWSTONE);
                                world.getBlockAt(xp, yL, zn).setType(Material.GLOWSTONE);
                                world.getBlockAt(xn, yL, zn).setType(Material.GLOWSTONE);
                                    
                            }
                            x++;
                        }
                    }, 0, 2L);
            final int threadID1 = aPlugin.getServer().getScheduler().scheduleSyncRepeatingTask(aPlugin,
                    new Runnable() {
                        @Override
                        public void run() {
                            
                            if (z <= radius) {
                                int zp = (int) l.getZ() + z;
                                int zn = (int) l.getZ() - z;
                                int asdf = (int) Math.sqrt(radius*radius - (z * z));
                                int xp = asdf + (int) l.getX();
                                int xn = (int) l.getX() - asdf;
                                world.getBlockAt(xp, yL, zp).setType(Material.GLOWSTONE);
                                world.getBlockAt(xn, yL, zp).setType(Material.GLOWSTONE);
                                world.getBlockAt(xp, yL, zn).setType(Material.GLOWSTONE);
                                world.getBlockAt(xn, yL, zn).setType(Material.GLOWSTONE);
                                    
                            }
                            z++;
                        }
                    }, 0, 2L);
            aPlugin.getServer().getScheduler().scheduleSyncDelayedTask(aPlugin, new Runnable() {
                @Override
                public void run() {
                    aPlugin.getServer().getScheduler().cancelTask(threadID);
                    aPlugin.getServer().getScheduler().cancelTask(threadID1);
                    x =0;
                    z=0;
                }
            }, 2 * radius);
        }
        
        @EventHandler
        public void onSRDestroyedEvent(ToSuperRegionDestroyedEvent event) {
            if (event.isEvolving()) {
                return;
            }

            SuperRegion sr = event.getSuperRegion();
            removeOuterRing(sr);

            if (event.isDevolving()) {
                return;
            }

            List<Location> childLocations = sr.getChildLocations();
            if (childLocations == null || childLocations.isEmpty()) {
                return;
            }
            SuperRegionType srt = aPlugin.getRegionManager().getSuperRegionType(sr.getType());
            long delay = (long) srt.getRawRadius() * 2;
            for (Location l : childLocations) {

                final Location loc = l;
                if (srt == null || srt.getChildren() == null || srt.getChildren().isEmpty()) {
                    return;
                }

                srt = aPlugin.getRegionManager().getSuperRegionType(srt.getChildren().get(0));
                final SuperRegionType srType = srt;
                if (srt == null) {
                    return;
                }

                aPlugin.getServer().getScheduler().scheduleSyncDelayedTask(aPlugin,
                    new Runnable() {

                    @Override
                    public void run() {
                        removeRing(loc, (int) srType.getRawRadius());
                    }
                        
                    }, delay);
                delay += srt.getRawRadius() * 2;
            }
        }

        private void removeOuterRing(SuperRegion sr) {
            SuperRegionType srt = aPlugin.getRegionManager().getSuperRegionType(sr.getType());
            if (srt == null) {
                return;
            }
            final Location l = sr.getLocation();
            final int radius = (int) rm.getSuperRegionType(sr.getType()).getRawRadius();
            removeRing(l, radius);
        }

        private void removeRing(final Location l, final int radius) {
            final World world = l.getWorld();
            x = 0;
            z = 0;
            int baseY = (int) l.getWorld().getHighestBlockAt(l).getY();
            baseY = baseY < 64 ? 64 : baseY;
            baseY = baseY + Y_LEVEL > l.getWorld().getMaxHeight() ? l.getWorld().getMaxHeight() - 1 : baseY + Y_LEVEL;
            final int yL = baseY;
            final int threadID = aPlugin.getServer().getScheduler().scheduleSyncRepeatingTask(aPlugin,
                    new Runnable() {
                        @Override
                        public void run() {
                            
                            if (x <= radius) {
                                int xp = (int) l.getX() + x;
                                int xn = (int) l.getX() - x;
                                int asdf = (int) Math.sqrt(radius*radius - (x * x));
                                int zp = asdf + (int) l.getZ();
                                int zn = (int) l.getZ() - asdf;
                                world.getBlockAt(xp, yL, zp).setType(Material.GRAVEL);
                                world.getBlockAt(xn, yL, zp).setType(Material.GRAVEL);
                                world.getBlockAt(xp, yL, zn).setType(Material.GRAVEL);
                                world.getBlockAt(xn, yL, zn).setType(Material.GRAVEL);
                                    
                            }
                            x++;
                        }
                    }, 0, 2L);
            final int threadID1 = aPlugin.getServer().getScheduler().scheduleSyncRepeatingTask(aPlugin,
                    new Runnable() {
                        @Override
                        public void run() {
                            
                            if (z <= radius) {
                                int zp = (int) l.getZ() + z;
                                int zn = (int) l.getZ() - z;
                                int asdf = (int) Math.sqrt(radius*radius - (z * z));
                                int xp = asdf + (int) l.getX();
                                int xn = (int) l.getX() - asdf;
                                world.getBlockAt(xp, yL, zp).setType(Material.GRAVEL);
                                world.getBlockAt(xn, yL, zp).setType(Material.GRAVEL);
                                world.getBlockAt(xp, yL, zn).setType(Material.GRAVEL);
                                world.getBlockAt(xn, yL, zn).setType(Material.GRAVEL);
                                    
                            }
                            z++;
                        }
                    }, 0, 2L);
            aPlugin.getServer().getScheduler().scheduleSyncDelayedTask(aPlugin, new Runnable() {
                @Override
                public void run() {
                    aPlugin.getServer().getScheduler().cancelTask(threadID);
                    aPlugin.getServer().getScheduler().cancelTask(threadID1);
                    x =0;
                    z=0;
                }
            }, 2 * radius);
        }
    }
    
}
