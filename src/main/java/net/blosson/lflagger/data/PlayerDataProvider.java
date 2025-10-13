package net.blosson.lflagger.data;

import java.util.Queue;

public interface PlayerDataProvider {
    Queue<PlayerData> getPredictionBuffer();
}