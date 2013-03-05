package fr.pludov.cadrage.focus;

public class Star {

	String titre;
	
	// Les coordonnées de l'étoiles
	final int clickX, clickY;
	Image clickImage;
	boolean excludeFromStat;
	
	StarCorrelationPosition positionStatus;
	double correlatedX, correlatedY;

	double magnitude;
	
	
	public Star(int clickX, int clickY, Image clickImage) {
		this.clickX = clickX;
		this.clickY = clickY;
		this.clickImage = clickImage;
		this.excludeFromStat = false;
		this.positionStatus = StarCorrelationPosition.None;
		this.correlatedX = 0;
		this.correlatedY = 0;
		this.magnitude = Double.NaN;
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
		this.positionStatus = StarCorrelationPosition.Deducted;
		this.correlatedX = x;
		this.correlatedY = y;
	}

	public void unsetCorrelatedPos()
	{
		this.positionStatus = StarCorrelationPosition.None;
	}

	public double getCorrelatedX() {
		return correlatedX;
	}

	public double getCorrelatedY() {
		return correlatedY;
	}

	public StarCorrelationPosition getPositionStatus() {
		return positionStatus;
	}

	public void setPositionStatus(StarCorrelationPosition positionStatus) {
		this.positionStatus = positionStatus;
	}

	public double getMagnitude() {
		return magnitude;
	}

	public void setMagnitude(double magnitude) {
		this.magnitude = magnitude;
	}
}
