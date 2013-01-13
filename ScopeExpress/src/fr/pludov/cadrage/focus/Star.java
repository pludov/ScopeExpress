package fr.pludov.cadrage.focus;

public class Star {

	String titre;
	
	// Les coordonnées de l'étoiles
	final int clickX, clickY;
	Image clickImage;
	boolean excludeFromStat;
	
	
	
	public Star(int clickX, int clickY, Image clickImage) {
		this.clickX = clickX;
		this.clickY = clickY;
		this.clickImage = clickImage;
		this.excludeFromStat = false;
	}

	public String getTitre() {
		return titre;
	}

	public void setTitre(String titre) {
		this.titre = titre;
	}

	public int getClickX() {
		return clickX;
	}

	public int getClickY() {
		return clickY;
	}

	public Image getClickImage() {
		return clickImage;
	}

	public void setClickImage(Image clickImage) {
		this.clickImage = clickImage;
	}

	public boolean isExcludeFromStat() {
		return excludeFromStat;
	}

	public void setExcludeFromStat(boolean excludeFromStat) {
		this.excludeFromStat = excludeFromStat;
	}

}
