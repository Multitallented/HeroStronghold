package multitallented.plugins.townships.effects;

import java.util.List;
import java.util.HashMap;
import multitallented.redcastlemedia.bukkit.townships.Townships;
import multitallented.redcastlemedia.bukkit.townships.Util;
import multitallented.redcastlemedia.bukkit.townships.effect.Effect;
import multitallented.redcastlemedia.bukkit.townships.events.ToPlayerInRegionEvent;
import multitallented.redcastlemedia.bukkit.townships.events.ToPreRegionCreatedEvent;
import multitallented.redcastlemedia.bukkit.townships.events.ToRegionCreatedEvent;
import multitallented.redcastlemedia.bukkit.townships.events.ToRenameEvent;
import multitallented.redcastlemedia.bukkit.townships.region.Region;
import multitallented.redcastlemedia.bukkit.townships.region.RegionManager;
import multitallented.redcastlemedia.bukkit.townships.region.RegionType;
import multitallented.redcastlemedia.bukkit.townships.region.SuperRegion;
import multitallented.redcastlemedia.bukkit.townships.region.SuperRegionType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 *
 * @author Multitallented
 */
public class EffectRaidport extends Effect {
    
    public EffectRaidport(Townships plugin) {
        super(plugin);
        registerEvent(new TeleportListener(this));
    }
    
    @Override
    public void init(Townships plugin) {
        super.init(plugin);
    }
    
    public class TeleportListener implements Listener {
        private final EffectRaidport effect;
        private HashMap<Region, Location> raidLocations = new HashMap<Region, Location>();
        public TeleportListener(EffectRaidport effect) {
            this.effect = effect;
        }
        
        @EventHandler
        public void onCustomEvent(ToPlayerInRegionEvent event) {
            if (!event.getLocation().getBlock().getRelative(BlockFace.UP).equals(event.getPlayer().getLocation().getBlock()) &&
                    !event.getLocation().getBlock().equals(event.getPlayer().getLocation().getBlock())) {
                return;
            }
            Location l =        event.getLocation();
            RegionManager rm =  getPlugin().getRegionManager();
            Region r =          rm.getRegion(l);
            RegionType rt =     rm.getRegionType(r.getType());

            boolean hasEffect = false;
            int distance = 50;
            for (String s : rt.getEffects()) {
                String[] parts = s.split("\\.");
                if (parts[0].startsWith("raid_port")) {
                    if (parts.length > 1) {
                        try {
                            distance = Integer.parseInt(parts[1]);
                        } catch (Exception e) {
                            
                        }
                    }
                    hasEffect = true;
                }
            }
            if (!hasEffect) {
                return;
            }
            
            Block block = l.getBlock().getRelative(BlockFace.UP);
            if (!(block.getState() instanceof Sign)) {
                return;
            }

            //Check to see if the Townships has enough reagents
            if (!effect.hasReagents(l)) {
                return;
            }

            Sign sign = (Sign) block.getState();

            SuperRegion sr;
            try {
                sr = rm.getSuperRegion(sign.getLine(0));
            } catch (Exception e) {
                block.breakNaturally();
                event.getPlayer().sendMessage(ChatColor.RED + "[Townships] Raid location lost. New target required.");
                return;
            }
            if (sr == null) {
                block.breakNaturally();
                event.getPlayer().sendMessage(ChatColor.RED + "[Townships] Raid location lost. New target required.");
                return;
            }
            SuperRegionType srt = rm.getSuperRegionType(sr.getType());
            if (srt == null) {
                block.breakNaturally();
                event.getPlayer().sendMessage(ChatColor.RED + "[Townships] Raid location lost. New target required.");
                return;
            }

            if (srt.getRawRadius() + distance < l.distance(sr.getLocation())) {
                block.breakNaturally();
                event.getPlayer().sendMessage(ChatColor.RED + "[Townships] Target out of range. New target required.");
                return;
            }

            Location targetLoc;
            if (!raidLocations.containsKey(r)) {
                targetLoc = findTargetLocation(sr);

                if (targetLoc == null) {
                    event.getPlayer().sendMessage(ChatColor.RED + "[Townships] Searching for suitable teleport target...");
                    return;
                }
            } else {
                targetLoc = raidLocations.get(r);
            }

            if (!isValidTeleportTarget(targetLoc, false)) {
                if (raidLocations.containsKey(r)) {
                    raidLocations.remove(r);
                }
                block.breakNaturally();
                event.getPlayer().sendMessage(ChatColor.RED + "[Townships] Raid location blocked. New target required.");
                return;
            }

            //Run upkeep but don't need to know if upkeep occured
            effect.forceUpkeep(event);
            event.getPlayer().teleport(targetLoc);
            event.getPlayer().sendMessage(ChatColor.GOLD + "[Townships] You have been teleported!");
        }

        private Location findTargetLocation(SuperRegion sr) {
            List<Region> potentialTargets = getPlugin().getRegionManager().getContainedRegions(sr);
            int i = 0;
            do {
                Region currentRegion = potentialTargets.get((int) (Math.floor(Math.random() * potentialTargets.size() * 0.999)));
                RegionType rt = getPlugin().getRegionManager().getRegionType(currentRegion.getType());
                if (rt == null) {
                    continue;
                }
                Location regionCenter = currentRegion.getLocation();
                double regionRadius = rt.getRawRadius() + 1;

                Location teleportTarget = findSafeTeleportTarget(regionCenter, regionRadius);
                if (teleportTarget != null) {
                    return teleportTarget;
                }

                i++;
            } while (i < 4);
            return null;
        }

        private Location findSafeTeleportTarget(Location center, double radius) {
            int i = (int) Math.round(center.getX() + radius);
            int j = (int) Math.round(center.getY() + radius);
            int k = (int) Math.round(center.getZ() + radius);

            //Top
            top: {
                int y = j;
                for (int x = (int) (i - Math.floor(radius * 2)); x < i; x++) {
                    for (int z = (int) (k - Math.floor(radius * 2)); z < k; z++) {
                        Location locationCheck = center.getWorld().getBlockAt(x, y, z).getLocation();
                        if (isValidTeleportTarget(locationCheck, true)) {
                            locationCheck.setX(Math.floor(locationCheck.getX()) + 0.5);
                            locationCheck.setZ(Math.floor(locationCheck.getZ()) + 0.5);
                            locationCheck.setY(Math.floor(locationCheck.getY()) + 1);
                            return locationCheck;
                        }
                    }
                }
            }
            //Left
            left: {
                int x = (int) (i - Math.floor(radius * 2));
                for (int y = (int) (j - Math.floor(radius * 2)); y < j; y++) {
                    for (int z = (int) (k - Math.floor(radius * 2)); z < k; z++) {
                        Location locationCheck = center.getWorld().getBlockAt(x, y, z).getLocation();
                        if (isValidTeleportTarget(locationCheck, true)) {
                            locationCheck.setX(Math.floor(locationCheck.getX()) + 0.5);
                            locationCheck.setZ(Math.floor(locationCheck.getZ()) + 0.5);
                            locationCheck.setY(Math.floor(locationCheck.getY()) + 1);
                            return locationCheck;
                        }
                    }
                }
            }
            //Right
            right: {
                int x = i;
                for (int y = (int) (j - Math.floor(radius * 2)); y < j; y++) {
                    for (int z = (int) (k - Math.floor(radius * 2)); z < k; z++) {
                        Location locationCheck = center.getWorld().getBlockAt(x, y, z).getLocation();
                        if (isValidTeleportTarget(locationCheck, true)) {
                            locationCheck.setX(Math.floor(locationCheck.getX()) + 0.5);
                            locationCheck.setZ(Math.floor(locationCheck.getZ()) + 0.5);
                            locationCheck.setY(Math.floor(locationCheck.getY()) + 1);
                            return locationCheck;
                        }
                    }
                }
            }
            //Front
            front: {
                int z = k;
                for (int y = (int) (j - Math.floor(radius * 2)); y < j; y++) {
                    for (int x = (int) (i - Math.floor(radius * 2)); x < i; x++) {
                        Location locationCheck = center.getWorld().getBlockAt(x, y, z).getLocation();
                        if (isValidTeleportTarget(locationCheck, true)) {
                            locationCheck.setX(Math.floor(locationCheck.getX()) + 0.5);
                            locationCheck.setZ(Math.floor(locationCheck.getZ()) + 0.5);
                            locationCheck.setY(Math.floor(locationCheck.getY()) + 1);
                            return locationCheck;
                        }
                    }
                }
            }
            //Back
            back: {
                int z = (int) (k - Math.floor(radius * 2));
                for (int y = (int) (j - Math.floor(radius * 2)); y < j; y++) {
                    for (int x = (int) (i - Math.floor(radius * 2)); x < i; x++) {
                        Location locationCheck = center.getWorld().getBlockAt(x, y, z).getLocation();
                        if (isValidTeleportTarget(locationCheck, true)) {
                            locationCheck.setX(Math.floor(locationCheck.getX()) + 0.5);
                            locationCheck.setZ(Math.floor(locationCheck.getZ()) + 0.5);
                            locationCheck.setY(Math.floor(locationCheck.getY()) + 1);
                            return locationCheck;
                        }
                    }
                }
            }
            return null;
        }

        private boolean isValidTeleportTarget(Location target, boolean preAdjusted) {
            if (!preAdjusted) {
                return !Util.isSolidBlock(target.getBlock().getType()) &&
                   !Util.isSolidBlock(target.getBlock().getRelative(BlockFace.UP).getType()) &&
                   Util.isSolidBlock(target.getBlock().getRelative(BlockFace.DOWN).getType());
            }
            return !Util.isSolidBlock(target.getBlock().getRelative(BlockFace.UP).getType()) &&
                   !Util.isSolidBlock(target.getBlock().getRelative(BlockFace.UP).getRelative(BlockFace.UP).getType()) &&
                   Util.isSolidBlock(target.getBlock().getType());
        }

        @EventHandler
        public void onPreRegionCreated(ToPreRegionCreatedEvent event) {
            Location l = event.getLocation();
            RegionType rt = event.getRegionType();
            Player player = event.getPlayer();

            for (String s : rt.getEffects()) {
                if (s.startsWith("raid_port") || s.startsWith("charging_raid_port")) {
                    int distance = 50;
                    String[] parts = s.split("\\.");
                    if (parts.length > 1) {
                        try {
                            distance = Integer.parseInt(parts[1]);
                        } catch (Exception e) {
                            
                        }
                    }
                    Block b = l.getBlock().getRelative(BlockFace.UP);
                    if (!(b.getState() instanceof Sign)) {
                        player.sendMessage(ChatColor.RED + "[Townships] You need a sign above the chest with the name of the target super region.");
                        event.setCancelled(true);
                        return;
                    }

                    //Find target Super-region
                    Sign sign = (Sign) b.getState();
                    String srName = sign.getLine(0);
                    SuperRegion sr = getPlugin().getRegionManager().getSuperRegion(srName);
                    if (sr == null) {
                        sign.setLine(0, "invalid target");
                        sign.update();
                        player.sendMessage(ChatColor.RED + "[Townships] You must put the name of the target super region on the first line of the sign.");
                        event.setCancelled(true);
                        return;
                    }

                    SuperRegionType srt = getPlugin().getRegionManager().getSuperRegionType(sr.getType());
                    if (srt == null) {
                        sign.setLine(0, "invalid target");
                        sign.update();
                        player.sendMessage(ChatColor.RED + "[Townships] Invalid target. Contact an administrator.");
                        return;
                    }

                    if (srt.getRawRadius() + distance < l.distance(sr.getLocation())) {
                        sign.setLine(0, "invalid target");
                        sign.update();
                        player.sendMessage(ChatColor.RED + "[Townships] Target out of range. New target required.");
                        return;
                    }

                    Bukkit.broadcastMessage(ChatColor.GRAY + "[Townships] " + ChatColor.RED +
                            player.getDisplayName() + ChatColor.WHITE + " has created a " +
                            ChatColor.RED + rt.getName() + ChatColor.WHITE + " targeting " + ChatColor.RED + sr.getName());
                    return;
                }
            }
        }

        @EventHandler
        public void onRegionCreated(ToRegionCreatedEvent event) {
            Region r = event.getRegion();
            Location l = r.getLocation();
            Player player = r.getOwners().get(0).getPlayer();
            RegionType rt = getPlugin().getRegionManager().getRegionType(r.getType());
            if (rt == null) {
                return;
            }

            for (String s : rt.getEffects()) {
                if (s.startsWith("raid_port") || s.startsWith("charging_raid_port")) {
                    ItemStack raidRemote = new ItemStack(Material.STICK, 1);
                    ItemMeta raidMeta = raidRemote.getItemMeta();
                    raidMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.RED + "Controller " + r.getType() + " " + r.getID());
                    raidRemote.setItemMeta(raidMeta);

                    l.getWorld().dropItemNaturally(l, raidRemote);
                    if (player != null) {
                        player.sendMessage(ChatColor.GREEN + "[Townships] You have been given an item to control this " + r.getType());
                    }
                    return;
                }
            }
        }

        @EventHandler
        public void onRaidControllerUse(PlayerInteractEvent event) {
            if (event.isCancelled() || !event.getAction().equals(Action.RIGHT_CLICK_BLOCK) || event.getClickedBlock() == null) {
                return;
            }

            ItemStack itemInHand = event.getPlayer().getItemInHand();
            if (itemInHand.getType() != Material.STICK || !itemInHand.hasItemMeta()) {
                return;
            }
            String[] displayName = ChatColor.stripColor(itemInHand.getItemMeta().getDisplayName()).split(" ");
            if (!displayName[0].equals("Controller") || displayName.length < 3) {
                return;
            }

            Player player = event.getPlayer();
            Region r;
            RegionType rt;
            RegionManager rm = getPlugin().getRegionManager();
            try {
                r = rm.getRegionByID(Integer.parseInt(displayName[2]));
                if (r == null) {
                    return;
                }
                rt = rm.getRegionType(r.getType());
                if (rt == null) {
                    return;
                }
            } catch (Exception e) {
                return;
            }

            boolean hasEffect = false;
            for (String s : rt.getEffects()) {
                if (s.startsWith("raid_port")) {
                    hasEffect = true;
                    break;
                }
            }
            if (!hasEffect) {
                return;
            }

            Location targetLoc = event.getClickedBlock().getLocation();
            if (!isValidTeleportTarget(targetLoc, true)) {
                player.sendMessage(ChatColor.RED + "[Townships] Unsafe target location");
                return;
            }
            
            boolean validTown = false;
            Block block = r.getLocation().getBlock().getRelative(BlockFace.UP);
            if (block.getType() != Material.WALL_SIGN) {
                player.sendMessage(ChatColor.RED + "[Townships] You need a sign above the chest with the name of the target super region.");
                event.setCancelled(true);
                return;
            }
            Sign sign;
            try {
                sign = (Sign) block.getState();
            } catch (Exception e) {
                player.sendMessage(ChatColor.RED + "[Townships] You need a sign above the chest with the name of the target super region.");
                event.setCancelled(true);
                return;
            }
            for (SuperRegion sr : rm.getContainingSuperRegions(targetLoc)) {
                if (sr.getName().equals(sign.getLine(0))) {
                    validTown = true;
                    break;
                }
            }
            if (!validTown) {
                player.sendMessage(ChatColor.RED + "[Townships] Invalid town target.");
                event.setCancelled(true);
                return;
            }
            
            if (!rm.getContainingBuildRegions(targetLoc).isEmpty()) {
                player.sendMessage(ChatColor.RED + "[Townships] You can't set a location inside a region.");
                event.setCancelled(true);
                return;
            }
            
            targetLoc.setX(Math.floor(targetLoc.getX()) + 0.5);
            targetLoc.setZ(Math.floor(targetLoc.getZ()) + 0.5);
            targetLoc.setY(Math.floor(targetLoc.getY()) + 1);

            raidLocations.put(r, targetLoc);
            player.sendMessage(ChatColor.GREEN + "[Townships] " + r.getType() + " " + r.getID() + " location set");
            event.setCancelled(true);
        }

        @EventHandler
        public void onRename(ToRenameEvent event) {
            RegionManager rm = getPlugin().getRegionManager();
            for (Region r : rm.getSortedRegions()) {
                RegionType rt = rm.getRegionType(r.getType());
                if (rt == null) {
                    continue;
                }

                boolean hasEffect = false;
                for (String s : rt.getEffects()) {
                    if (s.startsWith("raid_port")) {
                        hasEffect = true;
                        break;
                    }
                }
                if (!hasEffect) {
                    continue;
                }

                Sign sign;
                Block b = r.getLocation().getBlock().getRelative(BlockFace.UP);
                try {
                    if (!(b instanceof Sign)) {
                        continue;
                    }
                    sign = (Sign) b;
                } catch (Exception e) {
                    continue;
                }
                String srName = sign.getLine(0);
                SuperRegion sr = rm.getSuperRegion(srName);
                if (sr == null) {
                    continue;
                }
                if (sr.getName().equals(event.getOldName())) {
                    sign.setLine(0, event.getNewName());
                }
            }
        }
    }
    
}
