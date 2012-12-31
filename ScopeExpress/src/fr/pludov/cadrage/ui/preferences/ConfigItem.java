package fr.pludov.cadrage.ui.preferences;

import java.util.prefs.Preferences;

public class ConfigItem {
	Preferences prefs = loadPreferences();
	
	protected final String key;
	
	public ConfigItem(Class packageLocation, String storageName) {
		key = packageLocation.getCanonicalName() + ":" + storageName;
	}
	
	private static Preferences loadPreferences()
	{
		return Preferences.userNodeForPackage(ConfigItem.class);
	}
	
}
