package fr.pludov.cadrage.ui.preferences;

public class EnumConfigItem<ENUM extends Enum<ENUM>> extends ConfigItem {
	final Class<ENUM> enumClass;
	protected final ENUM defaultValue;
	
	
	public EnumConfigItem(Class<?> packageLocation, String storageName, Class<ENUM> enumClass, ENUM defaultValue) {

		super(packageLocation, storageName);
		this.enumClass = enumClass;
		this.defaultValue = defaultValue;
	}

	public ENUM get()
	{
		String value = prefs.get(this.key, "");
		try {
			return Enum.valueOf(this.enumClass, value);
		} catch(IllegalArgumentException e) {
			return defaultValue;
		}
	}

	public void set(ENUM value)
	{
		prefs.put(this.key, value.name());
	}

}
