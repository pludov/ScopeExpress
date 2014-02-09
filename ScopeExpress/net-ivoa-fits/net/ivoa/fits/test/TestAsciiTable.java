//package net.ivoa.fits.test;
//
//import java.io.File;
//
//import junit.framework.TestCase;
//import net.ivoa.fits.*;
//import net.ivoa.fits.data.AsciiTable;
//import net.ivoa.fits.hdu.AsciiTableHDU;
//import net.ivoa.util.*;
//
///**
// * This class tests the AsciiTableHDU and AsciiTable FITS classes and implicitly
// * the ByteFormatter and ByteParser classes in the nam.tam.util library. Tests
// * include: Create columns of every type Read columns of every type Create a
// * table column by column Create a table row by row Use deferred input on rows
// * Use deferred input on elements Read rows, columns and elements from in-memory
// * kernel. Specify width of columns. Rewrite data/header in place. Set and read
// * null elements.
// */
//public class TestAsciiTable extends TestCase {
//
//	private Fits f;
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
//	protected void setUp() {
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
//		
//		FitsFactory.setUseAsciiTables(true);
//	}
//
//	// if JUnit decide to run those methods in wrong order, then we are losts
//	// but that should'n happen
//	public void testAsciiTableCreate() throws Exception {
//		Object[] obj = new Object[]{realCol, intCol, longCol, doubleCol, strCol};
//		f = new Fits();
//		f.addHDU(Fits.makeHDU(obj));
//
//		BufferedFile bf = new BufferedFile("at1.fits", "rw");
//		f.write(bf);
//
//		bf.flush();
//		bf.close();
//	}
//
//	public void testAsciiTableRead() throws Exception {
//		f = new Fits("at1.fits");
//
//		AsciiTableHDU hdu = (AsciiTableHDU) f.getHDU(1);
//		Object[] info = (Object[]) hdu.getKernel();
//		float[] f1 = (float[]) info[0];
//		String[] s1 = (String[]) info[4];
//
//		for (int i = 0; i < f1.length; i += 1) {
//			assertEquals(realCol[i], f1[i], 0.0f);
//			assertEquals(strCol[i], s1[i].trim());
//		}
//	}
//
//	public void testAsciiTableCreate2() throws Exception {
//		AsciiTable data = new AsciiTable();
//
//		data.addColumn(longCol);
//		data.addColumn(realCol);
//		data.addColumn(intCol, 20);
//		data.addColumn(strCol, 10);
//
//		f = new Fits();
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
//		BufferedFile bf = new BufferedFile("at2.fits", "rw");
//		f.write(bf);
//
//		bf.flush();
//		bf.close();
//
//	}
//
//	public void testAsciiTableRead2() throws Exception {
//		f = new Fits("at2.fits");
//		for (int i = 0; i < 3; i += 1) {
//			f.readHDU().toString();
//		}
//		Object[] info = (Object[]) f.getHDU(1).getKernel();
//		float[] f1 = (float[]) info[1];
//		String[] s1 = (String[]) info[3];
//
//		for (int i = 0; i < 10; i += 1) {
//			assertEquals(realCol[i], f1[i], 0.0f);
//			assertTrue(strCol[i].startsWith(s1[i].trim()));
//		}
//		info = (Object[]) f.getHDU(2).getKernel();
//		f1 = (float[]) info[3];
//		s1 = (String[]) info[0];
//
//		for (int i = 0; i < 10; i += 1) {
//			assertEquals(realCol[i], f1[i], 0.0f);
//			assertEquals(strCol[i], s1[i].trim());
//		}
//
//		f = new Fits("at1.fits");
//
//		AsciiTableHDU hdu = (AsciiTableHDU) f.getHDU(1);
//		AsciiTable data = (AsciiTable) hdu.getData();
//
//		for (int i = 0; i < 10; i += 1) {
//			Object[] row = data.getRow(i);
//			float[] f2 = (float[]) row[0];
//			String[] s2 = (String[]) row[4];
//			f1 = (float[]) data.getElement(i, 0);
//			s1 = (String[]) data.getElement(i, 4);
//			assertEquals(f1[0], f2[0], 0.0f);
//			assertEquals(s1[0], s2[0]);
//		}
//
//		f1 = (float[]) data.getColumn(0);
//		s1 = (String[]) data.getColumn(4);
//		for (int i = 0; i < 10; i += 1) {
//			assertEquals(realCol[i], f1[i], 0.0f);
//			assertEquals(s1[i].trim(), strCol[i]);
//		}
//
//		for (int i = 0; i < 10; i += 1) {
//			Object[] row = data.getRow(i);
//			f1 = (float[]) row[0];
//			s1 = (String[]) row[4];
//			float[] f2 = (float[]) data.getElement(i, 0);
//			String[] s2 = (String[]) data.getElement(i, 4);
//			assertEquals(f1[0], f2[0], 0.0f);
//			assertEquals(s1[0], s2[0]);
//		}
//
//		f1 = (float[]) data.getColumn(0);
//		float[] f2 = (float[]) f1.clone();
//		for (int i = 0; i < f2.length; i += 1) {
//			f2[i] = 2 * f2[i];
//		}
//
//		data.setColumn(0, f2);
//		f1 = new float[]{3.14159f};
//		data.setElement(3, 0, f1);
//
//		hdu.setNullString(0, "**INVALID**");
//		data.setNull(5, 0, true);
//		data.setNull(6, 0, true);
//
//		Object[] row = new Object[5];
//		row[0] = new float[]{6.28f};
//		row[1] = new int[]{22};
//		row[2] = new long[]{0};
//		row[3] = new double[]{-3};
//		row[4] = new String[]{"A string"};
//		data.setRow(4, row);
//
//		// Rewrite (for grins rewrite header second)
//		hdu.getHeader().rewrite();
//
//		BufferedFile bf = new BufferedFile("at1.fits", "rw");
//		f.write(bf);
//
//		bf.flush();
//		bf.close();
//
//		f = new Fits("at1.fits");
//		data = (AsciiTable) f.getHDU(1).getData();
//		for (int i = 0; i < 35; i += 1) {
//			row = data.getRow(i);
//			f1 = (float[]) row[0];
//			s1 = (String[]) row[4];
//			if (i == 4) {
//				assertEquals(6.28f, f1[0], 0.0f);
//				assertEquals("A string", s1[0].trim());
//			} else {
//				if (i == 3) {
//					assertEquals(3.14159f, f1[0], 0.0f);
//				} else if (i == 5 || i == 6) {
//					try {
//						assertEquals(0, f1[0], 0);
//						fail("Expected null, get non-null");
//					} catch (NullPointerException ex) {
//					}
//				} else {
//					assertEquals(2 * realCol[i], f1[0], 0.0f);
//				}
//			}
//			f1 = (float[]) data.getElement(i, 0);
//			s1 = (String[]) data.getElement(i, 4);
//			if (i == 4) {
//				assertEquals(6.28f, f1[0], 0);
//				assertEquals("A string", s1[0].trim());
//			} else {
//				if (i == 3) {
//					assertEquals(3.14159f, f1[0], 0);
//				} else if (i == 5 || i == 6) {
//					try {
//						assertEquals(0, f1[0], 0);
//						fail("Expected null, get non-null");
//					} catch (NullPointerException ex) {
//					}
//				} else {
//					assertEquals((2 * realCol[i]), f1[0], 0);
//				}
//				assertEquals(strCol[i], s1[0].trim());
//			}
//		}
//
//		f1 = (float[]) data.getColumn(0);
//		for (int i = 0; i < 35; i += 1) {
//			if (i == 3) {
//				assertEquals(3.14159f, f1[i], 0);
//			} else if (i == 4) {
//				assertEquals(6.28f, f1[i], 0);
//			} else if (i == 5 || i == 6) {
//				assertTrue(data.isNull(i, 0));
//				assertEquals(0, f1[i], 0);
//			} else {
//				assertFalse(data.isNull(i, 0));
//				assertEquals((2 * realCol[i]), f1[i], 0);
//			}
//		}
//
//		// clean up
//		File fi = new File("at1.fits");
//		fi.delete();
//		fi = new File("at2.fits");
//		fi.delete();
//	}
//}
//
