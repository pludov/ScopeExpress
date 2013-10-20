package fr.pludov.cadrage.ui.preferences;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class ConfigItem {
	Preferences prefs = loadPreferences();
	protected final String key;
	
	public ConfigItem(Class<?> packageLocation, String storageName) {
		key = packageLocation.getCanonicalName() + ":" + storageName;
	}
	
	public boolean exists()
	{
		return prefs.get(key, null) != null;
	}
	
	private static Preferences loadPreferences()
	{
		return Preferences.userNodeForPackage(ConfigItem.class);
	}
	
}
