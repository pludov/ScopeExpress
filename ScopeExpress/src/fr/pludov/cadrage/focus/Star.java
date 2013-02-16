package fr.pludov.cadrage.focus;

public class Star {

	String titre;
	
	// Les coordonnées de l'étoiles
	final int clickX, clickY;
	Image clickImage;
	boolean excludeFromStat;
	
	boolean hasCorrelatedPos;
	double correlatedX, correlatedY;
	
	
	
	public Star(int clickX, int clickY, Image clickImage) {
		this.clickX = clickX;
		this.clickY = clickY;
		this.clickImage = clickImage;
		this.excludeFromStat = false;
		this.hasCorrelatedPos = false;
		this.correlatedX = 0;
		this.correlatedY = 0;
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

	public void setCorrelatedPos(double x, double y)
	{
		this.hasCorrelatedPos = true;
		this.correlatedX = x;
		this.correlatedY = y;
	}

	public void unsetCorrelatedPos()
	{
		this.hasCorrelatedPos = false;
	}

	public boolean isHasCorrelatedPos() {
		return hasCorrelatedPos;
	}

	public double getCorrelatedX() {
		return correlatedX;
	}

	public double getCorrelatedY() {
		return correlatedY;
	}
}
