package fr.pludov.cadrage;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import fr.pludov.cadrage.ui.utils.Utils;
import fr.pludov.cadrage.utils.WeakListenerCollection;

public class Image implements Serializable {

	private static final long serialVersionUID = 2030293814683489896L;

	public transient WeakListenerCollection<ImageListener> listeners;
	
	File file;
	
	// jamais null, jamais modifi�
	ImageDisplayParameter displayParameter;
	
	// Position issue du t�l�scope ?
	boolean scopePosition;
	double ra, dec;
	
	// Temps de pause en seconde.
	Double expositionDuration;

	// Valeur d'iso.
	Integer iso;
	
	//
	public double expoComposensation;
	
	// Valeur ajout�e au gamma : gamma_effectif = 10 ^ (this.gamma / 100)
	public double gamma;
	
	public double black;
	
	// Les �toiles d�t�ct�es
	private List<ImageStar> stars;
	
	// G�ometrie... Uniquement valide si stars est != null
	int width, height;
	
	public Image(File file)
	{
		this.file = file;
		this.setStars(null);
		this.listeners = new WeakListenerCollection<ImageListener>(ImageListener.class);
		this.expoComposensation = 0;
		this.gamma = 0.0;
		this.black = 0.0;
		this.expositionDuration = null;
		this.displayParameter = new ImageDisplayParameter();
		
	}


	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
	    in.defaultReadObject();
	    this.listeners = new WeakListenerCollection<ImageListener>(ImageListener.class);
	    if (this.displayParameter == null) this.displayParameter = new ImageDisplayParameter();
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public List<ImageStar> getStars() {
		return stars;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		if (this.width == width) return;
		this.width = width;
		listeners.getTarget().metadataChanged(this);
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		if (this.height == height) return;
		this.height = height;
		listeners.getTarget().metadataChanged(this);
	}

	public void setStars(List<ImageStar> stars) {
		this.stars = stars;
	}

	public boolean isScopePosition() {
		return scopePosition;
	}

	public void setScopePosition(boolean scopePosition) {
		this.scopePosition = scopePosition;
	}

	public double getRa() {
		return ra;
	}

	public void setRa(double ra) {
		if (ra == this.ra) return;
		this.ra = ra;
		if (scopePosition) {
			listeners.getTarget().scopePositionChanged(this);
		}
	}

	public double getDec() {
		return dec;
	}

	public void setDec(double dec) {
		if (dec == this.dec) return;
		this.dec = dec;
		if (scopePosition) {
			listeners.getTarget().scopePositionChanged(this);
		}
	}

	public double getExpoComposensation() {
		return expoComposensation;
	}

	public void setExpoComposensation(double expoComposensation) {
		if (this.expoComposensation == expoComposensation) return;
		this.expoComposensation = expoComposensation;
		listeners.getTarget().levelChanged(this);
	}

	public double getGamma() {
		return gamma;
	}

	public void setGamma(double gamma) {
		if (this.gamma == gamma) return;
		this.gamma = gamma;
		listeners.getTarget().levelChanged(this);

	}

	public double getBlack() {
		return black;
	}

	public void setBlack(double black) {
		if (this.black == black) return;
		this.black = black;
		listeners.getTarget().levelChanged(this);
	}

	public Double getPause() {
		return expositionDuration;
	}

	public void setPause(Double pause) {
		if (Utils.equalsWithNullity(this.expositionDuration, pause)) return;
		this.expositionDuration = pause;
		listeners.getTarget().metadataChanged(this);
	}
	
	public Integer getIso() {
		return iso;
	}
	
	public void setIso(Integer iso) {
		if (Utils.equalsWithNullity(this.iso, iso)) return;
		this.iso = iso;
		listeners.getTarget().metadataChanged(this);
	}


	public ImageDisplayParameter getDisplayParameter() {
		return displayParameter;
	}
	
}
