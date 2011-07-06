package fr.pludov.cadrage;

import java.io.File;
import java.util.List;

import fr.pludov.cadrage.utils.WeakListenerCollection;

public class Image {
	public final WeakListenerCollection<ImageListener> listeners;
	
	File file;
	
	// Est-ce que cette image va participer à la calibration ?
	boolean calibration;
	
	
	// Position du téléscope ?
	boolean scopePosition;
	double ra, dec;
	
	
	// Les étoiles détéctées
	private List<ImageStar> stars;
	
	// Géometrie... Uniquement valide si stars est != null
	int width, height;
	
	Image(File file)
	{
		this.file = file;
		this.setStars(null);
		this.listeners = new WeakListenerCollection<ImageListener>(ImageListener.class);
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

	public boolean isCalibration() {
		return calibration;
	}

	public void setCalibration(boolean calibration) {
		this.calibration = calibration;
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
		this.ra = ra;
	}

	public double getDec() {
		return dec;
	}

	public void setDec(double dec) {
		this.dec = dec;
	}
	
}
