package net.ivoa.fits.test;

import net.ivoa.fits.*;
import net.ivoa.fits.hdu.BasicHDU;
import net.ivoa.fits.hdu.ImageHDU;
import net.ivoa.image.*;
import net.ivoa.util.*;

import java.io.File;

import junit.framework.TestCase;

/**
 * Test the ImageHDU, ImageData and ImageTiler classes. - multiple HDU's in a
 * single file - deferred input of HDUs - creating and reading arrays of all
 * permitted types. - Tiles of 1, 2 and 3 dimensions - from a file - from
 * internal data - Multiple tiles extracted from an image.
 */
public class TestImage extends TestCase {
	private byte[][] bimg;

	protected void setUp() {
		bimg = new byte[40][40];
		for (int i = 10; i < 30; i += 1) {
			for (int j = 10; j < 30; j += 1) {
				bimg[i][j] = (byte) (i + j);
			}
		}
	}

	public void testImage() throws Exception {
		Fits f = new Fits();

		short[][] simg = (short[][]) ArrayFuncs.convertArray(bimg, short.class);
		int[][] iimg = (int[][]) ArrayFuncs.convertArray(bimg, int.class);
		long[][] limg = (long[][]) ArrayFuncs.convertArray(bimg, long.class);
		float[][] fimg = (float[][]) ArrayFuncs.convertArray(bimg, float.class);
		double[][] dimg = (double[][]) ArrayFuncs.convertArray(bimg,
				double.class);
		int[][][] img3 = new int[10][20][30];
		for (int i = 0; i < 10; i += 1) {
			for (int j = 0; j < 20; j += 1) {
				for (int k = 0; k < 30; k += 1) {
					img3[i][j][k] = i + j + k;
				}
			}
		}

		double[] img1 = (double[]) ArrayFuncs.flatten(dimg);

		// Make HDUs of various types.
		f.addHDU(Fits.makeHDU(bimg));
		f.addHDU(Fits.makeHDU(simg));
		f.addHDU(Fits.makeHDU(iimg));
		f.addHDU(Fits.makeHDU(limg));
		f.addHDU(Fits.makeHDU(fimg));
		f.addHDU(Fits.makeHDU(dimg));
		f.addHDU(Fits.makeHDU(img3));
		f.addHDU(Fits.makeHDU(img1));

		// Write a FITS file.

		BufferedFile bf = new BufferedFile("image2.fits", "rw");
		f.write(bf);
		bf.flush();
		bf.close();

		// Read a FITS file
		f = new Fits("image2.fits");

		BasicHDU[] hdus = f.read();

		ImageHDU h = (ImageHDU) hdus[1];

		// Put this HDU at the beginning of the FITS file
		f.insertHDU(h, 0);

		// Make it an extension again!
		f.insertHDU(hdus[0], 0);

		// Check out image tiling from files.

		ImageTiler t = h.getTiler();
		short[] stile = (short[]) t.getTile(new int[]{10, 10}, new int[]{2, 2});
		assertEquals(4, stile.length);
		assertEquals(20, stile[0]);
		assertEquals(21, stile[1]);
		assertEquals(21, stile[2]);
		assertEquals(22, stile[3]);

		stile = (short[]) t.getTile(new int[]{20, 20}, new int[]{2, 2});
		assertEquals(40, stile[0]);
		assertEquals(41, stile[1]);
		assertEquals(41, stile[2]);
		assertEquals(42, stile[3]);

		short[] xtile = new short[4];
		t.getTile(xtile, new int[]{20, 20}, new int[]{2, 2});
		assertEquals(40, xtile[0]);
		assertEquals(41, xtile[1]);
		assertEquals(41, xtile[2]);
		assertEquals(42, xtile[3]);

		// Check a 3-d image.
		h = (ImageHDU) hdus[6];
		t = h.getTiler();
		int[] itile = (int[]) t.getTile(new int[]{3, 3, 3}, new int[]{2, 2, 2});
		assertEquals(8, itile.length);
		assertEquals(9, itile[0]);
		assertEquals(10, itile[1]);
		assertEquals(10, itile[2]);
		assertEquals(11, itile[3]);
		assertEquals(10, itile[4]);
		assertEquals(11, itile[5]);
		assertEquals(11, itile[6]);
		assertEquals(12, itile[7]);

		// How about a 1-d image.
		h = (ImageHDU) hdus[7];
		t = h.getTiler();
		double[] dtile = (double[]) t.getTile(new int[]{410}, new int[]{3});
		assertEquals(3, dtile.length);
		assertEquals(20.0d, dtile[0], 0d);
		assertEquals(21.0d, dtile[1], 0d);
		assertEquals(22.0d, dtile[2], 0d);

		// Check from memory tiling!

		t = ((ImageHDU) hdus[1]).getTiler();
		stile = (short[]) t.getTile(new int[]{20, 20}, new int[]{2, 2});
		assertEquals(40, stile[0]);
		assertEquals(41, stile[1]);
		assertEquals(41, stile[2]);
		assertEquals(42, stile[3]);

		stile = (short[]) t.getTile(new int[]{25, 25}, new int[]{2, 2});
		assertEquals(50, stile[0]);
		assertEquals(51, stile[1]);
		assertEquals(51, stile[2]);
		assertEquals(52, stile[3]);

		h = (ImageHDU) hdus[6];
		t = h.getTiler();
		itile = (int[]) t.getTile(new int[]{3, 3, 3}, new int[]{2, 2, 2});
		assertEquals(8, itile.length);
		assertEquals(9, itile[0]);
		assertEquals(10, itile[1]);
		assertEquals(10, itile[2]);
		assertEquals(11, itile[3]);
		assertEquals(10, itile[4]);
		assertEquals(11, itile[5]);
		assertEquals(11, itile[6]);
		assertEquals(12, itile[7]);

		h = (ImageHDU) hdus[7];
		t = h.getTiler();
		dtile = (double[]) t.getTile(new int[]{410}, new int[]{3});
		assertEquals(3, dtile.length);
		assertEquals(20.0d, dtile[0], 0d);
		assertEquals(21d, dtile[1], 0d);
		assertEquals(22d, dtile[2], 0d);

		File fi = new File("image2.fits");
		fi.delete();
	}
	
	public void testCharImage () throws Exception
	{
		char[][] cimg = (char[][]) ArrayFuncs.convertArray(bimg, char.class);
		Fits f = new Fits ();
		
		f.addHDU(Fits.makeHDU(cimg));

		// Write a FITS file.

		BufferedFile bf = new BufferedFile("image3.fits", "rw");
		f.write(bf);
		bf.flush();
		bf.close();
		
		f = new Fits("image3.fits");
		BasicHDU[] bhdu = f.read();
	}

}