package fr.pludov.utils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

import fr.pludov.cadrage.focus.BitMask;
import fr.pludov.cadrage.focus.BitMask.ConnexityGroup;
import fr.pludov.io.CameraFrame;

/**
 * Trouve des étoiles dans une image.
 *
 */
public class MultiStarFinder {
	private static final Logger logger = Logger.getLogger(MultiStarFinder.class);
	
	List<StarFinder> stars;
	CameraFrame frame;
	BitMask checkedArea;
	
	public MultiStarFinder(CameraFrame frame) {
		this.frame = frame;	
		checkedArea = new BitMask(0, 0, frame.getWidth(), frame.getHeight());
		stars = new ArrayList<StarFinder>();
	}
	
	public void proceed()
	{
		int stepX = frame.getWidth() / 24;
		int stepY = stepX;

		percent(0);
		
		Histogram histogram = new Histogram();

		int [] blackLevelByChannel = new int[3];
		int [] blackStddevByChannel = new int[3];
		
		histogram.calc(frame, 0, 0, frame.getWidth(), frame.getHeight(), ChannelMode.Red);
		blackLevelByChannel[0] = histogram.getBlackLevel(0.60);
		blackStddevByChannel[0] = (int)Math.ceil(2 * histogram.getStdDev(0, blackLevelByChannel[0]));
		
		percent(5);
		
		histogram.calc(frame, 0, 0, frame.getWidth(), frame.getHeight(), ChannelMode.Green);
		blackLevelByChannel[1] = histogram.getBlackLevel(0.60);
		blackStddevByChannel[1] = (int)Math.ceil(2 * histogram.getStdDev(0, blackLevelByChannel[1]));
		
		percent(10);
		
		histogram.calc(frame, 0, 0, frame.getWidth(), frame.getHeight(), ChannelMode.Blue);
		blackLevelByChannel[2] = histogram.getBlackLevel(0.60);
		blackStddevByChannel[2] = (int)Math.ceil(2 * histogram.getStdDev(0, blackLevelByChannel[2]));

		percent(15);
		
		int [] limitByChannel = new int[3];
		for(int i = 0; i < 3; ++i)
		{
			limitByChannel[i] = blackStddevByChannel[i] + blackLevelByChannel[i];
		}
		
		BitMask notBlack = new BitMask(0, 0, frame.getWidth(), frame.getHeight());
		for(int y = 0; y < frame.getHeight(); ++y)
		{
			for(int x = 0; x < frame.getWidth(); ++x)
			{
				int adu = frame.getAdu(x, y);
				if (adu > limitByChannel[ChannelMode.getRGBBayerId(x, y)]) {
					notBlack.set(x, y);
				}
			}
		}
		
		// On a identifié les pixels non noirs.
		// Il faut segmenter l'image en zone de connexité.
		// Chaque zone de connexité représente une étoile potentielle.
		
		// - On va d'abord exclure les zone de connexité qui sont plus grande qu'un seuil arbitraire
		//   ( étoiles saturées, nébuleuses )
		
		// FIXME : travailler sur des super pixels pour reperer les étoiles (sinon, en halpha, on ne trouvera jamais rien !)
		percent(25);
		
		notBlack.erode();
		
		percent(30);
		
		notBlack.erode();
		
		percent(35);
		
		notBlack.grow(null);
		
		percent(40);
		
		notBlack.grow(null);
		
		percent(45);
		
		notBlack.substract(checkedArea);
		
		percent(50);
		
		// Ensuite, chaque zone de connexité représente une étoile potentielle.
		//  - on les trie par energie
		//  - on les parcours
		//  - si le nombre d'étoiles autours de la zone considérée est inferieur à la moyenne, considérer la zone
		double starPerPixelPixel = 120.0 / (frame.getWidth() * frame.getHeight());
		double starPerPixelRadiusCheck = 300;
		
		// Taille maxi d'une étoile (32 x 32) 
		int maxSurface = 2048;
		double maxStddev = 8;

		logger.info("Calc connexity groups");
		List<ConnexityGroup> groups = notBlack.calcConnexityGroups();
		logger.debug("evaluation groups statistics");
		
		for(Iterator<ConnexityGroup> it = groups.iterator(); it.hasNext();)
		{
			ConnexityGroup c = it.next();
			
			if (c.pixelPositions.size() > 2 * maxSurface)
			{
				it.remove();
			} else {
				// Calculer le poids
				double adusum = 0.0;
				double xmoy = 0;
				double ymoy = 0;
				for(int i = 0; i < c.pixelPositions.size(); i += 2)
				{
					int x = c.pixelPositions.get(i);
					int y = c.pixelPositions.get(i + 1);
					
					double v = frame.getAdu(x, y);
					v -= limitByChannel[ChannelMode.getRGBBayerId(x, y)];
					
					// FIXME : retirer le black et l'estimation du fond !
					xmoy += v * x;
					ymoy += v * y;
					adusum += v;
				}
				
				if (adusum > 0) {
					xmoy /= adusum;
					ymoy /= adusum;
				}
				c.weight = adusum;
				c.centerx = xmoy;
				c.centery = ymoy;
				
				double stddevVal = 0;
				
				for(int i = 0; i < c.pixelPositions.size(); i += 2)
				{
					int x = c.pixelPositions.get(i);
					int y = c.pixelPositions.get(i + 1);
					
					double v = frame.getAdu(x, y);
					v -= limitByChannel[ChannelMode.getRGBBayerId(x, y)];
					
					// FIXME : retirer le black et l'estimation du fond !
					
					double dst  = (x - xmoy) * (x - xmoy) + (y - ymoy) * (y - ymoy);
					stddevVal += v * dst;
				}
				if (adusum > 0) {
					stddevVal /= adusum;
				}
				c.stddev = Math.sqrt(stddevVal);
				if (c.stddev > maxStddev) {
					it.remove();
				}
			}
		}
		logger.debug("sorting groups");
		Collections.sort(groups, new Comparator<ConnexityGroup>() {
			@Override
			public int compare(ConnexityGroup o1, ConnexityGroup o2) {
				return -Double.compare(o1.weight, o2.weight); 
			}
		});
		
		percent(70);

		logger.debug("iterating groups");
		int aduSeuil = 0;
		int minimumAduSum = 0;
		List<StarFinder> finalStarFinderList = new LinkedList<StarFinder>();
		int maxStarCount = 400;
		int cgId = -1;
		for(ConnexityGroup cg : groups)
		{
			cgId++;
			percent(70 + Math.max((30 * cgId) / groups.size(), 30 * finalStarFinderList.size() / maxStarCount));
			
			StarFinder finder = new StarFinder(frame, (int)Math.round(cg.centerx / 2.0), (int)Math.round(cg.centery / 2.0), 25, 25);
			finder.setExcludeMask(checkedArea);
			
			finder.setIncludeMask(cg.getBitMask());
			finder.perform();

			if (finder.starFound) {
				
				if (finder.getTotalAduSum() >= aduSeuil)
				{
					if (finalStarFinderList.isEmpty()) {
						minimumAduSum = finder.getTotalAduSum();
					}
					finalStarFinderList.add(finder);
					
					if (finalStarFinderList.size() > maxStarCount)
					{
						break;
//						// Retirer le minimum
//						int newMinimumAduSum = 0;
//						boolean hasNewMinimumAduSum = false;
//						for(Iterator<StarFinder> it = finalStarFinderList.iterator(); it.hasNext(); )
//						{
//							StarFinder sf = it.next();
//							int sfAduSum = sf.getTotalAduSum();
//							if (sfAduSum <= minimumAduSum) {
//								it.remove();
//							} else {
//								// On le garde, mais on met peut être à jour le minimum
//								if ((!hasNewMinimumAduSum) || newMinimumAduSum > sfAduSum)
//								{
//									newMinimumAduSum = sfAduSum;
//									hasNewMinimumAduSum = true;
//								}
//							}
//						}
//						minimumAduSum = newMinimumAduSum;
//						aduSeuil = minimumAduSum;
					}
				}
				// FIXME: detection des images saturées
				checkedArea.add(finder.getStarMask());
			}
		}
		
		stars.addAll(finalStarFinderList);
		
		
//		int [] coords = null;
//		for(coords = notBlack.nextPixel(coords); coords != null; coords = notBlack.nextPixel(coords))
//		{
//			percent(50 + 50 * coords[1] / frame.getHeight());
//			
//			BitMask mask = notBlack.getConnexArea(coords[0], coords[1], 200);
//			notBlack.substract(mask);
//			// Calculer le milieu du mask
//			double [] maskCenter = mask.getCenter();
//			if (maskCenter == null) continue;
//			
//			
//			StarFinder finder = new StarFinder(frame, (int)Math.round(maskCenter[0] / 2.0), (int)Math.round(maskCenter[1] / 2.0), 25, 25);
//			finder.setExcludeMask(checkedArea);
//			finder.perform();
//			if (finder.starFound) {
//				boolean isBright = false;
//				
//				int totalAduSum = 0;
//				for(int i = 0; i < finder.aduSumByChannel.length; ++i)
//				{
//					totalAduSum += finder.aduSumByChannel[i];
//				}
//				
//				// FIXME: seuil en dur pour la détection des etoiles faibles
//				isBright = totalAduSum > 1000;
//				if (isBright) {
//					stars.add(finder);
//				}
//				
//				// FIXME: detection des images saturées
//				checkedArea.add(finder.getStarMask());
//			}	
//			
//		}
	}

	/**
	 * Appellé pour donner un apercu de la progression
	 */
	public void percent(int pct)
	{
		
	}
	
	public List<StarFinder> getStars() {
		return stars;
	}

	public BitMask getCheckedArea() {
		return checkedArea;
	}

}
