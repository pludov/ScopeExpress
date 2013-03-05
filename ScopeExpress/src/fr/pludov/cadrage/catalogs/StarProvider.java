package fr.pludov.cadrage.catalogs;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;


/**
 * Dec is -90/90 (s/n)
 * ra is 0-24h
 */
public class StarProvider {
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
	 * retourne une liste de ra/dec
	 */
	public static List<Double> getStarAroundNorth(double maxDec, double maxMag)
	{
		List<Double> result = new ArrayList<Double>();
		int starCount = 0;
		
		try {

			File origin = new File("C:\\Astro\\Catalogue");
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
						if (starFile.getTyc1() == 4628 && starFile.getTyc2() == 237) {
							System.out.println("la polaire !");
						}
						if (starFile.getDec() > maxDec && starFile.getMagnitude() < maxMag) {
							result.add(starFile.getRaCorrected(2012));
							result.add(starFile.getDecCorrected(2012));
							result.add(starFile.getMagnitude());
						}
					}

					proceedTo = tf.getRecSuppl1();
					for(long star = proceedFromSuppl; star < proceedTo; ++star)
					{
						supplFile.gotoRecord(star);
						if (supplFile.getDec() > maxDec  && supplFile.getMagnitude() < maxMag) {
							System.out.println("Found Suppl star at " + supplFile.getRa() + ", " + supplFile.getDec());
							result.add(supplFile.getRa());
							result.add(supplFile.getDec());
							result.add(supplFile.getMagnitude());
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
