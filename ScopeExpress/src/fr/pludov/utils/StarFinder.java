package fr.pludov.utils;

import fr.pludov.cadrage.focus.BitMask;
import fr.pludov.cadrage.focus.StarOccurence;
import fr.pludov.io.CameraFrame;

/**
 * Trouve les caractéristiques d'une étoile dans une frame
 * 
 */
public class StarFinder {

	final int square;
	final int searchRadius;
	final CameraFrame frame;
	int centerX, centerY;
	final int [] blackLevelByChannel;
	final int [] blackStddevByChannel;
	final int [] aduSumByChannel;
	final int [] aduMaxByChannel;
	
	// En sortie
	boolean starFound;
	
	// Centre de l'étoile
	double picX, picY;
	// Etalement
	double stddev, fwhm;
	// Masque de l'étoile (les pixels considérés comme appartenant)
	BitMask starMask;
	
	BitMask excludeMask;
	
	// Si positionné, on ne cherche que dans ce mask;
	BitMask includeMask;
	
	// Trouver une étoile à environ centerX, dans un rayon de square.
	public StarFinder(CameraFrame frame, int centerX, int centerY, int square, int searchRadius) {
		this.centerX = centerX;
		this.centerY = centerY;
		this.frame = frame;
		this.blackLevelByChannel = new int[3];
		this.blackStddevByChannel = new int[3];
		this.aduSumByChannel = new int[3];
		this.aduMaxByChannel = new int[3];
		this.square = square;
		this.searchRadius = square;
	}

	public void perform()
	{
		Histogram histogram = new Histogram();

		histogram.calc(frame, 2 * centerX - square, 2 * centerY - square, 2 * centerX + square, 2 * centerY + square, ChannelMode.Red);
		blackLevelByChannel[0] = histogram.getBlackLevel(0.5);
		blackStddevByChannel[0] = (int)Math.ceil(2 * histogram.getStdDev(0, blackLevelByChannel[0]));
		
		histogram.calc(frame, 2 * centerX - square, 2 * centerY - square, 2 * centerX + square, 2 * centerY + square, ChannelMode.Green);
		blackLevelByChannel[1] = histogram.getBlackLevel(0.5);
		blackStddevByChannel[1] = (int)Math.ceil(2 * histogram.getStdDev(1, blackLevelByChannel[1]));
		
		histogram.calc(frame, 2 * centerX - square, 2 * centerY - square, 2 * centerX + square, 2 * centerY + square, ChannelMode.Blue);
		blackLevelByChannel[2] = histogram.getBlackLevel(0.5);
		blackStddevByChannel[2] = (int)Math.ceil(2 * histogram.getStdDev(2, blackLevelByChannel[2]));
		
		// Calcul des pixels "non noirs"
		BitMask notBlack = new BitMask(
				2 * centerX - square,  2 * centerY - square,
				2 * centerX + square,  2 * centerY + square);
		for(int y = 2 * centerY - square; y <= 2 * centerY + square; ++y)
		{
			for(int x = 2 * centerX - square; x <= 2 * centerX + square; ++x)
			{
				int adu = frame.getAdu(x, y);
				if (adu > blackLevelByChannel[ChannelMode.getRGBBayerId(x, y)]) {
					notBlack.set(x, y);
				}
			}
		}
		notBlack.substract(excludeMask);
		
		BitMask notBlackEroded = new BitMask(notBlack);
		notBlackEroded.erode();
		notBlackEroded.erode();
		notBlackEroded.grow(null);
		notBlackEroded.substract(excludeMask);
		notBlackEroded.grow(null);
		notBlackEroded.substract(excludeMask);
		notBlackEroded.intersect(includeMask);
		
		int maxAdu = 0;
		int maxAduX = 2 * centerX, maxAduY = 2 * centerY;
		
		for(int y = 2 * centerY - searchRadius; y <= 2 * centerY + searchRadius; ++y)
		{
			for(int x = 2 * centerX - searchRadius; x <= 2 * centerX + searchRadius; ++x)
			{
				if (!notBlackEroded.get(x, y)) continue;
				int adu = frame.getAdu(x, y);
				int black = blackLevelByChannel[ChannelMode.getRGBBayerId(x, y)];
				adu -= black;
				if (adu >= maxAdu) {
					maxAdu = adu;
					maxAduX = x;
					maxAduY = y;
				}
			}
		}
		
		// On remonte le niveau de noir de 20%
		// blackLevel = (int)Math.round(blackLevel + 0.2 * (maxAdu - blackLevel));
		
		// On remonte arbitrairement le noir
		for(int i = 0; i < blackLevelByChannel.length; ++i) {
			blackLevelByChannel[i] += blackStddevByChannel[i];
		}
		
		// Re- Calcul des pixels "non noirs"
		notBlack = new BitMask(
				2 * centerX - square,  2 * centerY - square,
				2 * centerX + square,  2 * centerY + square);
		for(int y = 2 * centerY - square; y <= 2 * centerY + square; ++y)
		{
			for(int x = 2 * centerX - square; x <= 2 * centerX + square; ++x)
			{
				int adu = frame.getAdu(x, y);
				int black = blackLevelByChannel[ChannelMode.getRGBBayerId(x, y)];
				if (adu > black) {
					adu -= black;
					
					// FIXME: 0.25 = en dur
					if (adu > 0.25 * maxAdu) {
						notBlack.set(x, y);
					}
				}
			}
		}
		notBlack.substract(excludeMask);
		notBlackEroded = new BitMask(notBlack);
		notBlackEroded.erode();
		notBlackEroded.grow(null);
		notBlackEroded.substract(excludeMask);
		notBlackEroded.intersect(includeMask);
		
		if (!notBlackEroded.get(maxAduX, maxAduY)) {
			// Rien trouvé
			return;
		}
		
		// On marque le centre
		BitMask star = new BitMask(
				2 * centerX - square,  2 * centerY - square,
				2 * centerX + square,  2 * centerY + square);
		star.set(maxAduX, maxAduY);
		star.grow(notBlackEroded);
		
		// On élargi encore un coup l'étoile...
		star.grow(null);
		star.substract(excludeMask);
		star.grow(null);
		star.substract(excludeMask);
		star.grow(null);					
		star.substract(excludeMask);
		
		starMask = star;
		
		long xSum = 0;
		long ySum = 0;
		long aduSum = 0;
		
		for(int x = 2 * centerX - square; x <= 2 * centerX + square; ++x)
		{
			for(int y = 2 * centerY - square; y <= 2 * centerY + square; ++y)
			{
				if (!star.get(x, y)) continue;
				int channelId = ChannelMode.getRGBBayerId(x, y);
				int adu = frame.getAdu(x, y);
				int black = blackLevelByChannel[channelId];
				if (adu <= black) continue;
				adu -= black;
				this.aduSumByChannel[channelId] += adu;
				if (adu > this.aduMaxByChannel[channelId]) {
					this.aduMaxByChannel[channelId] = adu;
				}
				
				xSum += x * adu;
				ySum += y * adu;
				aduSum += adu;
			}
		}
		
		
		if (aduSum <= 0) {
			// Rien trouvé
			return;
		}
		
		
		picX = xSum * 1.0 / aduSum;
		picY = ySum * 1.0 / aduSum;
		picX /= 2;
		picY /= 2;
		centerX = (int)Math.round(picX);
		centerY = (int)Math.round(picY);

		double sumDstSquare = 0;
		long aduDst = 0;
//				for(int x = 2 * centerX - square; x <= 2 * centerX + square; ++x)
//				{
//					for(int y = 2 * centerY - square; y <= 2 * centerY + square; ++y)
//					{
//						if (!star.get(x, y)) continue;
//						
//						int adu = frame.getAdu(x, y);
//						int black = blackLevelByChannel[ChannelMode.getRGBBayerId(x, y)];
//						
//						if (adu <= black) continue;
//						adu -= black;
//						//adu = (int)(100.0*Math.sqrt(adu));
//						
//						double dst = (x - 2 * picX) * (x - 2 * picX) + (y - 2 * picY) * (y - 2 * picY);
//						
//						sumDstSquare += adu * dst;
//						aduDst += adu;
//					}
//				}

		for(int x = 2 * centerX - square; x <= 2 * centerX + square; ++x)
		{
			long aduForX = 0;
			for(int y = 2 * centerY - square; y <= 2 * centerY + square; ++y)
			{
				if (!star.get(x, y)) continue;
				
				int adu = frame.getAdu(x, y);
				int black = blackLevelByChannel[ChannelMode.getRGBBayerId(x, y)];
				
				if (adu <= black) continue;
				adu -= black;
				aduForX += adu;
			}
			
			double dst = (x - 2 * picX) * (x - 2 * picX);
			
			sumDstSquare += aduForX * dst;
			aduDst += aduForX;

		}

		
		stddev = Math.sqrt(sumDstSquare / aduDst);
		
		fwhm = 2.35 * stddev;
		starFound = true;
	}

	public int getSquare() {
		return square;
	}

	public CameraFrame getFrame() {
		return frame;
	}

	public int getCenterX() {
		return centerX;
	}

	public int getCenterY() {
		return centerY;
	}

	public int[] getBlackLevelByChannel() {
		return blackLevelByChannel;
	}

	public int[] getBlackStddevByChannel() {
		return blackStddevByChannel;
	}

	public boolean isStarFound() {
		return starFound;
	}

	public double getPicX() {
		return picX;
	}

	public double getPicY() {
		return picY;
	}

	public double getStddev() {
		return stddev;
	}

	public double getFwhm() {
		return fwhm;
	}

	public BitMask getStarMask() {
		return starMask;
	}

	public int[] getAduSumByChannel() {
		return aduSumByChannel;
	}

	public int[] getAduMaxByChannel() {
		return aduMaxByChannel;
	}

	public BitMask getExcludeMask() {
		return excludeMask;
	}

	public void setExcludeMask(BitMask excludeMask) {
		this.excludeMask = excludeMask;
	}

	public BitMask getIncludeMask() {
		return includeMask;
	}

	public void setIncludeMask(BitMask includeMask) {
		this.includeMask = includeMask;
	}
}
