package ru.simpleac.checks;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import ru.simpleac.data.PlayerDataManager;
import ru.simpleac.managers.ViolationManager;

/**
 * Простая проверка горизонтальной скорости с учётом Speed-эффекта.
 * Это грубая эвристика (без полноценного физического движка), поэтому
 * порог намеренно взят с запасом, чтобы снизить ложные срабатывания.
 */
public class SpeedCheck implements Listener {

    private final JavaPlugin plugin;
    private final PlayerDataManager dataManager;
    private final ViolationManager violationManager;

    public SpeedCheck(JavaPlugin plugin, PlayerDataManager dataManager, ViolationManager violationManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.violationManager = violationManager;
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("checks.speed.enabled", true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!enabled()) return;
        Player player = event.getPlayer();
        if (player.hasPermission("simpleac.bypass")) return;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
        if (player.isFlying() || player.getAllowFlight() || player.isGliding()) return;
        if (player.isInsideVehicle() || player.isSwimming()) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double horizontalSpeed = Math.hypot(dx, dz) * 20.0; // блоков в секунду (20 тиков/сек)

        double maxSpeed = plugin.getConfig().getDouble("checks.speed.max-blocks-per-sec", 6.5);

        int speedAmplifier = 0;
        if (player.getPotionEffect(PotionEffectType.SPEED) != null) {
            speedAmplifier = player.getPotionEffect(PotionEffectType.SPEED).getAmplifier() + 1;
        }
        double allowedSpeed = maxSpeed + (speedAmplifier * 2.0);

        if (horizontalSpeed > allowedSpeed) {
            violationManager.flag(player, "speed", 1.5,
                    "speed=" + String.format("%.2f", horizontalSpeed) + " b/s");
        }
    }
}
