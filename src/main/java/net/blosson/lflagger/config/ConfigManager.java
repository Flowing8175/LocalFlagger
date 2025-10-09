package net.blosson.lflagger.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Manages the loading and saving of the mod's configuration from a JSON file.
 * <p>
 * This class follows a singleton pattern to provide a single, globally accessible
 * point for all configuration data. It handles all file I/O and serialization/deserialization
 * between the {@link ModConfig} object and the {@code lflagger.json} file located in the
 * user's Minecraft config directory.
 */
public class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "lflagger.json");
    private static final ConfigManager INSTANCE = new ConfigManager();

    private ModConfig config;

    /**
     * Private constructor to enforce the singleton pattern.
     * It immediately loads the configuration upon instantiation.
     */
    private ConfigManager() {
        loadConfig();
    }

    /**
     * @return The singleton instance of the ConfigManager.
     */
    public static ConfigManager getInstance() {
        return INSTANCE;
    }

    /**
     * @return The currently loaded configuration object.
     */
    public ModConfig getConfig() {
        return config;
    }

    /**
     * Loads the configuration from the {@code lflagger.json} file.
     * <p>
     * If the file exists, it is parsed from JSON into the {@link ModConfig} object.
     * If the file is missing, corrupt, or empty, a new {@link ModConfig} object
     * is created with default values, and a new configuration file is saved to disk.
     */
    public void loadConfig() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                config = GSON.fromJson(reader, ModConfig.class);
                // If the file is empty or structurally invalid, fromJson can return null
                if (config == null) {
                    config = new ModConfig();
                }
            } catch (IOException | com.google.gson.JsonSyntaxException e) {
                System.err.println("[LFlagger] Failed to load or parse config file! It might be corrupt. Using default settings.");
                e.printStackTrace();
                // Use default config on failure
                config = new ModConfig();
            }
        } else {
            // If the config file doesn't exist, create one with default values
            config = new ModConfig();
            saveConfig();
        }
    }

    /**
     * Saves the current configuration object to the {@code lflagger.json} file.
     * The configuration is written in a human-readable "pretty-printed" JSON format.
     */
    public void saveConfig() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            System.err.println("[LFlagger] Failed to save config file!");
            e.printStackTrace();
        }
    }
}