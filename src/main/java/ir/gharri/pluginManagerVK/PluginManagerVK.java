package ir.gharri.pluginManagerVK;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class PluginManagerVK extends JavaPlugin implements Listener {

    private final WorldRules rules = new WorldRules();
    private File configFile;
    private FileConfiguration configVK;

    @Override
    public void onEnable() {
        loadConfigVK();
        rules.load(this);

        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new CommandFilter(this, rules), this);

        Bukkit.getScheduler().runTask(this, this::wrapAll);
    }

    private void loadConfigVK() {
        configFile = new File(getDataFolder(), "configvk.yml");
        if (!configFile.exists()) {
            saveResource("configvk.yml", false);
        }
        configVK = YamlConfiguration.loadConfiguration(configFile);
    }

    public FileConfiguration getConfigVK() {
        return configVK;
    }

    public void reloadConfigVK() {
        configVK = YamlConfiguration.loadConfiguration(configFile);
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent e) {
        Bukkit.getScheduler().runTask(this, this::wrapAll);
    }

    // بعد از اینکه بازیکن واقعاً world رو عوض کرد، وضعیت پلاگین‌ها رو با world جدید هماهنگ می‌کنیم
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent e) {
        syncPlayer(e.getPlayer());
    }

    public void wrapAll() {
        for (HandlerList list : HandlerList.getHandlerLists()) {
            for (RegisteredListener rl : list.getRegisteredListeners()) {
                if (rl.getPlugin() == this) continue;
                if (rl instanceof WorldFilteredListener) continue;
                if (!rules.isManaged(rl.getPlugin())) continue;

                rules.initIfNeeded(rl.getPlugin());
                list.unregister(rl);
                list.register(new WorldFilteredListener(rl, rules));
            }
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            syncPlayer(p);
        }
    }

    /** اگه پلاگین نباید بازیکن رو بشناسه ولی می‌شناسه، Quit می‌فرسته و برعکس. */
    public void syncPlayer(Player player) {
        for (String name : rules.managedNames()) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
            if (plugin == null || !plugin.isEnabled()) continue;

            boolean allowed = rules.isAllowed(plugin, player.getWorld());
            boolean known = rules.knownSet(plugin).contains(player.getUniqueId());

            if (allowed && !known) {
                rules.knownSet(plugin).add(player.getUniqueId());
                fireTo(plugin, new PlayerJoinEvent(player, Component.empty()));
            } else if (!allowed && known) {
                rules.knownSet(plugin).remove(player.getUniqueId());
                fireTo(plugin, new PlayerQuitEvent(player, Component.empty(),
                        PlayerQuitEvent.QuitReason.DISCONNECTED));
            }
        }
    }

    /** event رو فقط به listenerهای یه پلاگین مشخص می‌رسونه. */
    private void fireTo(Plugin plugin, Event event) {
        for (RegisteredListener rl : event.getHandlers().getRegisteredListeners()) {
            if (rl.getPlugin() != plugin) continue;
            try {
                if (rl instanceof WorldFilteredListener wl) {
                    wl.callOriginal(event);
                } else {
                    rl.callEvent(event);
                }
            } catch (Exception ex) {
                getLogger().warning("Error sending fake " + event.getEventName()
                        + " to " + plugin.getName() + ": " + ex.getMessage());
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reloadConfigVK();
            rules.load(this);
            wrapAll();
            Bukkit.getOnlinePlayers().forEach(Player::updateCommands);
            sender.sendMessage("PluginManagerVK reloaded.");
            return true;
        }
        sender.sendMessage("/pmvk reload");
        return true;
    }
}