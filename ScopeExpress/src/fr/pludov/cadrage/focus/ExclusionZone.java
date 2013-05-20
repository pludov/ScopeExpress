package fr.pludov.cadrage.focus;

import org.w3c.dom.Element;

import fr.pludov.utils.XmlSerializationContext;

/**
 * Permet d'exclure des zones de la projection (exemple: galaxies, etoiles doubles, ...)
 * 
 * Pour l'instant, pas de listener, donc ne pas faire de modif sur les instances.
 */
public class ExclusionZone {
	double centerx, centery;
	double rayon;
	
	public ExclusionZone() {
	}

	public boolean intersect(Star star)
	{
		if (star.getPositionStatus() == StarCorrelationPosition.None) return false;
		double scx = star.getCorrelatedX();
		double scy = star.getCorrelatedY();
		double dltx = Math.abs(scx - centerx);
		if (dltx > rayon) return false;
		double dlty = Math.abs(scy - centery);
		if (dlty > rayon) return false;
		
		double d2 = dltx*dltx + dlty * dlty;
		if (d2 > rayon * rayon) return false;
		
		return true;
	}
	
	public Element save(XmlSerializationContext xsc)
	{
		Element result = xsc.newNode(ExclusionZone.class.getSimpleName());
		
		xsc.setNodeAttribute(result, "centerX", centerx);
		xsc.setNodeAttribute(result, "centerY", centery);
		xsc.setNodeAttribute(result, "ray", rayon);
		return result;
		
	}

	public double getCenterx() {
		return centerx;
	}

	public void setCenterx(double centerx) {
		this.centerx = centerx;
	}

	public double getCentery() {
		return centery;
	}

	public void setCentery(double centery) {
		this.centery = centery;
	}

	public double getRayon() {
		return rayon;
	}

	public void setRayon(double rayon) {
		this.rayon = rayon;
	}
}
