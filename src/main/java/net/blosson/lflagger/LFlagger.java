package net.blosson.lflagger;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LFlagger implements ClientModInitializer {
    public static final String MOD_ID = "lflagger";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("LFlagger initialized. Managers will be engaged via mixins.");
    }
}