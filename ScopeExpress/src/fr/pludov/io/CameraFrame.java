package fr.pludov.io;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

public class CameraFrame {
	int width, height;
	// Données sous forme d'ADU 16-bits
	char [] buffer;
	
	boolean isCfa;
	
	public FitsPlane asGreyFits(double mul)
	{
		FitsPlane result = new FitsPlane(width / 2, height / 2);

		int inId = 0;
		int outId = 0;
		for(int y = 0; y < height / 2 ; ++y)
		{
			for(int x = 0; x < width / 2; ++x)
			{
				int offset = inId;
				int g1 = (int)buffer[offset];
				int r = (int)buffer[offset + 1];
				int b = (int)buffer[offset + width];
				int g2 = (int)buffer[offset + width + 1]; 

				result.value[outId] = (float)(mul * (g1 + r + b + g2));
				
				inId+=2;
				outId++;
			}
			inId += width;
		}
		
		return result;

	}
	
	
	public BufferedImage asGreyImage(double level)
	{
		BufferedImage result = new BufferedImage(width / 2, height / 2, BufferedImage.TYPE_BYTE_GRAY);

		WritableRaster raster = result.getRaster();
		byte [] inData = new byte[1];
		for(int y = 0; y < height / 2 ; ++y)
		{
			for(int x = 0; x < width / 2; ++x)
			{
				int offset = (x * 2) + (y * 2) * width;
//				int val = ((int)charBuffer[offset] + 
//							(int)charBuffer[offset + 1] + 
//							(int)charBuffer[offset + width] + 
//							(int)charBuffer[offset + width + 1] ) / (4 * 8);
				int val = (int)(level * ((int)buffer[offset]));
				if (val > 255) val = 255;
				inData[0] = (byte) (val);
				raster.setDataElements(x , y, inData);
			}
		}
		
		return result;
	}
	
	public BufferedImage asRgbImage(double level)
	{
		BufferedImage result = new BufferedImage(width / 2, height / 2, BufferedImage.TYPE_3BYTE_BGR);

		WritableRaster raster = result.getRaster();
		byte [] inData = new byte[3];
		for(int y = 0; y < height / 2 ; ++y)
		{
			for(int x = 0; x < width / 2; ++x)
			{
				int offset = (x * 2) + (y * 2) * width;
				int r = (int)(level * (int)buffer[offset]);
				int g1 = (int)(level * (int)buffer[offset + 1]);
				int g2 = (int)(level * (int)buffer[offset + width]); 
				int b = (int)(level * (int)buffer[offset + width + 1]);
				
				int g = (g1 + g2) / 2;
				if (g1 > 255) g1 = 255;
				if (r > 255) r = 255;
				if (g > 255) g = 255;
				if (b > 255) b = 255;
				
				inData[0] = (byte) (r);
				inData[1] = (byte) (g);
				inData[2] = (byte) (b);
				raster.setDataElements(x , y, inData);
			}
		}
		
		return result;
		
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public char[] getBuffer() {
		return buffer;
	}

	public boolean isCfa() {
		return isCfa;
	}
	
}
