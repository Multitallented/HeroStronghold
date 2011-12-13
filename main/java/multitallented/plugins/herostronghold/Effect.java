package main.java.multitallented.plugins.herostronghold;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author Multitallented
 */
public class Effect {
    private HeroStronghold plugin;
    
    public Effect(HeroStronghold plugin) {
        this.plugin = plugin;
    }
    
    public HeroStronghold getPlugin() {
        return plugin;
    }
    
    protected void registerEvent(Type type, Listener listener, Priority priority) {
        plugin.getServer().getPluginManager().registerEvent(type, listener, priority, plugin);
    }
    
    public int regionHasEffect(ArrayList<String> effects, String name) {
        int data = 0;
        if (effects == null || effects.isEmpty())
            return 0;
        
        for (String effect : effects) {
            String[] params = effect.split("\\.");
            if (params.length > 1 && params[0].equalsIgnoreCase(name)) {
                data = Integer.parseInt(params[1]);
            }
        }
        if (data < 1)
            return 0;
        return data;
    }
    
    public boolean isOwnerOfRegion(Player p, Location l) {
        return getPlugin().getRegionManager().getRegion(l).getOwners().contains(p.getName());
    }
    
    public boolean isMemberOfRegion(Player p, Location l) {
        return getPlugin().getRegionManager().getRegion(l).getMembers().contains(p.getName());
    }
    
    public boolean hasReagents(Location l) {
        RegionManager rm = getPlugin().getRegionManager();
        RegionType rt = rm.getRegionType(rm.getRegion(l).getType());
        Map<Material, Integer> reagentMap = new EnumMap<Material, Integer>(Material.class);
        for (ItemStack is : rt.getReagents()) {
            reagentMap.put(is.getType(), is.getAmount());
        }
        BlockState bs = l.getBlock().getState();
        if (!(bs instanceof Chest)) {
            return false;
        }
        for (ItemStack is : ((Chest) bs).getInventory().getContents()) {
            if (is != null) {
                Material mat = is.getType();
                if (!mat.equals(Material.AIR) && reagentMap.containsKey(mat)) {
                    if (reagentMap.get(mat) <= is.getAmount()) {
                        reagentMap.remove(mat);
                    } else {
                        reagentMap.put(mat, reagentMap.get(mat) - is.getAmount());
                    }
                }
            }
        }
        if (reagentMap.isEmpty())
            return true;
        
        return false;
    }
    
    public void forceUpkeep(Location l) {
        RegionManager rm = getPlugin().getRegionManager();
        Region r = rm.getRegion(l);
        RegionType rt = rm.getRegionType(r.getType());
        
        //Check and remove money from the player
        System.out.println("Money: " + rt.getMoneyOutput() + ":" + (HeroStronghold.econ != null));
        if (rt.getMoneyOutput() != 0 && HeroStronghold.econ != null) {
            Economy econ = HeroStronghold.econ;
            double output = rt.getMoneyOutput();
            if (r.getOwners().isEmpty())
                return;
            String playername = r.getOwners().get(0);
            System.out.println(playername + ": " + econ.getBalance(playername));
            if (output < 0  && econ.getBalance(playername) < Math.abs(output)) {
                return;
            } else if (output < 0) {
                econ.withdrawPlayer(playername, Math.abs(output));
            } else {
                econ.depositPlayer(playername, output);
            }
        }
        System.out.println(rt.getReagents().isEmpty() + ":" + rt.getOutput().isEmpty());
        if (rt.getReagents().isEmpty() && rt.getOutput().isEmpty())
            return;
        BlockState bs = l.getBlock().getState();
        System.out.println("Chest: " + (bs instanceof Chest));
        if (!(bs instanceof Chest))
            return;
        Chest chest = (Chest) bs;
        
        
        //Remove the upkeep items from the region chest and add items from output
        Map<Material, Integer> upkeepMap = new EnumMap<Material, Integer>(Material.class);
        Map<Material, Integer> outputMap = new EnumMap<Material, Integer>(Material.class);
        for (ItemStack is : rt.getUpkeep()) {
            if (is != null)
                upkeepMap.put(is.getType(), is.getAmount());
        }
        for (ItemStack is: rt.getOutput()) {
            if (is != null)
                outputMap.put(is.getType(), is.getAmount());
        }
        ItemStack[] is = chest.getInventory().getContents();
        ItemStack[] realIS = is.clone();
        for (int i = 0 ; i<realIS.length; i++) {
            Material mat = Material.AIR;
            if (realIS[i] != null)
                mat = realIS[i].getType();
            System.out.println(mat.name() + ":" + upkeepMap.containsKey(mat) + ":" + outputMap.isEmpty());
            if (!mat.equals(Material.AIR) && upkeepMap.containsKey(mat)) {
                if (realIS[i].getAmount() >= upkeepMap.get(mat)) {
                    is[i] = new ItemStack(Material.AIR, 0);
                } else {
                    int amount = upkeepMap.get(mat) - realIS[i].getAmount();
                    upkeepMap.put(mat, amount);
                    is[i].setAmount(amount);
                }
            } else if (!mat.equals(Material.AIR) && outputMap.containsKey(mat)) {
                if (realIS[i].getAmount() + outputMap.get(mat) <= 64) {
                    is[i].setAmount(is[i].getAmount() + outputMap.get(mat));
                    outputMap.remove(mat);
                } else {
                    int excess = is[i].getAmount() + outputMap.get(mat) - 64;
                    is[i].setAmount(64);
                    outputMap.put(mat, excess);
                }
            } else if (mat.equals(Material.AIR) && !outputMap.isEmpty()) {
                for (Material currentMat : outputMap.keySet()) {
                    if (outputMap.get(currentMat) <= 64) {
                        is[i] = new ItemStack(currentMat, outputMap.get(currentMat));
                        outputMap.remove(currentMat);
                    } else {
                        is[i] = new ItemStack(currentMat, 64);
                        outputMap.put(currentMat, outputMap.get(currentMat) - 64);
                    }
                    break;
                }
            }
        }
        chest.update();
    }
    
    public boolean upkeep(Location l) {
        RegionManager rm = getPlugin().getRegionManager();
        Region r = rm.getRegion(l);
        RegionType rt = rm.getRegionType(r.getType());
        if (Math.random() > rt.getUpkeepChance())
            return false;
        
        //Check and remove money from the player
        if (rt.getMoneyOutput() != 0 && HeroStronghold.econ != null) {
            Economy econ = HeroStronghold.econ;
            double output = rt.getMoneyOutput();
            if (r.getOwners().isEmpty())
                return false;
            String playername = r.getOwners().get(0);
            if (output < 0  && econ.getBalance(playername) < Math.abs(output)) {
                return false;
            } else if (output < 0) {
                econ.withdrawPlayer(playername, Math.abs(output));
            } else {
                econ.depositPlayer(playername, output);
            }
        }
        
        if (rt.getReagents().isEmpty() && rt.getOutput().isEmpty())
            return true;
        
        BlockState bs = l.getBlock().getState();
        if (!(bs instanceof Chest))
            return false;
        Chest chest = (Chest) bs;
        
        
        //Remove the upkeep items from the region chest and add items from output
        Map<Material, Integer> upkeepMap = new EnumMap<Material, Integer>(Material.class);
        Map<Material, Integer> outputMap = new EnumMap<Material, Integer>(Material.class);
        for (ItemStack is : rt.getUpkeep()) {
            if (is != null)
                upkeepMap.put(is.getType(), is.getAmount());
        }
        for (ItemStack is: rt.getOutput()) {
            if (is != null)
                outputMap.put(is.getType(), is.getAmount());
        }
        ItemStack[] is = chest.getInventory().getContents();
        ItemStack[] realIS = is.clone();
        for (int i = 0 ; i<realIS.length; i++) {
            Material mat = Material.AIR;
            if (realIS[i] != null)
                mat = realIS[i].getType();
            if (!mat.equals(Material.AIR) && upkeepMap.containsKey(mat)) {
                if (realIS[i].getAmount() >= upkeepMap.get(mat)) {
                    is[i] = new ItemStack(Material.AIR, 0);
                } else {
                    int amount = upkeepMap.get(mat) - realIS[i].getAmount();
                    upkeepMap.put(mat, amount);
                    is[i].setAmount(amount);
                }
            } else if (!mat.equals(Material.AIR) && outputMap.containsKey(mat)) {
                if (realIS[i].getAmount() + outputMap.get(mat) <= 64) {
                    is[i].setAmount(is[i].getAmount() + outputMap.get(mat));
                    outputMap.remove(mat);
                } else {
                    int excess = is[i].getAmount() + outputMap.get(mat) - 64;
                    is[i].setAmount(64);
                    outputMap.put(mat, excess);
                }
            } else if (mat.equals(Material.AIR) && !outputMap.isEmpty()) {
                for (Material currentMat : outputMap.keySet()) {
                    if (outputMap.get(currentMat) <= 64) {
                        is[i] = new ItemStack(currentMat, outputMap.get(currentMat));
                        outputMap.remove(currentMat);
                    } else {
                        is[i] = new ItemStack(currentMat, 64);
                        outputMap.put(currentMat, outputMap.get(currentMat) - 64);
                    }
                    break;
                }
            }
        }
        chest.update();
        return true;
    }
    
    public void init(HeroStronghold plugin) {
        this.plugin = plugin;
    }
}
