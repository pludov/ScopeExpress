package fr.pludov.scopeexpress.focus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import fr.pludov.scopeexpress.utils.Couple;


public class DarkLibrary {
	/** Un dark ne doit pas exceder ce % de durée de l'image (105) */
	double durationRatio = 1.05;
	/** Un dark ne doit pas être plus chaud que ça */
	double temperatatureDltMax = 1.5;
	
	List<Couple<DarkRequest, Image>> darkCollection;
	
	public Image getDark(DarkRequest de)
	{
		double bestFitVal = 0.0;
		Couple<DarkRequest, Image> bestFit = null;
		for(Couple<DarkRequest, Image> candidate : darkCollection) {
			double f = fit(de, candidate.getA());
			if (Double.isNaN(f)) {
				continue;
			}
			if (f > bestFitVal) {
				bestFitVal = f;
				bestFit = candidate;
			}
			
		}
		if (bestFit != null) {
			return bestFit.getB();
		}
		return null;
	}

	public Image getDarkFor(Image image)
	{
		return getDark(new DarkRequest(image.getMetadata()));
	}
	
	/** Retourne une note entre 0 (impossiblement mauvais) et 1 (excellent) */
	private double fit(DarkRequest image, DarkRequest darkLib)
	{
		double ratio = 1.0;
		if (image.duration != null) {
			if (darkLib.duration != null) {
				// C'est linéaire, on ne veut que ça approche 1, sans dépasser.
				double durationRatio = darkLib.duration / image.duration;
				if (durationRatio > 1.05) {
					return Double.NaN;
				}
				// Aversion pour les darks trop longs
				if (durationRatio > 1.0) {
					durationRatio = durationRatio * durationRatio * durationRatio * durationRatio;
					durationRatio = 1.0 / durationRatio;
				}
				ratio *= durationRatio;
			}
		}
		if (image.temp != null) {
			if (darkLib.temp != null) {
				double tempDelta = image.temp - darkLib.temp;
				// On veut que çà approche zero, sans être négatif
				if (tempDelta < -temperatatureDltMax) {
					return Double.NaN;
				}
				// Aversion pour les dark trop chauds
				if (tempDelta < 0) tempDelta *= 4;
				tempDelta /= 6;
				
				tempDelta = Math.abs(tempDelta);
				// Pour chaque doublage de bruit température, le ratio est divisé par deux
				ratio *= Math.pow(2, -tempDelta);
			}
		}
		
		return ratio;
	}
	
	void addImage(File file)
	{
		Image dark = new Image(file);
		DarkRequest dr = new DarkRequest(dark.getMetadata());
		darkCollection.add(new Couple<>(dr, dark));
	}
	
	
	public DarkLibrary()
	{
		darkCollection = new ArrayList<>();
		addImage(new File("C:\\APT_Images\\CameraCCD_1\\2014-10-04\\ATL_phdqjy_Bin1x1_s_2014-10-04_01-48-58.fit"));
		addImage(new File("C:\\APT_Images\\CameraCCD_1\\2014-10-27\\D_M81_Bin1x1_1s_2014-10-28_04-40-15.fit"));
	}
	
	private static DarkLibrary darkLibrary;
	public static synchronized DarkLibrary getInstance()
	{
		if (darkLibrary == null) {
			darkLibrary = new DarkLibrary();
		}
		return darkLibrary;
	}
}
