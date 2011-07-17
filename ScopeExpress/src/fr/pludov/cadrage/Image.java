package fr.pludov.cadrage;

import java.io.File;
import java.util.List;

import fr.pludov.cadrage.utils.WeakListenerCollection;

public class Image {
	public final WeakListenerCollection<ImageListener> listeners;
	
	File file;
	
	// Position du téléscope ?
	boolean scopePosition;
	double ra, dec;
	
	//
	public double expoComposensation;
	
	// Valeur ajoutée au gamma : gamma_effectif = 10 ^ (this.gamma / 100)
	public double gamma;
	
	public double black;
	
	// Les étoiles détéctées
	private List<ImageStar> stars;
	
	// Géometrie... Uniquement valide si stars est != null
	int width, height;
	
	public Image(File file)
	{
		this.file = file;
		this.setStars(null);
		this.listeners = new WeakListenerCollection<ImageListener>(ImageListener.class);
		this.expoComposensation = 0;
		this.gamma = 0.0;
		this.black = 0.0;
		
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
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
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
	
}
