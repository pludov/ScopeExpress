package fr.pludov.scopeexpress.ui.preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigItem {
	Preferences prefs;
	Class<?> packageLocation;
	protected String key;
	
	public ConfigItem(Class<?> packageLocation, String storageName) {
		key = storageName;
		this.packageLocation = packageLocation;
		prefs = loadPreferences();
	}
	
	public void setKey(String key)
	{
		this.key = key;
	}
	
	public boolean exists()
	{
		return prefs.get(key, null) != null;
	}
	
	private Preferences loadPreferences()
	{
		return Preferences.userNodeForPackage(packageLocation);
	}
	
	public static List<String> getKeyCollection(Class<?> packageLocation, Pattern p)
	{
		List<String> result = new ArrayList<String>();
		try {
			for(String s : Preferences.userNodeForPackage(packageLocation).keys())
			{
//				if (!s.startsWith(prefix)) {
//					continue;
//				}
				
				String key = s;//.substring(prefix.length());
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

	public String getKey() {
		return key;
	}
}
