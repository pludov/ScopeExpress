package fr.pludov.scopeexpress.ui.preferences;

public class DoubleConfigItem extends ConfigItem {
	final double defaultValue;
	public DoubleConfigItem(Class<?> packageLocation, String storageName, double defaultValue) {
		super(packageLocation, storageName);
		this.defaultValue = defaultValue;
	}


	public double get()
	{
		return prefs.getDouble(this.key, this.defaultValue);
	}

	public void set(double value)
	{
		prefs.putDouble(this.key, value);
	}
}
