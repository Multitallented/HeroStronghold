package multitallented.redcastlemedia.bukkit.townships.listeners.guis;



/**
 *
 * @author Autumn
 */
import java.util.ArrayList;
import java.util.List;

import multitallented.redcastlemedia.bukkit.townships.region.Region;
import multitallented.redcastlemedia.bukkit.townships.region.RegionType;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
        
public class WhoGUIListener implements Listener {
    
    public static void openWhoInventory(Region region, RegionType regionType, Player player) {
        int size = 18;
        Inventory inv = Bukkit.createInventory(null, size, ChatColor.RED + "Region Stats");
        
        ItemStack iconStack = new ItemStack(regionType.getIcon());
        ItemMeta iconMeta = iconStack.getItemMeta();
        iconMeta.setDisplayName("Region: " + region.getID());
        List<String> lore = new ArrayList<String>();
        lore.add(ChatColor.RESET + "" + ChatColor.BLUE + region.getType());
        iconMeta.setLore(lore);
        iconStack.setItemMeta(iconMeta);
        inv.setItem(0, iconStack);
        
        ItemStack costStack = new ItemStack(Material.EMERALD);
        ItemMeta costMeta = costStack.getItemMeta();
        
        costMeta.setLore(lore);
        costStack.setItemMeta(costMeta);
        inv.setItem(9, costStack);
        
        ItemStack requireStack = new ItemStack(Material.CHEST);
        inv.setItem(10, requireStack);
        
        ItemStack reagentStack = new ItemStack(Material.HOPPER);
        inv.setItem(11, reagentStack);
        
        ItemStack outputStack = new ItemStack(Material.DISPENSER);
        inv.setItem(12, outputStack);
        
        ItemStack effectsStack = new ItemStack(Material.POTION, 1, (short) 1);
        inv.setItem(13, effectsStack);
        
        ItemStack backStack = new ItemStack(Material.RECORD_4);
        inv.setItem(17, backStack);
        
        player.openInventory(inv);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!ChatColor.stripColor(event.getInventory().getName())
                .equalsIgnoreCase("Region Stats")) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        event.setCancelled(true);
        
        if(event.getCurrentItem()==null 
                || event.getCurrentItem().getType()==Material.AIR
                ||!event.getCurrentItem().hasItemMeta()){
            //player.closeInventory();
            return;
        }
        player.closeInventory();
        //player.performCommand("hs info " + ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()));
    }
}