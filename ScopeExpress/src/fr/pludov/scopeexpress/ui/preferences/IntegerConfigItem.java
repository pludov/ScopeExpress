package fr.pludov.scopeexpress.ui.preferences;

public class IntegerConfigItem extends ConfigItem {
	final int defaultValue;

	public IntegerConfigItem(Class<?> packageLocation, String storageName, int defaultValue) {
		super(packageLocation, storageName);
		this.defaultValue = defaultValue;
	}

	public int get()
	{
		return prefs.getInt(this.key, this.defaultValue);
	}

	public void set(int value)
	{
		prefs.putInt(this.key, value);
	}
}
