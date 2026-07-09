package ru.simpleac.commands;

import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.simpleac.data.PlayerData;
import ru.simpleac.data.PlayerDataManager;

public class AntiCheatCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final PlayerDataManager dataManager;

    public AntiCheatCommand(JavaPlugin plugin, PlayerDataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("simpleac.admin")) {
            sender.sendMessage(ChatColor.RED + "У вас нет прав на эту команду.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "/anticheat reload " + ChatColor.GRAY + "- перезагрузить конфиг");
            sender.sendMessage(ChatColor.YELLOW + "/anticheat vl <ник> " + ChatColor.GRAY + "- показать нарушения игрока");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "Конфиг SimpleAC перезагружен.");
                return true;

            case "vl":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Использование: /anticheat vl <ник>");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Игрок не найден.");
                    return true;
                }
                PlayerData data = dataManager.get(target.getUniqueId());
                sender.sendMessage(ChatColor.GOLD + "Нарушения " + target.getName() + ":");
                if (data.violations.isEmpty()) {
                    sender.sendMessage(ChatColor.GRAY + "  нет нарушений");
                } else {
                    data.violations.forEach((check, vl) ->
                            sender.sendMessage(ChatColor.GRAY + "  " + check + ": " + String.format("%.1f", vl)));
                }
                return true;

            default:
                sender.sendMessage(ChatColor.RED + "Неизвестная подкоманда.");
                return true;
        }
    }
}
