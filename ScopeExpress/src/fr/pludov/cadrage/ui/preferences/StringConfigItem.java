package fr.pludov.cadrage.ui.preferences;

public class StringConfigItem extends ConfigItem {
	final String defaultValue;
	
	public StringConfigItem(Class packageLocation, String storageName, String defaultValue) {
		super(packageLocation, storageName);
		this.defaultValue = defaultValue;
	}
	
	public String get()
	{
		return prefs.get(this.key, this.defaultValue);
	}

	public void set(String value)
	{
		prefs.put(this.key, value);
	}
}
