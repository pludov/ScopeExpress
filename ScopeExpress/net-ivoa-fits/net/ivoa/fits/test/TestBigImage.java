package net.ivoa.fits.test;

import net.ivoa.fits.*;
import net.ivoa.util.*;

import java.io.File;
import java.util.*;

import junit.framework.TestCase;

public class TestBigImage extends TestCase {

	public void testWrite() throws Exception {
		Fits f = new Fits();

		int[][] iimg = new int[4000][4000];
		long time0 = new Date().getTime();
		f.addHDU(Fits.makeHDU(iimg));
		//		System.err.println("Adding HDU: "
		//				+ ((new Date().getTime() - time0) / 1000.) + " seconds");
		BufferedFile bf = new BufferedFile("image1.fits", "rw");
		f.write(bf);
		bf.flush();
		bf.close();
		bf = null;
		f = null;
		//		System.err.println("Write HDU:"
		//+ ((new Date().getTime() - time0) / 1000.) + " seconds");

		File fi = new File("image.fits");
		fi.delete();
	}
}