package ir.gharri.pluginManagerVK;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WorldRules {

    private volatile Map<String, Set<String>> rules = new HashMap<>();
    // برای هر پلاگین: بازیکن‌هایی که پلاگین «می‌شناسه»
    private final Map<String, Set<UUID>> known = new ConcurrentHashMap<>();
    private final Set<String> initialized = ConcurrentHashMap.newKeySet();

    public void load(PluginManagerVK main) {
        Map<String, Set<String>> loaded = new HashMap<>();
        ConfigurationSection section = main.getConfigVK().getConfigurationSection("plugins");
        if (section != null) {
            for (String pluginName : section.getKeys(false)) {
                Set<String> worlds = new HashSet<>();
                for (String w : section.getStringList(pluginName + ".worlds")) {
                    worlds.add(w.toLowerCase());
                }
                loaded.put(pluginName.toLowerCase(), worlds);
            }
        }
        rules = loaded;
    }

    public Set<String> managedNames() {
        return rules.keySet();
    }

    public boolean isManaged(Plugin plugin) {
        return rules.containsKey(plugin.getName().toLowerCase());
    }

    public boolean isAllowed(Plugin plugin, World world) {
        Set<String> worlds = rules.get(plugin.getName().toLowerCase());
        if (worlds == null || world == null) return true;
        return worlds.contains(world.getName().toLowerCase());
    }

    public Set<UUID> knownSet(Plugin plugin) {
        return known.computeIfAbsent(plugin.getName().toLowerCase(),
                k -> ConcurrentHashMap.newKeySet());
    }

    /** اولین باری که پلاگین مدیریت می‌شه، همه‌ی بازیکن‌های آنلاین رو «شناخته‌شده» فرض می‌کنیم. */
    public void initIfNeeded(Plugin plugin) {
        if (initialized.add(plugin.getName().toLowerCase())) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                knownSet(plugin).add(p.getUniqueId());
            }
        }
    }
}