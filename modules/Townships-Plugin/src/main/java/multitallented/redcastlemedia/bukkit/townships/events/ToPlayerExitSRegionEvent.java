package multitallented.redcastlemedia.bukkit.townships.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 *
 * @author Multitallented
 */
public class ToPlayerExitSRegionEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final String name;
    private final Player player;
    private boolean loggingOut = false;

    public ToPlayerExitSRegionEvent(String name, Player player) {
        this.name = name;
        this.player = player;
    }
    public ToPlayerExitSRegionEvent(String name, Player player, boolean loggingOut) {
        this.name = name;
        this.player = player;
        this.loggingOut = loggingOut;
    }

    public boolean getLoggingOut() {
        return loggingOut;
    }

    public String getName() {
        return name;
    }

    public Player getPlayer() {
        return player;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

}
