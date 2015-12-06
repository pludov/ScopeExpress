package fr.pludov.scopeexpress.ui;

import java.io.File;

public class Configuration {

	private double latitude;
	private double longitude;
	
	private double focal;
	private double pixelSize;

	private String starCatalogPathTyc2;
	/** Chemin des indexes */
	private String astrometryNetPath;

	private boolean httpEnabled;
	private boolean ircEnabled;
	
	private boolean modified;
	
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

	public String getStarCatalogPathTyc2() {
		return starCatalogPathTyc2;
	}

	public void setStarCatalogPathTyc2(String starCatalogPathTyc2) {
		this.starCatalogPathTyc2 = starCatalogPathTyc2;
	}

	public File getTycho2Index()
	{
		if (starCatalogPathTyc2 == null || "".equals(starCatalogPathTyc2)) return null;
		return new File(new File(starCatalogPathTyc2).getParentFile(), "index.dat");
	}

	public File getTycho2Suppl1()
	{
		if (starCatalogPathTyc2 == null || "".equals(starCatalogPathTyc2)) return null;
		return new File(new File(starCatalogPathTyc2).getParentFile(), "suppl_1.dat");
	}

	public File getTycho2Suppl2()
	{
		if (starCatalogPathTyc2 == null || "".equals(starCatalogPathTyc2)) return null;
		return new File(new File(starCatalogPathTyc2).getParentFile(), "suppl_2.dat");
	}

	public File getTycho2Dat()
	{
		if (starCatalogPathTyc2 == null || "".equals(starCatalogPathTyc2)) return null;
		return new File(starCatalogPathTyc2);
	}
	
	private static File applicationDir;
	public synchronized static File getApplicationDataFolder()
	{
		if (applicationDir != null) return applicationDir;
		
		String target = System.getProperty("user.home");
		File dir = new File(target, ".ScopeExpress");
		if (!dir.exists()) {
			dir.mkdirs();
		}
		applicationDir = dir;
		return applicationDir;
	}

	public String getAstrometryNetPath() {
		return astrometryNetPath;
	}

	public void setAstrometryNetPath(String astrometryNetPath) {
		this.astrometryNetPath = astrometryNetPath;
	}

	public boolean isModified() {
		return modified;
	}

	public void setModified(boolean modified) {
		this.modified = modified;
	}

	public boolean isHttpEnabled() {
		return httpEnabled;
	}

	public void setHttpEnabled(boolean httpEnabled) {
		this.httpEnabled = httpEnabled;
	}

	public boolean isIrcEnabled() {
		return ircEnabled;
	}

	public void setIrcEnabled(boolean ircEnabled) {
		this.ircEnabled = ircEnabled;
	}
}
