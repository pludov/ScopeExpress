package fr.pludov.cadrage;

import java.io.File;
import java.util.List;

import fr.pludov.cadrage.utils.WeakListenerCollection;

public class Image {
	public final WeakListenerCollection<ImageListener> listeners;
	
	File file;
	
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

	void setStars(List<ImageStar> stars) {
		this.stars = stars;
	}
	
}
