package net.blosson.lflagger;

import net.blosson.lflagger.checks.CheckManager;
import net.blosson.lflagger.config.ConfigManager;
import net.fabricmc.api.ClientModInitializer;

/**
 * The main entry point for the LocalFlagger mod on the client side.
 * <p>
 * This class is responsible for initializing the mod's core components when the
 * Minecraft client starts. It sets up a singleton instance of itself to provide
 * global access to the main manager classes.
 */
public class LocalFlaggerMod implements ClientModInitializer {

	private static LocalFlaggerMod INSTANCE;
	private CheckManager checkManager;
	private ConfigManager configManager;

	/**
	 * This method is called by Fabric when the client is initializing.
	 * It sets up the mod's core systems in the correct order.
	 */
	@Override
	public void onInitializeClient() {
		INSTANCE = this;
		// Initialize the configuration manager first to ensure settings are loaded
		// before any other components need them.
		this.configManager = ConfigManager.getInstance();
		// Initialize the check manager, which will dynamically load all checks.
		this.checkManager = new CheckManager();
	}

	/**
	 * Provides global access to the single instance of the mod's main class.
	 *
	 * @return The singleton instance of {@link LocalFlaggerMod}.
	 */
	public static LocalFlaggerMod getInstance() {
		return INSTANCE;
	}

	/**
	 * Provides global access to the check manager.
	 *
	 * @return The singleton instance of {@link CheckManager}.
	 */
	public CheckManager getCheckManager() {
		return checkManager;
	}
}