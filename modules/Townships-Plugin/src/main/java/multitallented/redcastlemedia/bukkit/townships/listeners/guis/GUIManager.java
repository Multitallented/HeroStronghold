package multitallented.redcastlemedia.bukkit.townships.listeners.guis;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.HashMap;

import multitallented.redcastlemedia.bukkit.townships.Townships;
import multitallented.redcastlemedia.bukkit.townships.region.RegionType;
import multitallented.redcastlemedia.bukkit.townships.region.SuperRegionType;
import multitallented.redcastlemedia.bukkit.townships.region.TOItem;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 *
 * @author Multitallented
 */
public class GUIManager implements Listener {
    private volatile static HashMap<Player, GUI> guis = new HashMap<Player, GUI>();
    private volatile static boolean running = false;
    
    public GUIManager(Townships plugin) {
    }
    
    public static void closeAllMenus() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof MenuHolder) {
                player.closeInventory();
            }
        }
    }
    
    public synchronized static void addCycleItems(Player player, Inventory inv, int index, List<TOItem> items) {
        
        boolean startCycleThread = guis.isEmpty();
        if (guis.containsKey(player)) {
            guis.get(player).addCycleItems(index, items);
        } else {
            GUI currentGUI = new GUI(player, inv);
            currentGUI.addCycleItems(index, items);
            guis.put(player, currentGUI);
        }
        
        if (startCycleThread && !running) {
            running = true;
            ItemCycleThread ict = new ItemCycleThread();
            ict.start();
        }
    }
    
    private synchronized static HashMap<Player, GUI> getGuis() {
        return guis;
    }
    
    public synchronized static void clearCycleItems(Player player) {
        guis.remove(player);
    }
    
    private static class ItemCycleThread extends Thread {
        @Override
        public void run() {
//            try {
//                sleep(2000);
//            } catch (InterruptedException ex) {
//                return;
//            }
            while (!getGuis().isEmpty()) {

                HashMap<Player, GUI> theGuis = getGuis();
                for (GUI gui : theGuis.values()) {
                    gui.advanceItemPositions();
                }
                
                
                try {
                    sleep(2000);
                } catch (InterruptedException e) {
                    return;
                }
            }
            running = false;
        }
    }
    
    private static class GUI {
        private final Inventory inventory;
        private List<GUIItemSet> cycleItems;
        
        private class GUIItemSet {
            private final int index;
            private int position;
            private final List<TOItem> items;
            
            public GUIItemSet(int index, int position, List<TOItem> items) {
                this.index = index;
                this.position = position;
                this.items = items;
            }
            
            public void setPosition(int position) {
                this.position = position;
            }
            public int getIndex() {
                return index;
            }
            public int getPosition() {
                return position;
            }
            public List<TOItem> getItems() {
                return items;
            }
        }
        
        public synchronized void advanceItemPositions() {
            for (GUIItemSet guiItemSet : cycleItems) {
                int position = guiItemSet.getPosition();
                int pos = position;
                if (guiItemSet.getItems().size() - 2 < position) {
                    pos = 0;
                } else {
                    pos++;
                }
                TOItem nextItem = guiItemSet.getItems().get(pos);
                ItemStack is;
                if (nextItem.isWildDamage()) {
                    is = new ItemStack(nextItem.getMat(), nextItem.getQty());
                    ItemMeta isMeta = is.getItemMeta();
                    if (isMeta != null) {
                        List<String> lore = new ArrayList<String>();
                        lore.add("Any type acceptable");
                        isMeta.setLore(lore);
                        is.setItemMeta(isMeta);
                    }
                } else {
                    is = new ItemStack(nextItem.getMat(), nextItem.getQty(), (short) nextItem.getDamage());
                }
                
                inventory.setItem(guiItemSet.getIndex(), is);
                guiItemSet.setPosition(pos);
            }
        }
        
        public GUI(Player player, Inventory inv) {
            this.inventory = inv;
            this.cycleItems = new ArrayList<GUIItemSet>();
        }
        
        public void addCycleItems(int index, List<TOItem> items) {
            cycleItems.add(new GUIItemSet(index, 0, items));
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        HumanEntity he = event.getPlayer();
        if (he == null || !(he instanceof Player)) {
            return;
        }
        
        Player player = (Player) he;
        GUIManager.clearCycleItems(player);
    }
    
    public static void sanitizeCycleItems(HashMap<Integer, List<TOItem>> items) {
        for (Integer i : items.keySet()) {
            sanitizeGUIItems(items.get(i));
        }
    }

    public static void sanitizeGUIItems(HashMap<Integer, TOItem> items) {
        sanitizeGUIItems(items.values());
    }

    public static void sanitizeGUIItems(Collection<TOItem> items) {
        for (TOItem item : items) {
            Material mat = item.getMat();
            if (mat == Material.BED_BLOCK) {
                if (item.getQty() > 1) {
                    item.setQty(Math.round(item.getQty() / 2));
                }
                item.setQty(Math.round(item.getQty() / 2));
                item.setMat(Material.BED);
            } else if (mat == Material.WOODEN_DOOR) {
                if (item.getQty() > 1) {
                    item.setQty(Math.round(item.getQty() / 2));
                }
                item.setMat(Material.WOOD_DOOR);
            } else if (mat == Material.REDSTONE_WIRE) {
                item.setMat(Material.REDSTONE);
            } else if (mat == Material.IRON_DOOR_BLOCK) {
                if (item.getQty() > 1) {
                    item.setQty(Math.round(item.getQty() / 2));
                }
                item.setMat(Material.IRON_DOOR);
            } else if (mat == Material.DARK_OAK_DOOR) {
                if (item.getQty() > 1) {
                    item.setQty(Math.round(item.getQty() / 2));
                }
                item.setMat(Material.DARK_OAK_DOOR_ITEM);
            } else if (mat == Material.ACACIA_DOOR) {
                if (item.getQty() > 1) {
                    item.setQty(Math.round(item.getQty() / 2));
                }
                item.setMat(Material.ACACIA_DOOR_ITEM);
            } else if (mat == Material.SPRUCE_DOOR) {
                if (item.getQty() > 1) {
                    item.setQty(Math.round(item.getQty() / 2));
                }
                item.setMat(Material.SPRUCE_DOOR_ITEM);
            } else if (mat == Material.BIRCH_DOOR) {
                if (item.getQty() > 1) {
                    item.setQty(Math.round(item.getQty() / 2));
                }
                item.setMat(Material.BIRCH_DOOR_ITEM);
            } else if (mat == Material.JUNGLE_DOOR) {
                if (item.getQty() > 1) {
                    item.setQty(Math.round(item.getQty() / 2));
                }
                item.setMat(Material.JUNGLE_DOOR_ITEM);
            } else if (mat == Material.WALL_SIGN) {
                item.setMat(Material.SIGN);
            } else if (mat == Material.CROPS) {
                item.setMat(Material.WHEAT);
            } else if (mat == Material.CARROT) {
                item.setMat(Material.CARROT_ITEM);
            } else if (mat == Material.POTATO) {
                item.setMat(Material.POTATO_ITEM);
            } else if (mat == Material.SUGAR_CANE_BLOCK) {
                item.setMat(Material.SUGAR_CANE);
            } else if (mat == Material.FLOWER_POT) {
                item.setMat(Material.FLOWER_POT_ITEM);
            } else if (mat == Material.BURNING_FURNACE) {
              item.setMat(Material.FURNACE);
            } else if (mat == Material.REDSTONE_LAMP_ON) {
              item.setMat(Material.REDSTONE_LAMP_OFF);
            } else if (mat == Material.STATIONARY_WATER) {
              item.setMat(Material.WATER_BUCKET);
            } else if (mat == Material.STATIONARY_LAVA) {
              item.setMat(Material.LAVA_BUCKET);
            } else if (mat == Material.REDSTONE_COMPARATOR_OFF ||
                    mat == Material.REDSTONE_COMPARATOR_ON) {
                item.setMat(Material.REDSTONE_COMPARATOR);
            } else if (mat == Material.DIODE_BLOCK_OFF ||
                    mat == Material.DIODE_BLOCK_ON) {
                item.setMat(Material.DIODE);
            }
        }
    }

    public static int compareRegions(RegionType o1, RegionType o2) {
        ItemStack is1 = o1.getIcon();
        ItemStack is2 = o2.getIcon();
        int nameCompare = is1.getType().name().compareTo(is2.getType().name());
        if (nameCompare != 0) {
            return nameCompare;
        }
        int qty = is1.getAmount() - is2.getAmount();
        if (qty > 0) {
            return 1;
        } else if (qty < 0) {
            return -1;
        }

        return o1.getName().compareTo(o2.getName());
    }

    public static int compareSRegions(SuperRegionType o1, SuperRegionType o2) {
        ItemStack is1 = o1.getIcon();
        ItemStack is2 = o2.getIcon();
        int nameCompare = is1.getType().name().compareTo(is2.getType().name());
        if (nameCompare != 0) {
            return nameCompare;
        }
        int qty = is1.getAmount() - is2.getAmount();
        if (qty > 0) {
            return 1;
        } else if (qty < 0) {
            return -1;
        }

        return o1.getName().compareTo(o2.getName());
    }
}
