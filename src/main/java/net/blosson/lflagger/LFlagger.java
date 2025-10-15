package net.blosson.lflagger;

import net.blosson.lflagger.checks.CheckManager;
import net.blosson.lflagger.config.ConfigManager;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LFlagger implements ClientModInitializer {
    public static final String MOD_ID = "lflagger";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static LFlagger INSTANCE;
    private CheckManager checkManager;
    private ConfigManager configManager;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        this.configManager = ConfigManager.getInstance();
        this.checkManager = new CheckManager();
        LOGGER.info("LFlagger initialized.");
    }

    public static LFlagger getInstance() {
        return INSTANCE;
    }

    public CheckManager getCheckManager() {
        return checkManager;
    }
}