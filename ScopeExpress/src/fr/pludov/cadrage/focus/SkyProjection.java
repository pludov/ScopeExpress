package fr.pludov.cadrage.focus;

import java.awt.geom.NoninvertibleTransformException;

/**
 * Classe responsable de la projection du ciel sur un plan.
 * Pour l'instant, on a une projection polaire sur le nord uniquement.
 * Le seul paramètre ajustable est l'échelle
 * 
 * 
 * Dans cette projection, l'échelle est la taille angulaire d'un pixel
 * Cette taille est maximale au centre de la projection, et diminue progressivement à mesure qu'on s'en éloigne.
 *
 * Dec is -90/90 (s/n)
 * ra is 0-24h
 */
public class SkyProjection {

	// Multiplier une RA pour obtenir des radians
	public final static double raToRad = Math.PI / 180.0;
	public final static double decToRad = Math.PI / 180.0;
	public final static double epsilon = 1E-10;
	
	// Taille d'un pixel en arcsec
	double pixelArcSec;
	// Taille d'un pixel (unité) en radian
	double pixelRad;

	AffineTransform3D transform;
	AffineTransform3D invertedTransform;
	
	public SkyProjection(double pixelArcSec) {
		pixelRad = 2 * Math.PI * pixelArcSec / (3600 * 360);
		transform = new AffineTransform3D();
		try {
			invertedTransform = transform.invert();
		} catch(NoninvertibleTransformException e) {
			throw new RuntimeException("identity not invertible", e);
		}
	}

	
	public void setTransform(AffineTransform3D transform)
	{
		try {
			this.invertedTransform = transform.invert();
			this.transform = transform;
		} catch(NoninvertibleTransformException e) {
			throw new RuntimeException("parameter not invertible", e);
		}

	}
	
	// Projette une étoile sur la sphere 3D.
	// Dans cette projectino le pole nord pointe vers z (0,0,1).
	public static void convertRaDecTo3D(double [] radec, double [] rslt3d)
	{
		double ra = radec[0];
		double dec = radec[1];
		
		double x = Math.cos(raToRad * ra);
		double y = Math.sin(raToRad * ra);
		
		double zmul = Math.sin((90 - dec) * decToRad);
		double z = Math.cos((90 - dec) * decToRad);
		x *= zmul;
		y *= zmul;
		rslt3d[0] = x;
		rslt3d[1] = y;
		rslt3d[2] = z;
	}
	
	public static void convert3DToRaDec(double [] pt3d, double [] radec)
	{
		double x = pt3d[0];
		double y = pt3d[1];
		double z = pt3d[2];
		
		// z = cos((90 - dec) * decToRad)
		// (90 - dec) * decToRad = cos-1(z)
		// (90 - dec) = cos-1(z) / decToRad
		// dec = 90 - cos-1(z) / decToRad
		double dec = 90 - Math.acos(z) / decToRad;
		double ra;
		if (x*x + y*y > epsilon * epsilon) {
			double raRad = Math.atan2(y, x);
			if (raRad < 0) raRad += 2 * Math.PI;
			ra = raRad / raToRad;
		} else {
			ra = 0;
		}
		
		radec[0] = ra;
		radec[1] = dec;
	}
	/**
	 * Projete en 2D un point 3D sur lequel transform a déjà été appliqué.
	 */
	public boolean projectPreTransformed3d(double [] pos3d, double [] pos2d)
	{
		double x = pos3d[0];
		double y = pos3d[1];
		double z = pos3d[2];
		if (z < epsilon) {
			return false;
		}
		double iz = 1.0/(pixelRad * z);
		x *= iz;
		y *= iz;
		pos2d[0] = x;
		pos2d[1] = y;
		
		return true;
		
	}
	
	/**
	 * Convertir ra/dec en coordonnées sur la mosaic
	 * 
	 * x = cos(raToRad * ra) * tan((90 - dec) *decToRad) / pixelRad
	 * y = sin(raToRad * ra) * tan((90 - dec) *decToRad) / pixelRad
	 * @param radec
	 */
	public boolean project(double [] radec)
	{
		double [] pos3d = new double[3];
		convertRaDecTo3D(radec, pos3d);
		transform.convert(pos3d);

		double x = pos3d[0];
		double y = pos3d[1];
		double z = pos3d[2];
		if (z < epsilon) {
			return false;
		}
		double iz = 1.0/(pixelRad * z);
		x *= iz;
		y *= iz;
		radec[0] = x;
		radec[1] = y;
		
		return true;
	}


	public void unprojectPreTransformed3d(double [] pos2d, double[] pos3d)
	{
		double x = pos2d[0] * pixelRad;
		double y = pos2d[1] * pixelRad;
		
		double z3d = 1.0 / Math.sqrt(y*y + x*x + 1.0);
		double x3d = x * z3d;
		double y3d = y * z3d;
		pos3d[0] = x3d;
		pos3d[1] = y3d;
		pos3d[2] = z3d;
	}
	

	public void unproject(double [] xy)
	{
		// On veut retrouver les coordonnées 3D.
		// x = x3d / z3d
		// y = y3d / z3d
		// x3d * x3d + y3d * y3d + z3d * z3d = 1
		double x = xy[0]  * pixelRad;
		double y = xy[1]  * pixelRad;
		
		double z3d = 1.0 / Math.sqrt(y*y + x*x + 1.0);
		double x3d = x * z3d;
		double y3d = y * z3d;
		
		double [] pt3d = new double[] {x3d, y3d, z3d};
		invertedTransform.convert(pt3d);
		convert3DToRaDec(pt3d, xy);
//		
//		double y3d = (Math.sqrt(((y*y)/((y*y) + (x*x) + 1.0)), (1.0/2.0))*sign1);
//		
//		double dst = Math.sqrt(x*x + y*y);
//		if (dst <= espilon) {
//			xy[0] = 0;
//			xy[1] = 90;
//			return;
//		}
//		
//		// tan((90 - dec) *decToRad) / pixelRad = dst
//		// tan((90 - dec) *decToRad) = dst * pixelRad
//		// (90 - dec) *decToRad = arctan(dst * pixelRad)
//		// (90 - dec) = arctan(dst * pixelRad) / decToRad
//		// dec = 90 - arctan(dst * pixelRad) / decToRad
//		double dec = 90 - Math.atan(dst * pixelRad) / decToRad;
//		
//		// x / dst = cos(raToRad * ra);
//		// y / dst = sin(raToRad * ra);
//		double raRad = Math.atan2(y, x);
//		if (raRad < 0) raRad += Math.PI;
//		double ra = raRad / raToRad;
//		xy[0] = ra;
//		xy[1] = dec;
	}

	public AffineTransform3D getTransform() {
		return transform;
	}
	
}
