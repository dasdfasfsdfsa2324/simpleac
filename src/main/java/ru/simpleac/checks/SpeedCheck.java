package ru.simpleac.checks;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import ru.simpleac.data.PlayerData;
import ru.simpleac.data.PlayerDataManager;
import ru.simpleac.managers.ViolationManager;

/**
 * Проверка горизонтальной скорости с учётом Speed-эффекта.
 *
 * Важно: скорость считается НЕ по одному тику (это шумно и ловит лаги/рубербанд),
 * а как скользящее среднее за последние ~1 секунду движения, плюс нарушение
 * фиксируется только если превышение держится несколько тиков подряд.
 * Это сильно снижает ложные срабатывания на обычном спринт-беге/спринт-прыжках.
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

        PlayerData data = dataManager.get(player.getUniqueId());
        long now = System.currentTimeMillis();

        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double dist = Math.hypot(dx, dz);

        // копим расстояние и время за скользящее окно ~1 секунда
        data.speedWindowDistance += dist;
        long elapsedSinceWindowStart = now - data.speedWindowStartMs;

        if (elapsedSinceWindowStart < 1000) {
            return; // окно ещё не набралось, ждём
        }

        double avgSpeed = data.speedWindowDistance / (elapsedSinceWindowStart / 1000.0);

        double maxSpeed = plugin.getConfig().getDouble("checks.speed.max-blocks-per-sec", 8.2);

        int speedAmplifier = 0;
        if (player.getPotionEffect(PotionEffectType.SPEED) != null) {
            speedAmplifier = player.getPotionEffect(PotionEffectType.SPEED).getAmplifier() + 1;
        }
        double allowedSpeed = maxSpeed + (speedAmplifier * 2.0);

        int requiredConsecutive = plugin.getConfig().getInt("checks.speed.consecutive-windows", 3);

        if (avgSpeed > allowedSpeed) {
            data.speedViolationStreak++;
            if (data.speedViolationStreak >= requiredConsecutive) {
                violationManager.flag(player, "speed", 1.5,
                        "avgSpeed=" + String.format("%.2f", avgSpeed) + " b/s");
                data.speedViolationStreak = 0;
            }
        } else {
            data.speedViolationStreak = 0;
        }

        // сброс окна
        data.speedWindowDistance = 0.0;
        data.speedWindowStartMs = now;
    }
}
