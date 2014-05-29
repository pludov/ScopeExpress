package fr.pludov.cadrage.ui.preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	
	public static List<String> getKeyCollection(Class<?> packageLocation, Pattern p)
	{
		String prefix = packageLocation.getCanonicalName() + ":";
		List<String> result = new ArrayList<String>();
		try {
			for(String s : loadPreferences().keys())
			{
				if (!s.substring(0,  prefix.length()).equals(prefix)) {
					continue;
				}
				
				String key = s.substring(prefix.length());
				Matcher m = p.matcher(key);
				if (m.matches()) {
					result.add(key);
				}
			}
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
		
		return result;
	}
}
