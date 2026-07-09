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
 * Ловит зависание/полёт: игрок долго находится в воздухе без падения
 * при отсутствии легитимных причин (креатив, элитра, левитация, вода и т.д.)
 */
public class FlyCheck implements Listener {

    private final JavaPlugin plugin;
    private final PlayerDataManager dataManager;
    private final ViolationManager violationManager;

    public FlyCheck(JavaPlugin plugin, PlayerDataManager dataManager, ViolationManager violationManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.violationManager = violationManager;
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("checks.fly.enabled", true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!enabled()) return;
        Player player = event.getPlayer();
        if (player.hasPermission("simpleac.bypass")) return;

        if (player.getAllowFlight() || player.isFlying()) return;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
        if (player.isGliding()) return; // элитра
        if (player.isInsideVehicle()) return;
        if (player.isSwimming()) return;
        if (player.getPotionEffect(PotionEffectType.LEVITATION) != null) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        PlayerData data = dataManager.get(player.getUniqueId());

        boolean onGround = player.isOnGround();
        boolean inLiquidOrClimb = isInLiquidOrClimbable(player);

        if (onGround || inLiquidOrClimb) {
            data.airTicks = 0;
            return;
        }

        double deltaY = to.getY() - from.getY();
        data.airTicks++;

        int maxAirTicks = plugin.getConfig().getInt("checks.fly.max-air-ticks", 20);

        // Если игрок дольше нормы в воздухе и не проваливается вниз (deltaY >= -0.1 значит не падает)
        if (data.airTicks > maxAirTicks && deltaY >= -0.05) {
            violationManager.flag(player, "fly", 2.0,
                    "airTicks=" + data.airTicks + " dy=" + String.format("%.2f", deltaY));
        }
    }

    private boolean isInLiquidOrClimbable(Player player) {
        org.bukkit.Material type = player.getLocation().getBlock().getType();
        String name = type.name();
        return name.contains("WATER") || name.contains("LAVA")
                || name.equals("LADDER") || name.equals("VINE")
                || name.contains("SCAFFOLDING");
    }
}
