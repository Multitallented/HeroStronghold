package multitallented.redcastlemedia.bukkit.townships.listeners;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import multitallented.redcastlemedia.bukkit.townships.Townships;
import multitallented.redcastlemedia.bukkit.townships.region.SuperRegion;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 *
 * @author Multitallented
 */
public class SendMessageThread implements Runnable {
    private final Map<Player, String> channels;
    private final Player player;
    private final String channel;
    private final String message;
    private final String title;
    private final Townships plugin;

    public SendMessageThread(Townships plugin, String channel, Map<Player, String> channels, String title, Player player, String message) {
        this.channels = channels;
        this.player = player;
        this.channel = channel;
        this.message = message;
        this.plugin = plugin;
        if (title == null || title.equals("")) {
            this.title = "";
        } else {
            this.title = title + ", ";
        }
    }

    @Override
    public void run() {
        int i=0;
        Set<Player> removeThese = new HashSet<Player>();
        /*for (Player p : channels.keySet()) {
            if (!p.isOnline()) {
                removeThese.add(p);
            } else if (channels.get(p).equals(channel)) {
                p.sendMessage(ChatColor.GRAY + "[" + ChatColor.GREEN + channel + ChatColor.GRAY + "]" + title + player.getDisplayName()
                        + ": " + message);
                i++;
                sent.add(p);
            }
        }*/
        SuperRegion sr = plugin.getRegionManager().getSuperRegion(channel);
        if (sr != null) {
            for (OfflinePlayer s : sr.getMembers().keySet()) {
                Player p = s.getPlayer();
                if (p != null) {
                    p.sendMessage(ChatColor.GRAY + "[" + ChatColor.GREEN + channel + ChatColor.GRAY + "]" + title + player.getDisplayName()
                            + ": " + message);
                    i++;
                }
            }
            for (OfflinePlayer s : sr.getOwners()) {
                Player p = s.getPlayer();
                if (p != null) {
                    p.sendMessage(ChatColor.GRAY + "[" + ChatColor.GREEN + channel + ChatColor.GRAY + "]" + title + player.getDisplayName()
                            + ": " + message);
                    i++;
                }
            }
        }
        for (Player p : removeThese) {
            channels.remove(p);
        }
        if (i<=1) {
            player.sendMessage(ChatColor.GOLD + "[" + channel + "] No hears you. You are alone in this channel.");
        }
    }
    
}
