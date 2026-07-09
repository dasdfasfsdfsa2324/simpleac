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
 *  - удары с невозможным углом обзора к цели
 *  - удары со слишком большой дистанции (reach, считается до хитбокса, а не до глаз цели)
 *  - удары без взмаха руки (no-swing) - учитывается с большим окном по времени,
 *    т.к. порядок сетевых пакетов Animation/Damage не гарантирован даже у нормальных игроков
 *  - "мульти-аура": резкая смена цели с большим углом за очень короткое время
 *
 * Наказание применяется только если ПОДРЯД накопилось несколько подозрительных ударов
 * (debounce), а не с одного - это защищает от ложных срабатываний из-за лагов/пинга.
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

        double maxAngle = plugin.getConfig().getDouble("checks.killaura.max-angle", 60.0);
        double maxReach = plugin.getConfig().getDouble("checks.killaura.max-reach", 3.6);
        long maxSwingDelay = plugin.getConfig().getLong("checks.killaura.max-swing-delay-ms", 400);
        int requiredStreak = plugin.getConfig().getInt("checks.killaura.required-streak", 3);

        boolean suspicious = false;
        StringBuilder reasons = new StringBuilder();

        // Reach - считаем до ближайшей точки хитбокса, а не до глаз цели
        double distance = MathUtil.distanceToBox(player.getEyeLocation(), target.getBoundingBox());
        if (distance > maxReach) {
            suspicious = true;
            reasons.append("reach=").append(String.format("%.2f", distance)).append(" ");
        }

        // Angle
        double angle = MathUtil.angleBetween(player.getEyeLocation(), target.getEyeLocation());
        if (angle > maxAngle) {
            suspicious = true;
            reasons.append("angle=").append(String.format("%.1f", angle)).append(" ");
        }

        // No-swing (мягкая проверка с большим окном - сетевые пакеты не гарантируют порядок)
        if (now - data.lastSwingTime > maxSwingDelay) {
            suspicious = true;
            reasons.append("no-swing ");
        }

        // Multi-aura: смена цели быстрее чем за 100мс
        String targetId = target.getUniqueId().toString();
        boolean multiSwitch = data.lastAttackedTargetId != null
                && !data.lastAttackedTargetId.equals(targetId)
                && (now - data.lastAttackTime) < 100;
        if (multiSwitch) {
            suspicious = true;
            reasons.append("multi-target-switch ");
        }

        data.lastAttackedTargetId = targetId;
        data.lastAttackTime = now;

        if (suspicious) {
            data.killAuraSuspicionStreak++;
        } else {
            data.killAuraSuspicionStreak = 0;
        }

        // Наказываем только если несколько подозрительных ударов подряд - защита от ложных срабатываний
        if (data.killAuraSuspicionStreak >= requiredStreak) {
            violationManager.flag(player, "killaura", 3.0, reasons.toString().trim());
            data.killAuraSuspicionStreak = 0;
        }
    }
}
