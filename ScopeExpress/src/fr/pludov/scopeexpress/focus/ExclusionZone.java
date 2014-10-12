package fr.pludov.scopeexpress.focus;

import org.w3c.dom.Element;

import fr.pludov.utils.XmlSerializationContext;

/**
 * Permet d'exclure des zones de la projection (exemple: galaxies, etoiles doubles, ...)
 * 
 * Pour l'instant, pas de listener, donc ne pas faire de modif sur les instances.
 */
public class ExclusionZone {
	double [] sky3dPos;
	double rayon;
	// Correspond à la distance d'un segment au travers de la sphère d'unité
	double sky3dDst;
	
	public ExclusionZone() {
		sky3dPos = new double[3];
		rayon = 0;
	}

	public boolean intersect(Star star)
	{
		if (star.getPositionStatus() == StarCorrelationPosition.None) return false;
		
		double dltx = Math.abs(star.getSky3dPosition()[0] - sky3dPos[0]);
		if (dltx > sky3dDst) return false;
		
		double dlty = Math.abs(star.getSky3dPosition()[1] - sky3dPos[1]);
		if (dlty > sky3dDst) return false;
		
		double dltz = Math.abs(star.getSky3dPosition()[2] - sky3dPos[2]);
		if (dltz > sky3dDst) return false;
		
		
		double d2 = dltx*dltx + dlty * dlty + dltz * dltz;
		if (d2 > sky3dDst * sky3dDst) return false;
		
		return true;
	}
	
	public Element save(XmlSerializationContext xsc)
	{
		Element result = xsc.newNode(ExclusionZone.class.getSimpleName());
		
		xsc.setNodeAttribute(result, "centerX", sky3dPos[0]);
		xsc.setNodeAttribute(result, "centerY", sky3dPos[1]);
		xsc.setNodeAttribute(result, "centerZ", sky3dPos[2]);
		xsc.setNodeAttribute(result, "ray", rayon);
		return result;
		
	}

	public double getRayon() {
		return rayon;
	}

	/**
	 * Position le rayon d'exclusion en radian
	 * @param rayon
	 */
	public void setRayon(double rayon) {
		this.rayon = rayon;
		this.sky3dDst = SkyProjection.radDst2Sky3dDst(this.rayon);
	}

	public void setSky3dPosition(double[] sky3dPosition) {
		for(int i = 0; i < 3; ++i) {
			this.sky3dPos[i] = sky3dPosition[i];
		}
		
	}
}
