package fr.pludov.cadrage.focus.correlation;

import java.util.ArrayList;
import java.util.List;

import fr.pludov.cadrage.utils.CorrelationAlgo;
import fr.pludov.cadrage.utils.DynamicGrid;
import fr.pludov.cadrage.utils.DynamicGridPoint;
import fr.pludov.cadrage.utils.Ransac;

/**
 * Correlation est une collection d'images corrélée
 * 
 * Chaque image a un status et une position relative
 * 
 * Une liste des étoiles identifiées est gardée
 * 
 * 
 * @author Ludovic POLLET
 *
 */
public class Correlation {
	double tx, ty;
	double cs, sn;
	boolean found;
	
	public Correlation()
	{
		this.found = false;
	}
	
	public void correlate(List<? extends DynamicGridPoint> referenceStars, List<? extends DynamicGridPoint> imageStars)
	{
		int maxTriangle = 30000; // FIXME : c'est ad hoc...
		double starRay = 500; 	// Prendre en compte des triangles de au plus cette taille
		double starMinRay = 20;	// Elimine les petits triangles
		
		
		List<Triangle> referenceTriangle;
		
		List<Triangle> imageTriangle;
		
		do {
			System.err.println("Looking for source triangles, max size = " + starRay);
			referenceTriangle = getTriangleList(referenceStars, starMinRay, starRay, maxTriangle);
			if (referenceTriangle == null) {
				System.err.println("Too many triangles found, search for smaller ones");
				starRay *= 0.75;
				continue;
			}
			
			System.err.println("Looking for image triangles, max size = " + starRay);
			imageTriangle = getTriangleList(imageStars, starMinRay, starRay, maxTriangle);
			if (imageTriangle == null) {
				System.err.println("Too many triangles found in image, search for smaller ones");
				starRay *= 0.75;
				continue;
			}
			
			break;
		} while(true);
		
		DynamicGrid<Triangle> referenceTriangleGrid = new DynamicGrid<Triangle>(referenceTriangle);
		
		double tolerance = 0.005;
		int maxNbRansacPoints = 15000;
		
		List<RansacPoint> ransacPoints;
		
		while((ransacPoints = getRansacPoints(imageTriangle, referenceTriangleGrid, maxNbRansacPoints, tolerance)) == null)
		{
			System.err.println("Too many possible translations. Filter wiht more aggressive values...");
			tolerance *= 0.6;
		}
		
		
		System.err.println("Performing RANSAC with " + ransacPoints.size());
		
		CorrelationAlgo ransac = new Ransac();
		
		ransac.addEvaluator(new Ransac.AdditionalEvaluator() {
			@Override
			public double getEvaluator(Ransac.RansacPoint p) {
				return Math.sqrt(p.getRansacParameter(2) * p.getRansacParameter(2) + p.getRansacParameter(3) * p.getRansacParameter(3));
			}
		});

		
		ransac.addEvaluator(new Ransac.AdditionalEvaluator() {
			@Override
			public double getEvaluator(Ransac.RansacPoint p) {
				return Math.sqrt(p.getRansacParameter(0) * p.getRansacParameter(0) + p.getRansacParameter(1) * p.getRansacParameter(1));
			}
		});
		
		
//		double [] bestParameter = ransac.proceed(ransacPoints, 4, 
//				new double[] {	1024, 1024, 1, 1, 0.2, 100.0 },
//				0.1, 0.5);
		double [] bestParameter = ransac.proceed(ransacPoints, 4, 
				new double[] {	1024, 1024, 1, 1, 0.2, 100.0 },
				1 / Math.sqrt(ransacPoints.size()), 0.1);
		
		if (bestParameter == null) {
			throw new RuntimeException("Pas de corrélation trouvée");
		}
		
		
		System.err.println("Transformation is : translate:" + bestParameter[0]+"," + bestParameter[1]+
				" rotate=" + 180 * Math.atan2(bestParameter[3], bestParameter[2])/Math.PI +
				" scale=" + Math.sqrt(bestParameter[2] * bestParameter[2] + bestParameter[3] * bestParameter[3]));
		this.tx = bestParameter[0];
		this.ty = bestParameter[1];
		this.cs = bestParameter[2];
		this.sn = bestParameter[3];
		
		found = true;
	}

	private List<RansacPoint> getRansacPoints(
			List<Triangle> imageTriangle,
			DynamicGrid<Triangle> referenceTriangleGrid, 
			int maxCount, double distanceMax)
	{
		List<RansacPoint> ransacPoints = new ArrayList<RansacPoint>();
		double [] cssn1 = new double[3];
		double [] cssn2 = new double[3];
		double [] cssn3 = new double[3];
		
		for(Triangle t : imageTriangle)
		{
			// FIXME : ce radius devrait être vajusté en fonction du nombre de triangle, pour sortir suffisement de candidat
			// En même temps, le matching est absolu (on compare la précision des triangles)
			List<Triangle> correspondant = referenceTriangleGrid.getNearObject(t.x, t.y, distanceMax);
			
			
			int id = 1;
			for(Triangle c : correspondant)
			{
				c.calcRotation(t, 1, 2, cssn1);
				c.calcRotation(t, 1, 3, cssn2);
				c.calcRotation(t, 2, 3, cssn3);
				
				// Faire la moyenne des cos/sin, les rendre normé
				// Faire la moyenne des ratios.
				double cs = cssn1[0] + cssn2[0] + cssn3[0];
				double sn = cssn1[1] + cssn2[1] + cssn3[1];
				
				double angle = 180 * Math.atan2(sn, cs) / Math.PI;
				
				double div = Math.sqrt(cs * cs + sn * sn);
				div = 1.0 / div;
				cs *= div;
				sn *= div;
				
				
				
				double ratio = (cssn1[2] + cssn2[2] + cssn3[2]) / 3;
				cs *= ratio;
				sn *= ratio;
				
				double dlt = (cssn1[2] - ratio) * (cssn1[2] - ratio)  + (cssn2[2] - ratio) * (cssn2[2] - ratio) + + (cssn3[2] - ratio) * (cssn3[2] - ratio);
				
				if (dlt > 0.001) {
					// si les cos/sin ne sont pas orthogonaux, la translation déforme....
					continue;
				}
				
				// On  met un filtre sur le grossissement également
				if (ratio < 0.5 || ratio > 1.5) continue;
				
				double tx = 0, ty = 0;
				
				// Calcul de la translation
				for(int i = 1; i <= 3; ++i)
				{
					double xRef = t.getPointX(i);
					double yRef = t.getPointY(i);
					
					double xRotateScale = xRef * cs + yRef * sn; 
					double yRotateScale = yRef * cs - xRef * sn; 
					
					tx += c.getPointX(i) - xRotateScale;
					ty += c.getPointY(i) - yRotateScale;
				}
				
				tx /= 3;
				ty /= 3;
				
//				// Rotation
//				// On peut connaitre facilement le delta en divisant les distances
//				double ratio = (c.dst12 / t.dst12 + c.dst13 / t.dst13 + c.dst23 / t.dst23) / 3;
//				
//				// On va calculer l'angle de rotation sur le plus grand vecteur (dst23)
//				double Xa = c.s3.x - c.s2.x;
//				double Ya = c.s3.y - c.s2.y;
//				double Xb = t.s3.x - t.s2.x;
//				double Yb = t.s3.y - t.s2.y;
//				
//				double cos = (Xa*Xb+Ya*Yb)/(c.dst23*t.dst23);
//				double sin = (Xa*Yb-Ya*Xb)/(c.dst23*t.dst23);
//				
//				
//				
//				double tx = (c.s1.x + c.s2.x + c.s3.x - t.s1.x- t.s2.x- t.s3.x) / 3;
//				double ty = (c.s1.y - t.s1.y + c.s2.y - t.s2.y + c.s3.y - t.s3.y) / 3;
//			
				double delta = Math.sqrt((t.x - c.x)*(t.x - c.x) + (t.y - c.y) * (t.y - c.y));
				
				
				RansacPoint rp = new RansacPoint();
				rp.image = t;
				rp.original = c;
				rp.tx = tx;
				rp.ty = ty;
				rp.cs = cs;
				rp.sn = sn;
				
				ransacPoints.add(rp);
				
				System.err.println("Found possible translation (" + id+" ) " + tx +" - " + ty + " scale=" + ratio + ", angle="+angle+" with delta=" + delta);
				
				if (ransacPoints.size() > maxCount) {
					System.err.println("Too many translation founds. Retry with stricter filter");
					return null;
				}
				
				id++;
			}
		}
		return ransacPoints;
	}
	

	private List<Triangle> getTriangleList(List<? extends DynamicGridPoint> referenceStars, double minTriangleSize, double triangleSearchRadius, int maxTriangle)
	{
		// Trouver les points à moins de 50 pixels
		DynamicGrid<DynamicGridPoint> reference = new DynamicGrid<DynamicGridPoint>((List<DynamicGridPoint>)referenceStars);
		List<Triangle> result = new ArrayList<Triangle>();
		
		double [] i_d = new double[3];
		double [] r_d = new double[3];

		minTriangleSize = minTriangleSize * minTriangleSize;
		
		System.err.println("Searching for triangles in " + referenceStars.size() + " stars - minsize=" + Math.sqrt(minTriangleSize) + ", maxsize=" + triangleSearchRadius);
		for(DynamicGridPoint rst1 : referenceStars)
		{
			List<DynamicGridPoint> referencePeerList = reference.getNearObject(rst1.getX(), rst1.getY(), triangleSearchRadius);
			
			for(int a = 0; a < referencePeerList.size(); ++a)
			{
				DynamicGridPoint rst2 = referencePeerList.get(a);
				if (rst2 == rst1) continue;
				if (compareTo(rst1, rst2) >= 0) continue;
			
				double r_d1 = d2(rst1, rst2);
				if (r_d1 < minTriangleSize) continue;
				
				for(int b = a + 1; b < referencePeerList.size(); ++b)
				{
					DynamicGridPoint rst3 = referencePeerList.get(b);
					if (rst3 == rst1) continue;
					if (compareTo(rst2, rst3) >= 0) continue;
					
					r_d[0] = r_d1;
					r_d[1] = d2(rst1, rst3);
					r_d[2] = d2(rst2, rst3);
					
					if (r_d[0] < minTriangleSize || r_d[1] < minTriangleSize || r_d[2] < minTriangleSize) continue;
					
					Triangle t = new Triangle(rst1, rst2, rst3, r_d[0], r_d[1], r_d[2]);
					
					result.add(t);
					if (result.size() >  maxTriangle) {
						System.err.println("Found too many triangles... retry with stricter filter");
						return null;
					}
					System.err.println("Found triangle : " + Math.sqrt(r_d[0]) + " - " + Math.sqrt(r_d[1]) + " - " + Math.sqrt(r_d[2]));
				}
			}
		}
		
		System.err.println("Found " + result.size());
		
		return result;
	}

	// Permet d'avoir un ordre arbitraire sur les points pour éviter de traiter tous les triangles en x3.
	private static int compareTo(DynamicGridPoint a, DynamicGridPoint b)
	{
		double ax, bx;
		ax = a.getX();
		bx = b.getX();
		if (ax <  bx) {
			return 1;
		}
		if (ax > bx) {
			return -1;
		}
		double ay, by;
		ay = a.getY();
		by = b.getY();
		if (ay <  by) {
			return 1;
		}
		if (ay > by) {
			return -1;
		}
		
		return 0;
	}

	private static class Triangle implements DynamicGridPoint
	{
		// Plus grande distance : s2-s3
		// Plus petite distance : s1-s2
		DynamicGridPoint s1, s2, s3;
		
		double dst12, dst13, dst23;
		
		// Les rapports
		double x, y;
		
		Triangle(DynamicGridPoint s1, DynamicGridPoint s2, DynamicGridPoint s3, double dst12, double dst13, double dst23)
		{
			this.s1 = s1;
			this.s2 = s2;
			this.s3 = s3;
			
			if (dst12 > dst13 && dst12 > dst23)
			{
				// Le plus grand segment est 1-2. On echange 1 et 3
				DynamicGridPoint tmpStar = this.s1;
				this.s1 = this.s3;
				this.s3 = tmpStar;
				
				// Echange dst12 avec dst23. dst13 est inchangé
				double tmp = dst12;
				dst12 = dst23;
				dst23 = tmp;
			} else if (dst13 > dst12 && dst13 > dst23) {
				// Le plus grand segment est 1-3. On echange 1 et 2.
				DynamicGridPoint tmpStar = this.s1;
				this.s1 = this.s2;
				this.s2 = tmpStar;
				
				// Echange dst13 avec dst23. dst12 est inchangé
				double tmp = dst13;
				dst13 = dst23;
				dst23 = tmp;
			}
			
			//dst23 est maintenant le plus grand
			if (dst23 < dst12 || dst23 < dst13) {
				throw new RuntimeException("Ca marche pas ton truc !");
			}
				
			// On veut maintenant que le plus petit soit dst12.
			if (dst12 > dst13) {
				// Echanger 2 et 3. dst23 reste inchangé
				DynamicGridPoint tmpStar = this.s2;
				this.s2 = this.s3;
				this.s3 = tmpStar;
				
				double tmp = dst13;
				dst13 = dst12;
				dst12 = tmp;
			}
			

			//dst12 est maintenant le plus petit
			if (dst12 > dst23 || dst12 > dst13) {
				throw new RuntimeException("Ca marche pas ton truc ! (2)");
			}
			
			this.dst12 = Math.sqrt(dst12);
			this.dst13 = Math.sqrt(dst13);
			this.dst23 = Math.sqrt(dst23);
			
			x = this.dst12 / this.dst23;
			y = this.dst13 / this.dst23;
			
		}
		
		@Override
		public double getX() {
			return x;
		}
		
		@Override
		public double getY() {
			return y;
		}
		
		private double getPointX(int p)
		{
			switch(p - 1) {
			case 0:
				return s1.getX();
			case 1:
				return s2.getX();
			case 2:
				return s3.getX();
			}
			return 0;
		}
		
		private double getPointY(int p)
		{
			switch(p - 1) {
			case 0:
				return s1.getY();
			case 1:
				return s2.getY();
			case 2:
				return s3.getY();
			}
			return 0;			
		}
		
		private double getDistance(int p1, int p2)
		{
			if (p1 > p2) {
				int tmp = p1;
				p1 = p2;
				p2 = tmp;
			}
			if (p1 == 1) {
				if (p2 == 2) {
					return dst12;
				}
				return dst13;
			}
			return dst23;
		}
		
		// Fourni cos et sin et ratio pour passer de this à other
		public double [] calcRotation(Triangle other, int p1, int p2, double [] calc)
		{
			if (calc == null || calc.length != 3) {
				calc = new double[3];
			}
			
			// On va calculer l'angle de rotation sur le plus grand vecteur (dst23)
			double Xa = other.getPointX(p2) - other.getPointX(p1);
			double Ya = other.getPointY(p2) - other.getPointY(p1);
			double Xb = this.getPointX(p2) - this.getPointX(p1);
			double Yb = this.getPointY(p2) - this.getPointY(p1);
			
			calc[0] = (Xa*Xb+Ya*Yb)/(this.getDistance(p1, p2)*other.getDistance(p1, p2));
			calc[1] = (Ya*Xb-Xa*Yb)/(this.getDistance(p1, p2)*other.getDistance(p1, p2));
			calc[2] = this.getDistance(p1, p2) / other.getDistance(p1, p2);
			
			// On doit avoir :
			
			double XbVerif = (Xa * calc[0] + Ya * calc[1]) * calc[2];
			double YbVerif = (Ya * calc[0] - Xa * calc[1]) * calc[2];
			
			return calc;
		}
	}

	private static class RansacPoint implements Ransac.RansacPoint{
		Triangle original;
		Triangle image;
		
		double tx, ty, cs, sn;
		
		@Override
		public double getRansacParameter(int order) {
			switch(order) {
			case 0:
				return tx;
			case 1:
				return ty;
			case 2:
				return cs;
			case 3:
				return sn;
			default:
				return 0;
			}
		}
		
	}

	private static double d2(DynamicGridPoint d1, DynamicGridPoint d2)
	{
		return (d1.getX() - d2.getX()) * (d1.getX() - d2.getX()) + (d1.getY() - d2.getY()) * (d1.getY() - d2.getY());
	}

	public double getTx() {
		return tx;
	}

	public double getTy() {
		return ty;
	}

	public double getCs() {
		return cs;
	}

	public double getSn() {
		return sn;
	}

	public boolean isFound() {
		return found;
	}

}
