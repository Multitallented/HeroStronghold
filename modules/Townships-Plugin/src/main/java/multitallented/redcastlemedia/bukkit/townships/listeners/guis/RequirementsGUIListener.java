package multitallented.redcastlemedia.bukkit.townships.listeners.guis;

/**
 *
 * @author Autumn
 * @author Multitallented
 */

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

import multitallented.redcastlemedia.bukkit.townships.Townships;
import multitallented.redcastlemedia.bukkit.townships.Util;
import multitallented.redcastlemedia.bukkit.townships.region.RegionType;
import multitallented.redcastlemedia.bukkit.townships.region.SuperRegionType;
import multitallented.redcastlemedia.bukkit.townships.region.TOItem;

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
        
public class RequirementsGUIListener implements Listener {
    private static Townships to;
    public RequirementsGUIListener(Townships to) {
        RequirementsGUIListener.to = to;
    }
    
    public static void openRequirementsInventory(List<List<TOItem>> List,
                                                 Player player,
                                                 String title,
                                                 String back) {
        int index = 0;
        HashMap<Integer, TOItem> proxyInv = new HashMap<Integer, TOItem>();
        HashMap<Integer, List<TOItem>> cycleItems = new HashMap<Integer, List<TOItem>>();
        
        for (List<TOItem> subItems : List) {
            if (subItems.size() == 1) {
                TOItem item = subItems.get(0);
                int qty = item.getQty();
                
                ItemStack is = new ItemStack(item.getMat());
                int maxStack = is.getMaxStackSize();
                
                while (qty > maxStack) {
                    TOItem tempItem = item.clone();
                    tempItem.setQty(maxStack);
                    proxyInv.put(index, tempItem);
                    index++;
                    qty -= maxStack;
                }
                
                TOItem tempItem = item.clone();
                tempItem.setQty(qty);
                proxyInv.put(index, tempItem);
                index++;
            } else if (!subItems.isEmpty()) {
                int baseIndex = index;
                int baseIndexOffset = 0;
                
                TOItem item = subItems.get(0);
                int qty = item.getQty();

                ItemStack is = new ItemStack(item.getMat());
                int maxStack = is.getMaxStackSize();
                int orMax = 0;
                while (qty > maxStack) {
                    TOItem tempItem = item.clone();
                    tempItem.setQty(maxStack);
                    proxyInv.put(index, tempItem);
                    List<TOItem> tempListItems = new ArrayList<TOItem>();
                    tempListItems.add(tempItem);
                    cycleItems.put(baseIndex + baseIndexOffset, tempListItems);
                    index++;
                    orMax++;
                    baseIndexOffset++;
                    qty -= maxStack;
                }

                TOItem tempItem = item.clone();
                tempItem.setQty(qty);
                proxyInv.put(index, tempItem);
                List<TOItem> tempListItems = new ArrayList<TOItem>();
                tempListItems.add(tempItem);
                cycleItems.put(baseIndex + baseIndexOffset, tempListItems);
                index++;
                orMax++;
                
                int reqIndex = 1;
                for (TOItem currItem : subItems) {
                    if (currItem.equals(item)) {
                        continue;
                    }
                    baseIndexOffset = 0;
                    
                    ItemStack cis = new ItemStack(currItem.getMat());
                    int cqty = currItem.getQty();
                    int cMaxStack = cis.getMaxStackSize();
                    int cMax = 0;
                    while (cqty > cMaxStack) {
                        cMax++;
                        cqty -= cMaxStack;
                        
                        if (!cycleItems.containsKey(baseIndex + baseIndexOffset)) {
                            cycleItems.put(baseIndex + baseIndexOffset, new ArrayList<TOItem>());
                        }
                        TOItem clone = currItem.clone();
                        clone.setQty(cMaxStack);
                        
                        List<TOItem> subCycleList = cycleItems.get(baseIndex + baseIndexOffset);
                        while (subCycleList.size() < reqIndex + 1) {
                            subCycleList.add(new TOItem(Material.AIR, 0, 0));
                        }
                        subCycleList.set(reqIndex, clone);
                        
                        baseIndexOffset++;
                    }
                    cMax++;
                    if (!cycleItems.containsKey(baseIndex + baseIndexOffset)) {
                        cycleItems.put(baseIndex + baseIndexOffset, new ArrayList<TOItem>());
                    }
                    
                    List<TOItem> subCycleList = cycleItems.get(baseIndex + baseIndexOffset);
                    while (subCycleList.size() < reqIndex + 1) {
                        subCycleList.add(new TOItem(Material.AIR, 0, 0));
                    }
                    
                    TOItem clone = currItem.clone();
                    clone.setQty(cqty);
                    subCycleList.set(reqIndex, clone);
                    
                    if (cMax > orMax) {
                        index += cMax - orMax;
                        orMax = cMax;
                    }
                    reqIndex++;
                }
                for (int k = 0; k< orMax; k++) {
                    List<TOItem> subCycleList = cycleItems.get(baseIndex + k);
                    while (subCycleList.size() < reqIndex) {
                        subCycleList.add(new TOItem(Material.AIR, 0, 0));
                    }
                }
            }
        }
        
        int size = 9;
        if (index > size) {
            size = index + 9 - (index % 9);
            if (index % 9 == 0) {
                size -= 9;
            }
        }
        size += 9;
        //Inventory inv = Bukkit.createInventory(null, size, ChatColor.RED + title);
        Inventory inv = Bukkit.createInventory(new MenuHolder(Bukkit.createInventory(null, size)), size, ChatColor.RED + WordUtils.capitalize(title));
        
        GUIManager.sanitizeGUIItems(proxyInv);
        GUIManager.sanitizeCycleItems(cycleItems);
        String regionTypeName = ChatColor.stripColor(title).split(" ")[0];
        RegionType rt = to.getRegionManager().getRegionType(regionTypeName);
        if (rt != null) {
            ItemStack iconStack = new ItemStack(rt.getIcon());
            ItemMeta iconMeta = iconStack.getItemMeta();
            iconMeta.setDisplayName(rt.getName());
            List<String> lore = new ArrayList<String>();
            int diameter = (int) (Math.floor(rt.getRawRadius()) * 2 + 1);
            String sizeString = diameter + "x" + diameter;
            lore.add(ChatColor.RESET + "" + ChatColor.RED + "Size: " + sizeString);
            if (rt.getDescription() != null && !rt.getDescription().equals("")) {
                lore.addAll(Util.textWrap(ChatColor.RESET + "" + ChatColor.GOLD, rt.getDescription()));
            }
            iconMeta.setLore(lore);
            iconStack.setItemMeta(iconMeta);
            inv.setItem(0, iconStack);
        } else {
            SuperRegionType srt = to.getRegionManager().getSuperRegionType(regionTypeName);
            if (srt != null) {
                ItemStack iconStack = new ItemStack(srt.getIcon());
                ItemMeta iconMeta = iconStack.getItemMeta();
                iconMeta.setDisplayName(srt.getName());
                List<String> lore = new ArrayList<String>();
                int diameter = (int) (Math.floor(srt.getRawRadius()) * 2 + 1);
                String sizeString = diameter + "x" + diameter;
                lore.add(ChatColor.RESET + "" + ChatColor.RED + "Size: " + sizeString);
                if (srt.getDescription() != null && !srt.getDescription().equals("")) {
                    lore.addAll(Util.textWrap(ChatColor.RESET + "" + ChatColor.GOLD, srt.getDescription()));
                }
                iconMeta.setLore(lore);
                iconStack.setItemMeta(iconMeta);
                inv.setItem(0, iconStack);
            }
        }
        ItemStack backStack = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta backMeta = backStack.getItemMeta();
        backMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.GOLD + "Press to go BACK");
        List<String> lore = new ArrayList<String>();
        lore = new ArrayList<String>();
        lore.add(back);
        backMeta.setLore(lore);
        backStack.setItemMeta(backMeta);
        inv.setItem(8, backStack);
        
        for (Integer pIndex : proxyInv.keySet()) {
            TOItem nextItem = proxyInv.get(pIndex);
            ItemStack is;
            if (nextItem.isWildDamage()) {
                is = new ItemStack(nextItem.getMat(), nextItem.getQty());
                ItemMeta isMeta = is.getItemMeta();
                lore = new ArrayList<String>();
                lore.add("Any type acceptable");
                isMeta.setLore(lore);
                is.setItemMeta(isMeta);
            } else {
                is = new ItemStack(nextItem.getMat(), nextItem.getQty(), (short) nextItem.getDamage());
            }
            inv.setItem(pIndex + 9, is);
        }
        player.openInventory(inv);
        
        for (Integer cycleIndex : cycleItems.keySet()) {
            GUIManager.addCycleItems(player, inv, cycleIndex + 9, cycleItems.get(cycleIndex));
        }
    }
 
    @EventHandler
    public void click(InventoryClickEvent event) {
        if (event.isCancelled() || event.getInventory().getName() == null ||
                event.getInventory().getName().length() < 1 || !isRequirementsInventory(ChatColor.stripColor(event.getInventory().getName()).toLowerCase())) {
            return;
        }
        event.setCancelled(true);
        
        if(event.getCurrentItem()==null 
                || event.getCurrentItem().getType()==Material.AIR){
            //player.closeInventory();
            return;
        }
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        
        
        String backState = ChatColor.stripColor(event.getInventory().getItem(8).getItemMeta().getLore().get(0));
        if (event.getCurrentItem().getType() == Material.REDSTONE_BLOCK) {
            player.closeInventory();
            String[] parts = backState.split(" ");
            if (parts[0].equals("list") && parts.length > 2) {
                
                RegionType rt = to.getRegionManager().getRegionType(parts[2]);
                if (rt != null) {
                    InfoGUIListener.openInfoInventory(rt, player, parts[0] + parts[1]);
                    return;
                }
                SuperRegionType srt = to.getRegionManager().getSuperRegionType(parts[2]);
                if (srt == null) {
                    return;
                }
                InfoGUIListener.openInfoInventory(srt, player, parts[0] + parts[1]);
                return;
                
            } else if (parts.length > 2 && parts[0].equals("who")) {
                
                player.performCommand("to who " + parts[2]);
                
            } else if (parts[0].equals("Exit") && parts.length > 1) {
                
                RegionType rt = to.getRegionManager().getRegionType(parts[1]);
                if (rt != null) {
                    InfoGUIListener.openInfoInventory(rt, player, parts[0]);
                    return;
                }
                SuperRegionType srt = to.getRegionManager().getSuperRegionType(parts[1]);
                if (srt == null) {
                    return;
                }
                InfoGUIListener.openInfoInventory(srt, player, parts[0]);
                return;
            }
        }
    }

    private boolean isRequirementsInventory(String name) {
        String[] nameParts = name.split(" ");
        if (nameParts.length < 2) {
            return false;
        }
        name = nameParts[1];

        return name.equals("requirements") || name.equals("reagents") || name.equals("input") || name.equals("output");
    }
}
