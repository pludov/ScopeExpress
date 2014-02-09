package fr.pludov.io;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

import javax.imageio.ImageIO;

import net.ivoa.fits.Fits;
import net.ivoa.fits.data.Data;
import net.ivoa.fits.hdu.BasicHDU;
import net.ivoa.fits.hdu.ImageHDU;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.log4j.Logger;

import fr.pludov.cadrage.ui.utils.Utils;
import fr.pludov.utils.ChannelMode;

public class ImageProvider {
	private static final Logger logger = Logger.getLogger(ImageProvider.class);
	
	static class FileStatus
	{
		boolean loading;
		IOException problem;
		SoftReference<CameraFrame> frame;
	}
	
	static HashMap<File, FileStatus> frames = new HashMap<File, FileStatus>();
	
	private static void cleanCache()
	{
		synchronized(frames)
		{
			for(Iterator<Map.Entry<File, FileStatus>> it = frames.entrySet().iterator(); it.hasNext();)
			{
				Map.Entry<File, FileStatus> frameEntry = it.next(); 
				FileStatus status = frameEntry.getValue();
				if (status.loading) continue;
				if (status.problem != null) continue;
				if (status.frame.get() == null) {
					it.remove();
				}
			}
		}
	}
	
	public static CameraFrame readImage(final File file) throws IOException
	{
		FileStatus status;

		synchronized(frames)
		{
			cleanCache();

			// Attendre
			while(true) {
				status = frames.get(file);
				if (status != null && status.loading) {
					try {
						frames.wait();
					} catch(InterruptedException e) {
					}
				} else {
					if (status != null) {
						if (status.problem == null) {
							CameraFrame frame = status.frame.get();
							if (frame != null) return frame;
							status.loading = true;
							break;
						} else {
							throw status.problem;
						}
					} else {
						status = new FileStatus();
						status.loading = true;
						status.problem = null;
						status.frame = null;
						
						frames.put(file,  status);
						break;
					}
				}
			}
		}
		
		IOException problem = null;
		CameraFrame frame = null;
		// Charger l'image
		try {
			frame = doReadImage(file);
		} catch(Throwable t)
		{
			if (t instanceof IOException) {
				problem = (IOException)t;
			} else {
				problem = new IOException("Generic error", t);
			}
			frame = null;
		}
		
		synchronized(frames)
		{
			status.loading = false;
			status.problem = problem;
			if (frame != null) {
				status.frame = new SoftReference<CameraFrame>(frame);
			} else {
				status.frame = null;
			}
			frames.notifyAll();
		}
		
		if (problem != null) throw problem;
		return frame;
	}
	
	
	
	
	public static int parseInt(InputStream stream, char stop) throws IOException
	{
		StringBuilder result = new StringBuilder();
		
		while(true) {
			char c = (char)stream.read();
			if (c == stop) return Integer.parseInt(result.toString());
			if (c < '0' || c > '9') throw new IOException("PPM read error");
			result.append(c);
		}
	}
	
	private static CameraFrame doReadImage(final File file) throws IOException
	{
		if (file.getName().toLowerCase().matches(".*\\.fit(|s)")) {
			try {
				Fits fits = new Fits(file);
				BasicHDU basicHDU = fits.getHDU(0);
				if (basicHDU instanceof ImageHDU) {
					ImageHDU imageHDU = (ImageHDU) basicHDU;
					
					int [] axes = imageHDU.getAxes();
					int width = axes[1];
					int height = axes[0];
					
					Data data = imageHDU.getData();
					Object uncasteddatas = data.getData();
					char [] datas = null;
					if (uncasteddatas instanceof char[][]) {
						char [][] sdatas = (char[][]) uncasteddatas;
						datas = new char[width * height];
						int i = 0;
						for(int y = 0; y < height; ++y) {
							char [] sline = sdatas[y];
							for(int x = 0; x < width; ++x) {
								datas[i++] = sline[x]; 
							}
						}
					} else if (uncasteddatas instanceof short[][]) {
						short [][] sdatas = (short[][]) uncasteddatas;
						datas = new char[width * height];
						int i = 0;
						for(int y = 0; y < height; ++y) {
							short [] sline = sdatas[y];
							for(int x = 0; x < width; ++x) {
								datas[i++] = (char)((((int)sline[x]) - Short.MIN_VALUE) / 2); 
							}
						}
					}
					
					if (datas != null) {
						CameraFrame result = new CameraFrame();
						result.buffer = (char[])datas;
						result.width = width;
						result.height = height;
						// FIXME : en dur, aller chercher dans les méta du fits !
						result.isCfa = true;
						result.scanPixelsForHistogram();
						
						return result;
					
					}

				}
				throw new Exception("c'est pas fini !");
			} catch(Exception e) {
				throw new IOException("Echec de lecture FITS", e);
			}
		} else if (file.getName().toLowerCase().matches(".*\\.cr.")) {
			try {
				File library = Utils.locateDll("libjrawlib.dll");

				System.load(library.getAbsolutePath());
			} catch(Throwable t) {
				logger.warn("unable to load jrawlib:", t);
			}

			JRawLib loader = new JRawLib();
			
			loader.load(file);
			int width = loader.getWidth();
			int height = loader.getHeight();
			char [] charBuffer = loader.getData();
			CameraFrame result = new CameraFrame();
			result.buffer = charBuffer;
			result.width = loader.getWidth();
			result.height = loader.getHeight();
			result.maximum = loader.getMaximum();
			result.black = loader.getBlack();
			result.histogram = new int[3][];
			result.histogram[0] = loader.getRedHistogram();
			result.histogram[1] = loader.getGreenHistogram();
			result.histogram[2] = loader.getBlueHistogram();
			
			result.histogramNbPix = new int[3];
			result.histogramNbPix[0] = width * height / 4;
			result.histogramNbPix[1] = width * height / 2;
			result.histogramNbPix[2] = width * height / 4;
			
			return result;
		} else {
			
			
			BufferedImage buffer = ImageIO.read(file);
			if (buffer == null) {
				throw new IOException("Unsupported file format: " + file);
			}
			
			int width = buffer.getWidth();
			int height = buffer.getHeight();
			
			int outwidth = width + (width & 1);
			int outheight = height + (height & 1);
			char [] charBuffer = new char[outwidth * outheight];
			// On fait un "rebayer"
			for(int y = 0; y < height; ++y)
			{
				int basey = 2 * (y / 2);
				for(int x = 0; x < width; ++x)
				{
					int basex = 2 * (x/2);
					
					int rgb = buffer.getRGB(x, y);
					int r = rgb & 0xff;
					int g = (rgb >> 8) & 0xff;
					int b = (rgb >> 16) & 0xff;
					
					// G R
					// B G
					charBuffer[basex + outwidth * basey] += r;
					charBuffer[basex + 1 + outwidth * basey] += g;
					charBuffer[basex + outwidth * (basey + 1)] += g;
					charBuffer[basex + 1 + outwidth * (basey + 1)] += b;
				}
			}
			
			CameraFrame result = new CameraFrame();
			result.buffer = charBuffer;
			result.width = outwidth;
			result.height = outheight;
			result.maximum = 4 * 255;
			result.black = 0;
			result.histogram = new int[3][];
			result.histogram[0] = new int[4 * 255 + 1];
			result.histogram[1] = new int[4 * 255 + 1];
			result.histogram[2] = new int[4 * 255 + 1];
			
			for(int y = 0; y < outheight; ++y)
				for(int x = 0; x < outwidth; ++x)
				{
					int chid = ChannelMode.getRGBBayerId(x, y);
					int adu = charBuffer[x + (width / 2) * y];
					result.histogram[chid][adu]++;
				}
			
			result.histogramNbPix = new int[3];
			result.histogramNbPix[0] = outwidth * outheight / 4;
			result.histogramNbPix[1] = outwidth * outheight / 2;
			result.histogramNbPix[2] = outwidth * outheight / 4;
			
			return result;
		}
	}
	
//	public static BufferedImage readImage(final File file) throws IOException
//	{
//
//		if (file.getName().toLowerCase().matches(".*\\.cr.")) {
//			JRawLib loader = new JRawLib();
//			
//			loader.load(file);
//			int width = loader.getWidth();
//			int height = loader.getHeight();
//			char [] charBuffer = loader.getData();
//			BufferedImage result = new BufferedImage(width / 2, height / 2, BufferedImage.TYPE_BYTE_GRAY);
//
//			WritableRaster raster = result.getRaster();
//			byte [] inData = new byte[1];
//			for(int y = 0; y < height / 2 ; ++y)
//			{
//				for(int x = 0; x < width / 2; ++x)
//				{
//					int offset = (x * 2) + (y * 2) * width;
////					int val = ((int)charBuffer[offset] + 
////								(int)charBuffer[offset + 1] + 
////								(int)charBuffer[offset + width] + 
////								(int)charBuffer[offset + width + 1] ) / (4 * 8);
//					int val = ((int)charBuffer[offset]) / 4;
//					if (val > 255) val = 255;
//					inData[0] = (byte) (val);
//					raster.setDataElements(x , y, inData);
//				}
//			}
//						
//			return result;
//		} else {
//			BufferedImage result = ImageIO.read(file);
//			if (result == null) {
//				throw new IOException("Unsupported file format: " + file);
//			}
//			
//			return result;
//		}
//	}
}

