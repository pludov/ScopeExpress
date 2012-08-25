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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.log4j.Logger;

public class ImageProvider {
	private static final Logger logger = Logger.getLogger(ImageProvider.class);
	
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
	
	public static BufferedImage readImage(final File file) throws IOException
	{
		try {
//			System.loadLibrary("mingwm10");
//			File library = new File("../jrawlib/Debug/libgcc_s_dw2-1.dll");
//			System.load(library.getAbsolutePath());
			
			File library = new File("../jrawlib/Debug/libjrawlib.dll");

			System.load(library.getAbsolutePath());
		} catch(Throwable t) {
			logger.warn("unable to load jrawlib:", t);
		}

		if (file.getName().toLowerCase().matches(".*\\.cr.")) {
			JRawLib loader = new JRawLib();
			
			loader.load(file);
//
//			// custom white balance
//			int r, g_1, b, g_2;
//			
//			final Process process;
//			
//			try {
//				File dcraw = new File("dll\\dcraw.exe");
//				
//				process = Runtime.getRuntime().exec(new String[]{dcraw.getCanonicalPath(), "-c", "-D", "-4", file.getPath()});
//			} catch(IOException e) { 
//				logger.warn("unable to start dcraw:" , e);
//				throw new IOException("Unable to decode raw file. Setup dcraw.exe");
//			}
//			
//			final InputStream stream = process.getInputStream();
//			final InputStream error = process.getErrorStream();
//			new Thread("dcraw") {
//				public void run() {
//					try {
//						LineIterator iterator = IOUtils.lineIterator(error, (Charset)null);
//					
//						while(iterator.hasNext())
//						{
//							String line = iterator.next();
//							logger.warn(file.getPath() + ": " + line);
//						}
//						IOUtils.closeQuietly(error);
//					} catch(IOException e) {
//						logger.warn("IOException in dcraw error stream", e);
//					} finally {
//						try { 
//							process.waitFor();
//							logger.debug(file.getPath() +"=> done");
//						} catch(InterruptedException e) {
//						}
//					}
//				};
//			}.start();
//			
//			if (stream instanceof BufferedInputStream)
//			{
//				// stream = ((BufferedInputStream)stream);
//				BufferedInputStream bis = (BufferedInputStream)stream;
//				bis.markSupported();
//			}
//			if ((char)stream.read() != 'P' || ((char)stream.read() != '5') || ((char)stream.read() != '\n'))
//			{
//				throw new IOException("invalid PPM / not P5");
//			}
//			int width = parseInt(stream, ' ');
//			int height = parseInt(stream, '\n');
//			int pixels = parseInt(stream, '\n');
//			
//			if (pixels != 65535) {
//				throw new IOException("invalid PPM / not 16 bits");
//			}
//			byte [] content = new byte[2 * width * height * 4];
//			IOUtils.read(stream, content);
//			ByteBuffer byteBuffer = ByteBuffer.wrap(content);
//			CharBuffer charBuffer = byteBuffer.asCharBuffer();
			

			// new PPMImage(width, height, charBuffer);
			//buf.getRaster().
			int width = loader.getWidth();
			int height = loader.getHeight();
			char [] charBuffer = loader.getData();
			BufferedImage result = new BufferedImage(width / 2, height / 2, BufferedImage.TYPE_BYTE_GRAY);

			WritableRaster raster = result.getRaster();
			byte [] inData = new byte[1];
			for(int y = 0; y < height / 2 ; ++y)
			{
				for(int x = 0; x < width / 2; ++x)
				{
					int offset = (x * 2) + (y * 2) * width;
//					int val = ((int)charBuffer[offset] + 
//								(int)charBuffer[offset + 1] + 
//								(int)charBuffer[offset + width] + 
//								(int)charBuffer[offset + width + 1] ) / (4 * 8);
					int val = ((int)charBuffer[offset]) / 4;
					if (val > 255) val = 255;
					inData[0] = (byte) (val);
					raster.setDataElements(x , y, inData);
				}
			}
						
			return result;
		} else {
			BufferedImage result = ImageIO.read(file);
			if (result == null) {
				throw new IOException("Unsupported file format: " + file);
			}
			
			return result;
		}
	}
}
