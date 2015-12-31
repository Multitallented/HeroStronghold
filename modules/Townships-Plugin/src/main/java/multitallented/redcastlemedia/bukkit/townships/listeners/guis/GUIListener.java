package multitallented.redcastlemedia.bukkit.townships.listeners.guis;

/**
 *
 * @author Multitallented
 * @author Phoenix_Frenzy
 */
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import multitallented.redcastlemedia.bukkit.townships.Townships;
import multitallented.redcastlemedia.bukkit.townships.Util;
import multitallented.redcastlemedia.bukkit.townships.region.RegionManager;
import multitallented.redcastlemedia.bukkit.townships.region.RegionType;
import multitallented.redcastlemedia.bukkit.townships.region.SuperRegionType;
import multitallented.redcastlemedia.bukkit.townships.region.TOItem;
import net.milkbowl.vault.item.Items;


//import net.minecraft.util.org.apache.commons.lang3.text.WordUtils;
import org.apache.commons.lang.WordUtils;
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
        
public class GUIListener implements Listener {
    private static RegionManager rm;
    public GUIListener(RegionManager rm) {
        GUIListener.rm = rm;
    }
    
    public static void openCategoryInventory(Player player) {
        HashMap<String, List<String>> categories = rm.getRegionCategories();
        int size = 9;
        int actualSize = categories.keySet().size();
        boolean hasSuperRegions = !rm.getSuperRegionTypes().isEmpty();
        actualSize = hasSuperRegions ? actualSize + 1 : actualSize;
        if (actualSize > size) {
            size = actualSize + 9 - (actualSize % 9);
            if (actualSize % 9 == 0) {
                size -= 9;
            }
        }
        //Inventory inv = Bukkit.createInventory(null, size, ChatColor.RED + "Townships Categories");
        Inventory inv = Bukkit.createInventory(new MenuHolder(Bukkit.createInventory(null, size)), size, ChatColor.RED + "Townships Categories");
        
        
        int i = 0;
        for (String category : categories.keySet()) {

            //Determine if the player has permissions for any of these
            boolean hasAtLeastOne = false;
            for (String regionName : rm.getRegionCategories().get(category)) {
                if (Townships.perms.has(player, "townships.create." + regionName)) {
                    hasAtLeastOne = true;
                    break;
                }
            }

            if (!hasAtLeastOne) {
                continue;
            }

            if (category.equals("")) {
                category = "Other";
            }
            ItemStack is = new ItemStack(Material.CHEST);
            ItemMeta isMeta = is.getItemMeta();
            isMeta.setDisplayName(ChatColor.RESET + WordUtils.capitalize(category));
            is.setItemMeta(isMeta);
            inv.setItem(i, is);
            i++;
        }
        if (hasSuperRegions) {
            boolean hasAtLeastOne = false;
            for (String regionName : rm.getSuperRegionTypes()) {
                if (Townships.perms.has(player, "townships.create." + regionName)) {
                    hasAtLeastOne = true;
                    break;
                }
            }

            if (hasAtLeastOne) {
                ItemStack is = new ItemStack(Material.CHEST);
                ItemMeta isMeta = is.getItemMeta();
                isMeta.setDisplayName(ChatColor.RESET + "Towns");
                is.setItemMeta(isMeta);
                inv.setItem(i, is);
            }
        }
        
        player.openInventory(inv);
    }
    
    public static void openListInventory(List<RegionType> regions, List<SuperRegionType> superRegions, Player player, String category) {
        int size = 9;
        int actualSize = regions.size() + superRegions.size() + 1;
        if (actualSize > size) {
            size = actualSize + 9 - (actualSize % 9);
            if (actualSize % 9 == 0) {
                size -= 9;
            }
        }
        category = category.toLowerCase();
        //Inventory inv = Bukkit.createInventory(null, size, ChatColor.RED + category + " Regions");
        //Inventory inv = Bukkit.createInventory(new MenuHolder(Bukkit.createInventory(null, size)), size, ChatColor.RED + WordUtils.capitalize(category) +  " Regions");
        Inventory inv = Bukkit.createInventory(null, size, ChatColor.RED + WordUtils.capitalize(category) +  " Regions");
        
        NumberFormat formatter = NumberFormat.getCurrencyInstance();
        int i = 0;
        for (RegionType r : regions) {
            ItemStack is = new ItemStack(r.getIcon());
            ItemMeta isMeta = is.getItemMeta();
            String displayName = ChatColor.RESET + r.getName();
            List<String> lore = new ArrayList<String>();
            lore.add(ChatColor.RESET + "" + ChatColor.GRAY + "Region");
            if (r.getDescription() != null && !r.getDescription().equals("")) {
                lore.addAll(Util.textWrap(ChatColor.RESET + "" + ChatColor.GOLD, r.getDescription()));
            }
            if (r.getMoneyRequirement() > 0) {
                lore.add(ChatColor.RESET + "" + ChatColor.BLUE + "Cost: " + formatter.format(r.getMoneyRequirement()));
            }
            if (r.getRequirements().size() > 0) {
                lore.add("Requirements");
                for (List<TOItem> items : r.getRequirements()) {
                    String reagents = "";
                    for (TOItem item : items) {
                        if (!reagents.equals("")) {
                            reagents += " or ";
                        }
                        String itemName = "";
                        if (item.isWildDamage()) {
                            itemName = item.getMat().name().replace("_", " ").toLowerCase();
                        } else {
                            ItemStack ist = new ItemStack(item.getMat(), 1, (short) item.getDamage());
                            itemName = Items.itemByStack(ist).getName();
                        }
                        reagents += item.getQty() + " " + itemName;
                    }
                    lore.addAll(Util.textWrap("", reagents));
                }
            }
            isMeta.setDisplayName(displayName);
            
            //Trim lore
            boolean addEllipses = lore.size() > 20;
            if (addEllipses) {
                for (int k = lore.size(); k > 19; k--) {
                    lore.remove(k-1);
                }
                lore.add("To be continued...");
            }
            
            
            isMeta.setLore(lore);
            is.setItemMeta(isMeta);
            inv.setItem(i, is);
            i++;
        }
        for (SuperRegionType sr : superRegions) {
            ItemStack is = new ItemStack(sr.getIcon());
            ItemMeta isMeta = is.getItemMeta();
            String displayName = ChatColor.RESET + sr.getName();
            List<String> lore = new ArrayList<String>();
            lore.add(ChatColor.RESET + "" + ChatColor.GRAY + "Super Region");
            if (sr.getDescription() != null && !sr.getDescription().equals("")) {
                lore.addAll(Util.textWrap(ChatColor.GOLD + "", sr.getDescription()));
            }
            if (sr.getChildren().size() > 0) {
                lore.add(ChatColor.GREEN + "Upgrade from:");
                int lineCount = 0;
                String childString = "";
                for (String srt : sr.getChildren()) {
                    if (!childString.equals("")) {
                        childString += ", ";
                    } else {
                        childString += ChatColor.GREEN + "";
                    }
                    lineCount += srt.length();
                    if (lineCount > 50) {
                        lore.add(childString);
                        childString = new String();
                        lineCount = srt.length();
                    }
                    childString += srt;
                }
                lore.add(childString);
            }
            if (sr.getMoneyRequirement() > 0) {
                lore.add("Cost: " + formatter.format(sr.getMoneyRequirement()));
            }
            if (sr.getRequirements().size() > 0) {
                lore.add(ChatColor.BLUE + "Requirements:");
                for (String s : sr.getRequirements().keySet()) {
                    lore.add(ChatColor.BLUE + " " + sr.getRequirement(s) + " " + s);
                }
            }
            isMeta.setDisplayName(displayName);
            
            //Trim lore
            boolean addEllipses = lore.size() > 20;
            if (addEllipses) {
            	for (int k = lore.size(); k > 19; k--) {
            		lore.remove(k-1);
            	}
            	lore.add("To be continued...");
            }
            
            isMeta.setLore(lore);
            is.setItemMeta(isMeta);
            inv.setItem(i, is);
            i++;
        }
        ItemStack is = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta isMeta = is.getItemMeta();
        isMeta.setDisplayName(ChatColor.RESET + "Back to Categories");
        List<String> lore = new ArrayList<String>();
        lore.add("list " + category);
        isMeta.setLore(lore);
        is.setItemMeta(isMeta);
        inv.setItem(size - 1 , is);
        player.openInventory(inv);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.isCancelled()) {
            return;
        }
        String name = ChatColor.stripColor(event.getInventory().getName());
        boolean isCategory = name.equalsIgnoreCase("Townships Categories");
        String[] names = name.split(" ");
        if (!isCategory) {
            if (names.length != 2 || !names[1].equals("Regions")) {
                return;
            } else {
            }
        }
        Player player = (Player) event.getWhoClicked();
        event.setCancelled(true);
        
        if(event.getCurrentItem()==null 
                || event.getCurrentItem().getType()==Material.AIR
                ||!event.getCurrentItem().hasItemMeta()){
            //player.closeInventory();
            return;
        }
        if (isCategory) {
            String categoryOpen = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).toLowerCase();
            player.closeInventory();
            player.performCommand("to list " + categoryOpen);
            return;
        }
        
        if (event.getCurrentItem().hasItemMeta() && ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals("Back to Categories")) {
            player.closeInventory();
            player.performCommand("to list");
            return;
        }
        
        //player.performCommand("to info " + ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()));
        String regionName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).toLowerCase();
        RegionType rt = rm.getRegionType(regionName);
        SuperRegionType srt = null;
        if (rt == null) {
            srt = rm.getSuperRegionType(regionName);
            if (srt == null) {
                player.closeInventory();
                return;
            }
        }
        
        String backButton = ChatColor.stripColor(event.getInventory().getItem(event.getInventory().getSize() - 1).getItemMeta().getLore().get(0));
        player.closeInventory();
        if (rt != null) {
            InfoGUIListener.openInfoInventory(rt, player, backButton);
        } else {
            InfoGUIListener.openInfoInventory(srt, player, backButton);
        }
    }
}