//package net.ivoa.fits.test;
//
//import net.ivoa.fits.*;
//import net.ivoa.fits.hdu.ImageHDU;
//import net.ivoa.image.FitsExceptionInvalidSubset;
//import net.ivoa.image.ImageTiler;
//import net.ivoa.util.*;
//
//import java.io.*;
//
//import junit.framework.TestCase;
//
///**
// * This class tests the ImageTiler. It first creates a FITS file and then reads
// * it back and allows the user to select tiles. The values of the corner and
// * center pixels for the selected tile are displayed. Both file and memory tiles
// * are checked.
// */
//public class TestTiler extends TestCase {
//
//	private ImageHDU ihdu;
//
//	// return value of pixel x y
//	private static float getValue(int x, int y) {
//		return 1000 * x * y;
//	}
//
//	protected void setUp() throws Exception {
//		float[][] data = new float[300][300];
//
//		for (int i = 0; i < 300; i += 1) {
//			for (int j = 0; j < 300; j += 1) {
//				data[i][j] = getValue(i, j);
//			}
//		}
//
//		Fits f = new Fits();
//
//		BufferedFile bf = new BufferedFile("tiler1.fits", "rw");
//		f.addHDU(Fits.makeHDU(data));
//
//		f.write(bf);
//		bf.close();
//	}
//
//	protected void tearDown() throws Exception {
//		File fi = new File("tiler1.fits");
//		fi.delete();
//	}
//
//	private void doTestTile(int x, int y, int w, int h) throws Exception {
//		int[] cor = new int[]{x, y};
//		int[] siz = new int[]{w, h};
//
//		ImageTiler t = ihdu.getTiler();
//
//		float[] tile = new float[w * h];
//		t.getTile(tile, cor, siz);
//		for (int pos_x = x; pos_x < (x + w); pos_x++)
//			for (int pos_y = y; pos_y < (y + h); pos_y++) {
//				assertEquals(getValue(pos_x, pos_y), tile[h * (pos_x - x)
//						+ (pos_y - y)], 0);
//			}
//	}
//
//	public void testTiler() throws Exception {
//
//		Fits f = new Fits("tiler1.fits");
//
//		ihdu = (ImageHDU) f.readHDU();
//
//		doTestTile(10, 20, 40, 50);
//		doTestTile(0, 0, 300, 300);
//		doTestTile(0, 270, 45, 30);
//		try {
//			doTestTile(0, 0, 301, 301);
//			fail("Get tile after end of image");
//		} catch (FitsExceptionInvalidSubset ex) {
//		}
//	}
//}