package ru.simpleac;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.simpleac.checks.*;
import ru.simpleac.commands.AntiCheatCommand;
import ru.simpleac.data.PlayerData;
import ru.simpleac.data.PlayerDataManager;
import ru.simpleac.listeners.PlayerListener;
import ru.simpleac.managers.PunishmentManager;
import ru.simpleac.managers.ViolationManager;

public class SimpleAC extends JavaPlugin {

    private PlayerDataManager dataManager;
    private ViolationManager violationManager;
    private PunishmentManager punishmentManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        dataManager = new PlayerDataManager();
        punishmentManager = new PunishmentManager(this);
        violationManager = new ViolationManager(this, dataManager, punishmentManager);

        // Регистрация проверок
        Bukkit.getPluginManager().registerEvents(new KillAuraCheck(this, dataManager, violationManager), this);
        Bukkit.getPluginManager().registerEvents(new FlyCheck(this, dataManager, violationManager), this);
        Bukkit.getPluginManager().registerEvents(new SpiderCheck(this, dataManager, violationManager), this);
        Bukkit.getPluginManager().registerEvents(new BaritoneCheck(this, dataManager, violationManager), this);
        Bukkit.getPluginManager().registerEvents(new ScaffoldCheck(this, dataManager, violationManager), this);
        Bukkit.getPluginManager().registerEvents(new AutoClickerCheck(this, dataManager, violationManager), this);
        Bukkit.getPluginManager().registerEvents(new SpeedCheck(this, dataManager, violationManager), this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(dataManager), this);

        // Команда
        getCommand("anticheat").setExecutor(new AntiCheatCommand(this, dataManager));

        // Задача угасания очков нарушений (decay), чтобы не копились навечно за старые случайные флаги
        int interval = getConfig().getInt("decay.interval-seconds", 30);
        double amount = getConfig().getDouble("decay.amount", 1.0);
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (PlayerData data : dataManager.all().values()) {
                data.violations.replaceAll((check, vl) -> Math.max(0.0, vl - amount));
            }
        }, interval * 20L, interval * 20L);

        getLogger().info("SimpleAC включен. Проверки: KillAura, Fly, Spider, Baritone, Scaffold, AutoClicker, Speed.");
    }

    @Override
    public void onDisable() {
        getLogger().info("SimpleAC выключен.");
    }
}
