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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;
import java.util.WeakHashMap;

import javax.imageio.ImageIO;

import net.ivoa.fits.Fits;
import net.ivoa.fits.FitsException;
import net.ivoa.fits.FitsExceptionNoKey;
import net.ivoa.fits.data.Data;
import net.ivoa.fits.hdu.BasicHDU;
import net.ivoa.fits.hdu.ImageHDU;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.log4j.Logger;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.CanonMakernoteDirectory;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import fr.pludov.scopeexpress.ui.utils.Utils;
import fr.pludov.scopeexpress.utils.Couple;
import fr.pludov.utils.ChannelMode;

public class ImageProvider {
	private static final Logger logger = Logger.getLogger(ImageProvider.class);
	
	static class FileStatus
	{
		boolean loading;
		IOException problem;
		SoftReference<Couple<CameraFrame, CameraFrameMetadata>> frame;
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
	
	public static Couple<CameraFrame, CameraFrameMetadata> readImage(final File file) throws IOException
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
							Couple<CameraFrame,CameraFrameMetadata> frame = status.frame.get();
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
		Couple<CameraFrame, CameraFrameMetadata> frame = null;
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
				status.frame = new SoftReference<Couple<CameraFrame, CameraFrameMetadata>>(frame);
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
	
	public static Couple<CameraFrame, CameraFrameMetadata> readImageMetadata(final File file) throws IOException
	{
		if (isFits(file)) {
			return doReadImage(file);
		}
		
		try {
			CameraFrameMetadata result = new CameraFrameMetadata();
			
			Metadata metadata = ImageMetadataReader.readMetadata(file);
			Directory directory = metadata.getDirectory(ExifSubIFDDirectory.class);
			if (directory != null) {
				Double pause = directory.getDoubleObject(ExifSubIFDDirectory.TAG_EXPOSURE_TIME);
				result.setDuration(pause);
				
				Integer iso = directory.getInteger(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT);
				result.setGain(iso != null ? iso.doubleValue() : null);
				
				double cvFactor = 0.1; // Pour les CM
				Integer unit;
				unit = directory.getInteger(ExifSubIFDDirectory.TAG_FOCAL_PLANE_UNIT);
				if (unit == null) unit = 2;
				switch(unit) {
				case 2:
					cvFactor = 0.03937007870;
				case 1:
					Double xPixSize = null, yPixSize = null;
					if (directory.getRational(ExifSubIFDDirectory.TAG_FOCAL_PLANE_X_RES) != null) {
						// Pas de convertion
						double pixPerMm;
						// Pour l'avoir en pixel par mm:
						pixPerMm = directory.getRational(ExifSubIFDDirectory.TAG_FOCAL_PLANE_X_RES).doubleValue();
						pixPerMm *= cvFactor;
						
						xPixSize = 1000 / pixPerMm;
					}
					if (directory.getRational(ExifSubIFDDirectory.TAG_FOCAL_PLANE_Y_RES) != null) {
						// Pas de convertion
						double pixPerMm;
						// Pour l'avoir en pixel par mm:
						pixPerMm = directory.getRational(ExifSubIFDDirectory.TAG_FOCAL_PLANE_Y_RES).doubleValue();
						pixPerMm *= cvFactor;
						
						yPixSize = 1000 / pixPerMm;
					}
					if (xPixSize == null) {
						xPixSize = yPixSize;
					} else if (yPixSize == null) {
						yPixSize = xPixSize;
					} else {
						// On a deux valeurs
						double ratio = xPixSize / yPixSize;
						
						if (ratio > 0.99 && ratio < 1.01) {
							xPixSize = (xPixSize + yPixSize) / 2.0;
							yPixSize = xPixSize;
						}
					}
					result.setPixSizeX(xPixSize);
					result.setPixSizeY(yPixSize);
				}
				
				
			
			}
//			
//			ExifIFD0Directory directoryExifIFD0Directory = metadata.getDirectory(ExifIFD0Directory.class);
//			if (directoryExifIFD0Directory != null) {
//				String model = directoryExifIFD0Directory.getString(ExifIFD0Directory.TAG_MODEL);
//				result.setInstrument(model);
//				
//				Double xRes = directoryExifIFD0Directory.getDoubleObject(ExifIFD0Directory.TAG_X_RESOLUTION);
//				Double yRes = directoryExifIFD0Directory.getDoubleObject(ExifIFD0Directory.TAG_Y_RESOLUTION);
//				Integer resUnit = directoryExifIFD0Directory.getInteger(ExifIFD0Directory.TAG_RESOLUTION_UNIT);
//				System.out.println("plo)");
//			}
			

			CanonMakernoteDirectory directoryCanonMakernoteDirectory = metadata.getDirectory(CanonMakernoteDirectory.class);
			if (directoryCanonMakernoteDirectory != null) {
				Integer temp = directoryCanonMakernoteDirectory.getInteger(CanonMakernoteDirectory.ShotInfo.TAG_CAMERA_TEMPERATURE);
				if (temp != null && temp.intValue() != 0) {
					result.setCcdTemp(new Double(temp - 128));
				}
			}
			
			for (Directory directory2 : metadata.getDirectories()) {
			    for (Tag tag : directory2.getTags()) {
			        System.out.println(tag);
			    }
			}
			
			return new Couple<CameraFrame,CameraFrameMetadata>(null,result);
		} catch(ImageProcessingException e) {
			e.printStackTrace();
			return new Couple<CameraFrame,CameraFrameMetadata>(null, new CameraFrameMetadata());
		}
	}
	
	private static Couple<CameraFrame, CameraFrameMetadata> doReadImage(final File file) throws IOException
	{
		if (isFits(file)) {
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
								// FIXME : pkoi /2 ?
								datas[i++] = (char)((((int)sline[x]) - Short.MIN_VALUE) / 2); 
							}
						}
					}
					
					if (datas != null) {
						CameraFrame result = new CameraFrame();
						result.buffer = (char[])datas;
						result.width = width;
						result.height = height;
						try {
							String bayerPat = imageHDU.getTrimmedString("BAYERPAT");
							result.isCfa = bayerPat != null && !"".equals(bayerPat);
						} catch(FitsExceptionNoKey nokey) {
							result.isCfa = false;
						}
						result.maximum = 32767;
						
						CameraFrameMetadata fitsMetadata = new CameraFrameMetadata();
						fitsMetadata.setInstrument(imageHDU.getInstrument());
						Date obsDate = imageHDU.getObservationDate();
						if (obsDate != null) {
							fitsMetadata.setStartMsEpoch(obsDate.getTime());
						}
						
						try {
							fitsMetadata.setDuration(imageHDU.getHeader().getDoubleValue("EXPTIME"));
						} catch(FitsException e) {}
						try {
							fitsMetadata.setBinX(imageHDU.getHeader().getIntValue("XBINNING"));
						} catch(FitsException e) {}
						try {
							fitsMetadata.setBinY(imageHDU.getHeader().getIntValue("YBINNING"));
						} catch(FitsException e) {}
						try {
							fitsMetadata.setGain(imageHDU.getHeader().getDoubleValue("EGAIN"));
						} catch(FitsException e) {}
						try {
							fitsMetadata.setCcdTemp(imageHDU.getHeader().getDoubleValue("CCD-TEMP"));
						} catch(FitsException e) {}
						
						try {
							fitsMetadata.setPixSizeX(imageHDU.getHeader().getDoubleValue("XPIXSZ"));
						} catch(FitsException e) {}
						try {
							fitsMetadata.setPixSizeY(imageHDU.getHeader().getDoubleValue("YPIXSZ"));
						} catch(FitsException e) {}
						
						// Il reste: IMAGETYP et PIXEL SIZE X ET Y
						
						return new Couple<CameraFrame, CameraFrameMetadata>(result, fitsMetadata);
					}

				}
				throw new RuntimeException("FITS non reconnu");
			} catch(Exception e) {
				throw new IOException("Echec de lecture FITS", e);
			}
		} else if (isCr2(file)) {
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
			result.isCfa = true;
			
			return new Couple<CameraFrame, CameraFrameMetadata>(result, null);
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
			
			return new Couple<CameraFrame, CameraFrameMetadata>(result, null);
		}
	}

	private static Integer parseFitsInteger(String trimmedString) {
		if (trimmedString == null || "".equals(trimmedString)) {
			return null;
		}
		try {
			return Integer.parseInt(trimmedString);
		} catch(NumberFormatException e) {
			e.printStackTrace();
			return null;
		}
	}

	private static Double parseFitsDouble(String trimmedString) {
		if (trimmedString == null || "".equals(trimmedString)) {
			return null;
		}
		try {
			return Double.valueOf(trimmedString);
		} catch(NumberFormatException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static boolean isCr2(final File file) {
		return file.getName().toLowerCase().matches(".*\\.cr.");
	}

	public static boolean isFits(final File file) {
		return file.getName().toLowerCase().matches(".*\\.fit(|s)");
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

