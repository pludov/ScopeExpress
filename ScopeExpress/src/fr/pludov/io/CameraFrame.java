package fr.pludov.io;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

import fr.pludov.cadrage.ImageDisplayParameter;
import fr.pludov.cadrage.ImageDisplayParameter.ImageDisplayMetaDataInfo;
import fr.pludov.cadrage.utils.cache.Cache;

public class CameraFrame {
	int width, height;
	// Données sous forme d'ADU 16-bits
	char [] buffer;

	boolean isCfa;
	
	int black;
	// Les pixels atteignant cette valeur sont considéré comme saturés
	int maximum;

	int [] [] histogram;
	int [] histogramNbPix;
	/**
	 * Retourne une sous-image (coordonnées super pixel)
	 */
	public CameraFrame subFrame(int minx, int miny, int maxx, int maxy)
	{
		CameraFrame result = new CameraFrame();
		result.width = 2 * (maxx - minx + 1);
		result.height = 2 * (maxy - miny + 1);
		result.buffer = new char[result.width * result.height];
		result.black = this.black;
		result.maximum = this.maximum;
		result.histogram = this.histogram;
		result.histogramNbPix = this.histogramNbPix;
		char [] in = this.buffer;
		char [] out = result.buffer;
		int outptr = 0;
		for(int y = miny; y <= maxy; ++y) {
			for(int interline = 0; interline < 2; ++interline) {
				int inptr = 2 * minx + (2 * y + interline) * width;
				for(int x = minx ; x <= maxx; ++x) {
					out[outptr++] = in[inptr++]; 
					out[outptr++] = in[inptr++]; 
				}
			}
		}
		
		result.isCfa = this.isCfa;
		return result;
	}
	
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
	
	public FitsPlane asGreyFits(double mul, int x0, int y0, int x1, int y1)
	{
		FitsPlane result = new FitsPlane(x1 - x0 + 1, y1 - y0 + 1);

		int outId = 0;
		for(int y = y0; y <= y1 ; ++y)
		{
			int inId = 2 * (x0 + y * width);
			
			for(int x = x0; x <= x1; ++x)
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
	
	
	private static class BufferCacheItem {
		
		final ImageDisplayParameter displayParameter;
		final ImageDisplayMetaDataInfo metadataInfo;
		
		BufferCacheItem(ImageDisplayParameter displayParameter, ImageDisplayMetaDataInfo metadataInfo)
		{
			this.displayParameter = displayParameter.clone();
			this.metadataInfo = metadataInfo.clone();
		}
		
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof BufferCacheItem)) return false;
			BufferCacheItem other = (BufferCacheItem)obj;
			return other.displayParameter.equals(displayParameter)
					&& other.metadataInfo.equals(metadataInfo);
		}
		
		@Override
		public int hashCode() {
			return displayParameter.hashCode() ^ metadataInfo.hashCode();
		}
	}
	
	final Cache<BufferCacheItem, BufferedImage> cache = new Cache<BufferCacheItem, BufferedImage>() {
		@Override
		public BufferedImage produce(BufferCacheItem identifier) {
			return produceImage(identifier.displayParameter, identifier.metadataInfo);
		}
	};
	
	/**
	 * Le BufferedImage ne doit pas être modifié (il est partagé)
	 * @param displayParameter
	 * @param metadataInfo
	 * @return
	 */
	public BufferedImage asImage(ImageDisplayParameter displayParameter, ImageDisplayMetaDataInfo metadataInfo)
	{
		BufferCacheItem id = new BufferCacheItem(displayParameter, metadataInfo);
		return cache.get(id);
	}
	
	private BufferedImage produceImage(ImageDisplayParameter displayParameter, ImageDisplayMetaDataInfo metadataInfo)
	{
		
		switch(displayParameter.getChannelMode())
		{
		case Color:
			return asRgbImage(displayParameter, metadataInfo);
		case GreyScale:
		case NarrowBlue:
		case NarrowGreen:
		case NarrowRed:
			return asGreyImage(displayParameter, metadataInfo);
		}
		throw new RuntimeException("unimplemented");
	}
	
	public BufferedImage asGreyImage(ImageDisplayParameter displayParameter, ImageDisplayMetaDataInfo metadataInfo)
	{
		int imgW = width / 2;
		int imgH = height / 2;
		if (imgW < 1) imgW = 1;
		if (imgH < 1) imgH = 1;
		
		BufferedImage result = new BufferedImage(imgW, imgH, BufferedImage.TYPE_BYTE_GRAY);


		ImageDisplayParameter.AduLevelMapper greyMapper = displayParameter.getAduLevelMapper(this, metadataInfo, 0);

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
				int val = greyMapper.getLevelForAdu(((int)buffer[offset]));
				inData[0] = (byte) (val);
				raster.setDataElements(x , y, inData);
			}
		}
		
		return result;
	}
	
	private static final int bayer_r = 0;
	private static final int bayer_g1 = 1;
	private static final int bayer_g2 = 2;
	private static final int bayer_b = 3;
	
	public BufferedImage asRgbImageDebayer(ImageDisplayParameter displayParameter, ImageDisplayMetaDataInfo metadataInfo)
	{
		int imgW = width;
		int imgH = height;
		if (imgW < 1) imgW = 1;
		if (imgH < 1) imgH = 1;
		
		BufferedImage result = new BufferedImage(imgW, imgH, BufferedImage.TYPE_3BYTE_BGR);

		ImageDisplayParameter.AduLevelMapper rMapper = displayParameter.getAduLevelMapper(this, metadataInfo, 0);
		ImageDisplayParameter.AduLevelMapper gMapper = displayParameter.getAduLevelMapper(this, metadataInfo, 1);
		ImageDisplayParameter.AduLevelMapper bMapper = displayParameter.getAduLevelMapper(this, metadataInfo, 2);

		WritableRaster raster = result.getRaster();
		byte [] inData = new byte[3];
		for(int y = 0; y < height; ++y)
		{
			int rowDelta = 2 * (y & 1);
			for(int x = 0; x < width; ++x)
			{
				int offset = (x) + (y) * width;

				int bayerPos = (x & 1) + rowDelta;
				
				// R G1
				// G2 B
				
				int adur, adug, adub;


				// b
				switch(bayerPos) {
					case bayer_b:
						adub = buffer[offset];
						break;
					case bayer_g1:
						// dessus + dessous (les deux peuvent déborder
						if (y > 0 && y + 1 < height) {
							adub = ((int)buffer[offset - width]) + ((int)buffer[offset + width]);
							adub = adub >> 1;
						} else if (y > 0) {
							adub = ((int)buffer[offset - width]);
						} else if (y + 1 < height) {
							adub = ((int)buffer[offset + width]);
						} else {
							// on arrive là si on a une seule ligne
							adub = 0;
						}
						break;
					case bayer_g2:
						// gauche + droite (les deux peuvent déborder)
						if (x > 0 && x + 1 < width) {
							adub = ((int)buffer[offset - 1]) + ((int)buffer[offset + 1]);
							adub = adub >> 1;
						} else if (x > 0) {
							adub = ((int)buffer[offset - 1]);
						} else if (x + 1 < width) {
							adub = ((int)buffer[offset + 1]);
						} else {
							// on arrive là si on a une seule ligne
							adub = 0;
						}
						break;
					case bayer_r:
						// Les 4 diagonales
						int aducount = 0;
						int adusum = 0;
						if (x > 0 && y > 0) {
							adusum += ((int)buffer[offset - 1 - width]);
							aducount++;
						}
						
						if (x > 0 && y + 1 < height) {
							adusum += ((int)buffer[offset - 1 + width]);
							aducount++;
						}
						
						if (x + 1 < width && y > 0) {
							adusum += ((int)buffer[offset + 1 - width]);
							aducount++;
						}
						
						if (x + 1 < width && y + 1 < height) {
							adusum += ((int)buffer[offset + 1 + width]);
							aducount++;
						}
						
						
						if (aducount == 4) {
							adusum = adusum >> 2;
						} else if (aducount == 2) {
							adusum = adusum >> 1;
						} else if (aducount == 3) {
							adusum = adusum / 3;
						}
						adub = adusum;
						break;
					default:
						throw new RuntimeException("invalid bayer (b)");
				}
				// g
				switch(bayerPos) {
					case bayer_r:
					case bayer_b:
						// Les 4 dessus-dessous-gauche-droite
						int aducount = 0;
						int adusum = 0;

						if (x > 0) {
							adusum += ((int)buffer[offset - 1]);
							aducount++;
						}
						if (y > 0) {
							adusum += ((int)buffer[offset - width]);
							aducount++;
						}
						
						if (x + 1 < width) {
							adusum += ((int)buffer[offset + 1]);
							aducount++;
						}
						if (y + 1 < height) {
							adusum += ((int)buffer[offset + width]);
							aducount++;
						}

						if (aducount == 4) {
							adusum = adusum >> 2;
						} else if (aducount == 2) {
							adusum = adusum >> 1;
						} else if (aducount == 3) {
							adusum = adusum / 3;
						}
						adug = adusum;
						break;
					case bayer_g1:
					case bayer_g2:
						adug = buffer[offset];
						break;
					default:
						throw new RuntimeException("invalid bayer (g)");
				}
				
				// r
				switch(bayerPos) {
					case bayer_r:
						adur = buffer[offset];
						break;
					case bayer_g1:
						if (x + 1 < width) {
							adur = ((int)buffer[offset - 1])
									+ ((int)buffer[offset + 1]);
							adur = adur >> 1;
						} else {
							adur = ((int)buffer[offset - 1]);
						}
						break;
					case bayer_g2:
				
						if (y + 1 < height) {
							adur = ((int)buffer[offset - width])
									+ ((int)buffer[offset + width]);
							adur = adur >> 1;
						} else {
							adur = ((int)buffer[offset - width]);
						}
						break;
					case bayer_b:
						if ((x + 1 < width) && (y + 1 < height))
						{
							adur = ((int)buffer[offset - width - 1])
								+ ((int)buffer[offset - width + 1])
								+ ((int)buffer[offset + width - 1])
								+ ((int)buffer[offset + width + 1]);
							adur = adur >> 2;
						} else if (x + 1 < width) 
						{
							adur = ((int)buffer[offset - width - 1])
									+ ((int)buffer[offset - width + 1]);
							adur = adur >> 1;
						
						} else if (y + 1 < height)
						{
							adur = ((int)buffer[offset - width - 1])
									+ ((int)buffer[offset + width - 1]);
							adur = adur >> 1;	
						} else {
							adur = buffer[offset - width - 1];
						}
						break;
					default:
						throw new RuntimeException("invalid bayer pos");
				}


				int r = rMapper.getLevelForAdu(adur);
				int g = gMapper.getLevelForAdu(adug);
				int b = bMapper.getLevelForAdu(adub);
//				r = b;
//				g = b;
//				int g1 = (int)buffer[offset + 1];
//				int g2 = (int)buffer[offset + width]; 
//				int b = displayParameter.getLevelForAdu(metadataInfo, 2, (int)buffer[offset + width + 1]);
//				
//				int g = displayParameter.getLevelForAdu(metadataInfo, 1, (g1 + g2) / 2);
				
				inData[0] = (byte) (r);
				inData[1] = (byte) (g);
				inData[2] = (byte) (b);
				raster.setDataElements(x , y, inData);
			}
		}
		
		return result;
		
	}

	
	public BufferedImage asRgbImage(ImageDisplayParameter displayParameter, ImageDisplayMetaDataInfo metadataInfo)
	{
		int imgW = width / 2;
		int imgH = height / 2;
		if (imgW < 1) imgW = 1;
		if (imgH < 1) imgH = 1;
		
		BufferedImage result = new BufferedImage(imgW, imgH, BufferedImage.TYPE_3BYTE_BGR);

		ImageDisplayParameter.AduLevelMapper rMapper = displayParameter.getAduLevelMapper(this, metadataInfo, 0);
		ImageDisplayParameter.AduLevelMapper gMapper = displayParameter.getAduLevelMapper(this, metadataInfo, 1);
		ImageDisplayParameter.AduLevelMapper bMapper = displayParameter.getAduLevelMapper(this, metadataInfo, 2);
		
		
		WritableRaster raster = result.getRaster();
		byte [] inData = new byte[3 * (width / 2)];
		for(int y = 0; y < height / 2 ; ++y)
		{
			int inDataPtr = 0;
			for(int x = 0; x < width / 2; ++x)
			{
				int offset = (x * 2) + (y * 2) * width;
				int r = rMapper.getLevelForAdu(buffer[offset]);
				int g1 = (int)buffer[offset + 1];
				int g2 = (int)buffer[offset + width]; 
				int b = bMapper.getLevelForAdu((int)buffer[offset + width + 1]);
				
				int g = gMapper.getLevelForAdu((g1 + g2) / 2);
				
				inData[inDataPtr + 0] = (byte) (r);
				inData[inDataPtr + 1] = (byte) (g);
				inData[inDataPtr + 2] = (byte) (b);
				inDataPtr += 3;
			}
			raster.setDataElements(0, y, width / 2, 1, inData);
		}
		
		return result;
		
	}

	public int getAdu(int x, int y)
	{
		if (x < 0 || x >= width) return 0;
		if (y < 0 || y >= height) return 0;
		return (int)buffer[x + width * y];
		
	}
	
	/**
	 * en photosite
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * en photosite
	 */
	public int getHeight() {
		return height;
	}

	public char[] getBuffer() {
		return buffer;
	}

	public boolean isCfa() {
		return isCfa;
	}

	public int getBlack() {
		return black;
	}

	public int getMaximum() {
		return maximum;
	}
	
	public int getAduForHistogramPos(int channel, double pct)
	{
		int nbPix = this.histogramNbPix[channel];
		
		nbPix *= pct;
		
		int pixCount = 0;
		int [] channelHist = this.histogram[channel];
		for(int i = 0; i < channelHist.length; ++i)
		{
			if (pixCount >= nbPix) {
				return black + i;
			}
			pixCount += channelHist[i];
		}
		return maximum;
	}
}
