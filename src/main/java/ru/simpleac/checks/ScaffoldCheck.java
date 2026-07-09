package ru.simpleac.checks;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.java.JavaPlugin;
import ru.simpleac.data.PlayerData;
import ru.simpleac.data.PlayerDataManager;
import ru.simpleac.managers.ViolationManager;

import java.util.Deque;

/**
 * Ловит скаффолд/авто-мост: слишком частая, идеально ровная установка блоков под ноги
 * без взгляда вниз, характерная как для читов, так и для авто-бриджеров Baritone.
 */
public class ScaffoldCheck implements Listener {

    private final JavaPlugin plugin;
    private final PlayerDataManager dataManager;
    private final ViolationManager violationManager;

    public ScaffoldCheck(JavaPlugin plugin, PlayerDataManager dataManager, ViolationManager violationManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.violationManager = violationManager;
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("checks.scaffold.enabled", true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!enabled()) return;
        Player player = event.getPlayer();
        if (player.hasPermission("simpleac.bypass")) return;

        PlayerData data = dataManager.get(player.getUniqueId());
        long now = System.currentTimeMillis();

        Deque<Long> placements = data.recentPlacements;
        placements.addLast(now);
        while (!placements.isEmpty() && now - placements.peekFirst() > 1000) {
            placements.pollFirst();
        }

        int maxPerSecond = plugin.getConfig().getInt("checks.scaffold.max-placements-per-second", 8);
        if (placements.size() > maxPerSecond) {
            violationManager.flag(player, "scaffold", 2.0,
                    "placements/s=" + placements.size());
        }

        // Блок ставится прямо под ноги, при этом игрок совсем не смотрит вниз (типично для авто-моста)
        boolean belowFeet = event.getBlockPlaced().getLocation().getY() < player.getLocation().getY()
                && event.getBlockPlaced().getLocation().getBlockX() != player.getLocation().getBlockX()
                || event.getBlockPlaced().getLocation().getBlockZ() != player.getLocation().getBlockZ();

        if (belowFeet && player.getLocation().getPitch() < 30 && player.isSprinting()) {
            violationManager.flag(player, "scaffold", 1.0, "no-look-bridge");
        }
    }
}
