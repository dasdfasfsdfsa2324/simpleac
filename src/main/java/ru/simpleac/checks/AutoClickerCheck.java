package ru.simpleac.checks;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.plugin.java.JavaPlugin;
import ru.simpleac.data.PlayerData;
import ru.simpleac.data.PlayerDataManager;
import ru.simpleac.managers.ViolationManager;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Ловит автокликеры: слишком высокий CPS в сочетании с подозрительно низкой
 * дисперсией интервалов между кликами (человек кликает "рвано", бот - ровно).
 */
public class AutoClickerCheck implements Listener {

    private final JavaPlugin plugin;
    private final PlayerDataManager dataManager;
    private final ViolationManager violationManager;

    public AutoClickerCheck(JavaPlugin plugin, PlayerDataManager dataManager, ViolationManager violationManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.violationManager = violationManager;
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("checks.autoclicker.enabled", true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwing(PlayerAnimationEvent event) {
        if (!enabled()) return;
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;

        Player player = event.getPlayer();
        if (player.hasPermission("simpleac.bypass")) return;

        PlayerData data = dataManager.get(player.getUniqueId());
        long now = System.currentTimeMillis();

        Deque<Long> clicks = data.recentClicks;
        clicks.addLast(now);
        while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000) {
            clicks.pollFirst();
        }

        int maxCps = plugin.getConfig().getInt("checks.autoclicker.max-cps", 20);
        double minVariance = plugin.getConfig().getDouble("checks.autoclicker.min-interval-variance-ms", 15);

        if (clicks.size() < 8) return;

        if (clicks.size() > maxCps) {
            violationManager.flag(player, "autoclicker", 1.0, "cps=" + clicks.size());
        }

        // Дисперсия интервалов
        List<Long> list = new ArrayList<>(clicks);
        List<Long> intervals = new ArrayList<>();
        for (int i = 1; i < list.size(); i++) {
            intervals.add(list.get(i) - list.get(i - 1));
        }

        double mean = intervals.stream().mapToLong(Long::longValue).average().orElse(0);
        double variance = intervals.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average().orElse(0);
        double stdDev = Math.sqrt(variance);

        if (clicks.size() > maxCps * 0.7 && stdDev < minVariance) {
            violationManager.flag(player, "autoclicker", 2.0,
                    "cps=" + clicks.size() + " stdDev=" + String.format("%.1f", stdDev));
        }
    }
}
