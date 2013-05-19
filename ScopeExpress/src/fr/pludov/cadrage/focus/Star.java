package fr.pludov.cadrage.focus;

import org.w3c.dom.Element;

import fr.pludov.utils.XmlSerializationContext;

public class Star {
	
	// Les coordonnées de l'étoiles
	final int clickX, clickY;
	Image clickImage;
	boolean excludeFromStat;
	
	StarCorrelationPosition positionStatus;
	double correlatedX, correlatedY;

	double magnitude;
	
	// Si l'étoile vient d'un catalogue... FIXME: quel format ?
	String reference;
	
	Mosaic mosaic;
	
	
	public Star(int clickX, int clickY, Image clickImage) {
		this.clickX = clickX;
		this.clickY = clickY;
		this.clickImage = clickImage;
		this.excludeFromStat = false;
		this.positionStatus = StarCorrelationPosition.None;
		this.correlatedX = 0;
		this.correlatedY = 0;
		this.magnitude = Double.NaN;
		this.mosaic = null;
		this.reference = null;
	}

	public Element save(XmlSerializationContext xsc, XmlSerializationContext.NodeDictionary<Image> images)
	{
		Element result = xsc.newNode(Star.class.getSimpleName());
		xsc.setNodeAttribute(result, "clickX", this.clickX);
		xsc.setNodeAttribute(result, "clickY", this.clickY);
		xsc.setNodeAttribute(result, "clickImage", clickImage != null ? images.getIdForObject(this.clickImage) : null);
		xsc.setNodeAttribute(result, "excludeFromStat", this.excludeFromStat);
		xsc.setNodeAttribute(result, "positionStatus", this.positionStatus.name());
		xsc.setNodeAttribute(result, "correlatedX", this.correlatedX);
		xsc.setNodeAttribute(result, "correlatedY", this.correlatedY);
		xsc.setNodeAttribute(result, "magnitude", this.magnitude);
		xsc.setNodeAttribute(result, "reference", this.reference);
		return result;
	}
	
	public String getReference() {
		return reference;
	}

	public void setReference(String titre) {
		this.reference = titre;
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
