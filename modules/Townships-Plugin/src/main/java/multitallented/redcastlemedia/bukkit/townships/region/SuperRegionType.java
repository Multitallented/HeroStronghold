package multitallented.redcastlemedia.bukkit.townships.region;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author Multitallented
 */
public class SuperRegionType {
    private List<String> effects;
    private final double radius;
    private final double rawRadius;
    private Map<String, Integer> requirements;
    private final double moneyRequirement;
    private final double output;
    private final String name;
    private final List<String> children;
    private final int maxPower;
    private final int charter;
    private final int dailyPower;
    private final double exp;
    private final String centralStructure;
    private final String description;
    private final int population;
    private final ItemStack icon;
    private final HashMap<String, Integer> limits;
    private final double unlockCost;
    
    public SuperRegionType(String name, List<String> effects, double radius, List<String> requirements, double moneyRequirement, double output,
            List<String> children, int maxPower, int dailyPower, int charter, double exp, String centralStructure,
            String description, int population, ItemStack icon, List<String> limits, double unlockCost) {
        this.name=name;
        this.effects = effects;
        this.radius = radius;
        this.rawRadius = (int) Math.sqrt(radius);
        this.moneyRequirement = moneyRequirement;
        this.output = output;
        this.children = children;
        this.maxPower = maxPower;
        this.dailyPower = dailyPower;
        this.charter = charter;
        this.exp = exp;
        this.centralStructure = centralStructure;
        this.description = description;
        this.population = population;
        this.icon = icon;
        this.limits = processLimits(limits);
        this.unlockCost = unlockCost;
        setRequirements(requirements);
    }
    
    public double getUnlockCost() {
        return unlockCost;
    }
    
    private HashMap<String, Integer> processLimits(List<String> input) {
        HashMap<String, Integer> returnInput = new HashMap<String, Integer>();
        
        for (String s : input) {
            String[] inputParts = s.split("\\.");
            if (inputParts.length < 2) {
                continue;
            }
            int limitNumber = 0;
            try {
                limitNumber = Integer.parseInt(inputParts[1]);
            } catch (Exception e) {
                continue;
            }
            returnInput.put(inputParts[0], limitNumber);
        }
        return returnInput;
    }
    
    private void setRequirements(List<String> reqs) {
        Map<String, Integer> reqMap = new HashMap<String, Integer>();
        for (String s : reqs) {
            String[] args = s.split("\\.");
            if (args.length < 2) {
                continue;
            } else if (args.length < 3) {
                reqMap.put(args[0], Integer.parseInt(args[1]));
            } else {
                reqMap.put(args[0], Integer.parseInt(args[1]));
            }
        }
        requirements = reqMap;
    }
    
    public HashMap<String, Integer> getRegionLimits() {
        return limits;
    }
    
    public ItemStack getIcon() {
        return icon;
    }
    
    public int getPopulation() {
        return population;
    }
    
    public double getRawRadius() {
        return rawRadius;
    }
    
    public String getCentralStructure() {
        return centralStructure;
    }
    
    public List<String> getEffects() {
        return effects;
    }
    
    public double getExp() {
        return exp;
    }
    
    public int getDailyPower() {
        return dailyPower;
    }
    
    public double getOutput() {
        return output;
    }
    
    public String getName() {
        return name;
    }
    
    public boolean hasEffect(String input) {
        return effects.contains(input);
    }
    
    public boolean addEffect(String input) {
        return effects.add(input);
    }
    
    public double getMoneyRequirement() {
        return moneyRequirement;
    }
    
    public double getRadius() {
        return radius;
    }
    
    
    public Integer getRequirement(String name) {
        return requirements.get(name);
    }
    
    public Map<String, Integer> getRequirements() {
        return requirements;
    }
    
    public List<String> getChildren() {
        return children;
    }
    
    public int getMaxPower() {
        return maxPower;
    }
    
    public int getCharter() {
        return charter;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public boolean equals(Object obj) {
    	if (!(obj instanceof SuperRegionType)) {
    		return false;
    	}
    	return getName().equalsIgnoreCase(((SuperRegionType) obj).getName());
    }
}
