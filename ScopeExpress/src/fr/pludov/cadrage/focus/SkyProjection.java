package fr.pludov.cadrage.focus;

import java.awt.geom.NoninvertibleTransformException;
import java.util.Arrays;

import org.w3c.dom.Element;

import fr.pludov.utils.XmlSerializationContext;

/**
 * Classe responsable de la projection du ciel sur un plan.
 * Pour l'instant, on a une projection polaire sur le nord uniquement.
 * Le seul param�tre ajustable est l'�chelle
 * 
 * 
 * Dans cette projection, l'�chelle est la taille angulaire d'un pixel
 * Cette taille est maximale au centre de la projection, et diminue progressivement � mesure qu'on s'en �loigne.
 *
 * Dec is -90/90 (s/n)
 * ra is 0-24h
 */
public class SkyProjection {

	// Multiplier une RA pour obtenir des radians
	public final static double raToRad = Math.PI / 180.0;
	public final static double decToRad = Math.PI / 180.0;
	public final static double epsilon = 1E-10;
	
	// Taille d'un pixel (unit�) en radian
	private double pixelRad;
	private double centerx, centery;
	
	/// Convertion ciel vers image
	AffineTransform3D transform;
	AffineTransform3D invertedTransform;
	
	public SkyProjection(double pixelArcSec) {
		this.pixelRad = 2 * Math.PI * pixelArcSec / (3600 * 360);
		this.centerx = 0;
		this.centery = 0;
		transform = new AffineTransform3D();
		try {
			invertedTransform = transform.invert();
		} catch(NoninvertibleTransformException e) {
			throw new RuntimeException("identity not invertible", e);
		}
	}

	/**
	 * @param from
	 * @param rotationMatrix passage de unprojected3d � projected3d
	 */
	public SkyProjection(SkyProjection from, AffineTransform3D rotationMatrix) {
		this.pixelRad = from.pixelRad;
		this.centerx = from.centerx;
		this.centery = from.centery;
		this.invertedTransform = rotationMatrix;
		try {
			transform = rotationMatrix.invert();
		} catch(NoninvertibleTransformException e) {
			throw new RuntimeException("identity not invertible", e);
		}
	}

	/**
	 * @param from
	 * @param rotationMatrix passage de unprojected3d � projected3d
	 */
	public SkyProjection(SkyProjection from) {
		this.pixelRad = from.pixelRad;
		this.centerx = from.centerx;
		this.centery = from.centery;
		this.invertedTransform = from.invertedTransform;
		this.transform = from.transform;
	}

	public Element save(XmlSerializationContext xsc)
	{
		Element result = xsc.newNode(SkyProjection.class.getSimpleName());
		xsc.setNodeAttribute(result, "pixelRad", this.getPixelRad());
		for(int i = 0; i < 12; ++i)
		{
			xsc.setNodeAttribute(result, "m"+i, transform.fact(i));
		}
		
		return result;
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
	
	// Projette une �toile sur la sphere 3D.
	// Dans cette projectino le pole nord pointe vers z (0,0,1).
	public static void convertRaDecTo3D(double [] i_radec, double [] o_rslt3d)
	{
		double ra = i_radec[0];
		double dec = i_radec[1];
		
		double x = Math.cos(raToRad * ra);
		double y = Math.sin(raToRad * ra);
		
		double zmul = Math.sin((90 - dec) * decToRad);
		double z = Math.cos((90 - dec) * decToRad);
		x *= zmul;
		y *= zmul;
		o_rslt3d[0] = x;
		o_rslt3d[1] = y;
		o_rslt3d[2] = z;
	}
	
	public static void convert3DToRaDec(double [] i_pt3d, double [] o_radec)
	{
		double x = i_pt3d[0];
		double y = i_pt3d[1];
		double z = i_pt3d[2];
		
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
		
		o_radec[0] = ra;
		o_radec[1] = dec;
	}
	
	/**
	 * Donne la distance en degr� entre deux point du ciel
	 */
	public static double getDegreeDistance(double [] raDec1, double [] raDec2)
	{
		double [] expected3d = new double[3];
		SkyProjection.convertRaDecTo3D(raDec1, expected3d);
		
		double [] found3d = new double[3];
		SkyProjection.convertRaDecTo3D(raDec2, found3d);
		
		double dst = Math.sqrt(
					(expected3d[0] - found3d[0]) * (expected3d[0] - found3d[0])
					+ (expected3d[1] - found3d[1]) * (expected3d[1] - found3d[1])
					+ (expected3d[2] - found3d[2]) * (expected3d[2] - found3d[2]));
		
		double raAngle = Math.asin(dst / 2);
		double angle = raAngle * 180 / Math.PI;
		
		return angle;
	}
	
	/**
	 * Transforme une distance en radian, en distance sur la sphere celeste.
	 */
	public static double radDst2Sky3dDst(double radDst)
	{
		double x = Math.cos(radDst);
		double y = Math.sin(radDst);
		double refX = 1;
		double refY = 0;
		
		x -= refX;
		y -= refY;
		
		
		return Math.sqrt(x * x + y * y);
	
	}
	
	/**
	 * Projete en 2D un point 3D sur lequel transform a d�j� �t� appliqu�.
	 */
	public boolean image3dToImage2d(double [] i_pos3d, double [] o_pos2d)
	{
		double x = i_pos3d[0];
		double y = i_pos3d[1];
		double z = i_pos3d[2];
		if (z < epsilon) {
			return false;
		}
		double iz = 1.0/(getPixelRad() * z);
		x *= iz;
		y *= iz;
		x += centerx;
		y += centery;
		o_pos2d[0] = x;
		o_pos2d[1] = y;
		
		return true;
		
	}
	
	/**
	 * Convertir ra/dec en coordonn�es sur la mosaic
	 * 
	 * x = cos(raToRad * ra) * tan((90 - dec) *decToRad) / pixelRad
	 * y = sin(raToRad * ra) * tan((90 - dec) *decToRad) / pixelRad
	 * @param i_o_radec
	 */
	public boolean project(double [] i_o_radec)
	{
		double [] pos3d = new double[3];
		convertRaDecTo3D(i_o_radec, pos3d);
		transform.convert(pos3d);

		double x = pos3d[0];
		double y = pos3d[1];
		double z = pos3d[2];
		if (z < epsilon) {
			return false;
		}
		double iz = 1.0/(getPixelRad() * z);
		x *= iz;
		y *= iz;
		x += centerx;
		y += centery;
		i_o_radec[0] = x;
		i_o_radec[1] = y;
		
		return true;
	}


	public void image2dToImage3d(double [] i_pos2d, double[] o_pos3d)
	{
		double x = (i_pos2d[0] - centerx) * getPixelRad();
		double y = (i_pos2d[1] - centery) * getPixelRad();
		
		double z3d = 1.0 / Math.sqrt(y*y + x*x + 1.0);
		double x3d = x * z3d;
		double y3d = y * z3d;
		o_pos3d[0] = x3d;
		o_pos3d[1] = y3d;
		o_pos3d[2] = z3d;
	}

	public boolean sky3dToImage2d(double [] i_xyz, double [] o_xy)
	{
		double [] copy = Arrays.copyOf(i_xyz, 3);
		transform.convert(copy);
		return image3dToImage2d(copy, o_xy);
	}
	
	public void image2dToSky3d(double [] i_xy, double [] o_xyz)
	{
		// On veut retrouver les coordonn�es 3D.
		// x = x3d / z3d
		// y = y3d / z3d
		// x3d * x3d + y3d * y3d + z3d * z3d = 1
		double x = (i_xy[0] - centerx) * getPixelRad();
		double y = (i_xy[1] - centery) * getPixelRad();
		
		double z3d = 1.0 / Math.sqrt(y*y + x*x + 1.0);
		double x3d = x * z3d;
		double y3d = y * z3d;
		
		o_xyz[0] = x3d;
		o_xyz[1] = y3d;
		o_xyz[2] = z3d;
		invertedTransform.convert(o_xyz);
		
	}

	public static void normalize(double [] i_o_vect) throws ArithmeticException
	{
		double norm = 0;
		for(int i = 0; i < i_o_vect.length; ++i) {
			norm += i_o_vect[i] * i_o_vect[i];
		}
		norm = 1.0/ Math.sqrt(norm);
		for(int i = 0; i < i_o_vect.length; ++i) {
			i_o_vect[i] *= norm;
		}
		for(int i = 0; i < i_o_vect.length; ++i) {
			double v = i_o_vect[i];
		
			if (Double.isInfinite(v) || Double.isNaN(v)) {
				throw new ArithmeticException("Unable to normalize vector");
			}
		}
	}
	
	/// image2d => radec
	public void unproject(double [] i_o_xy)
	{
		// On veut retrouver les coordonn�es 3D.
		// x = x3d / z3d
		// y = y3d / z3d
		// x3d * x3d + y3d * y3d + z3d * z3d = 1
		double x = (i_o_xy[0] - centerx) * getPixelRad();
		double y = (i_o_xy[1] - centery) * getPixelRad();
		
		double z3d = 1.0 / Math.sqrt(y*y + x*x + 1.0);
		double x3d = x * z3d;
		double y3d = y * z3d;
		
		double [] pt3d = new double[] {x3d, y3d, z3d};
		invertedTransform.convert(pt3d);
		convert3DToRaDec(pt3d, i_o_xy);
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

	/// Permet de passer de la skyProjection � l'image
	public AffineTransform3D getTransform() {
		return transform;
	}

	/// Taille d'un pixel (unit�) en radian
	public double getPixelRad() {
		return pixelRad;
	}

	public void setPixelRad(double pixelRad) {
		this.pixelRad = pixelRad;
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
}
