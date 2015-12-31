package multitallented.redcastlemedia.bukkit.townships.region;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import multitallented.redcastlemedia.bukkit.townships.Townships;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 *
 * @author Multitallented
 */
public class DefaultRegions {
    
    //TODO copy default groups.yml file
    public static void createDefaultRegionFiles(Townships plugin) {
        File folder = new File(plugin.getDataFolder(), "RegionConfig");
        try {
            folder.mkdirs();
            File shackFile = new File(folder, "shack.yml");
            shackFile.createNewFile();
            FileConfiguration config = new YamlConfiguration();
            config.load(shackFile);
            List<String> tempList = new ArrayList<String>();
            tempList.add("deny_block_build_no_reagent.1");
            tempList.add("deny_explosion.1");
            tempList.add("deny_block_break.1");
            tempList.add("deny_player_interact.1");
            tempList.add("scheduled_upkeep.7200");
            tempList.add("deny_bucket_use_no_reagent.1");
            tempList.add("deny_fire_no_reagent.1");
            tempList.add("deny_damage.1");
            config.set("effects", tempList);
            tempList = new ArrayList<String>();
            config.set("radius", 4);
            tempList.add("WOODEN_DOOR.1");
            tempList.add("THIN_GLASS.3");
            tempList.add("BOOKSHELF.1");
            tempList.add("BED_BLOCK.1");
            tempList.add("FURNACE.1");
            tempList.add("WORKBENCH.1");
            tempList.add("WALL_SIGN.1");
            tempList.add("WOOD.24");
            tempList.add("COBBLESTONE.12");
            tempList.add("WOOL.1");
            tempList.add("CHEST.2");
            config.set("requirements", new ArrayList<String>(tempList));
            tempList = new ArrayList<String>();
            tempList.add("BREAD.2");
            tempList.add("IRON_INGOT.1");
            tempList.add("DIAMOND.1");
            config.set("reagents", new ArrayList<String>(tempList));
            tempList = new ArrayList<String>();
            tempList.add("BREAD.2");
            config.set("input", new ArrayList<String>(tempList));
            tempList = new ArrayList<String>();
            tempList.add("DIRT.1");
            config.set("output", new ArrayList<String>(tempList));
            tempList = new ArrayList<String>();
            tempList.add("ward");
            tempList.add("village");
            config.set("super-regions", new ArrayList<String>(tempList));
            config.set("cost", 500.0);
            config.set("payout", 50.0);
            config.set("description", "A place to live");
            config.set("icon", "BED.1");
            config.save(shackFile);
        } catch (Exception e) {
            
        }
        try {
            File shackFile = new File(folder, "mancannon.yml");
            shackFile.createNewFile();
            FileConfiguration config = new YamlConfiguration();
            config.load(shackFile);
            List<String> tempList = new ArrayList<String>();
            tempList.add("man_cannon.3");
            config.set("effects",tempList);
            tempList = new ArrayList<String>();
            config.set("radius", 2);
            tempList.add("POWERED_RAIL.3");
            tempList.add("PISTON_BASE.1");
            config.set("requirements", new ArrayList<String>(tempList));
            tempList = new ArrayList<String>();
            tempList.add("REDSTONE.16");
            config.set("reagents", new ArrayList<String>(tempList));
            config.set("cost", 200.0);
            config.set("description", "A player launching machine");
            config.set("icon", "POWERED_RAIL.1");
            config.save(shackFile);
        } catch (Exception e) {
            
        }
        try {
            File shackFile = new File(folder, "arrowturret.yml");
            shackFile.createNewFile();
            FileConfiguration config = new YamlConfiguration();
            config.load(shackFile);
            List<String> tempList = new ArrayList<String>();
            tempList.add("shoot_arrow.20");
            config.set("effects",tempList);
            tempList = new ArrayList<String>();
            config.set("radius", 20);
            config.set("build-radius", 3);
            tempList.add("DISPENSER.1");
            tempList.add("FENCE.5");
            tempList.add("REDSTONE_WIRE.4");
            config.set("requirements", new ArrayList<String>(tempList));
            tempList = new ArrayList<String>();
            tempList.add("REDSTONE.4");
            tempList.add("ARROW.1");
            config.set("reagents", new ArrayList<String>(tempList));
            tempList = new ArrayList<String>();
            tempList.add("ARROW.1");
            config.set("input", new ArrayList<String>(tempList));
            config.set("cost", 1500.0);
            config.set("exp", 200);
            config.set("description", "A turret that shoots arrows at players");
            config.set("icon", "ARROW.1");
            config.save(shackFile);
        } catch (Exception e) {
            
        }
    }
    
    public static void createDefaultSuperRegionFiles(Townships plugin) {
        File folder = new File(plugin.getDataFolder(), "SuperRegionConfig");
        try {
            folder.mkdirs();
            File shackFile = new File(folder, "ward.yml");
            shackFile.createNewFile();
            FileConfiguration config = new YamlConfiguration();
            config.load(shackFile);
            List<String> tempList = new ArrayList<String>();
            tempList.add("denyblockbuild");
            tempList.add("denyexplosion");
            tempList.add("denyblockbreak");
            tempList.add("denybucketuse");
            tempList.add("denyfirenoreagent");
            tempList.add("denyfriendlyfire");
            config.set("effects", tempList);
            tempList = new ArrayList<String>();
            config.set("radius", 50);
            tempList.add("mancannon.2");
            config.set("requirements", new ArrayList<String>(tempList));
            config.set("money-requirement", 5000.0);
            config.set("money-output-daily", -100.0);
            config.set("max-power", 40);
            config.set("daily-power-increase", 4);
            config.set("exp", 500);
            config.save(shackFile);
            config.set("description", "A small area of protection");
            config.set("icon", "GLASS.1");
        } catch (Exception e) {
            
        }
        try {
            File shackFile = new File(folder, "village.yml");
            shackFile.createNewFile();
            FileConfiguration config = new YamlConfiguration();
            config.load(shackFile);
            List<String> tempList = new ArrayList<String>();
            tempList.add("denyblockbuild");
            tempList.add("denyexplosion");
            tempList.add("denyblockbreak");
            tempList.add("denybucketuse");
            tempList.add("denyfirenoreagent");
            tempList.add("denypvp");
            config.set("effects", tempList);
            tempList = new ArrayList<String>();
            config.set("radius", 50);
            tempList.add("arrowturret.2");
            tempList.add("mancannon.2");
            tempList.add("shack.1");
            config.set("requirements", new ArrayList<String>(tempList));
            config.set("central-structure", "shack");
            config.set("money-requirement", 10000.0);
            config.set("money-output-daily", -1000.0);
            config.set("max-power", 400);
            config.set("daily-power-increase", 40);
            config.set("charter", 4);
            tempList = new ArrayList<String>();
            tempList.add("ward");
            config.set("children", new ArrayList<String>(tempList));
            config.set("exp", 5000);
            config.set("description", "A town");
            config.set("icon", "WOOD_DOOR.1");
            config.save(shackFile);
        } catch (Exception e) {
            
        }
    }

    public static void migrateRegions(File regionFile, Townships plugin) {
        File folder = new File(plugin.getDataFolder(), "RegionConfig");
        try {
            FileConfiguration config = new YamlConfiguration();
            config.load(regionFile);
            Set<String> keys = config.getKeys(false);
            folder.mkdirs();
            for (String s : keys) {
                try {
                    FileConfiguration currentConfig = new YamlConfiguration();
                    ConfigurationSection cs = config.getConfigurationSection(s);
                    File file = new File(folder, s + ".yml");
                    file.createNewFile();
                    currentConfig.load(file);
                    for (String s0 : cs.getKeys(false)) {
                        currentConfig.set(s0, cs.get(s0));
                    }
                    currentConfig.save(file);
                } catch (Exception e) {
                    System.out.println("[Townships] failed to migrate " + s + ".yml");
                }
            }
        } catch (Exception e) {
            System.out.println("[Townships] failed to migrate regions.yml");
        }
    }
    
    public static void migrateSuperRegions(File sRegionFile, Townships plugin) {
        File folder = new File(plugin.getDataFolder(), "SuperRegionConfig");
        try {
            FileConfiguration config = new YamlConfiguration();
            config.load(sRegionFile);
            Set<String> keys = config.getKeys(false);
            folder.mkdirs();
            for (String s : keys) {
                FileConfiguration currentConfig = new YamlConfiguration();
                ConfigurationSection cs = config.getConfigurationSection(s);
                File file = new File(folder, s + ".yml");
                file.createNewFile();
                currentConfig.load(file);
                for (String s0 : cs.getKeys(false)) {
                    currentConfig.set(s0, cs.get(s0));
                }
                currentConfig.save(file);
            }
        } catch (Exception e) {
            System.out.println("[Townships] failed to migrate super-regions.yml");
        }
    }
}
