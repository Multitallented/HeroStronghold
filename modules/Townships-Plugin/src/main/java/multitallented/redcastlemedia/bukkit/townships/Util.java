package multitallented.redcastlemedia.bukkit.townships;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import multitallented.redcastlemedia.bukkit.townships.region.Region;
import multitallented.redcastlemedia.bukkit.townships.region.RegionManager;
import multitallented.redcastlemedia.bukkit.townships.region.RegionType;
import multitallented.redcastlemedia.bukkit.townships.region.TOItem;
import net.milkbowl.vault.item.Items;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 *
 * @author multitallented
 * @author Alex_M
 */
public class Util {

    public static boolean validateFileName(String fileName) {
        return fileName.matches("^[^.\\\\/:*?\"<>|]?[^\\\\/:*?\"<>|]*")
        && getValidFileName(fileName).length()>0;
    }

    public static String getValidFileName(String fileName) throws IllegalStateException {
        String newFileName = fileName.replaceAll("^[.\\\\/:*?\"<>|]?[\\\\/:*?\"<>|]*", "");
        if(newFileName.length()==0)
            throw new IllegalStateException(
                    "File Name " + fileName + " results in a empty fileName!");
        return newFileName;
    }

    public static List<String> textWrap(String prefix, String input) {
        List<String> lore = new ArrayList<String>();
        String sendMe = new String(input);
        String[] sends = sendMe.split(" ");
        String outString = "";
        for (String s : sends) {
            if (outString.length() > 40) {
                lore.add(outString);
                outString = "";
            }
            if (!outString.equals("")) {
                outString += prefix + " ";
            } else {
                outString += prefix;
            }
            outString += s;
        }
        lore.add(outString);
        return lore;
    }
    
    public static ItemStack removeAttributes(ItemStack is) {
        
        //TODO get this working when tags work again
        /* net.minecraft.server.v1_7_R3.ItemStack nmsStack = CraftItemStack.asNMSCopy(is);
        NBTTagCompound tag;
        if (!nmsStack.hasTag()){
            tag = new NBTTagCompound();
            nmsStack.setTag(tag);
        }
        else {
            tag = nmsStack.getTag();
        }
        NBTTagList am = new NBTTagList();
        tag.set("AttributeModifiers", am);
        nmsStack.setTag(tag);
        return CraftItemStack.asBukkitCopy(nmsStack);*/
        return is;
    }
    
    public static String parseColors(String input) {
        input = input.replaceAll("@\\{AQUA\\}", ChatColor.AQUA + "");
        input = input.replaceAll("@\\{BLACK\\}", ChatColor.BLACK + "");
        input = input.replaceAll("@\\{BLUE\\}", ChatColor.BLUE + "");
        input = input.replaceAll("@\\{BOLD\\}", ChatColor.BOLD + "");
        input = input.replaceAll("@\\{DARK_AQUA\\}", ChatColor.DARK_AQUA + "");
        input = input.replaceAll("@\\{DARK_BLUE\\}", ChatColor.DARK_BLUE + "");
        input = input.replaceAll("@\\{DARK_GRAY\\}", ChatColor.DARK_GRAY + "");
        input = input.replaceAll("@\\{DARK_GREEN\\}", ChatColor.DARK_GREEN + "");
        input = input.replaceAll("@\\{DARK_PURPLE\\}", ChatColor.DARK_PURPLE + "");
        input = input.replaceAll("@\\{DARK_RED\\}", ChatColor.DARK_RED + "");
        input = input.replaceAll("@\\{GOLD\\}", ChatColor.GOLD + "");
        input = input.replaceAll("@\\{GREEN\\}", ChatColor.GREEN + "");
        input = input.replaceAll("@\\{ITALIC\\}", ChatColor.ITALIC + "");
        input = input.replaceAll("@\\{LIGHT_PURPLE\\}", ChatColor.LIGHT_PURPLE + "");
        input = input.replaceAll("@\\{MAGIC\\}", ChatColor.MAGIC + "");
        input = input.replaceAll("@\\{RED\\}", ChatColor.RED + "");
        input = input.replaceAll("@\\{RESET\\}", ChatColor.RESET + "");
        input = input.replaceAll("@\\{STRIKETHROUGH\\}", ChatColor.STRIKETHROUGH + "");
        input = input.replaceAll("@\\{UNDERLINE\\}", ChatColor.UNDERLINE + "");
        input = input.replaceAll("@\\{WHITE\\}", ChatColor.WHITE + "");
        input = input.replaceAll("@\\{YELLOW\\}", ChatColor.YELLOW + "");
        return input;
    }

    public static boolean isSolidBlock(Material type) {
        return type != Material.AIR &&
                type != Material.LEVER &&
                type != Material.WALL_SIGN &&
                type != Material.TORCH &&
                type != Material.STONE_BUTTON &&
                type != Material.WOOD_BUTTON;
    }
    
    public static boolean containsItems(List<List<TOItem>> req, Inventory inv) {
        if (inv == null) {
            return false;
        }
        
        outer: for (List<TOItem> orReqs : req) {
            for (TOItem orReq : orReqs) {

                int amount = 0;
                for (ItemStack iss : inv.getContents()) {
                    if (iss == null) {
                        continue;
                    }
                    
                    if (orReq.equivalentItem(iss, true)) {
                        if ((iss.getAmount() + amount) >= orReq.getQty()) {
                            continue outer;
                        } else {
                            amount += iss.getAmount();
                        }
                    }
                }
            }
            return false;
        }
        return true;
    }
    
    public static boolean removeItems(List<List<TOItem>> req, Inventory inv) {
        if (inv == null) {
            return false;
        }
        
        //clone the list
        List<List<TOItem>> hsItemsList = new ArrayList<List<TOItem>>();
        for (List<TOItem> hsItems : req) {
            List<TOItem> tempList = new ArrayList<TOItem>();
            for (TOItem hsItem : hsItems) {
                tempList.add(hsItem.clone());
            }
            hsItemsList.add(tempList);
        }
        
        
        List<Integer> removeItems = new ArrayList<Integer>();
        HashMap<Integer, Integer> reduceItems = new HashMap<Integer, Integer>();
        
        for (int i =0; i< inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null) {
                continue;
            }
            
            int j=0;
            boolean removeIndex = false;
            outer1: for (List<TOItem> hsItems : hsItemsList) {
                for (TOItem hsItem : hsItems) {
                    if (hsItem.equivalentItem(item, true)) {
                        
                        if (item.getAmount() > hsItem.getQty()) {
                            reduceItems.put(i, hsItem.getQty());
                            removeIndex = true;
                        } else if (item.getAmount() == hsItem.getQty()) {
                            removeItems.add(i);
                            removeIndex = true;
                        } else {
                            removeItems.add(i);
                            hsItem.setQty(hsItem.getQty() - item.getAmount());
                        }
                        break outer1;
                        
                    }
                }
                j++;
            }
            if (removeIndex) {
                hsItemsList.remove(j);
            }
        }
        
        if (!hsItemsList.isEmpty()) {
            return false;
        }
        
        for (Integer i : reduceItems.keySet()) {
            inv.getItem(i).setAmount(inv.getItem(i).getAmount() - reduceItems.get(i));
        }
        
        for (Integer i : removeItems) {
            inv.setItem(i, null);
        }
        
        return true;
    }
    
    public static List<ItemStack> addItems(List<List<TOItem>> addItems, Inventory inv) {
        List<ItemStack> remainingItems = new ArrayList<ItemStack>();
        
        outer: for (List<TOItem> tempItems : addItems) {
            double rand = Math.random();
            double prevChance = 0;
            for (TOItem item : tempItems) {
                if ((prevChance < rand) && (prevChance + item.getChance() > rand)) {
                    ItemStack is = null;
                    if (!item.isWildDamage()) {
                        is = new ItemStack(item.getMat(), 1, (short) item.getDamage());
                    } else {
                        is = new ItemStack(item.getMat(), 1);
                    }
                    if (item.getDisplayName() != null) {
                        ItemMeta im = is.getItemMeta();
                        im.setDisplayName(item.getDisplayName());
                        if (item.getLore() != null) {
                            im.setLore(item.getLore());
                        }
                        is.setItemMeta(im);
                    }
                    if (inv == null) {
                        remainingItems.add(is);
                        continue;
                    }
                    int amount = item.getQty();
                    int max = is.getMaxStackSize();
                    int damageValue = item.isWildDamage() ? 0 : item.getDamage();
                    String displayName = is.hasItemMeta() ? is.getItemMeta().getDisplayName() : null;
                    List<String> lore = is.hasItemMeta() ? is.getItemMeta().getLore() : null;
                    for (ItemStack iss : inv) {
                        if (iss == null) {
                            ItemStack isa;
                            if (amount > max) {
                                isa = new ItemStack(is.getType(), max, (short) damageValue);
                            } else {
                                isa = new ItemStack(is.getType(), amount, (short) damageValue);
                            }
                            if (displayName != null) {
                                ItemMeta ima = isa.getItemMeta();
                                ima.setDisplayName(displayName);
                                if (lore != null) {
                                    ima.setLore(lore);
                                }
                                isa.setItemMeta(ima);
                            }
                            inv.addItem(isa);
                            if (amount > max) {
                                amount -= max;
                                continue;
                            } else {
                                continue outer;
                            }
                        }
                        if (iss.getType() == is.getType() && 
                                iss.getDurability() == is.getDurability() && 
                                iss.getAmount() < iss.getMaxStackSize() &&
                                ((!iss.hasItemMeta() && !is.hasItemMeta()) ||
                                (iss.hasItemMeta() && is.hasItemMeta() &&
                                iss.getItemMeta().getDisplayName().equals(is.getItemMeta().getDisplayName())))) {
                            if (amount + iss.getAmount() > iss.getMaxStackSize()) {
                                amount = amount - (iss.getMaxStackSize() - iss.getAmount());
                                iss.setAmount(iss.getMaxStackSize());
                            } else {
                                iss.setAmount(amount + iss.getAmount());
                                continue outer;
                            }
                        }
                    }
                    
                    if (amount > 0) {
                        is.setAmount(amount);
                        remainingItems.add(is);
                    }
                    continue outer;
                    
                }
                prevChance += item.getChance();
            }
        }
        
        return remainingItems;
    }
    
    public static List<String> hasCreationRequirements(Location loc, RegionType rt, RegionManager rm) {
        if (rt.getRequirements().isEmpty()) {
            return new ArrayList<String>();
        }
        
        List<List<TOItem>> reqMap = new ArrayList<List<TOItem>>();

        for (List<TOItem> currentStack : rt.getRequirements()) {
            List<TOItem> tempMap = new ArrayList<TOItem>();
                    
            for (TOItem hsItem : currentStack) {
                tempMap.add(hsItem.clone());
            }
            reqMap.add(tempMap);
        }
        
        double lx = Math.floor(loc.getX()) + 0.4;
        double ly = Math.floor(loc.getY()) + 0.4;
        double lz = Math.floor(loc.getZ()) + 0.4;
        double buildRadius = rt.getRawBuildRadius();
        
        int x = (int) Math.round(lx - buildRadius);
        int y = (int) Math.round(ly - buildRadius);
        y = y < 0 ? 0 : y;
        int z = (int) Math.round(lz - buildRadius);
        int xMax = (int) Math.round(lx + buildRadius);
        int yMax = (int) Math.round(ly + buildRadius);
        yMax = yMax > loc.getWorld().getMaxHeight() - 1 ? loc.getWorld().getMaxHeight() - 1 : yMax;
        int zMax = (int) Math.round(lz + buildRadius);
        World world = loc.getWorld();

        for (int i = x; i < xMax; i++) {
            for (int j = y; j < yMax; j++) {
                for (int k = z; k < zMax; k++) {
                    ItemStack is = world.getBlockAt(i,j,k).getState().getData().toItemStack();
                    
                    int p = 0;
                    boolean destroyIndex = false;
                    outer1: for (List<TOItem> tempMap : reqMap) {
                        for (TOItem item : tempMap) {
                            if (item.getMat() == is.getType() && (item.isWildDamage() || item.damageMatches(is.getDurability()))) {
                                if (item.getQty() < 2) {
                                    destroyIndex = true;
                                } else {
                                    item.setQty(item.getQty() - 1);
                                }
                                break outer1;
                            }
                        }
                        p++;
                    }
                    if (destroyIndex) {
                        reqMap.remove(p);
                        
                        if (reqMap.isEmpty()) {
                            return new ArrayList<String>();
                        }
                    }
                }
            }
        }
        
        List<String> message = new ArrayList<String>();
        
        for (List<TOItem> items : reqMap) {
            String tempMessage = "- ";
            for (TOItem item : items) {
                String itemName = "";
                if (item.isWildDamage()) {
                    itemName = item.getMat().name();
                } else {
                    ItemStack ist = new ItemStack(item.getMat(), 1, (short) item.getDamage());
                    itemName = Items.itemByStack(ist).getName();
                }
                tempMessage += item.getQty() + ":" + itemName + " or ";
            }
            tempMessage = tempMessage.substring(0, tempMessage.length() - 4);
            message.add(tempMessage);
        }
        
        return message;
    }
    
    public static boolean hasRequiredBlocks(Location loc, RegionType rt, RegionManager rm) {
        if (loc.getBlock().getType() != Material.CHEST) {
            return false;
        }
        
        if (rt.getRequirements().isEmpty()) {
            return true;
        }
        
        List<List<TOItem>> reqMap = new ArrayList<List<TOItem>>();

        for (List<TOItem> currentStack : rt.getRequirements()) {
            List<TOItem> tempMap = new ArrayList<TOItem>();
                    
            for (TOItem hsItem : currentStack) {
                tempMap.add(hsItem.clone());
            }
            reqMap.add(tempMap);
        }
        double lx = Math.floor(loc.getX()) + 0.4;
        double ly = Math.floor(loc.getY()) + 0.4;
        double lz = Math.floor(loc.getZ()) + 0.4;
        double buildRadius = rt.getRawBuildRadius();
        
        int x = (int) Math.round(lx - buildRadius);
        int y = (int) Math.round(ly - buildRadius);
        y = y < 0 ? 0 : y;
        int z = (int) Math.round(lz - buildRadius);
        int xMax = (int) Math.round(lx + buildRadius);
        int yMax = (int) Math.round(ly + buildRadius);
        yMax = yMax > loc.getWorld().getMaxHeight() - 1 ? loc.getWorld().getMaxHeight() - 1 : yMax;
        int zMax = (int) Math.round(lz + buildRadius);
        World world = loc.getWorld();

        for (int i = x; i < xMax; i++) {
            for (int j = y; j < yMax; j++) {
                for (int k = z; k < zMax; k++) {
                    ItemStack is = world.getBlockAt(i,j,k).getState().getData().toItemStack();
                    
                    int p = 0;
                    boolean destroyIndex = false;
                    outer1: for (List<TOItem> tempMap : reqMap) {
                        for (TOItem item : tempMap) {
                            if (item.getMat() == is.getType() && (item.isWildDamage() || item.damageMatches(is.getDurability()))) {
                                if (item.getQty() < 2) {
                                    destroyIndex = true;
                                } else {
                                    item.setQty(item.getQty() - 1);
                                }
                                break outer1;
                            }
                        }
                        p++;
                    }
                    if (destroyIndex) {
                        reqMap.remove(p);
                        
                        if (reqMap.isEmpty()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
    
    public static boolean hasRequiredBlocks(Region region, RegionManager rm) {
        return Util.hasRequiredBlocks(region.getLocation(), rm.getRegionType(region.getType()), rm);
    }

    @Deprecated
    public static ItemStack stringToItemStack(String input) {
        try {
            String[] inputArray = input.split("\\.");
            ItemStack returnStack;
            if (inputArray.length > 2) {
                int qty = Integer.parseInt(inputArray[2]);
                int damage = Integer.parseInt(inputArray[1]);
                Material mat = Material.getMaterial(inputArray[0]);
                if (mat == null) {
                    returnStack = new ItemStack(Integer.parseInt(inputArray[0]), qty, (short) damage);
                } else {
                    returnStack = new ItemStack(Material.getMaterial(inputArray[0]), qty, (short) damage);
                }
            } else if (inputArray.length > 1) {
                int qty = Integer.parseInt(inputArray[1]);
                Material mat = Material.getMaterial(inputArray[0]);
                if (mat == null) {
                    returnStack = new ItemStack(Integer.parseInt(inputArray[0]), qty);
                } else {
                    returnStack = new ItemStack(Material.getMaterial(inputArray[0]), qty);
                }
            } else {
                Material mat = Material.getMaterial(inputArray[0]);
                if (mat == null) {
                    returnStack = new ItemStack(Integer.parseInt(inputArray[0]));
                } else {
                    returnStack = new ItemStack(Material.getMaterial(inputArray[0]));
                }
            }
            return returnStack;
        } catch (Exception e) {
            return new ItemStack(Material.STONE);
        }
    }
}
