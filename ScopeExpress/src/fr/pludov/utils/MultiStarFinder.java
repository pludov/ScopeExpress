package fr.pludov.utils;

import java.util.ArrayList;
import java.util.List;

import fr.pludov.cadrage.focus.BitMask;
import fr.pludov.io.CameraFrame;

/**
 * Trouve des étoiles dans une image.
 *
 */
public class MultiStarFinder {

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
		blackLevelByChannel[0] = histogram.getBlackLevel(0.25);
		blackStddevByChannel[0] = (int)Math.ceil(2 * histogram.getStdDev(0, blackLevelByChannel[0]));
		
		percent(5);
		
		histogram.calc(frame, 0, 0, frame.getWidth(), frame.getHeight(), ChannelMode.Green);
		blackLevelByChannel[1] = histogram.getBlackLevel(0.25);
		blackStddevByChannel[1] = (int)Math.ceil(2 * histogram.getStdDev(0, blackLevelByChannel[1]));
		
		percent(10);
		
		histogram.calc(frame, 0, 0, frame.getWidth(), frame.getHeight(), ChannelMode.Blue);
		blackLevelByChannel[2] = histogram.getBlackLevel(0.25);
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
		
		int [] coords = null;
		for(coords = notBlack.nextPixel(coords); coords != null; coords = notBlack.nextPixel(coords))
		{
			percent(50 + 50 * coords[1] / frame.getHeight());
			
			BitMask mask = notBlack.getConnexArea(coords[0], coords[1], 50);
			notBlack.substract(mask);
			
			// Calculer le milieu du mask
			
			StarFinder finder = new StarFinder(frame, coords[0] / 2, coords[1] / 2, 25, 25);
			finder.setExcludeMask(checkedArea);
			finder.perform();
			if (finder.starFound) {
				boolean isBright = false;
				
				int totalAduSum = 0;
				for(int i = 0; i < finder.aduSumByChannel.length; ++i)
				{
					totalAduSum += finder.aduSumByChannel[i];
				}
				
				// FIXME: seuil en dur pour la détection des etoiles faibles
				isBright = totalAduSum > 6000;
				if (isBright) {
					stars.add(finder);
				}
				
				// FIXME: detection des images saturées
				checkedArea.add(finder.getStarMask());
			}	
			
		}
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
