package net.blosson.lflagger;

import net.blosson.lflagger.checks.CheckManager;
import net.fabricmc.api.ClientModInitializer;

public class LocalFlaggerMod implements ClientModInitializer {

	private static LocalFlaggerMod INSTANCE;
	private CheckManager checkManager;

	@Override
	public void onInitializeClient() {
		INSTANCE = this;
		this.checkManager = new CheckManager();
	}

	public static LocalFlaggerMod getInstance() {
		return INSTANCE;
	}

	public CheckManager getCheckManager() {
		return checkManager;
	}
}