package ru.simpleac.checks;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.plugin.java.JavaPlugin;
import ru.simpleac.data.PlayerData;
import ru.simpleac.data.PlayerDataManager;
import ru.simpleac.managers.ViolationManager;
import ru.simpleac.util.MathUtil;

/**
 * Ловит:
 *  - удары без взмаха руки (packet-aura / no-swing)
 *  - удары с невозможным углом обзора к цели
 *  - удары со слишком большой дистанции (reach)
 *  - "мульти-аура": резкая смена цели с большим углом за очень короткое время
 */
public class KillAuraCheck implements Listener {

    private final JavaPlugin plugin;
    private final PlayerDataManager dataManager;
    private final ViolationManager violationManager;

    public KillAuraCheck(JavaPlugin plugin, PlayerDataManager dataManager, ViolationManager violationManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.violationManager = violationManager;
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("checks.killaura.enabled", true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;
        PlayerData data = dataManager.get(event.getPlayer().getUniqueId());
        data.lastSwingTime = System.currentTimeMillis();
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!enabled()) return;
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        Player player = (Player) event.getDamager();
        if (player.hasPermission("simpleac.bypass")) return;

        LivingEntity target = (LivingEntity) event.getEntity();
        PlayerData data = dataManager.get(player.getUniqueId());
        long now = System.currentTimeMillis();

        double maxAngle = plugin.getConfig().getDouble("checks.killaura.max-angle", 40.0);
        double maxReach = plugin.getConfig().getDouble("checks.killaura.max-reach", 3.3);
        long maxSwingDelay = plugin.getConfig().getLong("checks.killaura.max-swing-delay-ms", 250);

        // Reach
        double distance = player.getEyeLocation().distance(target.getEyeLocation());
        if (distance > maxReach) {
            violationManager.flag(player, "killaura", 2.0,
                    "reach=" + String.format("%.2f", distance));
        }

        // Angle
        double angle = MathUtil.angleBetween(player.getEyeLocation(), target.getEyeLocation());
        if (angle > maxAngle) {
            violationManager.flag(player, "killaura", 2.5,
                    "angle=" + String.format("%.1f", angle));
        }

        // No-swing
        if (now - data.lastSwingTime > maxSwingDelay) {
            violationManager.flag(player, "killaura", 1.5, "no-swing");
        }

        // Multi-aura: смена цели быстрее чем за 150мс
        String targetId = target.getUniqueId().toString();
        if (data.lastAttackedTargetId != null
                && !data.lastAttackedTargetId.equals(targetId)
                && (now - data.lastAttackTime) < 150) {
            violationManager.flag(player, "killaura", 3.0, "multi-target-switch");
        }

        data.lastAttackedTargetId = targetId;
        data.lastAttackTime = now;
    }
}
