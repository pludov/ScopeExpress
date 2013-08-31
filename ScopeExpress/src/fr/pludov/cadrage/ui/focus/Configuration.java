package fr.pludov.cadrage.ui.focus;

import fr.pludov.cadrage.ui.preferences.BooleanConfigItem;
import fr.pludov.cadrage.ui.preferences.StringConfigItem;

public class Configuration {

	private double latitude;
	private double longitude;
	
	public Configuration() {
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	private static Configuration current;
	
	public static Configuration getCurrentConfiguration()
	{
		synchronized(Configuration.class)
		{
			if (current == null) {
				Configuration c = new Configuration();
				ConfigurationEdit.loadDefaults(c);
				current = c;
			}
			return current;
		}
		
	}
	
}
