package fr.pludov.cadrage.catalogs;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.primitives.ArrayDoubleList;
import org.apache.commons.collections.primitives.DoubleList;
import org.apache.log4j.Logger;

import fr.pludov.cadrage.focus.SkyProjection;

/**
 * Dec is -90/90 (s/n)
 * ra is 0-24h
 */
public class StarProvider {
	private static final Logger logger = Logger.getLogger(StarProvider.class);
	
	File directory;
	
	public StarProvider(File directory)
	{
		this.directory = directory;
	}
//	
//	void loadIndex() throws IOException
//	{
//		RandomAccessFile file = new RandomAccessFile(new File(directory, "index.dat"), "r");
//		try {
//			
//			
//		} finally {
//			file.close();
//		}
//	}
//	
//	public List<CatalogStar> getStarAround(double ra, double dec, double ray, double magMax)
//	{
//		
//	}

	
	/**
	 * retourne une liste de position 2D, magnitude
	 */
	public static DoubleList getStarAroundNorth(SkyProjection sky, double angle, double maxMag)
	{
		DoubleList result = new ArrayDoubleList(10000);
		int starCount = 0;
		
		// Calculer les mindec/maxdec
		double mindec, maxdec;
		double minra, maxra;
		
		double [] star3d = new double[3];
		double [] star2d = new double[2];
		double [] starRaDec = new double[2];
		
		// Ouverture en radian
		double radius = Math.PI * angle / 180.0;
		
		double minZ = Math.cos(radius);
		
		try {

			File origin = new File("C:\\Astro\\Catalogue");
			IndexFile tf = new IndexFile();
			tf.open(new File(origin, "index.dat"));
			StarFile starFile = new StarFile();
			starFile.open(new File(origin, "tyc2.dat"));
			
			SupplFile supplFile = new SupplFile();
			supplFile.open(new File(origin, "suppl_1.dat"));
			
			boolean wantProceed = false;
			long proceedFromCatalog = 1;
			long proceedFromSuppl = 1;
			while(tf.hasNext())
			{
				tf.next();
				if (wantProceed)
				{
					long proceedTo = tf.getRecT2();
					for(long star = proceedFromCatalog; star < proceedTo; ++star)
					{
						starFile.gotoRecord(star);
						if (starFile.getTyc1() == 4628 && starFile.getTyc2() == 237) {
							System.out.println("la polaire !");
						}
						
						if (starFile.getMagnitude() > maxMag) {
							continue;
						}
						
						starRaDec[0] = starFile.getRaCorrected(2013);
						starRaDec[1] = starFile.getDecCorrected(2013);
						
						SkyProjection.convertRaDecTo3D(starRaDec, star3d);
						
						sky.getTransform().convert(star3d);
						
						if (star3d[2] < minZ) continue;
						
						sky.projectPreTransformed3d(star3d, star2d);
						
						result.add(star2d[0]);
						result.add(star2d[1]);
						result.add(starFile.getMagnitude());
						starCount ++;
					}

					proceedTo = tf.getRecSuppl1();
					for(long star = proceedFromSuppl; star < proceedTo; ++star)
					{
						supplFile.gotoRecord(star);
						if (supplFile.getMagnitude() > maxMag) {
							continue;
						}
						
						starRaDec[0] = supplFile.getRa();
						starRaDec[1] = supplFile.getDec();
						
						SkyProjection.convertRaDecTo3D(starRaDec, star3d);
						
						sky.getTransform().convert(star3d);
						
						if (star3d[2] < minZ) continue;
						
						sky.projectPreTransformed3d(star3d, star2d);
						
						result.add(star2d[0]);
						result.add(star2d[1]);
						result.add(supplFile.getMagnitude());
						starCount ++;
					}
				}
				
				// FIXME: trouver l'intersection avec le cercle qui nous intéresse...

				// On veut l'intesection d'un disque avec un rectangle
				// Il faut déterminer le point du rectangle le plus proche du centre du disque
				
				// Calculer le barycentre du rectangle
				// Trouver la distance max
				// En déduire l'angle max (rayon d'un cercle incluant le rectangle)
				// Calculer l'angle "barycentre-visée"
				// Si l'angle est plus grand que radius + anglevisé, on ignore le rectangle...
				
				double [] barycentre = new double[3]; 
				
				for(int i = 0; i < 2; ++i)
				{
					for(int j = 0; j < 2; ++j)
					{
						starRaDec[0] = (i == 0) ? tf.getMinRa() : tf.getMaxRa();
						starRaDec[1] = (j == 0) ? tf.getMinDec() : tf.getMaxDec();
						
						SkyProjection.convertRaDecTo3D(starRaDec, star3d);
						sky.getTransform().convert(star3d);
						barycentre[0] += star3d[0];
						barycentre[1] += star3d[1];
						barycentre[2] += star3d[2];
					}
				}
				
				double l = Math.sqrt(barycentre[0] * barycentre[0] + barycentre[1] * barycentre[1] + barycentre[2] * barycentre[2]);
				
				barycentre[0] /= l;
				barycentre[1] /= l;
				barycentre[2] /= l;
				
				double maxdst2 = 0;
				for(int i = 0; i < 2; ++i)
				{
					for(int j = 0; j < 2; ++j)
					{
						starRaDec[0] = (i == 0) ? tf.getMinRa() : tf.getMaxRa();
						starRaDec[1] = (j == 0) ? tf.getMinDec() : tf.getMaxDec();
						
						SkyProjection.convertRaDecTo3D(starRaDec, star3d);
						sky.getTransform().convert(star3d);
				
						double dx = star3d[0] - barycentre[0];
						double dy = star3d[1] - barycentre[1];
						double dz = star3d[2] - barycentre[2];
						
						double dst2 = (dx * dx + dy * dy + dz * dz);
						if (dst2 > maxdst2) maxdst2 = dst2;
					}
				}
				
				double radiusRectangle = Math.acos((2 - maxdst2) / 2);
				
				// Calcul de l'angle
				double dst2BarycentreVisee = barycentre[0] * barycentre[0] + barycentre[1] * barycentre[1] + (barycentre[2] - 1) * (barycentre[2] - 1);
				double angleBarycentreVisee = Math.acos((2 - dst2BarycentreVisee) / 2);
				
				if (angleBarycentreVisee - radiusRectangle < radius)
				{
					proceedFromCatalog = tf.getRecT2();
					proceedFromSuppl = tf.getRecSuppl1();
					wantProceed = true;
				} else {
					wantProceed = false;
				}
			}
		} catch(Throwable t) {
			t.printStackTrace();
		}
		logger.info("Readed " + starCount);
		return result;
	}
	
	public static void main(String[] args) {
//		StarProvider provider = new StarProvider();
		int starCount = 0;
		
		try {
			double maxDec = 88;
			
			File origin = new File("\\\\Ombre.fbi\\astronomie\\catalog\\tyc2");
			IndexFile tf = new IndexFile();
			tf.open(new File(origin, "index.dat"));
			StarFile starFile = new StarFile();
			starFile.open(new File(origin, "tyc2.dat"));
			
			SupplFile supplFile = new SupplFile();
			supplFile.open(new File(origin, "suppl_1.dat"));
			
			boolean wantProceed = false;
			long proceedFromCatalog = 0;
			long proceedFromSuppl = 0;
			while(tf.hasNext())
			{
				tf.next();
				if (wantProceed)
				{
					long proceedTo = tf.getRecT2();
					for(long star = proceedFromCatalog; star < proceedTo; ++star)
					{
						starFile.gotoRecord(star);
						if (starFile.getDec() > maxDec) {
							System.out.println("Found star at " + starFile.getRa() + ", " + starFile.getDec());
							starCount++;
						}
					}

					proceedTo = tf.getRecSuppl1();
					for(long star = proceedFromSuppl; star < proceedTo; ++star)
					{
						supplFile.gotoRecord(star);
						if (supplFile.getDec() > maxDec) {
							System.out.println("Found Suppl star at " + supplFile.getRa() + ", " + supplFile.getDec());
							starCount ++;
						}
					}

				}
				
				if (tf.getMaxDec() > maxDec) {
					proceedFromCatalog = tf.getRecT2();
					proceedFromSuppl = tf.getRecSuppl1();
					wantProceed = true;
				} else {
					wantProceed = false;
				}
			}
		} catch(Throwable t) {
			t.printStackTrace();
		}
		
		System.out.println("found " + starCount + " stars");
	}
}
