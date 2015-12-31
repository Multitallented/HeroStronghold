package multitallented.plugins.townships.effects;

import java.util.HashMap;
import java.util.HashSet;
import multitallented.redcastlemedia.bukkit.townships.Townships;
import multitallented.redcastlemedia.bukkit.townships.effect.Effect;
import multitallented.redcastlemedia.bukkit.townships.events.ToPlayerEnterSRegionEvent;
import multitallented.redcastlemedia.bukkit.townships.events.ToPlayerExitSRegionEvent;
import multitallented.redcastlemedia.bukkit.townships.region.RegionManager;
import multitallented.redcastlemedia.bukkit.townships.region.SuperRegion;
import multitallented.redcastlemedia.bukkit.townships.region.SuperRegionType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 *
 * @author Multitallented
 */
public class EffectPermission extends Effect {
    private final Townships plugin;
    public EffectPermission(Townships plugin) {
        super(plugin);
        this.plugin = plugin;
        registerEvent(new IntruderListener(this));
    }
    
    @Override
    public void init(Townships plugin) {
        super.init(plugin);
    }
    
    public class IntruderListener implements Listener {
        private final EffectPermission effect;
        private HashMap<String, HashSet<String>> grantedPerms = new HashMap<String, HashSet<String>>();
        public IntruderListener(EffectPermission effect) {
            this.effect = effect;
        }
        
        @EventHandler
        public void onSRegionEnter(ToPlayerEnterSRegionEvent event) {
            Player player = event.getPlayer();
            if (Townships.perms == null) {
                return;
            }
            RegionManager rm = getPlugin().getRegionManager();
            SuperRegion sr = rm.getSuperRegion(event.getName());
            
            SuperRegionType srt = rm.getSuperRegionType(sr.getType());
            if (srt == null) {
                return;
            }
            
            String permission = hasPermission(srt);
            if (permission.isEmpty()) {
                return;
            }
            
            if (permission.startsWith("o:")) {
                if (!sr.hasOwner(event.getPlayer())) {
                    return;
                }
                
                permission = permission.replace("o:", "");
            }
            
            if ((grantedPerms.containsKey(player.getName()) && 
                    grantedPerms.get(player.getName()).contains(permission)) ||
                    Townships.perms.has(player, permission)) {
                return;
            }
            
            Townships.perms.playerAddTransient(player, permission);
            if (!grantedPerms.containsKey(player.getName())) {
                grantedPerms.put(player.getName(), new HashSet<String>());
            }
            grantedPerms.get(player.getName()).add(permission);
        }
        
        @EventHandler
        public void onSRegionExit(ToPlayerExitSRegionEvent event) {
            if (Townships.perms == null) {
                return;
            }
            Player player = event.getPlayer();
            RegionManager rm = getPlugin().getRegionManager();
            SuperRegion sr = rm.getSuperRegion(event.getName());
            if (sr == null) {
                return;
            }
            SuperRegionType srt = rm.getSuperRegionType(sr.getType());
            if (srt == null) {
                return;
            }
            
            String permission = hasPermission(srt);
            if (permission.startsWith("o:")) {
                permission = permission.replace("o:", "");
            }
            if (permission.isEmpty()) {
                return;
            }
            
            if (grantedPerms.containsKey(player.getName()) && grantedPerms.get(player.getName()).contains(permission)) {
                Townships.perms.playerRemoveTransient(player, permission);
                grantedPerms.get(player.getName()).remove(permission);
            }
        }
        
        private String hasPermission(SuperRegionType srt) {
            String permission = "";
            for (String s : srt.getEffects()) {
                String[] parts = s.split("\\.");
                if (parts[0].equals("permission") && parts.length > 1) {
                    for (int i= 1; i< parts.length; i++) {
                        if (i > 1) {
                            permission += ".";
                        }
                        permission += parts[i];
                    }
                }
            }
            return permission;
        }
    }
}
