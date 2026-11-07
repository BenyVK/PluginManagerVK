package ir.gharri.pluginManagerVK;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;

public class CommandFilter implements Listener {

    private final PluginManagerVK main;
    private final WorldRules rules;

    public CommandFilter(PluginManagerVK main, WorldRules rules) {
        this.main = main;
        this.rules = rules;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        String label = e.getMessage().substring(1).split(" ")[0].toLowerCase();
        Command cmd = Bukkit.getCommandMap().getCommand(label);
        if (cmd instanceof PluginCommand pc && !rules.isAllowed(pc.getPlugin(), e.getPlayer().getWorld())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("Unknown command. Type \"/help\" for help.");
        }
    }

    @EventHandler
    public void onCommandSend(PlayerCommandSendEvent e) {
        e.getCommands().removeIf(name -> {
            Command cmd = Bukkit.getCommandMap().getCommand(name);
            return cmd instanceof PluginCommand pc
                    && !rules.isAllowed(pc.getPlugin(), e.getPlayer().getWorld());
        });
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        Bukkit.getScheduler().runTask(main, () -> e.getPlayer().updateCommands());
    }
}