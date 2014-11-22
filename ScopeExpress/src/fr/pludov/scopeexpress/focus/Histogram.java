package fr.pludov.scopeexpress.focus;

import java.util.Arrays;

import fr.pludov.io.CameraFrame;
import fr.pludov.utils.ChannelMode;

public class Histogram {
	final int [] count;
	int pixCount;
	
	Histogram() {
		this.count = new int[65536];
		this.pixCount = 0;
	}

	private void reset()
	{
		if (pixCount > 0) {
			Arrays.fill(this.count, 0);
			pixCount = 0;
		}
	}
	
	public static Histogram forArea(CameraFrame cf, int x0, int y0, int x1, int y1, ChannelMode mode)
	{
		Histogram result = new Histogram();
		result.calc(cf, x0, y0, x1, y1, mode);
		return result;
	}
	
	public double getMoy(int minAdu, int maxAdu)
	{
		long result = 0;
		long aduSum = 0;
		for(int i = minAdu; i < maxAdu; ++i)
		{
			long c = this.count[i];
			result += i * c;
			aduSum += c;
		}
		
		return result * 1.0 / aduSum;
	}
	
	public double getStdDev(int minAdu, int maxAdu)
	{
		double moy = getMoy(minAdu, maxAdu);
		double avgdst = 0;
		long adusum = 0;
		for(int i = minAdu; i < maxAdu; ++i)
		{
			int c = this.count[i];
			avgdst += c * (i - moy) * (i - moy);
			adusum += c;
		}
		return Math.sqrt(avgdst / adusum);
	}
	
	public int getBlackLevel(double percent)
	{
		int blackLevel = 0;
		int blackPixelCount = 0;
		while(blackLevel < count.length && blackPixelCount < pixCount * percent)
		{
			blackPixelCount += count[blackLevel];
			blackLevel++;
		}
		
		return blackLevel;
	}
	
	public int getValue(int adu)
	{
		if ((adu < 0) || (adu >= count.length)) return 0;
		return count[adu];
	}
	
	void calc(CameraFrame frame, int x0, int y0, int x1, int y1, ChannelMode mode)
	{
		reset();
		
		// Faire un histogramme, trouver le max.
		for(int y = y0; y <= y1; ++y)
		{
			if ((y < 0) || (y >= frame.getHeight())) continue;
			for(int x = x0; x <= x1; ++x)
			{
				if ((x < 0) || (x >= frame.getWidth())) continue;
				
				if (mode != ChannelMode.Bayer) {
					if (((x & 1) + (y & 1)) != mode.ordinal()) {
						continue;
					}
					
				}
				
				int adu = frame.getAdu(x, y);
				count[adu] ++;
				pixCount++;
			}
		}
	}
	
}
