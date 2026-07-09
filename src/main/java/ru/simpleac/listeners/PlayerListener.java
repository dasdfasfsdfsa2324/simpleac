package ru.simpleac.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.simpleac.data.PlayerDataManager;

public class PlayerListener implements Listener {

    private final PlayerDataManager dataManager;

    public PlayerListener(PlayerDataManager dataManager) {
        this.dataManager = dataManager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        dataManager.remove(event.getPlayer().getUniqueId());
    }
}
