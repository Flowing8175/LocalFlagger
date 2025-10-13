package net.blosson.lflagger.data;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Deque;
import java.util.LinkedList;

public class PlayerData {

    public final long clientTimestamp;
    public final Vec3d position;
    public final Vec3d lastPosition;
    public final Box boundingBox;
    public final boolean onGround;
    public final float fallDistance;
    public final int serverPing;
    public final float serverTps;
    public final Deque<PositionSnapshot> positionHistory;

    public PlayerData(PlayerEntity player, PlayerData lastData, float currentTps) {
        this.clientTimestamp = System.currentTimeMillis();
        this.position = player.getPos();
        this.boundingBox = player.getBoundingBox();
        this.onGround = player.isOnGround();
        this.fallDistance = (float) player.fallDistance;
        this.serverTps = currentTps;

        if (player instanceof ClientPlayerEntity) {
            ClientPlayerEntity localPlayer = (ClientPlayerEntity) player;
            if (localPlayer.networkHandler != null && localPlayer.networkHandler.getPlayerListEntry(localPlayer.getUuid()) != null) {
                this.serverPing = localPlayer.networkHandler.getPlayerListEntry(localPlayer.getUuid()).getLatency();
            } else {
                this.serverPing = 0;
            }
        } else {
            this.serverPing = 0;
        }

        if (lastData != null) {
            this.lastPosition = lastData.position;
            this.positionHistory = new LinkedList<>(lastData.positionHistory);
        } else {
            this.lastPosition = player.getPos();
            this.positionHistory = new LinkedList<>();
        }

        this.positionHistory.add(new PositionSnapshot(this.clientTimestamp, this.position));
        if (this.positionHistory.size() > 20) {
            this.positionHistory.poll();
        }
    }
}