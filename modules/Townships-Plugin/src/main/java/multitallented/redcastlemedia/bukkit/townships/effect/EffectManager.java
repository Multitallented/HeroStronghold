package multitallented.redcastlemedia.bukkit.townships.effect;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import multitallented.redcastlemedia.bukkit.townships.Townships;

/**
 *
 * @author Multitallented
 * This code was mostly borrowed from the Heroes dev team with
 * permission (Kainzo, Sleaker, DThielke, Rigby, RightLegRed, Multitallented) Thanks Kainzo ^_^
 * I am part of the Herocraft Coding Team
 */
public class EffectManager {
    private final LinkedHashMap<String, Effect> effects;
    private final HashMap<String, File> effectFiles;
    private final Townships plugin;
    private final File dir;
    private final URLClassLoader classLoader;
    public EffectManager(Townships plugin) {
        effects = new LinkedHashMap<String, Effect>();
        effectFiles = new HashMap<String, File>();
        this.plugin = plugin;
        dir = new File(plugin.getDataFolder(), "effects");
        dir.mkdir();

        List<URL> urls = new ArrayList<URL>();
        if (dir.list().length == 0) {
            plugin.warning("No effects found in the effects folder");
        }
        for (String effectFile : dir.list()) {
            if (effectFile.contains(".jar")) {
                File file = new File(dir, effectFile);
                String name = effectFile.toLowerCase().replace(".jar", "").replace("effect", "");
                if (effectFiles.containsKey(name)) {
                    plugin.warning("Duplicate effect jar found! Please remove " + effectFile + " or " + effectFiles.get(name).getName());
                    continue;
                }
                effectFiles.put(name, file);
                try {
                    urls.add(file.toURI().toURL());
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        }
        ClassLoader cl = plugin.getClass().getClassLoader();
        classLoader = URLClassLoader.newInstance(urls.toArray(new URL[0]), cl);
        
        loadEffects();
    }
    
    public Effect getEffect(String name) {
        if (name == null) {
            return null;
        }
        return effects.get(name);
    }
    
    public boolean hasEffect(String name) {
        return effects.containsKey(name);
    }
    
    public Effect loadEffect(File file) {
        try {
            JarFile jarFile = new JarFile(file);
            Enumeration<JarEntry> entries = jarFile.entries();

            String mainClass = null;
            while (entries.hasMoreElements()) {
                JarEntry element = entries.nextElement();
                if (element.getName().equalsIgnoreCase("effect.info")) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(jarFile.getInputStream(element)));
                    mainClass = reader.readLine().substring(12);
                    break;
                }
            }

            if (mainClass != null) {
                Class<?> clazz = Class.forName(mainClass, true, classLoader);
                for (Class<?> subclazz : clazz.getClasses()) {
                    Class.forName(subclazz.getName(), true, classLoader);
                }
                Class<? extends Effect> effectClass = clazz.asSubclass(Effect.class);
                Constructor<? extends Effect> ctor = effectClass.getConstructor(plugin.getClass());
                Effect effect = ctor.newInstance(plugin);
                effect.init(plugin);
                return effect;
            } else
                throw new Exception();
        } catch (Exception e) {
            plugin.warning("The effect " + file.getName() + " failed to load");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Load all the skills.
     */
    public void loadEffects() {
        for (Entry<String, File> entry : effectFiles.entrySet()) {
            // if the Skill is already loaded, skip it
            if (hasEffect(entry.getKey())) 
                continue;

            Effect effect = loadEffect(entry.getValue());
            if (effect != null) {
                effects.put(entry.getKey(),effect);
            }
        }
    }
    
}
