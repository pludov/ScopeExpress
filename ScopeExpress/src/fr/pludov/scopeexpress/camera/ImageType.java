package fr.pludov.scopeexpress.camera;

/**
 * Used in fits
 */
public enum ImageType {
	light,
	bias, 
	flat,
	dark;
	
	
	public String toFits()
	{
		return name().toUpperCase();
	}

}
