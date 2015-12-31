package multitallented.redcastlemedia.bukkit.townships.listeners.guis;

/**
 *
 * @author Phoenix_Frenzy
 * @author Multitallented
 */
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import multitallented.redcastlemedia.bukkit.townships.Townships;
import multitallented.redcastlemedia.bukkit.townships.Util;
import multitallented.redcastlemedia.bukkit.townships.region.RegionManager;
import multitallented.redcastlemedia.bukkit.townships.region.RegionType;
import multitallented.redcastlemedia.bukkit.townships.region.SuperRegionType;
import multitallented.redcastlemedia.bukkit.townships.region.TOItem;
import net.milkbowl.vault.item.Items;

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
        
public class InfoGUIListener implements Listener {
    private final RegionManager rm;
    public InfoGUIListener(RegionManager rm) {
        this.rm = rm;
    }
    
    public static void openInfoInventory(RegionType region, Player player, String back) {
        int size = 18;
        //Inventory inv = Bukkit.createInventory(null, size, ChatColor.RED + "Region Info");
        Inventory inv = Bukkit.createInventory(new MenuHolder(Bukkit.createInventory(null, size)), size, ChatColor.RED + "Region Info");
        
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
        
        ItemStack iconStack = new ItemStack(region.getIcon());
        ItemMeta iconMeta = iconStack.getItemMeta();
        iconMeta.setDisplayName(WordUtils.capitalize(region.getName()));
        List<String> lore = new ArrayList<String>();

        int diameter = (int) (Math.floor(region.getRawBuildRadius()) * 2 + 1);
        int effectDiameter = (int) (Math.floor(region.getRawRadius()) * 2 + 1);

        String sizeString = diameter + "x" + diameter + "x" + diameter;
        String rangeString = effectDiameter + "x" + effectDiameter + "x" + effectDiameter;

        lore.add(ChatColor.RESET + "" + ChatColor.RED + "Size: " + sizeString);
        if (effectDiameter != diameter) {
            lore.add(ChatColor.RESET + "" + ChatColor.RED + "Range: " + rangeString);
        }

        if (region.getDescription() != null && !region.getDescription().equals("")) {
            lore.addAll(Util.textWrap(ChatColor.RESET + "" + ChatColor.GOLD, region.getDescription()));
        }
        iconMeta.setLore(lore);
        iconStack.setItemMeta(iconMeta);
        inv.setItem(0, iconStack);
        
        ItemStack costStack = new ItemStack(Material.EMERALD);
        ItemMeta costMeta = costStack.getItemMeta();
        costMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.GREEN + "Money:");
        if (region.getMoneyRequirement() > 0) {
            lore.add(ChatColor.RESET + "" + ChatColor.RED + "Build Cost: " + formatter.format(region.getMoneyRequirement()));
        }

	    double defaultSalvage = Townships.getConfigManager().getSalvage();
	    double salvageValue = region.getSalvage() != 0 ? region.getSalvage() : defaultSalvage > 0 ? defaultSalvage * region.getMoneyRequirement() / 100 : 0;
	    if (region.getSalvage() > 0) {
		    lore.add(ChatColor.RESET + "" + ChatColor.GREEN + "Salvage Value: " + formatter.format(salvageValue));
	    } else if (region.getSalvage() < 0) {
		    lore.add(ChatColor.RESET + "" + ChatColor.RED + "Salvage Value: " + formatter.format(salvageValue));
	    }

        if (region.getMoneyOutput() > 0) {
            lore.add(ChatColor.RESET + "" + ChatColor.GREEN + "Payout: +" + formatter.format(region.getMoneyOutput()));
        } else if (region.getMoneyOutput() < 0) {
            lore.add(ChatColor.RESET + "" + ChatColor.RED + "Payout: " + formatter.format(region.getMoneyOutput()));
        }
        costMeta.setLore(lore);
        costStack.setItemMeta(costMeta);
        inv.setItem(9, costStack);
        
        ItemStack requireStack = new ItemStack(Material.IRON_PICKAXE);
        ItemMeta requireMeta = requireStack.getItemMeta();
        requireMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.GOLD + "Requirements:");
        lore = new ArrayList<String>();
        lore.add(ChatColor.GOLD + "Build a structure with these blocks.");
        if (region.getRequirements().size() > 0) {
            lore.add("Requirements");
            for (List<TOItem> items : region.getRequirements()) {
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
        
        requireMeta.setLore(lore);
        requireStack.setItemMeta(requireMeta);
        inv.setItem(10, Util.removeAttributes(requireStack));
        
        ItemStack reagentStack = new ItemStack(Material.CHEST);
        ItemMeta reagentMeta = reagentStack.getItemMeta();
        reagentMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.GOLD + "Reagents:");
        lore = new ArrayList<String>();
        lore.add(ChatColor.GOLD + "Items required to run the region.");
        reagentMeta.setLore(lore);
        reagentStack.setItemMeta(reagentMeta);
        inv.setItem(11, reagentStack);
        
        ItemStack upkeepStack = new ItemStack(Material.HOPPER);
        ItemMeta upkeepMeta = upkeepStack.getItemMeta();
        upkeepMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.GOLD + "Input:");
        lore = new ArrayList<String>();
        lore.add(ChatColor.GOLD + "Items consumed by the region.");
        upkeepMeta.setLore(lore);
        upkeepStack.setItemMeta(upkeepMeta);
        inv.setItem(12, upkeepStack);
        
        ItemStack outputStack = new ItemStack(Material.DISPENSER);
        ItemMeta outputMeta = outputStack.getItemMeta();
        outputMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.GOLD + "Output:");
        lore = new ArrayList<String>();
        lore.add(ChatColor.GOLD + "Items produced by the region.");
        outputMeta.setLore(lore);
        outputStack.setItemMeta(outputMeta);
        inv.setItem(13, outputStack);
        
        ItemStack effectsStack = new ItemStack(Material.POTION, 1, (short) 1);
        ItemMeta effectMeta = effectsStack.getItemMeta();
        effectMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.GOLD + "Effects:");
        effectMeta.setLore(region.getEffects());
        effectsStack.setItemMeta(effectMeta);
        inv.setItem(14, Util.removeAttributes(effectsStack));
        
        if (!region.getBiome().isEmpty() || region.getMinY() != -1 || region.getMaxY() != -1) {
            ItemStack biomeStack = new ItemStack(Material.GRASS);
            ItemMeta biomeMeta = biomeStack.getItemMeta();
            biomeMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.GOLD + "Available in biomes:");
            lore = new ArrayList<String>();
            lore.addAll(region.getBiome());
            if (region.getMinY() != -1) {
                lore.add("Above " + region.getMinY() + "y");
            }
            if (region.getMaxY() != -1) {
                lore.add("Below " + region.getMaxY() + "y");
            }
            biomeMeta.setLore(lore);
            biomeStack.setItemMeta(biomeMeta);
            inv.setItem(15, Util.removeAttributes(biomeStack));
        }
        
        if (!region.getSuperRegions().isEmpty()) {
            ItemStack townStack = new ItemStack(Material.WOOD_DOOR);
            ItemMeta townMeta = townStack.getItemMeta();
            townMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.GOLD + "Available for towns:");
            townMeta.setLore(region.getSuperRegions());
            townStack.setItemMeta(townMeta);
            if (!region.getBiome().isEmpty() || region.getMinY() != -1 || region.getMaxY() != -1) {
                inv.setItem(16, Util.removeAttributes(townStack));
            } else {
                inv.setItem(15, Util.removeAttributes(townStack));
            }
        }

        ItemStack createStack = new ItemStack(Material.IRON_AXE);
        ItemMeta createMeta = createStack.getItemMeta();
        createMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.GREEN + "Create this Structure");
        lore = new ArrayList<String>();
        lore.add(ChatColor.RESET + "" + ChatColor.RED + "/to create " + region.getName());
        createMeta.setLore(lore);
        createStack.setItemMeta(createMeta);
        inv.setItem(17, Util.removeAttributes(createStack));
        
        ItemStack backStack = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta backMeta = backStack.getItemMeta();
        backMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.GOLD + "Press to go BACK");
        lore = new ArrayList<String>();
        if (back == null) {
            lore.add(ChatColor.RESET + "" + ChatColor.RED + "Exit");
        } else {
            lore.add(ChatColor.RESET + "" + ChatColor.RED + back);
        }
        backMeta.setLore(lore);
        backStack.setItemMeta(backMeta);
        inv.setItem(8, backStack);
        
        player.openInventory(inv);
    }
    
    public static void openInfoInventory(SuperRegionType region, Player player, String back) {
        int size = 18;
        //Inventory inv = Bukkit.createInventory(null, size, "Region Info");
        Inventory inv = Bukkit.createInventory(new MenuHolder(Bukkit.createInventory(null, size)),
                size, ChatColor.RED + "SuperRegion Info");
        
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
        
        ItemStack iconStack = new ItemStack(region.getIcon());
        ItemMeta iconMeta = iconStack.getItemMeta();
        iconMeta.setDisplayName(WordUtils.capitalize(region.getName()));
        List<String> lore = new ArrayList<String>();
        int diameter = (int) (Math.floor(region.getRawRadius()) * 2 + 1);
        String sizeString = diameter + "x" + diameter;
        lore.add(ChatColor.RESET + "" + ChatColor.RED + "Size: " + sizeString);
        if (region.getDescription() != null && !region.getDescription().equals("")) {
            lore.addAll(Util.textWrap(ChatColor.RESET + "" + ChatColor.GOLD, region.getDescription()));
        }
        iconMeta.setLore(lore);
        iconStack.setItemMeta(iconMeta);
        inv.setItem(0, iconStack);
        
        ItemStack costStack = new ItemStack(Material.EMERALD);
        ItemMeta costMeta = costStack.getItemMeta();
        costMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.GREEN + "Money:");
        if (region.getMoneyRequirement() > 0) {
            lore.add(ChatColor.RESET + "" + ChatColor.RED + "Build Cost: " + formatter.format(region.getMoneyRequirement()));
        }
        if (region.getOutput() > 0) {
            lore.add(ChatColor.RESET + "" + ChatColor.GREEN + "Per Use: +" + formatter.format(region.getOutput()));
        } else if (region.getOutput() < 0) {
            lore.add(ChatColor.RESET + "" + ChatColor.RED + "Per Use: " + formatter.format(region.getOutput()));
        }
        costMeta.setLore(lore);
        costStack.setItemMeta(costMeta);
        inv.setItem(9, costStack);
        
        ItemStack requireStack = new ItemStack(Material.IRON_PICKAXE);
        ItemMeta requireMeta = requireStack.getItemMeta();
        requireMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.GOLD + "Requirements:");
        lore = new ArrayList<String>();
        lore.add(ChatColor.GOLD + "Build a town with these structures.");
        if (region.getRequirements().size() > 0) {
            lore.add(ChatColor.BLUE + "Requirements:");
            for (String s : region.getRequirements().keySet()) {
                lore.add(ChatColor.BLUE + " " + region.getRequirement(s) + " " + s);
            }
        }
        requireMeta.setLore(lore);
        requireStack.setItemMeta(requireMeta);
        inv.setItem(10, Util.removeAttributes(requireStack));
        
        ItemStack limitsStack = new ItemStack(Material.BEDROCK);
        ItemMeta limitsMeta = limitsStack.getItemMeta();
        limitsMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.GOLD + "Limits:");
        lore = new ArrayList<String>();
        if (region.getRequirements().size() > 0) {
            lore.add(ChatColor.BLUE + "Max number of structures:");
            for (String s : region.getRegionLimits().keySet()) {
                lore.add(ChatColor.BLUE + " " + region.getRegionLimits().get(s) + " " + s);
            }
        }
        limitsMeta.setLore(lore);
        limitsStack.setItemMeta(limitsMeta);
        inv.setItem(11, Util.removeAttributes(limitsStack));
        
        ItemStack effectsStack = new ItemStack(Material.POTION, 1, (short) 1);
        ItemMeta effectMeta = effectsStack.getItemMeta();
        effectMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.GOLD + "Effects:");
        effectMeta.setLore(region.getEffects());
        effectsStack.setItemMeta(effectMeta);
        inv.setItem(12, Util.removeAttributes(effectsStack));
        
        ItemStack backStack = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta backMeta = backStack.getItemMeta();
        backMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.GOLD + "Press to go BACK");
        lore = new ArrayList<String>();
        if (back == null) {
            lore.add(ChatColor.RESET + "" + ChatColor.RED + "Exit");
        } else {
            lore.add(ChatColor.RESET + "" + ChatColor.RED + back);
        }
        backMeta.setLore(lore);
        backStack.setItemMeta(backMeta);
        inv.setItem(8, backStack);
        
        player.openInventory(inv);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!ChatColor.stripColor(event.getInventory().getName())
                .equalsIgnoreCase("Region Info") && 
                !ChatColor.stripColor(event.getInventory().getName())
                .equalsIgnoreCase("SuperRegion Info")) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        event.setCancelled(true);
        
        if(event.getCurrentItem()==null 
                || event.getCurrentItem().getType()==Material.AIR){
            //player.closeInventory();
            return;
        }
        
        String backState = ChatColor.stripColor(event.getInventory().getItem(8).getItemMeta().getLore().get(0));
        if (event.getCurrentItem().getType() == Material.REDSTONE_BLOCK) {
            player.closeInventory();
            String[] parts = backState.split(" ");
            if (backState.startsWith("list")) {
                if (parts.length > 1 && rm.getRegionCategories().containsKey(parts[1])) {
                    player.performCommand("to list " + parts[1]);
                } else {
                    player.performCommand("to list");
                }
            } else if (parts.length > 1 && parts[0].equals("who")) {
                player.performCommand("to who " + parts[1]);
            }
            return;
        }
        String regionTypeName = "";
        regionTypeName = ChatColor.stripColor(event.getInventory().getItem(0).getItemMeta().getDisplayName()).toLowerCase();
        
        RegionType rt = rm.getRegionType(regionTypeName);
        
        if (rt != null && event.getCurrentItem().getType() == Material.IRON_PICKAXE) {
            player.closeInventory();
            RequirementsGUIListener.openRequirementsInventory(new ArrayList<List<TOItem>>(rt.getRequirements()), player, rt.getName()+ " requirements", backState + " " + regionTypeName);
            return;
        }
        if (rt != null && event.getCurrentItem().getType() == Material.IRON_AXE) {
            player.closeInventory();
            player.performCommand("to create " + rt.getName());
            return;
        }
        if (event.getCurrentItem().getType() == Material.CHEST) {
            player.closeInventory();
            RequirementsGUIListener.openRequirementsInventory(new ArrayList<List<TOItem>>(rt.getReagents()), player, rt.getName() + " reagents", backState + " " + regionTypeName);
            return;
        }
        if (event.getCurrentItem().getType() == Material.HOPPER) {
            player.closeInventory();
            RequirementsGUIListener.openRequirementsInventory(new ArrayList<List<TOItem>>(rt.getUpkeep()), player, rt.getName() + " input", backState + " " + regionTypeName);
            return;
        }
        if (event.getCurrentItem().getType() == Material.DISPENSER) {
            player.closeInventory();
            RequirementsGUIListener.openRequirementsInventory(new ArrayList<List<TOItem>>(rt.getOutput()), player, rt.getName() + " output", backState + " " + regionTypeName);
            return;
        }
        player.closeInventory();
        //player.performCommand("hs info " + ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()));
    }
}