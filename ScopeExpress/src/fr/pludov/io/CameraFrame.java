package fr.pludov.io;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

import fr.pludov.cadrage.ImageDisplayParameter;

public class CameraFrame {
	int width, height;
	// Données sous forme d'ADU 16-bits
	char [] buffer;

	// null si inconnu
	Double pause;
	// null si inconnu
	Integer iso;
	
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
	
	
	public BufferedImage asImage(ImageDisplayParameter displayParameter)
	{
		switch(displayParameter.channelMode)
		{
		case Color:
			return asRgbImage(displayParameter);
		case GreyScale:
		case NarrowBlue:
		case NarrowGreen:
		case NarrowRed:
			return asGreyImage(displayParameter);
		}
		throw new RuntimeException("unimplemented");
	}
	
	public BufferedImage asGreyImage(ImageDisplayParameter displayParameter)
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
				int val = displayParameter.getLevelForAdu(0, ((int)buffer[offset]));
				inData[0] = (byte) (val);
				raster.setDataElements(x , y, inData);
			}
		}
		
		return result;
	}
	
	public BufferedImage asRgbImage(ImageDisplayParameter displayParameter)
	{
		BufferedImage result = new BufferedImage(width / 2, height / 2, BufferedImage.TYPE_3BYTE_BGR);

		WritableRaster raster = result.getRaster();
		byte [] inData = new byte[3];
		for(int y = 0; y < height / 2 ; ++y)
		{
			for(int x = 0; x < width / 2; ++x)
			{
				int offset = (x * 2) + (y * 2) * width;
				int r = displayParameter.getLevelForAdu(0, buffer[offset]);
				int g1 = (int)buffer[offset + 1];
				int g2 = (int)buffer[offset + width]; 
				int b = displayParameter.getLevelForAdu(2, (int)buffer[offset + width + 1]);
				
				int g = displayParameter.getLevelForAdu(1, (g1 + g2) / 2);
				
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
