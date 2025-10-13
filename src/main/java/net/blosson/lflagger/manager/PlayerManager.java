package net.blosson.lflagger.manager;

import net.blosson.lflagger.data.PlayerData;
import net.minecraft.client.network.OtherClientPlayerEntity;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager {

    private static final PlayerManager INSTANCE = new PlayerManager();
    private final Map<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();

    private PlayerManager() {}

    public static PlayerManager getInstance() {
        return INSTANCE;
    }

    public void onPlayerTick(OtherClientPlayerEntity player) {
        // This method will be called from a mixin on every other player's tick.
        UUID uuid = player.getUuid();
        PlayerData lastData = playerDataMap.get(uuid);
        float tps = TickManager.getInstance().getCurrentTps();

        PlayerData newData = new PlayerData(player, lastData, tps);
        playerDataMap.put(uuid, newData);

        // TODO: Run checks here
    }

    public void onPlayerLeave(UUID uuid) {
        playerDataMap.remove(uuid);
    }
}