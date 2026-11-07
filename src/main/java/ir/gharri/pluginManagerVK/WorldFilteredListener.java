package ir.gharri.pluginManagerVK;

import org.bukkit.World;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.hanging.HangingEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleEvent;
import org.bukkit.event.weather.WeatherEvent;
import org.bukkit.event.world.ChunkEvent;
import org.bukkit.event.world.WorldEvent;
import org.bukkit.plugin.RegisteredListener;

import java.util.UUID;

public class WorldFilteredListener extends RegisteredListener {

    private final RegisteredListener original;
    private final WorldRules rules;

    public WorldFilteredListener(RegisteredListener original, WorldRules rules) {
        super(original.getListener(), (l, e) -> {}, original.getPriority(),
                original.getPlugin(), original.isIgnoringCancelled());
        this.original = original;
        this.rules = rules;
    }

    /** برای eventهای ساختگی: بدون فیلتر مستقیم به پلاگین می‌ره. */
    public void callOriginal(Event event) throws EventException {
        original.callEvent(event);
    }

    @Override
    public void callEvent(Event event) throws EventException {
        // چرخه‌ی حیات بازیکن: بر اساس «می‌شناسه یا نمی‌شناسه»
        if (event instanceof PlayerJoinEvent je) {
            if (!rules.isAllowed(getPlugin(), je.getPlayer().getWorld())) return;
            rules.knownSet(getPlugin()).add(je.getPlayer().getUniqueId());
            original.callEvent(event);
            return;
        }
        if (event instanceof PlayerQuitEvent qe) {
            if (!rules.knownSet(getPlugin()).remove(qe.getPlayer().getUniqueId())) return;
            original.callEvent(event);
            return;
        }
        if (event instanceof PlayerKickEvent ke) {
            if (!rules.knownSet(getPlugin()).contains(ke.getPlayer().getUniqueId())) return;
            original.callEvent(event);
            return;
        }
        // تغییر world: فقط اگه هر دو طرف مجاز باشن (وگرنه Join/Quit ساختگی جایگزینشه)
        if (event instanceof PlayerChangedWorldEvent ce) {
            if (!rules.isAllowed(getPlugin(), ce.getFrom())
                    || !rules.isAllowed(getPlugin(), ce.getPlayer().getWorld())) return;
            original.callEvent(event);
            return;
        }

        World world = resolveWorld(event);
        if (!rules.isAllowed(getPlugin(), world)) return;
        original.callEvent(event);
    }

    private World resolveWorld(Event e) {
        if (e instanceof PlayerEvent pe) return pe.getPlayer().getWorld();
        if (e instanceof BlockEvent be) return be.getBlock().getWorld();
        if (e instanceof EntityEvent ee) return ee.getEntity().getWorld();
        if (e instanceof WorldEvent we) return we.getWorld();
        if (e instanceof ChunkEvent ce) return ce.getWorld();
        if (e instanceof WeatherEvent we) return we.getWorld();
        if (e instanceof VehicleEvent ve) return ve.getVehicle().getWorld();
        if (e instanceof HangingEvent he) return he.getEntity().getWorld();
        if (e instanceof InventoryEvent ie) {
            HumanEntity viewer = ie.getView().getPlayer();
            return viewer.getWorld();
        }
        return null;
    }
}