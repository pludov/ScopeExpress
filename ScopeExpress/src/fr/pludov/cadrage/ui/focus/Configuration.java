package fr.pludov.cadrage.ui.focus;

import fr.pludov.cadrage.ui.preferences.BooleanConfigItem;
import fr.pludov.cadrage.ui.preferences.StringConfigItem;

public class Configuration {

	private double latitude;
	private double longitude;
	
	private double focal;
	private double pixelSize;
	
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

	public double getFocal() {
		return focal;
	}

	public void setFocal(double focal) {
		this.focal = focal;
	}

	public double getPixelSize() {
		return pixelSize;
	}

	public void setPixelSize(double pixelSize) {
		this.pixelSize = pixelSize;
	}

	
}
