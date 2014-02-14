package fr.pludov.cadrage.focus;

import org.w3c.dom.Element;

import fr.pludov.utils.XmlSerializationContext;

public class Star {
	
	// Les coordonnées de l'étoiles
	final int clickX, clickY;
	Image clickImage;
	boolean excludeFromStat;
	
	StarCorrelationPosition positionStatus;
	// Pour l'instant, relatif à la mosaic. Plus tard, sera relatif au pole où à la première image
	double [] sky3dPosition;

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
		this.sky3dPosition = new double[3];
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
		xsc.setNodeAttribute(result, "skyX", this.sky3dPosition[0]);
		xsc.setNodeAttribute(result, "skyY", this.sky3dPosition[1]);
		xsc.setNodeAttribute(result, "skyZ", this.sky3dPosition[2]);
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

	public void setCorrelatedPos(double [] sky3d)
	{
		this.positionStatus = StarCorrelationPosition.Deducted;
		for(int i = 0 ; i < 3; ++i) {
			this.sky3dPosition[i] = sky3d[i];
		}
	}

	public void unsetCorrelatedPos()
	{
		this.positionStatus = StarCorrelationPosition.None;
	}

	public double [] getSky3dPosition()
	{
		return this.sky3dPosition;
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
