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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.log4j.Logger;

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
		if (file.getName().toLowerCase().matches(".*\\.cr.")) {
			try {
				File library = new File("../jrawlib/Debug/libjrawlib.dll");

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
			
			return result;
		}		
		throw new RuntimeException("unsupported file type");
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
