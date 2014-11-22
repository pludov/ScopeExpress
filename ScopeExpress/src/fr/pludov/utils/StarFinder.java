package fr.pludov.utils;

import org.apache.log4j.Logger;

import fr.pludov.io.CameraFrame;
import fr.pludov.scopeexpress.ImageDisplayParameter;
import fr.pludov.scopeexpress.focus.BitMask;
import fr.pludov.scopeexpress.focus.Histogram;

/**
 * Trouve les caractéristiques d'une étoile dans une frame
 * 
 */
public class StarFinder {
	private static final Logger logger = Logger.getLogger(StarFinder.class);
	
	final int square;
	final int searchRadius;
	final CameraFrame frame;
	final ImageDisplayParameter frameDisplayParameter;
	int centerX, centerY;
	StarDetectionMode starDetectionMode;;
	final int [] blackLevelByChannel;
	final int [] blackStddevByChannel;
	final int [] aduSumByChannel;
	final int [] aduMaxByChannel;
	
	// En sortie
	boolean starFound;

	// Si un pixel atteint le niveau de saturation (cameraFrame.maximum)
	boolean saturationDetected;
	
	// Centre de l'étoile
	double picX, picY;
	// Etalement
	double stddev, fwhm;
	// Sous l'angle le plus gentil...
	double minStddev, minFwhm, minFwhmAngle;
	double maxStddev, maxFwhm, maxFwhmAngle;
	
	// Masque de l'étoile (les pixels considérés comme appartenant)
	BitMask starMask;
	
	BitMask excludeMask;
	
	// Si positionné, on ne cherche que dans ce mask;
	BitMask includeMask;
	
	static final StarDetectionMode GreyScale = new StarDetectionMode(1, ImageDisplayParameter.ChannelMode.GreyScale, ChannelMode.Bayer)
	{
		int getChannelId(int x, int y) {
			return 0;
		}
	};
	
	static final StarDetectionMode CfaMode = new StarDetectionMode(3, ImageDisplayParameter.ChannelMode.Color, ChannelMode.Red, ChannelMode.Green, ChannelMode.Blue)
	{
		int getChannelId(int x, int y) {
			return ChannelMode.getRGBBayerId(x, y);
		}
	};
	
	
	// Trouver une étoile à environ centerX, dans un rayon de square.
	public StarFinder(CameraFrame frame, int centerX, int centerY, int square, int searchRadius) {
		this.centerX = centerX;
		this.centerY = centerY;
		this.frame = frame;
		this.starDetectionMode = frame.isCfa() ? CfaMode : GreyScale;
		this.frameDisplayParameter = new ImageDisplayParameter();
		frameDisplayParameter.setDarkEnabled(false);
		frameDisplayParameter.setChannelMode(this.starDetectionMode.displayMode);
		this.blackLevelByChannel = new int[this.starDetectionMode.channelCount];
		this.blackStddevByChannel = new int[this.starDetectionMode.channelCount];
		this.aduSumByChannel = new int[this.starDetectionMode.channelCount];
		this.aduMaxByChannel = new int[this.starDetectionMode.channelCount];
		this.square = square;
		this.searchRadius = square;
	}

	public void perform()
	{

		for(int i = 0; i < this.starDetectionMode.channelCount; ++i)
		{
			Histogram histogram = Histogram.forArea(frame, 2 * centerX - square, 2 * centerY - square, 2 * centerX + square, 2 * centerY + square, this.starDetectionMode.channels[i]);
			blackLevelByChannel[i] = histogram.getBlackLevel(0.4);
			blackStddevByChannel[i] = (int)Math.ceil(2 * histogram.getStdDev(0, blackLevelByChannel[i]));
		}
		
		int [] maxAduByChannel = new int[this.starDetectionMode.channelCount];
		// Calcul des pixels "non noirs"
		BitMask notBlack = new BitMask(
				2 * centerX - square,  2 * centerY - square,
				2 * centerX + square,  2 * centerY + square);
		for(int y = 2 * centerY - square; y <= 2 * centerY + square; ++y)
		{
			for(int x = 2 * centerX - square; x <= 2 * centerX + square; ++x)
			{
				if (includeMask != null && !includeMask.get(x, y)) continue;
				int adu = frame.getAdu(x, y);
				int channelId = this.starDetectionMode.getChannelId(x, y);
				if (adu > blackLevelByChannel[channelId]) {
					if (adu > maxAduByChannel[channelId]) {
						maxAduByChannel[channelId] = adu;
					}
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
		// notBlackEroded.intersect(includeMask);
		
		int maxAdu = 0;
		int maxAduX = 2 * centerX, maxAduY = 2 * centerY;
		
		for(int [] xy = notBlackEroded.nextPixel(null); xy != null; xy = notBlackEroded.nextPixel(xy))
		{
			int x = xy[0];
			int y = xy[1];

			int adu = frame.getAdu(x, y);
			int black = blackLevelByChannel[this.starDetectionMode.getChannelId(x, y)];
			adu -= black;
			if (adu >= maxAdu) {
				maxAdu = adu;
				maxAduX = x;
				maxAduY = y;
			}
		}

		frameDisplayParameter.setAutoHistogram(false);
		for(int i = 0; i < blackLevelByChannel.length; ++i) {
			int b = blackLevelByChannel[i] + blackStddevByChannel[i];
			int h = maxAduByChannel[i];
			int m = (2 * b + h) / 3;
			frameDisplayParameter.setLow(i, b);
			frameDisplayParameter.setMedian(i, m);
			frameDisplayParameter.setHigh(i, h);
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
				int black = blackLevelByChannel[this.starDetectionMode.getChannelId(x, y)];
				if (adu > black) {
					adu -= black;
					
					// FIXME: avant on avait 0.25 * maxAdu
					if (adu > 0) {
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
		// notBlackEroded.intersect(includeMask);
		
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
		
		int cameraSat = frame.getMaximum();
		this.saturationDetected = false;
		for(int xy [] = star.nextPixel(null); xy != null; xy = star.nextPixel(xy))
		{
			int x = xy[0];
			int y = xy[1];

			int channelId = this.starDetectionMode.getChannelId(x, y);
			int adu = frame.getAdu(x, y);
			// en cas d'utilisation de black, on fait en sorte de garder saturé les pixels saturés
			if (adu >= cameraSat) {
				this.saturationDetected = true;
			}
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

		
//				for(int x = 2 * centerX - square; x <= 2 * centerX + square; ++x)
//				{
//					for(int y = 2 * centerY - square; y <= 2 * centerY + square; ++y)
//					{
//						if (!star.get(x, y)) continue;
//						
//						int adu = frame.getAdu(x, y);
//						int black = blackLevelByChannel[this.channelMode.getChannelId(x, y)];
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
		starFound = true;
		
		double maxAngle = 0, minAngle = 0;
		double maxFwhm = 0, minFwhm = 0;
		double fwhmSum = 0;
		int stepCount = 128;
		for(int step = 0; step < stepCount; ++step)
		{
			double angle = step * Math.PI / stepCount;
		
			double cs = Math.cos(angle);
			double sn = Math.sin(angle);
			
			double sumDstSquare = 0;
			double sumDstSquareDivider = 0;
		
			// MedianCalculator medianCalculator = new MedianCalculator();	
			
			// on veut le x moyen tel que : 
			//    Centerx = somme(x.adu) / somme(adu)
			// Et après l'écart type:
			//    Stddev = somme(adu.(x - centerx)) / somme(adu)
			for(int x = 2 * centerX - square; x <= 2 * centerX + square; ++x)
			{
				for(int y = 2 * centerY - square; y <= 2 * centerY + square; ++y)
				{
					if (!star.get(x, y)) continue;
					
					int adu = frame.getAdu(x, y);
					int black = blackLevelByChannel[this.starDetectionMode.getChannelId(x, y)];
					
					if (adu <= black) continue;
					adu -= black;
					
					double dx = (x - 2 * picX);
					double dy = (y - 2 * picY);
					double dst = cs * dx + sn * dy;
					
//					
//					double dst = cs * (x - 2 * picX) * cs * (x - 2 * picX) +
//							sn * (y - 2 * picY) * sn * (y - 2 * picY);
//					
					// medianCalculator.addEntry(adu, dst);
					double adus = adu;
					sumDstSquare += adus * dst * dst;
					sumDstSquareDivider += adus;
				}
			}
	
			
			double stddev = Math.sqrt(sumDstSquare / sumDstSquareDivider);
			// double meandev = Math.sqrt(medianCalculator.getMedian());
			// logger.info("found stddev = " + stddev + "  meandev = " + meandev);
			double fwhm = 2.35 * stddev;
			
			if (step == 0 || fwhm > maxFwhm)
			{
				maxFwhm = fwhm;
				maxAngle = angle;
			}
			
			if (step == 0 || fwhm < minFwhm)
			{
				minFwhm = fwhm;
				minAngle = angle;
			}
			
			fwhmSum += fwhm;
		}
		
		this.fwhm = fwhmSum / stepCount;
		this.stddev = this.fwhm / 2.35;
		
		this.maxFwhm = maxFwhm;
		this.maxStddev = maxFwhm / 2.35;
		this.maxFwhmAngle = maxAngle;
		this.minFwhm = minFwhm;
		this.minStddev = minFwhm / 2.35;
		this.minFwhmAngle = minAngle;
		logger.info("found fwhm in " + minFwhm +" ... " + maxFwhm + " min=" + (minAngle * 180/Math.PI) + " max=" + + (maxAngle * 180/Math.PI));
	}
	
	public int getTotalAduSum()
	{
		int totalAduSum = 0;
		for(int i = 0; i < aduSumByChannel.length; ++i)
		{
			totalAduSum += aduSumByChannel[i];
		}
		return totalAduSum;
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

	public double getMinStddev() {
		return minStddev;
	}

	public double getMinFwhm() {
		return minFwhm;
	}

	public double getMaxStddev() {
		return maxStddev;
	}

	public double getMaxFwhm() {
		return maxFwhm;
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

	public boolean isSaturationDetected() {
		return saturationDetected;
	}

	public double getMinFwhmAngle() {
		return minFwhmAngle;
	}

	public void setMinFwhmAngle(double minFwhmAngle) {
		this.minFwhmAngle = minFwhmAngle;
	}

	public double getMaxFwhmAngle() {
		return maxFwhmAngle;
	}

	public void setMaxFwhmAngle(double maxFwhmAngle) {
		this.maxFwhmAngle = maxFwhmAngle;
	}

	public ImageDisplayParameter getFrameDisplayParameter() {
		return frameDisplayParameter;
	}

	public StarDetectionMode getStarDetectionMode() {
		return starDetectionMode;
	}
}
