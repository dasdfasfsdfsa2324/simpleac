package ru.simpleac.checks;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import ru.simpleac.data.PlayerData;
import ru.simpleac.data.PlayerDataManager;
import ru.simpleac.managers.ViolationManager;
import ru.simpleac.util.MathUtil;

/**
 * Baritone/пат-файндинг боты обычно двигаются НЕЧЕЛОВЕЧЕСКИ ровно:
 *  - идеально прямые линии по X/Z без микро-отклонений в течение долгого времени
 *  - yaw камеры идеально совпадает с направлением движения каждый тик (без дрожания мыши)
 *
 * Это эвристика с вероятностью ложных срабатываний, поэтому VL начисляется
 * малыми порциями и требует продолжительного паттерна.
 * Рекомендуется сочетать с проверками Speed/Timer/Reach для повышения точности.
 */
public class BaritoneCheck implements Listener {

    private final JavaPlugin plugin;
    private final PlayerDataManager dataManager;
    private final ViolationManager violationManager;

    public BaritoneCheck(JavaPlugin plugin, PlayerDataManager dataManager, ViolationManager violationManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.violationManager = violationManager;
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("checks.baritone.enabled", true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!enabled()) return;
        Player player = event.getPlayer();
        if (player.hasPermission("simpleac.bypass")) return;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
        if (player.isInsideVehicle() || player.isGliding()) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double dist = Math.hypot(dx, dz);

        PlayerData data = dataManager.get(player.getUniqueId());

        // недостаточно движения для анализа
        if (dist < 0.05) {
            data.straightLineTicks = 0;
            data.perfectRotationTicks = 0;
            data.lastMoveHeading = null;
            return;
        }

        double heading = MathUtil.headingOf(dx, dz);

        // --- Проверка идеально прямой линии движения ---
        if (data.lastMoveHeading != null) {
            double diff = MathUtil.normalizeAngleDiff(heading - data.lastMoveHeading);
            if (diff < 0.05) {
                data.straightLineTicks++;
            } else {
                data.straightLineTicks = 0;
            }
        }
        data.lastMoveHeading = heading;

        int straightThreshold = plugin.getConfig().getInt("checks.baritone.straight-line-ticks", 60);
        if (data.straightLineTicks > straightThreshold) {
            violationManager.flag(player, "baritone", 1.0,
                    "straight-line=" + data.straightLineTicks + " ticks");
            data.straightLineTicks = straightThreshold / 2; // не спамить каждый тик
        }

        // --- Проверка "идеальной" синхронизации взгляда с направлением движения ---
        float yaw = to.getYaw();
        double angleDiff = MathUtil.normalizeAngleDiff(yaw - heading);

        if (angleDiff < 0.2) {
            data.perfectRotationTicks++;
        } else {
            data.perfectRotationTicks = 0;
        }

        int rotThreshold = plugin.getConfig().getInt("checks.baritone.perfect-rotation-ticks", 40);
        if (data.perfectRotationTicks > rotThreshold) {
            violationManager.flag(player, "baritone", 1.5,
                    "perfect-rotation=" + data.perfectRotationTicks + " ticks");
            data.perfectRotationTicks = rotThreshold / 2;
        }
    }
}
