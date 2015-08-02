package fr.pludov.scopeexpress.http.server.jsonbean;

public class ImageStatus implements Cloneable {

	public String name;
	
	public boolean starSearched;
	public int starCount;
	
	/** FWHM */
	public Double fwhm;
	
	/** TODO */
	public boolean correlated;
	/** TODO: Distance depuis la première image correllée */
	public double distance;
	/** TODO: Angle par rapport à la première image correllée */
	public double angle;

	/** Début de la photo */
	public Double date;
	/** Durée de la photo */
	public Double duration;

	/** Température de prise de vue */
	public Double temp;
	
	public ImageStatus() {
	}

}
