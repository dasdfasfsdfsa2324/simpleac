package ru.simpleac.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.simpleac.data.PlayerData;
import ru.simpleac.data.PlayerDataManager;

import java.util.HashMap;
import java.util.Map;

public class ViolationManager {

    private final JavaPlugin plugin;
    private final PlayerDataManager dataManager;
    private final PunishmentManager punishmentManager;

    // отслеживает, сколько раз уже был пробит порог, чтобы правильно выбирать ступень наказания
    private final Map<String, Integer> punishStageCounters = new HashMap<>();

    public ViolationManager(JavaPlugin plugin, PlayerDataManager dataManager, PunishmentManager punishmentManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.punishmentManager = punishmentManager;
    }

    public void flag(Player player, String checkName, double amount, String info) {
        if (player.hasPermission("simpleac.bypass")) return;

        PlayerData data = dataManager.get(player.getUniqueId());
        data.addVl(checkName, amount);
        double vl = data.getVl(checkName);

        alert(player, checkName, vl, info);

        double threshold = plugin.getConfig().getDouble("checks." + checkName + ".vl-to-punish", 10.0);
        if (threshold <= 0) return;

        if (vl >= threshold) {
            String key = player.getUniqueId() + ":" + checkName;
            int stage = punishStageCounters.merge(key, 1, Integer::sum);
            punishmentManager.punish(player, checkName, stage);
            data.setVl(checkName, 0.0);
        }
    }

    private void alert(Player player, String checkName, double vl, String info) {
        if (!plugin.getConfig().getBoolean("alerts.enabled", true)) return;

        String prefix = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("prefix", "&8[&cSimpleAC&8] &r"));

        String msg = prefix + ChatColor.YELLOW + player.getName() + ChatColor.GRAY + " провалил "
                + ChatColor.RED + checkName.toUpperCase() + ChatColor.GRAY + " (vl=" + String.format("%.1f", vl) + ") "
                + ChatColor.DARK_GRAY + info;

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("simpleac.alerts")) {
                online.sendMessage(msg);
            }
        }
        Bukkit.getConsoleSender().sendMessage(msg);
    }
}
