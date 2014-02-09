//package net.ivoa.fits.test;
//
//import java.io.File;
//
//import junit.framework.TestCase;
//import net.ivoa.fits.*;
//import net.ivoa.fits.data.AsciiTable;
//import net.ivoa.fits.hdu.BasicHDU;
//import net.ivoa.util.*;
//
//public class TestFitsCopy extends TestCase {
//
//	private float[] realCol;
//
//	private int[] intCol;
//
//	private long[] longCol;
//
//	private double[] doubleCol;
//
//	private String[] strCol;
//
//	public TestFitsCopy(String name) {
//		super(name);
//		realCol = new float[50];
//		for (int i = 0; i < realCol.length; i += 1) {
//			realCol[i] = 10000.F * (i) * (i) * (i);
//		}
//
//		intCol = (int[]) ArrayFuncs.convertArray(realCol, int.class);
//		longCol = (long[]) ArrayFuncs.convertArray(realCol, long.class);
//		doubleCol = (double[]) ArrayFuncs.convertArray(realCol, double.class);
//
//		strCol = new String[realCol.length];
//
//		for (int i = 0; i < realCol.length; i += 1) {
//			strCol[i] = "ABC" + String.valueOf(realCol[i]) + "CDE";
//		}
//	}
//
//	public void test1() throws Exception {
//		FitsFactory.setUseAsciiTables(true);
//		AsciiTable data = new AsciiTable();
//
//		data.addColumn(longCol);
//		data.addColumn(realCol);
//		data.addColumn(intCol, 20);
//		data.addColumn(strCol, 10);
//
//		Fits f = new Fits();
//		f.addHDU(Fits.makeHDU(data));
//
//		// Create a table row by row .
//		data = new AsciiTable();
//		Object[] row = new Object[4];
//		for (int i = 0; i < realCol.length; i += 1) {
//			row[0] = new String[]{strCol[i]};
//			row[3] = new float[]{realCol[i]};
//			row[1] = new int[]{intCol[i]};
//			row[2] = new double[]{doubleCol[i]};
//			data.addRow(row);
//		}
//		f.addHDU(Fits.makeHDU(data));
//
//		BufferedFile bf = new BufferedFile("test1.fits", "rw");
//		f.write(bf);
//
//		bf.flush();
//		bf.close();
//
//		f = new Fits("test1.fits");
//
//		int i = 0;
//		BasicHDU h;
//
//		do {
//			h = f.readHDU();
//			if (h != null) {
//				Object[] info = (Object[]) h.getKernel();
//				i += 1;
//			}
//		} while (h != null);
//
//		bf = new BufferedFile("test2.fits", "rw");
//		f.write(bf);
//		bf.close();
//
//		File fi = new File("test1.fits");
//		fi.delete();
//
//		fi = new File("test2.fits");
//		fi.delete();
//	}
//}