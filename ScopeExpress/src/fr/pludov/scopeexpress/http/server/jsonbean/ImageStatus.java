package fr.pludov.scopeexpress.http.server.jsonbean;

public class ImageStatus implements Cloneable {

	public String name;
	
	public boolean starSearched;
	public int starCount;
	
	/** FWHM */
	public Double fwhm;
	
	/** TODO */
	public boolean correlated;
	/** TODO: Distance depuis la premi�re image correll�e */
	public double distance;
	/** TODO: Angle par rapport � la premi�re image correll�e */
	public double angle;

	/** D�but de la photo */
	public Double date;
	/** Dur�e de la photo */
	public Double duration;

	/** Temp�rature de prise de vue */
	public Double temp;
	
	public ImageStatus() {
	}

}
