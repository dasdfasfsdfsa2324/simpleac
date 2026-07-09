package ru.simpleac.checks;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import ru.simpleac.data.PlayerDataManager;
import ru.simpleac.managers.ViolationManager;

/**
 * Ловит подъём вверх вдоль стены без лестницы/лианы/скаффолдинга (Spider-type читы).
 */
public class SpiderCheck implements Listener {

    private final JavaPlugin plugin;
    private final PlayerDataManager dataManager;
    private final ViolationManager violationManager;

    private static final BlockFace[] SIDES = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };

    public SpiderCheck(JavaPlugin plugin, PlayerDataManager dataManager, ViolationManager violationManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.violationManager = violationManager;
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("checks.spider.enabled", true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!enabled()) return;
        Player player = event.getPlayer();
        if (player.hasPermission("simpleac.bypass")) return;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
        if (player.getAllowFlight() || player.isFlying() || player.isGliding() || player.isSwimming()) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        double deltaY = to.getY() - from.getY();
        double deltaXZ = Math.hypot(to.getX() - from.getX(), to.getZ() - from.getZ());

        if (player.isOnGround()) return;
        if (deltaY <= 0.05) return; // не поднимается
        if (deltaXZ > 0.2) return;  // это может быть прыжок с разбега, не "приклеивание" к стене

        if (isOnClimbable(player.getLocation())) return; // легально (лестница/лиана/скаффолдинг)

        if (isAgainstSolidWall(player.getLocation())) {
            violationManager.flag(player, "spider", 3.0,
                    "dy=" + String.format("%.2f", deltaY));
        }
    }

    private boolean isOnClimbable(Location loc) {
        Material type = loc.getBlock().getType();
        String name = type.name();
        return name.equals("LADDER") || name.equals("VINE") || name.contains("SCAFFOLDING");
    }

    private boolean isAgainstSolidWall(Location loc) {
        for (BlockFace face : SIDES) {
            Material type = loc.getBlock().getRelative(face).getType();
            if (type.isSolid()) return true;
        }
        return false;
    }
}
