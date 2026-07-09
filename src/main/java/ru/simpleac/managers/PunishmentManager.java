package ru.simpleac.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class PunishmentManager {

    private final JavaPlugin plugin;

    public PunishmentManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * stage начинается с 1 (первое достижение порога), 2 (второе) и т.д.
     */
    public void punish(Player player, String checkName, int stage) {
        List<String> commands = plugin.getConfig().getStringList("punishments." + checkName);
        if (commands.isEmpty()) return;

        int index = Math.min(stage - 1, commands.size() - 1);
        String cmd = commands.get(index).replace("%player%", player.getName());

        Bukkit.getScheduler().runTask(plugin, () ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
    }
}
