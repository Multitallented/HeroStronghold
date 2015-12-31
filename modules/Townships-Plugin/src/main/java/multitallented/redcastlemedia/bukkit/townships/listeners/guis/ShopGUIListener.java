package multitallented.redcastlemedia.bukkit.townships.listeners.guis;

/**
 *
 * @author Phoenix_Frenzy
 * @author Multitallented
 */
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

public class ShopGUIListener implements Listener {
    private static RegionManager rm;
    public ShopGUIListener(Townships to) {
        ShopGUIListener.rm = to.getRegionManager();
    }
    
    public static void openCategoryShop(Player player) {
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
        Inventory inv = Bukkit.createInventory(new MenuHolder(Bukkit.createInventory(null, size)), 
                size, ChatColor.RED + "Shop Categories");
        
        
        int i = 0;
        for (String category : categories.keySet()) {
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
            ItemStack is = new ItemStack(Material.CHEST);
            ItemMeta isMeta = is.getItemMeta();
            isMeta.setDisplayName(ChatColor.RESET + "Towns");
            is.setItemMeta(isMeta);
            inv.setItem(i, is);
        }
        
        player.openInventory(inv);
    }

    public static void openListShop(List<RegionType> regions, List<SuperRegionType> superRegions, Player player, String category) {
        int size = 9;
        int actualSize = regions.size() + superRegions.size() + 1;
        if (actualSize > size) {
            size = actualSize + 9 - (actualSize % 9);
            if (actualSize % 9 == 0) {
                size -= 9;
            }
        }
        
        category = category.toLowerCase();
        Inventory inv = Bukkit.createInventory(new MenuHolder(Bukkit.createInventory(null, size)), 
                size, ChatColor.RED + WordUtils.capitalize(category) +  " Shop");
        
        NumberFormat formatter = NumberFormat.getCurrencyInstance();
        int i = 0;
        for (RegionType r : regions) {
            ItemStack is = new ItemStack(r.getIcon());
            ItemMeta isMeta = is.getItemMeta();
            String displayName = ChatColor.RESET + r.getName();
            List<String> lore = new ArrayList<String>();
            lore.add(ChatColor.RESET + "" + ChatColor.GRAY + "Region");
            lore.add(ChatColor.RESET + "" + ChatColor.RED + "Unlock Cost: " + formatter.format(r.getUnlockCost()));
            if (r.getDescription() != null && !r.getDescription().equals("")) {
                lore.addAll(Util.textWrap(ChatColor.RESET + "" + ChatColor.GOLD, r.getDescription()));
            }
            if (r.getMoneyRequirement() > 0) {
                lore.add(ChatColor.RESET + "" + ChatColor.BLUE + "Build Cost: " + formatter.format(r.getMoneyRequirement()));
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
            //Trim lore
            boolean addEllipses = lore.size() > 20;
            if (addEllipses) {
            	for (int k = lore.size(); k > 19; k--) {
            		lore.remove(k-1);
            	}
            	lore.add("To be continued...");
            }
            
            isMeta.setDisplayName(displayName);
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
            lore.add(ChatColor.RESET  + "" + ChatColor.RED + "Unlock Cost: " + formatter.format(sr.getUnlockCost()));
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
                lore.add("Build Cost: " + formatter.format(sr.getMoneyRequirement()));
            }
            if (sr.getRequirements().size() > 0) {
                lore.add(ChatColor.BLUE + "Requirements:");
                for (String s : sr.getRequirements().keySet()) {
                    lore.add(ChatColor.BLUE + " " + sr.getRequirement(s) + " " + s);
                }
            }
            
            //Trim lore
            boolean addEllipses = lore.size() > 20;
            if (addEllipses) {
            	for (int k = lore.size(); k > 19; k--) {
            		lore.remove(k-1);
            	}
            	lore.add("To be continued...");
            }
            isMeta.setDisplayName(displayName);
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
    
    public static void openConfirmation(Player player, String regionName) {
        int size = 9;
        
        
        //Inventory inv = Bukkit.createInventory(null, size, ChatColor.RED + "Townships Categories");
        Inventory inv = Bukkit.createInventory(new MenuHolder(Bukkit.createInventory(null, size)), 
                size, ChatColor.RED + "Unlock Confirmation");
        
        double unlockCost = 0;
        RegionType rt = rm.getRegionType(regionName);
        SuperRegionType srt = rm.getSuperRegionType(regionName);
        if (rt != null) {
            unlockCost = rt.getUnlockCost();
            ItemStack iconStack = new ItemStack(rt.getIcon());
            ItemMeta iconMeta = iconStack.getItemMeta();
            iconMeta.setDisplayName(org.apache.commons.lang.WordUtils.capitalize(rt.getName()));
            iconStack.setItemMeta(iconMeta);
            inv.setItem(0, iconStack);
        } else if (srt != null) {
            unlockCost = srt.getUnlockCost();
            ItemStack iconStack = new ItemStack(srt.getIcon());
            ItemMeta iconMeta = iconStack.getItemMeta();
            iconMeta.setDisplayName(org.apache.commons.lang.WordUtils.capitalize(srt.getName()));
            iconStack.setItemMeta(iconMeta);
            inv.setItem(0, iconStack);
        } else {
            return;
        }
        
        NumberFormat formatter = NumberFormat.getCurrencyInstance();
        ItemStack is = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta isMeta = is.getItemMeta();
        isMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.GREEN + "Unlock for " + formatter.format(unlockCost));
        is.setItemMeta(isMeta);
        inv.setItem(3, is);
        
        is = new ItemStack(Material.SIGN);
        isMeta = is.getItemMeta();
        isMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.GREEN + "More Info");
        is.setItemMeta(isMeta);
        inv.setItem(4, is);
        
        is = new ItemStack(Material.REDSTONE_BLOCK);
        isMeta = is.getItemMeta();
        isMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.GREEN + "Cancel Purchase");
        is.setItemMeta(isMeta);
        inv.setItem(5, is);
        
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.isCancelled()) {
            return;
        }
        String name = ChatColor.stripColor(event.getInventory().getName());
        if (name.equals("Unlock Confirmation")) {
            event.setCancelled(true);
            String regionName = ChatColor.stripColor(event.getInventory().getContents()[0].getItemMeta().getDisplayName()).toLowerCase();
            Player player = (Player) event.getWhoClicked();
            if (event.getCurrentItem().getType() == Material.EMERALD_BLOCK) {
                player.closeInventory();
                player.performCommand("to unlock " + regionName);
            } else if (event.getCurrentItem().getType() == Material.SIGN) {
                player.closeInventory();
                player.performCommand("to info " + regionName);
            } else if (event.getCurrentItem().getType() == Material.REDSTONE_BLOCK) {
                player.closeInventory();
            }
            return;
        }
        
        boolean isCategory = name.equalsIgnoreCase("Shop Categories");
        String[] names = name.split(" ");
        if (!isCategory) {
            if (names.length != 2 || !names[1].equalsIgnoreCase("Shop")) {
                return;
            } else {
                names[0].toLowerCase();
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
            player.performCommand("to shop " + categoryOpen);
            return;
        }

        if (event.getCurrentItem().hasItemMeta() && 
                ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals("Back to Categories")) {
            player.closeInventory();
            player.performCommand("to shop");
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

        //String backButton = ChatColor.stripColor(event.getInventory().getItem(event.getInventory().getSize() - 1).getItemMeta().getLore().get(0));
        player.closeInventory();
        player.performCommand("to confirm " + regionName);
    }
}