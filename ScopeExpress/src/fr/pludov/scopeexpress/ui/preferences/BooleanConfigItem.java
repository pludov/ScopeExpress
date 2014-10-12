package fr.pludov.scopeexpress.ui.preferences;

public class BooleanConfigItem extends ConfigItem {
	final boolean defaultValue;
	
	public BooleanConfigItem(Class packageLocation, String storageName, boolean defaultValue) {
		super(packageLocation, storageName);
		this.defaultValue = defaultValue;
	}
	
	public boolean get()
	{
		return prefs.getBoolean(this.key, this.defaultValue);
	}

	public void set(boolean value)
	{
		prefs.putBoolean(this.key, value);
	}
}
