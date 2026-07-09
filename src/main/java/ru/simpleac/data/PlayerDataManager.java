package ru.simpleac.data;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {

    private final Map<UUID, PlayerData> dataMap = new ConcurrentHashMap<>();

    public PlayerData get(UUID uuid) {
        return dataMap.computeIfAbsent(uuid, u -> new PlayerData());
    }

    public void remove(UUID uuid) {
        dataMap.remove(uuid);
    }

    public Map<UUID, PlayerData> all() {
        return dataMap;
    }
}
