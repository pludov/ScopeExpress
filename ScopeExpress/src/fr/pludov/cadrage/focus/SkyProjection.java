package fr.pludov.cadrage.focus;

/**
 * Classe responsable de la projection du ciel sur un plan.
 * Pour l'instant, on a une projection polaire sur le nord uniquement.
 * Le seul param�tre ajustable est l'�chelle
 * 
 * 
 * Dans cette projection, l'�chelle est la taille angulaire d'un pixel
 * Cette taille est maximale au centre de la projection, et diminue progressivement � mesure qu'on s'en �loigne.
 */
public class SkyProjection {

	// Multiplier une RA pour obtenir des radians
	public final double raToRad = Math.PI / 180.0;
	public final double decToRad = Math.PI / 180.0;

	// Taille d'un pixel en arcsec
	double pixelArcSec;
	// Taille d'un pixel (unit�) en radian
	double pixelRad;
	
	public SkyProjection(double pixelArcSec) {
		pixelRad = 2 * Math.PI * pixelArcSec / (3600 * 360);

	}

	public void project(double [] radec)
	{
		double ra = radec[0];
		double dec = radec[1];
		
		double angle = raToRad * ra;
		double anglePolaire = (90 - dec) *decToRad;
		
		double x = Math.cos(angle);
		double y = Math.sin(angle);
		
		double mult = Math.tan(anglePolaire);
		mult /= pixelRad;
		// Facteur d'�chelle, ad-hoc
		// mult *= 10240 / radius;
		
		x *= mult;
		y *= mult;
		radec[0] = x;
		radec[1] = y;
	}
	
	
}
