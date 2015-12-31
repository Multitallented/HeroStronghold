package multitallented.plugins.townships.effects;

import multitallented.redcastlemedia.bukkit.townships.Townships;
import multitallented.redcastlemedia.bukkit.townships.effect.Effect;
import multitallented.redcastlemedia.bukkit.townships.events.ToPlayerEnterSRegionEvent;
import multitallented.redcastlemedia.bukkit.townships.events.ToPlayerExitSRegionEvent;
import multitallented.redcastlemedia.bukkit.townships.region.Region;
import multitallented.redcastlemedia.bukkit.townships.region.RegionManager;
import multitallented.redcastlemedia.bukkit.townships.region.RegionType;
import multitallented.redcastlemedia.bukkit.townships.region.SuperRegion;
import multitallented.redcastlemedia.bukkit.townships.region.SuperRegionType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;

/**
 *
 * @author Multitallented
 */
public class EffectIntruder extends Effect {
    private final Townships plugin;
    private final HashMap<String, Long> lastMessage = new HashMap<String, Long>();
    public EffectIntruder(Townships plugin) {
        super(plugin);
        this.plugin = plugin;
        registerEvent(new IntruderListener(this));
    }
    
    @Override
    public void init(Townships plugin) {
        super.init(plugin);
    }
    
    public class IntruderListener implements Listener {
        private final EffectIntruder effect;
        public IntruderListener(EffectIntruder effect) {
            this.effect = effect;
        }

        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent event) {
            if (lastMessage.containsKey(event.getPlayer().getName())) {
                lastMessage.remove(event.getPlayer().getName());
            }
        }

        @EventHandler
        public void onSRegionEnter(ToPlayerEnterSRegionEvent event) {
            RegionManager rm = getPlugin().getRegionManager();
            SuperRegion sr = rm.getSuperRegion(event.getName());
            if (sr == null) {
                return;
            }

            if (sr.hasOwner(event.getPlayer()) || sr.hasMember(event.getPlayer())) {
                return;
            }

            Region r = getIntruderRegion(sr);

            if (r == null) {
                return;
            }

            if (!hasReagents(r.getLocation())) {
                return;
            }

            if (!upkeep(r.getLocation())) {
                return;
            }

            broadcastMessageToAllTownMembers(sr, true, event.getPlayer().getDisplayName());
            //event.getPlayer().sendMessage(ChatColor.WHITE + "[Townships] You have entered " + ChatColor.RED + sr.getName());
        }
        
        @EventHandler
        public void onSRegionExit(ToPlayerExitSRegionEvent event) {
            RegionManager rm = getPlugin().getRegionManager();
            SuperRegion sr = rm.getSuperRegion(event.getName());
            if (sr == null) {
                return;
            }
            if (sr.hasOwner(event.getPlayer()) || sr.hasMember(event.getPlayer())) {
                return;
            }

            Region r = getIntruderRegion(sr);

            if (r == null) {
                return;
            }

            if (!hasReagents(r.getLocation())) {
                return;
            }

            if (!upkeep(r.getLocation())) {
                return;
            }

            broadcastMessageToAllTownMembers(sr, false, event.getPlayer().getDisplayName());
            //event.getPlayer().sendMessage(ChatColor.WHITE + "[Townships] You have exited " + ChatColor.RED + sr.getName());
        }

        private Region getIntruderRegion(SuperRegion sr) {
            for (Region r : getPlugin().getRegionManager().getContainedRegions(sr)) {
                RegionType rt = getPlugin().getRegionManager().getRegionType(r.getType());
                if (rt == null) {
                    continue;
                }
                for (String s : rt.getEffects()) {
                    if (s.startsWith("intruder")) {
                        return r;
                    }
                }
            }
            return null;
        }

        private void broadcastMessageToAllTownMembers(SuperRegion sr, boolean entering, String playername) {
            if (lastMessage.containsKey(playername)) {
                if (lastMessage.get(playername) + 60000 > System.currentTimeMillis()) {
                    return;
                }
            }
            lastMessage.put(playername, System.currentTimeMillis());

            String message = ChatColor.GRAY + "[Townships] " + ChatColor.WHITE + playername + ChatColor.GRAY + " has ";
            if (entering) {
                message += "entered " + ChatColor.WHITE + sr.getName();
            } else {
                message += "exited " + ChatColor.WHITE + sr.getName();
            }

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (sr.hasMember(p) || sr.hasOwner(p)) {
                    p.sendMessage(message);
                }
            }
        }
    }
}
